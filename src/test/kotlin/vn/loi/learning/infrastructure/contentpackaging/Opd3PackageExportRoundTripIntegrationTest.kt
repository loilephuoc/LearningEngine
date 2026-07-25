package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.CanonicalMediaReference
import vn.loi.learning.application.contentpackaging.CanonicalMediaStatus
import vn.loi.learning.application.contentpackaging.CanonicalMediaType
import vn.loi.learning.application.contentpackaging.CanonicalTopicPackage
import vn.loi.learning.application.contentpackaging.LegacyMediaByteReader
import vn.loi.learning.application.contentpackaging.LegacyTopicSourceMetadata
import vn.loi.learning.application.contentpackaging.Opd3PackageExporter
import vn.loi.learning.application.contentpackaging.PackageMediaAssetCollector
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.application.contentpackaging.export.DefaultExportContentPackageUseCase
import vn.loi.learning.application.contentpackaging.export.ExportContentPackageCommand
import vn.loi.learning.application.contentpackaging.export.ExportContentPackageResult
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentpackaging.JvmDeterministicZipWriter

class Opd3PackageExportRoundTripIntegrationTest {

    private lateinit var tempDir: Path
    private lateinit var mediaDir: Path

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("opd3_export_roundtrip_test_")
        mediaDir = tempDir.resolve("media_storage")
        Files.createDirectories(mediaDir)
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `full end to end round trip import source package to export OPD3 to import exported OPD3 into clean repositories`() {
        // 1. Create source OPD3 archive using production Opd3PackageExporter
        val sourceZip = tempDir.resolve("SourceTopic.opd3")
        val contentId = ContentId("cnt-SourceTopic-1")

        val canonicalPkg = CanonicalTopicPackage(
            topicId = TopicId.deriveForLegacyPackage("SourceTopic", "OPD3"),
            logicalTopicName = "SourceTopic",
            sourceMetadata = LegacyTopicSourceMetadata("SourceTopic", "SourceTopic.json", "SourceTopic.pkg", "OPD3", "1.0.0"),
            contents = listOf(
                Content(
                    id = contentId,
                    type = ContentType.WORD,
                    text = ContentText(primaryText = "Sun in SourceTopic", translatedText = "Mat troi"),
                    media = ContentMedia(primaryAudio = "sun.mp3", image = "sun.png")
                )
            ),
            learningItems = listOf(
                LearningItem(
                    id = LearningItemId("cnt-SourceTopic-1-rec"),
                    contentId = contentId,
                    mode = LearningMode.MEANING_RECOGNITION
                )
            ),
            mediaReferences = listOf(
                CanonicalMediaReference("sun.mp3", "sun.mp3", CanonicalMediaType.AUDIO, contentId, status = CanonicalMediaStatus.PRESENT),
                CanonicalMediaReference("sun.png", "sun.png", CanonicalMediaType.IMAGE, contentId, status = CanonicalMediaStatus.PRESENT)
            )
        )

        val mediaCollector = PackageMediaAssetCollector(
            mediaByteReader = { _, assetPath ->
                when (assetPath) {
                    "sun.mp3" -> "audio-sample-data".toByteArray(Charsets.UTF_8)
                    "sun.png" -> "image-sample-data".toByteArray(Charsets.UTF_8)
                    else -> null
                }
            }
        )
        val mediaBundle = mediaCollector.collect(canonicalPkg)

        val exporter = Opd3PackageExporter(zipWriter = JvmDeterministicZipWriter())
        val sourceExportResult = exporter.export(canonicalPkg, mediaBundle)
        Files.write(sourceZip, sourceExportResult.zipBytes)

        // Write sample media files to media directory
        Files.writeString(mediaDir.resolve("sun.mp3"), "audio-sample-data")
        Files.writeString(mediaDir.resolve("sun.png"), "image-sample-data")

        // 2. Initialize origin application context & import source package
        val originAppContext = LearningApplicationFactory.createInMemory()
        val originImporter = originAppContext.packageImporter(sourceZip)
        val importResult = originImporter.importCandidate(
            PackageCatalogId("test-catalog"),
            PackageScanCandidate(sourceZip.toString())
        )

        assertEquals(1, importResult.importedContentCount)

        val originConflictImporter = originAppContext.conflictAwareImporter!!
        val defaultLibId = originAppContext.defaultLibraryId!!
        val decision = originConflictImporter.inspectCandidate(
            candidatePackageId = importResult.contentPackage.id,
            candidateTopicId = importResult.contentPackage.topicId,
            candidateName = importResult.contentPackage.name,
            candidateVersion = importResult.contentPackage.version,
            libraryId = defaultLibId
        )
        val outcome = originConflictImporter.executeImport(
            decision = decision,
            libraryId = defaultLibId,
            contentCount = importResult.importedContentCount,
            learningItemCount = importResult.importedLearningItemCount
        )

        val installedPkgs = originAppContext.installedPackageRepository!!.findAll()
        assertEquals(1, installedPkgs.size)
        val instPkg = installedPkgs.first()
        assertEquals("SourceTopic", instPkg.name.value)

        // 3. Export package to .opd3 file using DefaultExportContentPackageUseCase
        val exportedOpd3File = tempDir.resolve("ExportedSourceTopic.opd3")
        val mediaByteReader = LegacyMediaByteReader { _, assetPath ->
            val cleanPath = assetPath.removePrefix("media/")
            val f = mediaDir.resolve(cleanPath)
            if (Files.exists(f)) Files.readAllBytes(f) else null
        }

        val exportUseCase = DefaultExportContentPackageUseCase(
            installedPackageRepository = originAppContext.installedPackageRepository!!,
            contentPackageRepository = originAppContext.contentPackageRepository!!,
            contentLibraryRepository = originAppContext.contentLibraryRepository!!,
            contentRepository = originAppContext.contentRepository!!,
            learningItemRepository = originAppContext.learningItemRepository!!,
            opd3PackageExporter = Opd3PackageExporter(zipWriter = JvmDeterministicZipWriter()),
            mediaByteReader = mediaByteReader
        )

        val exportResult = exportUseCase.execute(
            ExportContentPackageCommand(
                installedPackageId = instPkg.id,
                destinationPath = exportedOpd3File
            )
        )

        assertIs<ExportContentPackageResult.Success>(exportResult)
        assertTrue(Files.exists(exportedOpd3File))
        assertTrue(Files.size(exportedOpd3File) > 0)
        assertEquals(1, exportResult.contentCount)
        assertEquals(1, exportResult.learningItemCount)
        assertEquals(2, exportResult.mediaAssetCount)

        // 4. Initialize CLEAN application context & import exported .opd3 archive using PRODUCTION importer
        val cleanAppContext = LearningApplicationFactory.createInMemory()
        val cleanImporter = cleanAppContext.packageImporter(exportedOpd3File)
        val cleanImportResult = cleanImporter.importCandidate(
            PackageCatalogId("clean-catalog"),
            PackageScanCandidate(exportedOpd3File.toString())
        )

        assertEquals(1, cleanImportResult.importedContentCount)

        val cleanConflictImporter = cleanAppContext.conflictAwareImporter!!
        val cleanDefaultLibId = cleanAppContext.defaultLibraryId!!
        val cleanDecision = cleanConflictImporter.inspectCandidate(
            candidatePackageId = cleanImportResult.contentPackage.id,
            candidateTopicId = cleanImportResult.contentPackage.topicId,
            candidateName = cleanImportResult.contentPackage.name,
            candidateVersion = cleanImportResult.contentPackage.version,
            libraryId = cleanDefaultLibId
        )
        cleanConflictImporter.executeImport(
            decision = cleanDecision,
            libraryId = cleanDefaultLibId,
            contentCount = cleanImportResult.importedContentCount,
            learningItemCount = cleanImportResult.importedLearningItemCount
        )

        val cleanInstalledPkgs = cleanAppContext.installedPackageRepository!!.findAll()
        assertEquals(1, cleanInstalledPkgs.size)

        val cleanInstPkg = cleanInstalledPkgs.first()
        assertEquals("SourceTopic", cleanInstPkg.name.value)
        assertEquals(instPkg.version.value, cleanInstPkg.version.value)

        // Verify logical equivalence of contents & learning items
        val cleanContents = cleanAppContext.contentRepository!!.findAll()
        assertEquals(1, cleanContents.size)
        val content = cleanContents.first()
        assertEquals("Sun in SourceTopic", content.text.primaryText)
        assertEquals("sun.mp3", content.media.primaryAudio)
        assertEquals("sun.png", content.media.image)

        val cleanLearningItems = cleanAppContext.learningItemRepository!!.findAllEnabled()
        assertEquals(1, cleanLearningItems.size)
        assertEquals(content.id, cleanLearningItems.first().contentId)
    }

