package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

class ContentIntroductionPresentationTest {
    private val contentId = ContentId("content-a")

    @Test
    fun `unseen new content requires one introduction by content identity`() {
        assertEquals(
            ContentIntroductionState.REQUIRED,
            resolveContentIntroductionState(SessionItemOrigin.NEW, contentId, emptySet())
        )
        assertEquals(
            ContentIntroductionState.COMPLETED,
            resolveContentIntroductionState(
                SessionItemOrigin.NEW,
                contentId,
                setOf(contentId)
            )
        )
    }

    @Test
    fun `review content never enters introduction`() {
        assertEquals(
            ContentIntroductionState.NOT_APPLICABLE,
            resolveContentIntroductionState(SessionItemOrigin.REVIEW, contentId, emptySet())
        )
    }

    @Test
    fun `production screen binds introduction to one shot Vietnamese audio transition`() {
        val source = source("StudyScreen.kt")
        assertTrue(source.contains("uiState.contentIntroductionState"))
        assertTrue(source.contains("focusedAnswerModel.meaningAudioPath?.let(audioController::playOnce)"))
    }

    private fun source(name: String): String {
        val relative = "src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"
        return java.nio.file.Files.readString(
            sequenceOf(
                java.nio.file.Path.of("desktop").resolve(relative),
                java.nio.file.Path.of(relative)
            ).first(java.nio.file.Files::exists)
        )
    }
}
