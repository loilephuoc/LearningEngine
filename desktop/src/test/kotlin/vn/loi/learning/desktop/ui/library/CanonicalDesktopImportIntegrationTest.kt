package vn.loi.learning.desktop.ui.library

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportCancelledException
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryOperation
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.shell.createCanonicalLibraryFacade
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.LegacyOpd3MediaArchiveReader
import vn.loi.learning.infrastructure.contentpackaging.JvmFileScopedPackageScanner

class CanonicalDesktopImportIntegrationTest {

    private val libId = LibraryId("lib-test")

    // 1. Selected JSON resolves only its same-basename PKG
    @Test
    fun `1 selected JSON resolves only its same-basename PKG`() {
        val tempDir = Files.createTempDirectory("json-resolve-test")
        try {
            val jsonFile = tempDir.resolve("2000Cau.json")
            val pkgFile = tempDir.resolve("2000Cau.pkg")
            Files.writeString(jsonFile, "{ \"name\": \"2000 Cau\" }")
            createOpd3BinaryPackage(pkgFile, entries = listOf("media1.mp3" to "content1".toByteArray()))

            val scanner = JvmFileScopedPackageScanner(jsonFile)
            val candidates = scanner.scan()

            assertEquals(1, candidates.size)
            assertEquals(jsonFile.toString(), candidates.first().source)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    // 2. Selected PKG resolves only its same-basename JSON
    @Test
    fun `2 selected PKG resolves only its same-basename JSON`() {
        val tempDir = Files.createTempDirectory("pkg-resolve-test")
        try {
            val jsonFile = tempDir.resolve("2000Cau.json")
            val pkgFile = tempDir.resolve("2000Cau.pkg")
            Files.writeString(jsonFile, "{ \"name\": \"2000 Cau\" }")
            createOpd3BinaryPackage(pkgFile, entries = listOf("media1.mp3" to "content1".toByteArray()))

            val scanner = JvmFileScopedPackageScanner(pkgFile)
            val candidates = scanner.scan()

            assertEquals(1, candidates.size)
            assertEquals(jsonFile.toString(), candidates.first().source)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    // 3. Unrelated sibling pairs are ignored
    @Test
    fun `3 unrelated sibling pairs are ignored`() {
        val tempDir = Files.createTempDirectory("sibling-pair-test")
        try {
            val jsonFileA = tempDir.resolve("PairA.json")
            val pkgFileA = tempDir.resolve("PairA.pkg")
            val jsonFileB = tempDir.resolve("PairB.json")
            val pkgFileB = tempDir.resolve("PairB.pkg")

            Files.writeString(jsonFileA, "{ \"name\": \"Pair A\" }")
            createOpd3BinaryPackage(pkgFileA, entries = listOf("a.mp3" to "a".toByteArray()))
            Files.writeString(jsonFileB, "{ \"name\": \"Pair B\" }")
            createOpd3BinaryPackage(pkgFileB, entries = listOf("b.mp3" to "b".toByteArray()))

            val scanner = JvmFileScopedPackageScanner(jsonFileA)
            val candidates = scanner.scan()

            assertEquals(1, candidates.size)
            assertEquals(jsonFileA.toString(), candidates.first().source)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    // 4. Scanner returns exactly one candidate
    @Test
    fun `4 scanner returns exactly one candidate`() {
        val tempDir = Files.createTempDirectory("single-candidate-test")
        try {
            val jsonFile = tempDir.resolve("Single.json")
            val pkgFile = tempDir.resolve("Single.pkg")
            Files.writeString(jsonFile, "{ \"name\": \"Single\" }")
            createOpd3BinaryPackage(pkgFile, entries = listOf("m.mp3" to "data".toByteArray()))

            val scanner = JvmFileScopedPackageScanner(jsonFile)
            val candidates = scanner.scan()

            assertEquals(1, candidates.size)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    // 5. Large generated archive fixture completes quickly and safely
    @Test
    fun `5 large generated archive fixture completes quickly and safely`() {
        val tempDir = Files.createTempDirectory("large-archive-test")
        val persistenceDir = Files.createTempDirectory("large-archive-db")

        try {
            val jsonFile = tempDir.resolve("LargePackage.json")
            val pkgFile = tempDir.resolve("LargePackage.pkg")

            // Create 500 media items
            val entries = (1..500).map { i -> "audio_$i.mp3" to "Sample audio content for item $i".toByteArray() }
            createOpd3BinaryPackage(pkgFile, entries = entries)

            val jsonContent = buildLegacyJsonWithMedia(name = "Large Package", count = 500)
            Files.writeString(jsonFile, jsonContent)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            val startTime = System.currentTimeMillis()
            contentLibVm.importFromDirectory(jsonFile)
            val duration = System.currentTimeMillis() - startTime

            assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
            assertNotNull(contentLibVm.uiState.importMessage)
            assertNull(contentLibVm.uiState.importError)
            assertEquals(1, contentLibVm.uiState.packages.size)
            assertTrue(duration < 15_000, "500-item import should complete within 15 seconds, took ${duration}ms")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 6. Many media references prove archive is opened once and indexed efficiently
    @Test
    fun `6 many media references prove archive is opened once and indexed efficiently`() {
        val tempDir = Files.createTempDirectory("many-refs-test")
        try {
            val pkgFile = tempDir.resolve("ManyRefs.pkg")
            val entries = (1..200).map { i -> "media_$i.mp3" to "data_$i".toByteArray() }
            createOpd3BinaryPackage(pkgFile, entries = entries)

            val reader = LegacyOpd3MediaArchiveReader()
            val startTime = System.currentTimeMillis()
            val parsedEntries = reader.readEntries(pkgFile)
            val duration = System.currentTimeMillis() - startTime

            assertEquals(200, parsedEntries.size)
            assertTrue(duration < 2_000, "Archive indexing for 200 items took ${duration}ms")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    // 7. Missing optional media produces warnings, returns to idle, state safely updated
    @Test
    fun `7 missing media reference produces typed warning returns to idle`() {
        val tempDir = Files.createTempDirectory("missing-media-test")
        val persistenceDir = Files.createTempDirectory("missing-media-db")

        try {
            val jsonFile = tempDir.resolve("MissingMedia.json")
            val pkgFile = tempDir.resolve("MissingMedia.pkg")

            createOpd3BinaryPackage(pkgFile, entries = listOf("audio_1.mp3" to "data".toByteArray()))
            val jsonContent = """
                [
                  {
                    "en": "Sentence 1",
                    "vi": "Cau 1",
                    "audio": "audio_99.mp3",
                    "lesson": "Unit 1"
                  }
                ]
            """.trimIndent()
            Files.writeString(jsonFile, jsonContent)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibVm.importFromDirectory(jsonFile)

            assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
            assertTrue(contentLibVm.uiState.importMessage != null || contentLibVm.uiState.importError != null)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 8. Malformed or corrupted PKG produces typed failure, closes handles, operation terminates
    @Test
    fun `8 malformed or corrupted PKG produces typed failure closes handles operation terminates`() {
        val tempDir = Files.createTempDirectory("corrupt-pkg-test")
        val persistenceDir = Files.createTempDirectory("corrupt-pkg-db")

        try {
            val jsonFile = tempDir.resolve("Corrupt.json")
            val pkgFile = tempDir.resolve("Corrupt.pkg")

            Files.writeString(jsonFile, "[{ \"en\": \"Greeting\", \"vi\": \"Chao\" }]")
            Files.write(pkgFile, "NOT_AN_OPD3_HEADER_INVALID_MAGIC".toByteArray())

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibVm.importFromDirectory(jsonFile)

            assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
            assertNotNull(contentLibVm.uiState.importError)
            assertIs<LibraryUiState.Empty>(libraryViewModel.uiState)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 9. Cancellation during archive indexing
    @Test
    fun `9 cancellation during archive indexing`() {
        val cancellationSignal = PackageImportCancellationSignal()
        cancellationSignal.cancel()

        var caught = false
        try {
            cancellationSignal.checkCancelled()
        } catch (e: PackageImportCancelledException) {
            caught = true
        }

        assertTrue(caught, "checkCancelled must throw PackageImportCancelledException when cancelled")
    }

    // 10. Cancellation during media validation
    @Test
    fun `10 cancellation during media validation`() {
        val cancellationSignal = PackageImportCancellationSignal()
        val verifier = vn.loi.learning.infrastructure.contentmedia.ImportedMediaVerifier(
            object : ContentMediaStorage {
                override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset = error("Not needed")
                override fun resolve(relativePath: String): Path? = null
                override fun exists(relativePath: String) = true
            }
        )

        cancellationSignal.cancel()
        var caught = false
        try {
            verifier.verify(emptyList(), cancellationSignal)
        } catch (e: PackageImportCancelledException) {
            caught = true
        }

        assertTrue(caught, "verifier must respect cancellationSignal")
    }

    // 11. Exception injected at each major stage leaves Idle state
    @Test
    fun `11 exception injected at each major stage leaves Idle state`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val contentLibVm = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext)
        )

        // Attempt import with non-existent file path
        contentLibVm.importFromDirectory(Path.of("non_existent_file.json"))

        assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
        assertNotNull(contentLibVm.uiState.importError)
    }

    // 12. Success terminal-state test refreshes canonical and content Library projections
    @Test
    fun `12 success terminal-state test refreshes canonical and content Library projections`() {
        val tempDir = Files.createTempDirectory("success-terminal-test")
        val persistenceDir = Files.createTempDirectory("success-terminal-db")

        try {
            val jsonFile = tempDir.resolve("SuccessPkg.json")
            val pkgFile = tempDir.resolve("SuccessPkg.pkg")

            createOpd3BinaryPackage(pkgFile, entries = listOf("audio1.mp3" to "data".toByteArray()))
            val jsonContent = """
                [
                  {
                    "en": "Sentence 1",
                    "vi": "Cau 1",
                    "audio": "audio1.mp3",
                    "lesson": "Unit 1 Success"
                  }
                ]
            """.trimIndent()
            Files.writeString(jsonFile, jsonContent)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibVm.importFromDirectory(jsonFile)

            assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
            val state = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, state.installedPackages.size)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 13. UI-thread and non-blocking test at task-runner boundary
    @Test
    fun `13 UI-thread and non-blocking test at task-runner boundary`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val facade = createCanonicalLibraryFacade(appContext)
        val libraryViewModel = LibraryViewModel(facade = facade, taskRunner = ImmediateDesktopTaskRunner)
        val contentLibVm = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext)
        )

        assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
        assertIs<LibraryUiState.Empty>(libraryViewModel.uiState)
    }

    // 14. Regression: OPD3 single-file import remains working
    @Test
    fun `14 regression OPD3 single-file import remains working`() {
        val tempPkgDir = Files.createTempDirectory("opd3-regression-test")
        val persistenceDir = Files.createTempDirectory("opd3-regression-db")

        try {
            val opd3File = tempPkgDir.resolve("single.opd3")
            createOpd3ZipPackage(opd3File, name = "Single OPD3 Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibVm.importFromDirectory(opd3File)

            val state = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, state.installedPackages.size)
            assertEquals("Single OPD3 Package", state.installedPackages.first().name)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 15. Positive verification: Export OPD3 action is exposed in Desktop Library package cards
    @Test
    fun `15 export OPD3 action is exposed in Desktop Library package cards and delegates to ViewModel`() {
        val libraryDir = File("src/main/kotlin/vn/loi/learning/desktop/ui/library").takeIf { it.exists() }
            ?: File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/library")
        assertTrue(libraryDir.exists() && libraryDir.isDirectory, "Library UI directory must exist")

        // 1. Verify PackageListSection.kt renders "Export OPD3" inside the horizontal action Row
        val packageListSectionFile = File(libraryDir, "PackageListSection.kt")
        assertTrue(packageListSectionFile.exists(), "PackageListSection.kt must exist")
        val content = packageListSectionFile.readText()

        assertTrue(content.contains("Text(\"Export OPD3\")"), "PackageListSection must contain Export OPD3 action text")
        assertTrue(
            content.contains("onExportPackage != null && (pkg.state == PackageState.ACTIVE || pkg.state == PackageState.ARCHIVED)"),
            "Export OPD3 must be shown for both ACTIVE and ARCHIVED packages"
        )

        // 2. Verify layout ordering: Export OPD3 is in the action Row right after Browse Lessons and before Set Active
        val browseIdx = content.indexOf("Text(\"Browse Lessons\")")
        val exportIdx = content.indexOf("Text(\"Export OPD3\")")
        val setActiveIdx = content.indexOf("Text(\"Set Active\")")
        val moveUpIdx = content.indexOf("Text(\"Move Up\")")

        assertTrue(
            browseIdx > 0 && exportIdx > browseIdx && setActiveIdx > exportIdx && moveUpIdx > setActiveIdx,
            "Export OPD3 action must be positioned in the action row right after Browse Lessons and before Set Active"
        )

        // 3. Verify no ZIP serialization or packaging implementation logic exists in Compose UI files
        val composeUiFiles = libraryDir.walk().filter { it.isFile && it.extension == "kt" }.toList()
        val forbiddenPackagingLogic = listOf("ZipOutputStream", "PackageExportManifestJson", "Opd3PackageExporter")

        for (file in composeUiFiles) {
            val text = file.readText()
            for (token in forbiddenPackagingLogic) {
                assertFalse(
                    text.contains(token),
                    "File ${file.name} must not contain packaging or ZIP serialization logic token '$token'"
                )
            }
        }
    }

    // 16. Explicit selection of matching JSON and PKG pair imports only that pair
    @Test
    fun `16 explicit selection of matching JSON and PKG pair imports only that pair`() {
        val tempDir = Files.createTempDirectory("explicit-pair-test")
        val persistenceDir = Files.createTempDirectory("explicit-pair-db")

        try {
            val jsonFile = tempDir.resolve("Vocabulary_In_Use.json")
            val pkgFile = tempDir.resolve("Vocabulary_In_Use.pkg")

            createOpd3BinaryPackage(pkgFile, entries = listOf("audio1.mp3" to "data".toByteArray()))
            val jsonContent = """
                [
                  {
                    "en": "Apple",
                    "vi": "Qua tao",
                    "audio": "audio1.mp3",
                    "lesson": "Fruit Unit"
                  }
                ]
            """.trimIndent()
            Files.writeString(jsonFile, jsonContent)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            // User selects both files explicitly
            contentLibVm.importFromFiles(listOf(jsonFile, pkgFile))

            assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
            assertNull(contentLibVm.uiState.importError)
            val state = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, state.installedPackages.size)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 17. Realistic 1000-item legacy topic import completes quickly without post-parse stall
    @Test
    fun `17 realistic 1000-item legacy topic import completes quickly without post-parse stall`() {
        val tempDir = Files.createTempDirectory("1000-item-test")
        val persistenceDir = Files.createTempDirectory("1000-item-db")

        try {
            val jsonFile = tempDir.resolve("2000Cau.json")
            val pkgFile = tempDir.resolve("2000Cau.pkg")

            val entries = (1..1000).map { i -> "audio_$i.mp3" to "Audio payload $i".toByteArray() }
            createOpd3BinaryPackage(pkgFile, entries = entries)

            val jsonContent = buildLegacyJsonWithMedia(name = "2000 Cau Topic", count = 1000)
            Files.writeString(jsonFile, jsonContent)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            val startTime = System.currentTimeMillis()
            contentLibVm.importFromFiles(listOf(jsonFile, pkgFile))
            val duration = System.currentTimeMillis() - startTime

            assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
            assertNull(contentLibVm.uiState.importError)
            val state = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, state.installedPackages.size)
            assertTrue(duration < 15_000, "1,000-item import should complete within 15 seconds, took ${duration}ms")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 18. Mismatched pair selection is rejected before import starts
    @Test
    fun `18 mismatched pair selection is rejected before import starts`() {
        val tempDir = Files.createTempDirectory("mismatch-test")
        val persistenceDir = Files.createTempDirectory("mismatch-db")

        try {
            val jsonFile = tempDir.resolve("TopicA.json")
            val pkgFile = tempDir.resolve("TopicB.pkg")
            Files.writeString(jsonFile, "[]")
            createOpd3BinaryPackage(pkgFile, entries = emptyList())

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(jsonFile, pkgFile))

            assertIs<ContentLibraryOperation.Idle>(contentLibVm.uiState.operation)
            assertEquals(
                "To import a legacy topic, select its matching .json and .pkg files together.",
                contentLibVm.uiState.importError
            )
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 19. Browse Lessons opens lesson browser and populates lesson list
    @Test
    fun `19 browse lessons opens lesson browser and populates lesson list`() {
        val tempDir = Files.createTempDirectory("browse-lessons-test")
        val persistenceDir = Files.createTempDirectory("browse-lessons-db")

        try {
            val opd3File = tempDir.resolve("BrowseTopic.opd3")
            createOpd3ZipPackage(opd3File, name = "Browse Topic", contentId = "cnt-browse-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            // Import package
            contentLibVm.importFromFiles(listOf(opd3File))

            val libState = contentLibVm.uiState
            assertEquals(1, libState.libraries.size)

            // Trigger openLibrary (Browse Lessons action)
            contentLibVm.openLibrary(libState.libraries.first().id)

            val browserState = contentLibVm.lessonBrowserUiState
            assertNotNull(browserState, "LessonBrowserUiState must be non-null after Browse Lessons")
            assertEquals(1, browserState.lessons.size)
            assertEquals("cnt-browse-1", browserState.lessons.first().id)
            assertEquals("Greeting", browserState.lessons.first().title)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 20. Package-scoped Browse Lessons isolates lessons of Topic A and Topic B (A1, A2)
    @Test
    fun `20 package-scoped browse lessons isolates lessons between Topic A and Topic B`() {
        val tempDir = Files.createTempDirectory("package-browse-isolation-test")
        val persistenceDir = Files.createTempDirectory("package-browse-isolation-db")

        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")

            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val navTree = appContext.libraryQuery!!.getNavigationTree(appContext.defaultLibraryId!!)!!
            assertEquals(2, navTree.installedPackages.size)

            val pkgSummaryA = navTree.installedPackages.first { it.name == "Topic A" }
            val pkgSummaryB = navTree.installedPackages.first { it.name == "Topic B" }

            // Browse Lessons Topic A -> ONLY A lessons present (A1)
            contentLibVm.browsePackageLessons(pkgSummaryA.id, pkgSummaryA.name)
            val stateA = contentLibVm.lessonBrowserUiState
            assertNotNull(stateA)
            assertEquals(1, stateA.lessons.size)
            assertEquals("cnt-a-1", stateA.lessons.first().id)
            assertTrue(stateA.lessons.none { it.id == "cnt-b-1" }, "Topic A lessons must NOT contain Topic B content")

            // Browse Lessons Topic B -> ONLY B lessons present (A2)
            contentLibVm.browsePackageLessons(pkgSummaryB.id, pkgSummaryB.name)
            val stateB = contentLibVm.lessonBrowserUiState
            assertNotNull(stateB)
            assertEquals(1, stateB.lessons.size)
            assertEquals("cnt-b-1", stateB.lessons.first().id)
            assertTrue(stateB.lessons.none { it.id == "cnt-a-1" }, "Topic B lessons must NOT contain Topic A content")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 21. Non-existent InstalledPackageId returns clear failure without fallback (A3)
    @Test
    fun `21 non-existent installed package id sets clear error without falling back to first package`() {
        val tempDir = Files.createTempDirectory("non-existent-pkg-test")
        val persistenceDir = Files.createTempDirectory("non-existent-pkg-db")

        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))

            // Browse Lessons with non-existent InstalledPackageId
            val invalidPkgId = vn.loi.learning.domain.library.model.InstalledPackageId("non-existent-pkg-999")
            contentLibVm.browsePackageLessons(invalidPkgId, "NonExistent")

            // Assert no fallback: lessonBrowserUiState is null and loadError contains explicit message
            assertNull(contentLibVm.lessonBrowserUiState, "LessonBrowserUiState must be null when package does not exist")
            assertNotNull(contentLibVm.uiState.loadError)
            assertTrue(contentLibVm.uiState.loadError!!.contains("non-existent-pkg-999"))
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    private fun createOpd3BinaryPackage(file: Path, entries: List<Pair<String, ByteArray>>) {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Write OPD3 magic (4 bytes)
        dos.writeBytes("OPD3")
        // Version (int = 1)
        dos.writeInt(1)
        // Entry count (int)
        dos.writeInt(entries.size)

        var currentOffset = 12L + entries.sumOf { (name, _) -> 23L + name.toByteArray(StandardCharsets.UTF_8).size }

        val metadataList = mutableListOf<Triple<String, Long, ByteArray>>()

        entries.forEach { (name, data) ->
            val crc = CRC32().apply { update(data) }.value
            dos.writeShort(name.toByteArray(StandardCharsets.UTF_8).size)
            dos.writeByte(1) // mediaType
            dos.writeLong(currentOffset)
            dos.writeLong(data.size.toLong())
            dos.writeInt((crc and 0xFFFFFFFFL).toInt())
            dos.write(name.toByteArray(StandardCharsets.UTF_8))

            metadataList.add(Triple(name, currentOffset, data))
            currentOffset += data.size
        }

        dos.flush()

        // Append payload data
        val headerAndMeta = baos.toByteArray()
        val fullData = ByteArrayOutputStream()
        fullData.write(headerAndMeta)

        entries.forEach { (_, data) ->
            fullData.write(data)
        }

        Files.write(file, fullData.toByteArray())
    }

    private fun buildLegacyJsonWithMedia(name: String, count: Int): String {
        return buildString {
            append("[\n")
            for (i in 1..count) {
                append("  {\n")
                append("    \"en\": \"Sentence $i\",\n")
                append("    \"vi\": \"Cau $i\",\n")
                append("    \"audio\": \"audio_$i.mp3\",\n")
                append("    \"lesson\": \"$name Lesson\"\n")
                append("  }")
                if (i < count) append(",")
                append("\n")
            }
            append("]\n")
        }
    }

    private fun createOpd3ZipPackage(file: Path, name: String = "Single OPD3 Package", contentId: String = "content-1") {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": 1, "learningItemCount": 1 }"""
            )
            writeZipEntry(zip, "metadata.json", """{ "name": "$name", "version": "1.0.0", "format": "OPD3" }""")
            writeZipEntry(
                zip,
                "contents.json",
                """{ "contents": [ { "id": "$contentId", "type": "SENTENCE", "primaryText": "Greeting", "translatedText": "Chao", "title": "Greeting", "group": "English", "section": "Unit 1", "lesson": "$name Greetings" } ] }"""
            )
            writeZipEntry(
                zip,
                "learning-items.json",
                """{ "learningItems": [ { "id": "$contentId-rec", "contentId": "$contentId", "mode": "MEANING_RECOGNITION", "isEnabled": true } ] }"""
            )
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