    @Test
    fun `large package round trip exceeding 4096 entries exports successfully and imports into clean context`() {
        val entryCountTarget = 4500
        val sourceZip = tempDir.resolve("LargeTopic.opd3")
        val contentList = mutableListOf<Content>()
        val learningItemList = mutableListOf<LearningItem>()
        val mediaRefList = mutableListOf<CanonicalMediaReference>()
        val mediaByteStorage = mutableMapOf<String, ByteArray>()

        for (i in 1..entryCountTarget) {
            val contentId = ContentId("cnt-large-$i")
            val mediaPath = "audio_$i.mp3"
            contentList.add(
                Content(
                    id = contentId,
                    type = ContentType.SENTENCE,
                    text = ContentText(primaryText = "Sentence $i", translatedText = "Cau $i"),
                    media = ContentMedia(primaryAudio = mediaPath)
                )
            )
            learningItemList.add(
                LearningItem(
                    id = LearningItemId("cnt-large-$i-rec"),
                    contentId = contentId,
                    mode = LearningMode.MEANING_RECOGNITION
                )
            )
            mediaRefList.add(
                CanonicalMediaReference(
                    logicalPath = mediaPath,
                    referencedAsset = mediaPath,
                    mediaType = CanonicalMediaType.AUDIO,
                    owningContentId = contentId,
                    status = CanonicalMediaStatus.PRESENT
                )
            )
            mediaByteStorage[mediaPath] = "audio-payload-$i".toByteArray(Charsets.UTF_8)
            Files.writeString(mediaDir.resolve(mediaPath), "audio-payload-$i")
        }

        val canonicalPkg = CanonicalTopicPackage(
            topicId = TopicId.deriveForLegacyPackage("LargeTopic", "OPD3"),
            logicalTopicName = "LargeTopic",
            sourceMetadata = LegacyTopicSourceMetadata("LargeTopic", "LargeTopic.json", "LargeTopic.pkg", "OPD3", "1.0.0"),
            contents = contentList,
            learningItems = learningItemList,
            mediaReferences = mediaRefList
        )

        val mediaCollector = PackageMediaAssetCollector(
            mediaByteReader = { _, assetPath -> mediaByteStorage[assetPath] }
        )
        val mediaBundle = mediaCollector.collect(canonicalPkg)
        assertEquals(entryCountTarget, mediaBundle.assets.size)

        val exporter = Opd3PackageExporter(zipWriter = JvmDeterministicZipWriter())
        val sourceExportResult = exporter.export(canonicalPkg, mediaBundle)
        Files.write(sourceZip, sourceExportResult.zipBytes)

        // 1. Verify export success and zip contains >4096 entries
        val zipFile = java.util.zip.ZipFile(sourceZip.toFile())
        val actualZipEntries = zipFile.size()
        zipFile.close()
        assertTrue(actualZipEntries > 4096, "Exported zip should have >4096 entries, actual: $actualZipEntries")
        assertEquals(entryCountTarget + 5, actualZipEntries)

        // 2. Import into clean app context
        val cleanAppContext = LearningApplicationFactory.createInMemory()
        val cleanImporter = cleanAppContext.packageImporter(sourceZip)
        val cleanImportResult = cleanImporter.importCandidate(
            PackageCatalogId("large-catalog"),
            PackageScanCandidate(sourceZip.toString())
        )

        assertEquals(entryCountTarget, cleanImportResult.importedContentCount)
        assertEquals(entryCountTarget, cleanImportResult.importedLearningItemCount)

        val cleanConflictImporter = cleanAppContext.conflictAwareImporter!!
        val cleanDefaultLibId = cleanAppContext.defaultLibraryId!!
        val cleanDecision = cleanConflictImporter.inspectCandidate(
            candidatePackageId = cleanImportResult.contentPackage.id,
            candidateTopicId = cleanImportResult.contentPackage.topicId,
            candidateName = cleanImportResult.contentPackage.name,
            candidateVersion = cleanImportResult.contentPackage.version,
            libraryId = cleanDefaultLibId
        )
        cleanConflictImporter.executeImport(
            decision = cleanDecision,
            libraryId = cleanDefaultLibId,
            contentCount = cleanImportResult.importedContentCount,
            learningItemCount = cleanImportResult.importedLearningItemCount
        )

        val cleanInstalledPkgs = cleanAppContext.installedPackageRepository!!.findAll()
        assertEquals(1, cleanInstalledPkgs.size)
        assertEquals("LargeTopic", cleanInstalledPkgs.first().name.value)
        assertEquals(entryCountTarget, cleanAppContext.contentRepository!!.findAll().size)
    }

