package vn.loi.learning.android.reminder

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import vn.loi.learning.android.controller.ControllerContext
import vn.loi.learning.android.controller.StudyControllerBridge
import vn.loi.learning.android.controller.StudyControllerTarget
import vn.loi.learning.domain.study.memory.model.ReviewRating

class AndroidVocabularyReminderAudioControllerTest {

    @Test
    fun `51 to 55 Controller target in Reminder Review provides navigation without rating mutation`() = runBlocking {
        var page = 2
        val maxPages = 5
        var audioPlayed = false

        val target = object : StudyControllerTarget {
            override fun currentContext(): ControllerContext = ControllerContext.STUDY_REVEALED
            override suspend fun revealAnswer(): Boolean = false
            override suspend fun rate(rating: ReviewRating): Boolean = false // Read-only!
            override suspend fun next(): Boolean {
                if (page < maxPages - 1) {
                    page++
                    return true
                }
                return false
            }
            override suspend fun previous(): Boolean {
                if (page > 0) {
                    page--
                    return true
                }
                return false
            }
            override fun replayAudio(): Boolean {
                audioPlayed = true
                return true
            }
            override fun describeState(): String = "ReminderReview"
        }

        StudyControllerBridge.register(target)

        assertEquals("ReminderReview", StudyControllerBridge.activeTarget?.describeState())

        // Next advances
        assertTrue(StudyControllerBridge.activeTarget!!.next())
        assertEquals(3, page)

        // Previous rewinds
        assertTrue(StudyControllerBridge.activeTarget!!.previous())
        assertEquals(2, page)

        // Replay audio
        assertTrue(StudyControllerBridge.activeTarget!!.replayAudio())
        assertTrue(audioPlayed)

        // Rating is rejected
        assertFalse(StudyControllerBridge.activeTarget!!.rate(ReviewRating.GOOD))
        assertFalse(StudyControllerBridge.activeTarget!!.rate(ReviewRating.AGAIN))

        StudyControllerBridge.unregister(target)
    }
}
