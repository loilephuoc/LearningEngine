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
}
