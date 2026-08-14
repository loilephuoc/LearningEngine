package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession

class AndroidCompletionProjectionTest {
    @Test
    fun `completion mode family uses only canonical persisted session facts`() {
        assertEquals("Learn New", androidCompletionModeFamily(session(StudyMode.LEARN_NEW)))
        assertEquals("Typing", androidCompletionModeFamily(session(StudyMode.TYPING)))
        assertEquals("Review", androidCompletionModeFamily(session(StudyMode.ADAPTIVE)))
    }

    @Test
    fun `completion result reports canonical total and only useful mixed split`() {
        assertEquals("1 item completed", completionResultDescription(1, 1, 0))
        assertEquals("3 items completed", completionResultDescription(3, 0, 3))
        assertEquals("4 items completed · 1 new · 3 review", completionResultDescription(4, 1, 3))
        assertFailsWith<IllegalArgumentException> { completionResultDescription(2, 1, 0) }
        assertFailsWith<IllegalArgumentException> {
            AndroidStudyState.Completion(
                sessionId = "invalid",
                canUndo = false,
                totalCompleted = 2,
                newCompleted = 1,
                reviewCompleted = 0
            )
        }
    }

    @Test
    fun `completion card groups announcement while leaving actions outside result semantics`() {
        val component = Files.readString(Path.of(
            "src/main/kotlin/vn/loi/learning/android/ui/LearningEngineComponents.kt"
        )).substringAfter("fun LearningEngineCompletionCard(")
        assertTrue(component.contains("modeLabel: String? = null"))
        assertTrue(component.contains("summary: String? = null"))
        assertTrue(component.contains("Modifier.semantics(mergeDescendants = true)"))
        assertTrue(component.indexOf("Modifier.semantics(mergeDescendants = true)") <
            component.indexOf("LearningEngineSecondaryButton("))
    }

    private fun session(mode: StudyMode): StudySession = StudySession.start(
        id = SessionId("completion-${mode.name.lowercase()}"),
        learnerId = LearnerId("completion-learner"),
        startedAt = Moment(1_000),
        policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 1),
        studyMode = mode
    )
}
