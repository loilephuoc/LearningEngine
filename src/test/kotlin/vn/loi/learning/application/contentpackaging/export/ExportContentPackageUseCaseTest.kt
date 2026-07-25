package vn.loi.learning.application.contentpackaging.export

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.LegacyMediaByteReader
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3ArchiveReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3EntryReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3PackageDescriptorReader
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

class ExportContentPackageUseCaseTest {

    private lateinit var tempDir: Path
    private lateinit var instPkgRepo: InMemoryInstalledPackageRepository
    private lateinit var contentPkgRepo: InMemoryContentPackageRepository
    private lateinit var contentLibRepo: InMemoryContentLibraryRepository
    private lateinit var contentRepo: InMemoryContentRepository
    private lateinit var learningItemRepo: InMemoryLearningItemRepository

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("export_use_case_test_")
        instPkgRepo = InMemoryInstalledPackageRepository()
        contentPkgRepo = InMemoryContentPackageRepository()
        contentLibRepo = InMemoryContentLibraryRepository()
        contentRepo = InMemoryContentRepository()
        learningItemRepo = InMemoryLearningItemRepository()
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `export ACTIVE package successfully creates valid opd3 file`() {
        val (instPkg, destFile) = setupActivePackageWithContent("PackageActive", PackageState.ACTIVE)

        val mediaByteReader = LegacyMediaByteReader { _, asset ->
            if (asset.contains("sun.mp3")) "mp3-bytes".toByteArray(Charsets.UTF_8) else null
        }

        val useCase = createUseCase(mediaByteReader = mediaByteReader)

        val result = useCase.execute(
            ExportContentPackageCommand(
                installedPackageId = instPkg.id,
                destinationPath = destFile
            )
        )

        assertIs<ExportContentPackageResult.Success>(result)
        assertEquals(destFile, result.outputPath)
        assertTrue(Files.exists(destFile))
        assertTrue(Files.size(destFile) > 0)
        assertEquals(1, result.contentCount)
        assertEquals(1, result.learningItemCount)
        assertEquals(1, result.mediaAssetCount)
    }

    @Test
    fun `export ARCHIVED package successfully creates valid opd3 file`() {
        val (instPkg, destFile) = setupActivePackageWithContent("PackageArchived", PackageState.ARCHIVED)

        val mediaByteReader = LegacyMediaByteReader { _, asset ->
            if (asset.contains("sun.mp3")) "mp3-bytes".toByteArray(Charsets.UTF_8) else null
        }

        val useCase = createUseCase(mediaByteReader = mediaByteReader)

        val result = useCase.execute(
            ExportContentPackageCommand(
                installedPackageId = instPkg.id,
                destinationPath = destFile
            )
        )

        assertIs<ExportContentPackageResult.Success>(result)
        assertTrue(Files.exists(destFile))
    }

    @Test
    fun `reject REMOVED or non-existent installed package`() {
        val missingId = InstalledPackageId("missing-pkg")
        val destFile = tempDir.resolve("missing.opd3")

        val useCase = createUseCase()

        val result = useCase.execute(
            ExportContentPackageCommand(
                installedPackageId = missingId,
                destinationPath = destFile
            )
        )

        assertIs<ExportContentPackageResult.Failure.PackageNotFound>(result)
        assertFalse(Files.exists(destFile))
    }

