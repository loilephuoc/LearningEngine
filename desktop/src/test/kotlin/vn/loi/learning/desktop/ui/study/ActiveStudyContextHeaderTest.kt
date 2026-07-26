package vn.loi.learning.desktop.ui.study

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ActiveStudyContextHeaderTest {

    @Test
    fun `Test 1 - Vocabulary item to fill in does not become the header`() {
        val tempDir = Files.createTempDirectory("header-test-vocab-temp")
        val persistenceDir = Files.createTempDirectory("header-test-vocab-db")
        try {
            val fileA = tempDir.resolve("VocabPackage.opd3")
            createOpd3ZipPackageWithCustomItems(
                fileA,
                name = "Vocabulary_In_Use_Elementary",
                contentId = "vocab-1",
                itemTitle = "to fill in",
                lessonName = null
            )

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Vocabulary_In_Use_Elementary" }

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = activePkgA.id,
                    contentId = ContentId("vocab-1-1")
                )
            )

            val state = studyVm.uiState
            assertTrue(state.sessionStarted)
            assertNotEquals("to fill in", state.studyTitle, "Vocabulary item title MUST NOT become the study context header")
            assertEquals("Vocabulary_In_Use_Elementary", state.studyTitle)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 2 - Vocabulary header stays equal to package topic title across multiple item transitions`() {
        val tempDir = Files.createTempDirectory("header-test-transitions-temp")
        val persistenceDir = Files.createTempDirectory("header-test-transitions-db")
        try {
            val fileA = tempDir.resolve("VocabPackage.opd3")
            createOpd3ZipPackage(fileA, name = "Vocabulary_In_Use_Elementary", contentId = "cnt-a-1", contentCount = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Vocabulary_In_Use_Elementary" }

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            appContext.libraryCommand!!.setActivePackage(defaultLibId, activePkgA.id)
            studyVm.startStudy()
            assertEquals("Vocabulary_In_Use_Elementary", studyVm.uiState.studyTitle)

            // Transition item 1 -> item 2
            studyVm.reviewEasy()
            assertEquals("Vocabulary_In_Use_Elementary", studyVm.uiState.studyTitle, "Header must remain stable across item transitions")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 3 - A real listening lesson title remains a valid container header`() {
        val tempDir = Files.createTempDirectory("header-test-listening-temp")
        val persistenceDir = Files.createTempDirectory("header-test-listening-db")
        try {
            val fileA = tempDir.resolve("ListeningPackage.opd3")
            createOpd3ZipPackageWithCustomItems(
                fileA,
                name = "Short_Stories_Section_1",
                contentId = "listening-1",
                itemTitle = "Story Audio",
                lessonName = "2. Jessica's first day of school"
            )

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Short_Stories_Section_1" }

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = activePkgA.id,
                    contentId = ContentId("listening-1-1")
                )
            )

            val state = studyVm.uiState
            assertTrue(state.sessionStarted)
            assertEquals("2. Jessica's first day of school", state.studyTitle)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 4 - Switching active Topic and starting a new session displays the new Topic`() {
        val tempDir = Files.createTempDirectory("header-test-switch-temp")
        val persistenceDir = Files.createTempDirectory("header-test-switch-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Vocabulary_In_Use_Elementary", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Advanced_Grammar_In_Use", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree4 = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree4.installedPackages.first { it.name == "Vocabulary_In_Use_Elementary" }
            val activePkgB = navTree4.installedPackages.first { it.name == "Advanced_Grammar_In_Use" }

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            // Start General Study Session for Active Topic A
            appContext.libraryCommand!!.setActivePackage(defaultLibId, activePkgA.id)
            studyVm.startStudy()
            assertEquals("Vocabulary_In_Use_Elementary", studyVm.uiState.studyTitle)

            // Switch to Active Package B in Library and start General Study Session B
            appContext.libraryCommand!!.setActivePackage(defaultLibId, activePkgB.id)
            studyVm.startStudy()
            assertEquals("Advanced_Grammar_In_Use", studyVm.uiState.studyTitle)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 5 - Resume preserves saved session Topic even when another Topic is currently active`() {
        val tempDir = Files.createTempDirectory("header-test-resume-temp")
        val persistenceDir = Files.createTempDirectory("header-test-resume-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Vocabulary_In_Use_Elementary", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Advanced_Grammar_In_Use", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree5 = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree5.installedPackages.first { it.name == "Vocabulary_In_Use_Elementary" }
            val activePkgB = navTree5.installedPackages.first { it.name == "Advanced_Grammar_In_Use" }

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            // Start Session A for Active Topic A
            appContext.libraryCommand!!.setActivePackage(defaultLibId, activePkgA.id)
            studyVm.startStudy()
            assertEquals("Vocabulary_In_Use_Elementary", studyVm.uiState.studyTitle)

            // Switch active topic in Library to Topic B
            appContext.libraryCommand!!.setActivePackage(defaultLibId, activePkgB.id)

            // Recover/Load Session A directly from StudyFacade
            val resumedState = studyFacade.load()
            assertTrue(resumedState.sessionStarted)
            assertEquals("Vocabulary_In_Use_Elementary", resumedState.studyTitle)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 6 - Session without single-topic context uses fallback safely`() {
        val persistenceDir = Files.createTempDirectory("header-test-fallback-db")
        try {
            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            // Start General Study without active topic/package
            studyVm.startStudy()
            val state = studyVm.uiState
            assertEquals("All learning items", state.studyTitle)
        } finally {
            persistenceDir.toFile().deleteRecursively()
        }
    }

    private fun createOpd3ZipPackage(file: Path, name: String, contentId: String, contentCount: Int = 1) {
        createOpd3ZipPackageWithCustomItems(file, name, contentId, itemTitle = name, lessonName = null, contentCount = contentCount)
    }

    private fun createOpd3ZipPackageWithCustomItems(
        file: Path,
        name: String,
        contentId: String,
        itemTitle: String,
        lessonName: String?,
        contentCount: Int = 1
    ) {
        val contents = (1..contentCount).joinToString(",") { i ->
            val lessonJson = if (lessonName != null) """"lesson": "$lessonName",""" else ""
            """{ "id": "$contentId-$i", "type": "SENTENCE", "primaryText": "$itemTitle $i", "translatedText": "Chao $i", "title": "$itemTitle $i", $lessonJson "group": "English", "section": "Unit 1" }"""
        }
        val items = (1..contentCount).joinToString(",") { i ->
            """{ "id": "$contentId-$i-rec", "contentId": "$contentId-$i", "mode": "MEANING_RECOGNITION", "isEnabled": true }"""
        }

        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "id": "$name", "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": $contentCount, "learningItemCount": $contentCount }"""
            )
            writeZipEntry(zip, "metadata.json", """{ "id": "$name", "name": "$name", "version": "1.0.0", "format": "OPD3" }""")
            writeZipEntry(zip, "contents.json", """{ "contents": [ $contents ] }""")
            writeZipEntry(zip, "learning-items.json", """{ "learningItems": [ $items ] }""")
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
