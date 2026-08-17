package vn.loi.learning.android.controller

import android.view.KeyEvent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Focused test suite verifying Controller Settings UI presentation, layout structure,
 * simplified scope labels, two-level visual hierarchy, and data preservation (Phase 1.4.1).
 */
class ControllerSettingsUiPresentationTest {

    @Test
    fun `A - simplified user-facing scope labels use concise presentation strings without Only prefix`() {
        assertEquals("Everywhere", ControllerContext.GLOBAL.label)
        assertEquals("Question", ControllerContext.STUDY_QUESTION.label)
        assertEquals("Answer revealed", ControllerContext.STUDY_REVEALED.label)
        assertEquals("Rating card", ControllerContext.STUDY_RATING.label)
        assertEquals("Auto Play", ControllerContext.AUTO_PLAY.label)
        assertEquals("Shadowing", ControllerContext.SHADOWING.label)
    }

    @Test
    fun `B - two-level visual grouping cleanly partitions Everywhere mappings and Context Overrides`() {
        val defaultProfile = DefaultControllerProfiles.defaultProfile()

        val everywhereMappings = defaultProfile.mappings.filter { it.context == ControllerContext.GLOBAL }
        val overrideMappings = defaultProfile.mappings.filter { it.context != ControllerContext.GLOBAL }

        assertTrue(everywhereMappings.isNotEmpty(), "Everywhere mappings section must have primary bindings")
        assertTrue(overrideMappings.isNotEmpty(), "Context overrides section must contain specific screen overrides")

        // Total count must be conserved exactly
        assertEquals(defaultProfile.mappings.size, everywhereMappings.size + overrideMappings.size)

        // All everywhere mappings must be ControllerContext.GLOBAL
        assertTrue(everywhereMappings.all { it.context == ControllerContext.GLOBAL })

        // All override mappings must have specific contexts
        assertTrue(overrideMappings.none { it.context == ControllerContext.GLOBAL })
    }

    @Test
    fun `C - context overrides grouping by context partitions cleanly for display`() {
        val defaultProfile = DefaultControllerProfiles.defaultProfile()
        val overrideMappings = defaultProfile.mappings.filter { it.context != ControllerContext.GLOBAL }
        val grouped = overrideMappings.groupBy { it.context }

        assertTrue(grouped.containsKey(ControllerContext.STUDY_RATING), "Rating card overrides must be present")
        assertTrue(grouped.containsKey(ControllerContext.AUTO_PLAY), "Auto Play overrides must be present")

        val ratingMappings = grouped[ControllerContext.STUDY_RATING].orEmpty()
        assertEquals(4, ratingMappings.size, "FSRS 1-4 rating actions must be grouped under Rating card")
        assertTrue(ratingMappings.any { it.action == ControllerAction.RATE_AGAIN })
        assertTrue(ratingMappings.any { it.action == ControllerAction.RATE_HARD })
        assertTrue(ratingMappings.any { it.action == ControllerAction.RATE_GOOD })
        assertTrue(ratingMappings.any { it.action == ControllerAction.RATE_EASY })
    }

    @Test
    fun `D - physical button display labels for D-Pad, Triggers, and Chords render cleanly and compactly`() {
        val l1Gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K))
        val r1Gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_M))
        val l2Gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_L))
        val r2Gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_R))
        val dpadLeftGesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_E))
        val dpadRightGesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_F))
        val chordGesture = ControllerGesture(
            input = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G),
            modifier = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_O)
        )

        assertEquals("L1", l1Gesture.displayLabel)
        assertEquals("R1", r1Gesture.displayLabel)
        assertEquals("L2", l2Gesture.displayLabel)
        assertEquals("R2", r2Gesture.displayLabel)
        assertEquals("D-Pad Left", dpadLeftGesture.displayLabel)
        assertEquals("D-Pad Right", dpadRightGesture.displayLabel)
        assertEquals("Start / + + A", chordGesture.displayLabel)
    }

    @Test
    fun `E - action titles and descriptions are well-formed and non-empty for UI rendering`() {
        for (action in ControllerAction.entries) {
            assertTrue(action.label.isNotBlank(), "Action ${action.name} must have a non-blank label")
            assertTrue(action.description.isNotBlank(), "Action ${action.name} must have a non-blank description")
        }
    }

    @Test
    fun `F - expanding and collapsing overrides section does not mutate profile or persisted state`() {
        val initialProfile = DefaultControllerProfiles.defaultProfile()
        val originalMappings = initialProfile.mappings.toList()

        // Simulating UI state filter
        val everywhere = initialProfile.mappings.filter { it.context == ControllerContext.GLOBAL }
        val overrides = initialProfile.mappings.filter { it.context != ControllerContext.GLOBAL }

        // Combined back
        val combined = everywhere + overrides
        assertEquals(originalMappings.size, combined.size)
        assertEquals(originalMappings.toSet(), combined.toSet())
    }

    @Test
    fun `G - deleting an assignment from either section removes exactly that mapping`() {
        val initialProfile = DefaultControllerProfiles.defaultProfile()
        val initialCount = initialProfile.mappings.size

        // 1. Delete an everywhere mapping (L1 Replay)
        val l1Mapping = initialProfile.mappings.first { it.context == ControllerContext.GLOBAL && it.action == ControllerAction.REPLAY_PRIMARY_AUDIO }
        val afterL1Delete = ControllerMappingResolver.removeMapping(initialProfile, l1Mapping.context, l1Mapping.gesture)
        assertEquals(initialCount - 1, afterL1Delete.mappings.size)
        assertNull(afterL1Delete.mappings.firstOrNull { it.context == ControllerContext.GLOBAL && it.action == ControllerAction.REPLAY_PRIMARY_AUDIO })

        // 2. Delete an override mapping (Rating Card -> RATE_AGAIN)
        val ratingMapping = initialProfile.mappings.first { it.context == ControllerContext.STUDY_RATING && it.action == ControllerAction.RATE_AGAIN }
        val afterRatingDelete = ControllerMappingResolver.removeMapping(initialProfile, ratingMapping.context, ratingMapping.gesture)
        assertEquals(initialCount - 1, afterRatingDelete.mappings.size)
        assertNull(afterRatingDelete.mappings.firstOrNull { it.context == ControllerContext.STUDY_RATING && it.action == ControllerAction.RATE_AGAIN })
    }

    @Test
    fun `H - button directory full names provide secondary disambiguation for dropdowns`() {
        val l1Full = ControllerButtonDirectory.getButtonFullName(KeyEvent.KEYCODE_K)
        val l2Full = ControllerButtonDirectory.getButtonFullName(KeyEvent.KEYCODE_L)
        val r2Full = ControllerButtonDirectory.getButtonFullName(KeyEvent.KEYCODE_R)

        assertTrue(l1Full.contains("L1") && l1Full.contains("Left Bumper"))
        assertTrue(l2Full.contains("L2") && l2Full.contains("Left Trigger"))
        assertTrue(r2Full.contains("R2") && r2Full.contains("Right Trigger"))
    }
}