    @Test
    fun `reject legacy orphan package where installed package is absent`() {
        val pkgId = PackageId("orphan-pkg")
        contentPkgRepo.save(
            ContentPackage(
                id = pkgId,
                descriptor = PackageDescriptor(name = "Orphan Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(ContentLibraryId("lib-orphan"))
            )
        )

        val destFile = tempDir.resolve("orphan.opd3")
        val useCase = createUseCase()

        val result = useCase.execute(
            ExportContentPackageCommand(
                installedPackageId = InstalledPackageId("orphan-pkg"),
                destinationPath = destFile
            )
        )

        assertIs<ExportContentPackageResult.Failure.PackageNotFound>(result)
        assertFalse(Files.exists(destFile))
    }

    @Test
    fun `reject export when referenced media asset is missing`() {
        val (instPkg, destFile) = setupActivePackageWithContent("PackageMissingMedia", PackageState.ACTIVE)

        val mediaByteReader = LegacyMediaByteReader { _, _ -> null }

        val useCase = createUseCase(mediaByteReader = mediaByteReader)

        val result = useCase.execute(
            ExportContentPackageCommand(
                installedPackageId = instPkg.id,
                destinationPath = destFile
            )
        )

        assertIs<ExportContentPackageResult.Failure.MissingMedia>(result)
        assertEquals("sun.mp3", result.assetPath)
        assertFalse(Files.exists(destFile))
    }

    @Test
    fun `reject existing destination file when overwrite is false`() {
        val (instPkg, destFile) = setupActivePackageWithContent("PackageOverwrite", PackageState.ACTIVE)
        Files.writeString(destFile, "pre-existing content")

        val useCase = createUseCase()

        val result = useCase.execute(
            ExportContentPackageCommand(
                installedPackageId = instPkg.id,
                destinationPath = destFile,
                overwrite = false
            )
        )

        assertIs<ExportContentPackageResult.Failure.DestinationExists>(result)
        assertEquals("pre-existing content", Files.readString(destFile))
    }

    @Test
    fun `cross package isolation ensures export only includes target package content`() {
        val (instPkgA, destFileA) = setupActivePackageWithContent("PackageA", PackageState.ACTIVE)
        setupActivePackageWithContent("PackageB", PackageState.ACTIVE)

        val mediaByteReader = LegacyMediaByteReader { _, _ -> "bytes".toByteArray() }

        val useCase = createUseCase(mediaByteReader = mediaByteReader)

        val result = useCase.execute(
            ExportContentPackageCommand(
                installedPackageId = instPkgA.id,
                destinationPath = destFileA
            )
        )

        assertIs<ExportContentPackageResult.Success>(result)
        assertEquals(1, result.contentCount)

        val descriptorReader = JvmOpd3PackageDescriptorReader(
            archiveReader = JvmOpd3ArchiveReader(),
            entryReader = JvmOpd3EntryReader()
        )
        val candidate = vn.loi.learning.application.contentpackaging.PackageScanCandidate(destFileA.toString())
        val descriptor = descriptorReader.read(candidate)

        assertEquals("PackageA", descriptor.name)
    }

    private fun setupActivePackageWithContent(name: String, state: PackageState): Pair<InstalledPackage, Path> {
        val pkgId = PackageId(name)
        val instPkgId = InstalledPackageId(name)
        val libId = ContentLibraryId("lib-$name")
        val contentId = ContentId("cnt-$name-1")

        val instPkg = InstalledPackage.reconstitute(
            id = instPkgId,
            libraryId = LibraryId("default-library"),
            packageId = pkgId,
            topicId = TopicId.deriveForLegacyPackage(name, "OPD3"),
            name = PackageName(name),
            version = PackageVersion("1.0.0"),
            state = state,
            installedAt = Instant.now(),
            contentCount = 1,
            learningItemCount = 1
        )
        instPkgRepo.save(instPkg)

        contentPkgRepo.save(
            ContentPackage(
                id = pkgId,
                descriptor = PackageDescriptor(name = name, version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )

        contentLibRepo.save(
            ContentLibrary(
                id = libId,
                descriptor = LibraryDescriptor(name = "$name Library"),
                contentIds = setOf(contentId)
            )
        )

        contentRepo.save(
            Content(
                id = contentId,
                type = ContentType.WORD,
                text = ContentText(primaryText = "Sun in $name", translatedText = "Mat troi"),
                media = ContentMedia(primaryAudio = "sun.mp3")
            )
        )

        learningItemRepo.save(
            LearningItem(
                id = LearningItemId("${contentId.value}-rec"),
                contentId = contentId,
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        return instPkg to tempDir.resolve("$name.opd3")
    }

    private fun createUseCase(
        mediaByteReader: LegacyMediaByteReader = LegacyMediaByteReader { _, _ -> null }
    ): DefaultExportContentPackageUseCase =
        DefaultExportContentPackageUseCase(
            installedPackageRepository = instPkgRepo,
            contentPackageRepository = contentPkgRepo,
            contentLibraryRepository = contentLibRepo,
            contentRepository = contentRepo,
            learningItemRepository = learningItemRepo,
            opd3PackageExporter = vn.loi.learning.application.contentpackaging.Opd3PackageExporter(
                zipWriter = vn.loi.learning.infrastructure.contentpackaging.JvmDeterministicZipWriter()
            ),
            mediaByteReader = mediaByteReader
        )
}
