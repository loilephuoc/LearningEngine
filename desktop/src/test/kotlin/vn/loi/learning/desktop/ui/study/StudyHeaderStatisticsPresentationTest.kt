package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.domain.study.memory.model.Moment

class StudyHeaderStatisticsPresentationTest {
    @Test
    fun `available and last known good produce two compact groups with full semantics`() {
        val value = statistics()
        val available = resolveStudyHeaderStatisticsPresentation(
            StudyHeaderStatisticsState.Available(value),
            StudyStatisticsStrings.ENGLISH
        )!!
        assertEquals(listOf("Total", "New", "Review", "Due"), available.primary.map { it.label })
        assertEquals(listOf("Again", "Hard", "Good", "Easy"), available.ratings.map { it.label })
        assertEquals(4, available.primary.size)
        assertEquals(4, available.ratings.size)
        assertTrue(available.accessibilityDescription.contains("Total 10"))
        assertTrue(available.accessibilityDescription.contains("New 1/20"))
        assertTrue(available.accessibilityDescription.contains("Review 4/100"))
        assertTrue(available.accessibilityDescription.contains("Easy 1"))
        assertEquals(
            available,
            resolveStudyHeaderStatisticsPresentation(
                StudyHeaderStatisticsState.Unavailable(value),
                StudyStatisticsStrings.ENGLISH
            )
        )
    }

    @Test
    fun `loading and unavailable without prior data are not rendered as zero`() {
        assertNull(
            resolveStudyHeaderStatisticsPresentation(
                StudyHeaderStatisticsState.Loading,
                StudyStatisticsStrings.ENGLISH
            )
        )
        assertNull(
            resolveStudyHeaderStatisticsPresentation(
                StudyHeaderStatisticsState.Unavailable(),
                StudyStatisticsStrings.ENGLISH
            )
        )
    }

    @Test
    fun `Study header consumes application state without repository or Material color authority`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyHeaderStatisticsPresentation.kt")
        )
        val screen = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt")
        )
        assertTrue(!source.contains("Repository"))
        assertTrue(!source.contains("MaterialTheme"))
        val function = screen.substringAfter("private fun StudyHeaderStatisticsRow")
            .substringBefore("private fun LessonProgressCard")
        assertTrue(function.contains("LETheme.colors"))
        assertTrue(!function.contains("MaterialTheme"))
        assertTrue(!function.contains("Color("))
    }

    private fun statistics() = StudyHeaderStatistics(
        session = vn.loi.learning.application.packageprogress.StudySessionProgressStatistics(
            "session", 1, 20, 6, 4, 100, 5
        ),
        packageLearning = vn.loi.learning.application.packageprogress.StudyPackageLearningStatistics(
            "scope", Moment(100), 10, 2, 1, 2, 6, 1, Moment(200)
        )
    )
}
