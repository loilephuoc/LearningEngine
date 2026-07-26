package vn.loi.learning.desktop.ui.study

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun `Test 1 - Topic session displays its Topic name`() {
        val tempDir = Files.createTempDirectory("header-test-1-temp")
        val persistenceDir = Files.createTempDirectory("header-test-1-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Vocabulary_In_Use_Elementary", contentId = "cnt-a-1")

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

            // General study for active topic package
            appContext.libraryCommand!!.setActivePackage(defaultLibId, activePkgA.id)
            studyVm.startStudy()
            val state = studyVm.uiState
            assertTrue(state.sessionStarted)
            assertEquals("Vocabulary_In_Use_Elementary", state.studyTitle)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 2 - Switching active Topic and starting a new session displays the new Topic`() {
        val tempDir = Files.createTempDirectory("header-test-2-temp")
        val persistenceDir = Files.createTempDirectory("header-test-2-db")
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
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Vocabulary_In_Use_Elementary" }
            val activePkgB = navTree.activePackages.first { it.name == "Advanced_Grammar_In_Use" }

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
    fun `Test 3 - Resume preserves saved session Topic even when another Topic is currently active`() {
        val tempDir = Files.createTempDirectory("header-test-3-temp")
        val persistenceDir = Files.createTempDirectory("header-test-3-db")
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
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Vocabulary_In_Use_Elementary" }
            val activePkgB = navTree.activePackages.first { it.name == "Advanced_Grammar_In_Use" }

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
    fun `Test 4 - Session without single-topic context uses fallback safely`() {
        val persistenceDir = Files.createTempDirectory("header-test-4-db")
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
        val contents = (1..contentCount).joinToString(",") { i ->
            """{ "id": "$contentId-$i", "type": "SENTENCE", "primaryText": "Greeting $i", "translatedText": "Chao $i", "title": "Greeting $i", "group": "English", "section": "Unit 1", "lesson": "$name Greetings" }"""
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
