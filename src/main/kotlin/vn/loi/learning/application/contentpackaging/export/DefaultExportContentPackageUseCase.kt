package vn.loi.learning.application.contentpackaging.export

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import vn.loi.learning.application.contentpackaging.CanonicalMediaReference
import vn.loi.learning.application.contentpackaging.CanonicalMediaStatus
import vn.loi.learning.application.contentpackaging.CanonicalMediaType
import vn.loi.learning.application.contentpackaging.CanonicalTopicPackage
import vn.loi.learning.application.contentpackaging.LegacyMediaByteReader
import vn.loi.learning.application.contentpackaging.LegacyTopicSourceMetadata
import vn.loi.learning.application.contentpackaging.Opd3PackageExporter
import vn.loi.learning.application.contentpackaging.Opd3PackageVerifier
import vn.loi.learning.application.contentpackaging.PackageMediaAssetCollector
import vn.loi.learning.application.contentmedia.MediaReferencePolicy
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

class DefaultExportContentPackageUseCase(
    private val installedPackageRepository: InstalledPackageRepository,
    private val contentPackageRepository: ContentPackageRepository,
    private val contentLibraryRepository: ContentLibraryRepository,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val opd3PackageExporter: Opd3PackageExporter,
    private val mediaByteReader: LegacyMediaByteReader = LegacyMediaByteReader { _, _ -> null },
    private val mediaDirectory: Path? = null,
    private val opd3PackageVerifier: Opd3PackageVerifier = Opd3PackageVerifier(),
    private val contentMediaStorage: ContentMediaStorage? = null
) : ExportContentPackageUseCase {

    override fun execute(command: ExportContentPackageCommand): ExportContentPackageResult {
        val destPath = command.destinationPath
        val progress = command.progressListener ?: PackageExportProgressListener { _, _, _, _ -> }

        // 1. Destination validation & overwrite policy
        if (Files.exists(destPath) && !command.overwrite) {
            return ExportContentPackageResult.Failure.DestinationExists(destPath.toString())
        }

        try {
            val parent = destPath.parent
            if (parent != null) {
                Files.createDirectories(parent)
            }
        } catch (e: Exception) {
            return ExportContentPackageResult.Failure.InvalidDestination(destPath.toString())
        }

        // 2. Lifecycle Authority Check (InstalledPackageRepository)
        progress.onProgress(ExportProgressStage.RESOLVING_PACKAGE, "Resolving package authority...", 0, 100)
        val instPkg = installedPackageRepository.findById(command.installedPackageId)
            ?: return ExportContentPackageResult.Failure.PackageNotFound(command.installedPackageId.value)

        if (instPkg.state != PackageState.ACTIVE && instPkg.state != PackageState.ARCHIVED) {
            return ExportContentPackageResult.Failure.PackageNotExportable(
                installedPackageId = instPkg.id.value,
                state = instPkg.state.name
            )
        }

        // 3. Resolve Ownership Graph
        progress.onProgress(ExportProgressStage.COLLECTING_CONTENT, "Collecting package contents...", 5, 100)
        val canonicalPkgKeys = setOf(instPkg.packageId.value, instPkg.id.value, instPkg.name.value)
        val contentPackages = contentPackageRepository.findAll().filter { cp ->
            cp.id.value in canonicalPkgKeys || canonicalPkgKeys.any { key -> cp.id.value.contains(key) }
        }

        val targetLibraryIds = contentPackages.flatMap { it.libraryIds }.mapTo(HashSet()) { it.value }
        canonicalPkgKeys.forEach { targetLibraryIds.add(it) }

        val targetLibraries = contentLibraryRepository.findAll().filter { lib ->
            lib.id.value in targetLibraryIds || targetLibraryIds.any { key -> lib.id.value.contains(key) }
        }

        val targetContentIds = targetLibraries.flatMap { it.contentIds }.mapTo(HashSet()) { it }

        // Fallback: If no library mapped, resolve contents by prefix
        val allContents = contentRepository.findAll()
        val packageContents = if (targetContentIds.isNotEmpty()) {
            allContents.filter { it.id in targetContentIds }
        } else {
            allContents.filter { content ->
                canonicalPkgKeys.any { key -> content.id.value.startsWith(key) || content.id.value.contains("-$key-") }
            }
        }

        val exportedContentIds = packageContents.mapTo(HashSet()) { it.id }
        val allLearningItems = learningItemRepository.findAllEnabled()
        val packageLearningItems = allLearningItems.filter { it.contentId in exportedContentIds }

        // 4. Collect Media References & Assets
        val storage = contentMediaStorage
        val mediaReferences = mutableListOf<CanonicalMediaReference>()
        val effectiveMediaByteReader = LegacyMediaByteReader { source, assetPath ->
            // Try custom reader first
            val bytes = mediaByteReader.readAssetBytes(source, assetPath)
            if (bytes != null) return@LegacyMediaByteReader bytes

            // Try reading from ContentMediaStorage authority
            if (storage != null) {
                val cleanPath = assetPath.removePrefix("media/")
                val resolved = storage.resolve(cleanPath) ?: storage.resolve(assetPath)
                if (resolved != null && Files.isRegularFile(resolved)) {
                    return@LegacyMediaByteReader Files.readAllBytes(resolved)
                }
            } else if (mediaDirectory != null) {
                val cleanPath = assetPath.removePrefix("media/")
                val candidateFile = mediaDirectory.resolve(cleanPath)
                if (Files.exists(candidateFile) && Files.isRegularFile(candidateFile)) {
                    return@LegacyMediaByteReader Files.readAllBytes(candidateFile)
                }
            }
            null
        }

        fun canonicalizeExportRef(ref: String?): Pair<String?, String?> {
            if (ref.isNullOrBlank()) return Pair(null, null)
            if (MediaReferencePolicy.isNoImageSentinel(ref)) return Pair(null, null)
            val cleanPath = ref.trim().replace('\\', '/').removePrefix("media/").removePrefix("/media/")
            val resolvedPath = storage?.resolve(cleanPath) ?: storage?.resolve(ref)
            if (resolvedPath != null && Files.isRegularFile(resolvedPath)) {
                val resolvedFileName = resolvedPath.fileName.toString()
                val refFileName = cleanPath.substringAfterLast('/')
                val canonicalCleanPath = if (!refFileName.equals(resolvedFileName, ignoreCase = true)) {
                    val parent = cleanPath.substringBeforeLast('/', missingDelimiterValue = "")
                    if (parent.isNotEmpty()) "$parent/$resolvedFileName" else resolvedFileName
                } else {
                    cleanPath
                }
                return Pair(canonicalCleanPath, canonicalCleanPath)
            }
            return Pair(cleanPath, cleanPath)
        }

        data class PendingMediaAsset(
            val owningContent: vn.loi.learning.domain.content.model.Content,
            val assetPath: String,
            val logicalPath: String,
            val mediaType: CanonicalMediaType
        )

        val exportedContents = mutableListOf<vn.loi.learning.domain.content.model.Content>()
        val pendingMediaAssets = mutableListOf<PendingMediaAsset>()

        packageContents.forEach { content ->
            var currentMedia = content.media
            if (MediaReferencePolicy.isNoImageSentinel(currentMedia.image)) {
                currentMedia = currentMedia.copy(image = null)
            }

            val (canonicalImg, imgLogical) = canonicalizeExportRef(currentMedia.image)
            val (canonicalPrimaryAudio, primaryAudioLogical) = canonicalizeExportRef(currentMedia.primaryAudio)
            val (canonicalTranslatedAudio, translatedAudioLogical) = canonicalizeExportRef(currentMedia.translatedAudio)
            val (canonicalExampleAudio, exampleAudioLogical) = canonicalizeExportRef(currentMedia.exampleAudio)
            val (canonicalExampleTranslatedAudio, exampleTranslatedAudioLogical) = canonicalizeExportRef(currentMedia.exampleTranslatedAudio)

            val updatedMedia = currentMedia.copy(
                image = canonicalImg,
                primaryAudio = canonicalPrimaryAudio,
                translatedAudio = canonicalTranslatedAudio,
                exampleAudio = canonicalExampleAudio,
                exampleTranslatedAudio = canonicalExampleTranslatedAudio
            )
            val updatedContent = content.copy(media = updatedMedia)
            exportedContents.add(updatedContent)

            val assets = listOfNotNull(
                updatedMedia.primaryAudio?.let { Triple(it, primaryAudioLogical ?: it, CanonicalMediaType.AUDIO) },
                updatedMedia.translatedAudio?.let { Triple(it, translatedAudioLogical ?: it, CanonicalMediaType.AUDIO) },
                updatedMedia.image?.let { Triple(it, imgLogical ?: it, CanonicalMediaType.IMAGE) },
                updatedMedia.exampleAudio?.let { Triple(it, exampleAudioLogical ?: it, CanonicalMediaType.AUDIO) },
                updatedMedia.exampleTranslatedAudio?.let { Triple(it, exampleTranslatedAudioLogical ?: it, CanonicalMediaType.AUDIO) }
            )

            assets.forEach { (assetPath, logicalPath, mediaType) ->
                pendingMediaAssets.add(
                    PendingMediaAsset(
                        owningContent = updatedContent,
                        assetPath = assetPath,
                        logicalPath = logicalPath,
                        mediaType = mediaType
                    )
                )
            }
        }

        val totalMedia = pendingMediaAssets.size
        if (totalMedia == 0) {
            progress.onProgress(ExportProgressStage.COLLECTING_MEDIA, "Scanning media assets...", 75, 100)
        } else {
            progress.onProgress(ExportProgressStage.COLLECTING_MEDIA, "Exporting media 0 / $totalMedia...", 10, 100)
        }

        pendingMediaAssets.forEachIndexed { index, pending ->
            val cleanPath = pending.logicalPath.removePrefix("media/")
            val bytes = effectiveMediaByteReader.readAssetBytes(instPkg.name.value, cleanPath)
                ?: effectiveMediaByteReader.readAssetBytes(instPkg.name.value, pending.assetPath)

            if (bytes == null) {
                // Strict missing media rejection (AC-07)
                return ExportContentPackageResult.Failure.MissingMedia(
                    assetPath = pending.assetPath,
                    contentId = pending.owningContent.id.value
                )
            }

            mediaReferences.add(
                CanonicalMediaReference(
                    referencedAsset = pending.assetPath,
                    logicalPath = cleanPath,
                    mediaType = pending.mediaType,
                    owningContentId = pending.owningContent.id,
                    status = CanonicalMediaStatus.PRESENT
                )
            )

            val processed = index + 1
            val fraction = processed.toFloat() / totalMedia.toFloat()
            val mediaPercent = (10 + (fraction * 65)).toInt().coerceIn(10, 75)
            progress.onProgress(
                ExportProgressStage.COLLECTING_MEDIA,
                "Exporting media $processed / $totalMedia...",
                mediaPercent,
                100
            )
        }

        val pkgFormat = contentPackages.firstOrNull()?.format ?: "OPD3"
        val pkgVersion = instPkg.version.value
        val topicId = instPkg.topicId
        val canonicalPackage = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = instPkg.name.value,
            sourceMetadata = LegacyTopicSourceMetadata(
                logicalTopicName = instPkg.name.value,
                jsonSource = "${instPkg.name.value}.json",
                packageSource = "${instPkg.name.value}.pkg",
                format = pkgFormat,
                version = pkgVersion
            ),
            contents = exportedContents,
            learningItems = packageLearningItems,
            mediaReferences = mediaReferences
        )

        val mediaCollector = PackageMediaAssetCollector(mediaByteReader = effectiveMediaByteReader)
        val mediaBundle = mediaCollector.collect(canonicalPackage, customByteReader = effectiveMediaByteReader)

        // 5. OPD3 Export & Atomicity
        progress.onProgress(ExportProgressStage.WRITING_METADATA, "Packaging metadata...", 80, 100)
        progress.onProgress(ExportProgressStage.WRITING_CONTENT, "Packaging content...", 82, 100)
        progress.onProgress(ExportProgressStage.WRITING_MEDIA, "Writing OPD3 archive...", 85, 100)

        var tempFile: Path? = null
        try {
            val exportResult = opd3PackageExporter.export(canonicalPackage, mediaBundle)

            tempFile = Files.createTempFile(
                destPath.parent ?: Path.of("."),
                "opd3_export_${System.currentTimeMillis()}_",
                ".tmp"
            )

            Files.write(tempFile, exportResult.zipBytes)

            // 6. Validate Produced Artifact
            progress.onProgress(ExportProgressStage.VALIDATING_PACKAGE, "Validating OPD3 package archive...", 92, 100)
            val verificationReport = opd3PackageVerifier.verify(exportResult.zipBytes)
            if (!verificationReport.isValid) {
                cleanupQuietly(tempFile)
                return ExportContentPackageResult.Failure.ValidationFailed(
                    details = verificationReport.errors.joinToString("; ")
                )
            }

            // 7. Atomic Move/Replace
            progress.onProgress(ExportProgressStage.FINALIZING, "Finalizing package output...", 98, 100)
            try {
                Files.move(tempFile, destPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (atomicEx: Exception) {
                // Fallback to replace existing if cross-drive atomic move is unsupported
                Files.move(tempFile, destPath, StandardCopyOption.REPLACE_EXISTING)
            }

            progress.onProgress(ExportProgressStage.COMPLETED, "OPD3 package export complete.", 100, 100)
            return ExportContentPackageResult.Success(
                outputPath = destPath,
                sha256Checksum = exportResult.sha256Checksum,
                contentCount = packageContents.size,
                learningItemCount = packageLearningItems.size,
                mediaAssetCount = mediaBundle.assets.size
            )
        } catch (e: Exception) {
            cleanupQuietly(tempFile)
            return ExportContentPackageResult.Failure.IOFailure(e)
        }
    }

    private fun cleanupQuietly(file: Path?) {
        if (file != null) {
            try {
                Files.deleteIfExists(file)
            } catch (_: Exception) {
            }
        }
    }
}