    @Test
    fun `duplicate media references across multiple contents are deduplicated to single canonical archive entry`() {
        val sharedAudioPath = "shared_audio.mp3"
        val content1Id = ContentId("cnt-dedup-1")
        val content2Id = ContentId("cnt-dedup-2")

        val canonicalPkg = CanonicalTopicPackage(
            topicId = TopicId.deriveForLegacyPackage("DedupTopic", "OPD3"),
            logicalTopicName = "DedupTopic",
            sourceMetadata = LegacyTopicSourceMetadata("DedupTopic", "DedupTopic.json", "DedupTopic.pkg", "OPD3", "1.0.0"),
            contents = listOf(
                Content(id = content1Id, type = ContentType.WORD, text = ContentText("Word 1", "Tu 1"), media = ContentMedia(primaryAudio = sharedAudioPath)),
                Content(id = content2Id, type = ContentType.WORD, text = ContentText("Word 2", "Tu 2"), media = ContentMedia(primaryAudio = sharedAudioPath))
            ),
            learningItems = listOf(
                LearningItem(id = LearningItemId("cnt-dedup-1-rec"), contentId = content1Id, mode = LearningMode.MEANING_RECOGNITION),
                LearningItem(id = LearningItemId("cnt-dedup-2-rec"), contentId = content2Id, mode = LearningMode.MEANING_RECOGNITION)
            ),
            mediaReferences = listOf(
                CanonicalMediaReference(sharedAudioPath, sharedAudioPath, CanonicalMediaType.AUDIO, content1Id, status = CanonicalMediaStatus.PRESENT),
                CanonicalMediaReference(sharedAudioPath, sharedAudioPath, CanonicalMediaType.AUDIO, content2Id, status = CanonicalMediaStatus.PRESENT)
            )
        )

        val mediaCollector = PackageMediaAssetCollector(
            mediaByteReader = { _, _ -> "shared-audio-bytes".toByteArray(Charsets.UTF_8) }
        )
        val mediaBundle = mediaCollector.collect(canonicalPkg)

        // Assert media collector deduplicated the shared asset to exactly 1 entry
        assertEquals(1, mediaBundle.assets.size)
        assertEquals(sharedAudioPath, mediaBundle.assets.first().logicalPath)
        assertEquals(2, mediaBundle.manifest.entries.first().owningContentIds.size)
    }

    @Test
    fun `absurd entry count exceeding 50000 limit is safely rejected`() {
        val validator = Opd3ArchiveStructureValidator(Opd3ArchiveStructureLimits(maximumEntryCount = 50_000))
        val mockZipPath = tempDir.resolve("absurd.zip")

        java.util.zip.ZipOutputStream(Files.newOutputStream(mockZipPath)).use { zip ->
            for (i in 1..50_001) {
                zip.putNextEntry(java.util.zip.ZipEntry("file_$i.json"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
        }

        java.util.zip.ZipFile(mockZipPath.toFile()).use { zipFile ->
            kotlin.test.assertFailsWith<vn.loi.learning.application.contentpackaging.PackageArchiveEntryCountExceededException> {
                validator.validate(zipFile)
            }
        }
    }
}
