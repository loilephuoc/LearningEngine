package vn.loi.learning.android.study

import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test

class StudyAudioOwnershipTest {
    @Test
    fun `image answer loop claim is idempotent for recomposition and transfers on next item`() {
        val ownership = StudyAudioOwnership()
        val first = ownership.update("image-a", feedbackActive = false)!!
        assertTrue(ownership.claimAutoplay(first, AudioRole.EXPECTED_ANSWER))
        assertFalse(ownership.claimAutoplay(first, AudioRole.EXPECTED_ANSWER))

        val second = ownership.update("image-b", feedbackActive = false)!!
        assertFalse(ownership.permitsManualPlayback(first))
        assertTrue(ownership.claimAutoplay(second, AudioRole.EXPECTED_ANSWER))
    }
    @Test
    fun `AnimatedContent overlap grants autoplay only to authoritative incoming item`() {
        val ownership = StudyAudioOwnership()
        val itemA = ownership.update("A", feedbackActive = false)!!
        assertTrue(ownership.claimAutoplay(itemA, AudioRole.MEANING))
        assertFalse(ownership.claimAutoplay(itemA, AudioRole.MEANING))

        ownership.update("A", feedbackActive = true)
        assertFalse(ownership.claimAutoplay(itemA, AudioRole.EXPECTED_ANSWER))

        val itemB = ownership.update("B", feedbackActive = true)!!
        assertNotEquals(itemA, itemB)
        assertFalse(ownership.claimAutoplay(itemA, AudioRole.EXPECTED_ANSWER))
        assertFalse(ownership.claimAutoplay(itemB, AudioRole.MEANING))

        ownership.update("B", feedbackActive = false)
        assertFalse(ownership.claimAutoplay(itemA, AudioRole.EXPECTED_ANSWER))
        assertTrue(ownership.claimAutoplay(itemB, AudioRole.MEANING))
        assertFalse(ownership.claimAutoplay(itemB, AudioRole.MEANING))
    }

    @Test
    fun `stale generation cannot control current playback`() {
        val ownership = StudyAudioOwnership()
        val firstA = ownership.update("A", feedbackActive = false)!!
        val itemB = ownership.update("B", feedbackActive = false)!!
        val secondA = ownership.update("A", feedbackActive = false)!!

        assertNotEquals(firstA, secondA)
        assertFalse(ownership.isCurrent(firstA))
        assertFalse(ownership.permitsManualPlayback(itemB))
        assertTrue(ownership.isCurrent(secondA))
        assertTrue(ownership.permitsManualPlayback(secondA))
    }

    @Test
    fun `rapid transitions never leave two current owners`() {
        val ownership = StudyAudioOwnership()
        val tokens = (1..20).map { ownership.update("item-$it", feedbackActive = false)!! }
        assertTrue(ownership.isCurrent(tokens.last()))
        tokens.dropLast(1).forEach { assertFalse(ownership.isCurrent(it)) }
    }

    @Test
    fun `foreground loss synchronously stops every registered Study audio owner`() {
        val ownership = StudyAudioOwnership()
        val stopped = mutableListOf<String>()
        ownership.registerForegroundStop("item") { stopped += "item" }
        ownership.registerForegroundStop("gate") { stopped += "gate" }
        ownership.stopForForegroundLoss()
        assertEquals(listOf("item", "gate"), stopped)
    }

    @Test
    fun `disposed runtime is not stopped and current item remains manually usable`() {
        val ownership = StudyAudioOwnership()
        val token = ownership.update("item", feedbackActive = false)!!
        var staleStops = 0
        ownership.registerForegroundStop("stale") { staleStops++ }
        ownership.unregisterForegroundStop("stale")
        ownership.stopForForegroundLoss()
        assertEquals(0, staleStops)
        assertTrue(ownership.permitsManualPlayback(token))
    }
}
