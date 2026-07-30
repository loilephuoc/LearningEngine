package vn.loi.learning.desktop.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopStudySessionPolicyTest {
    @Test
    fun `maps current desktop preferences to a new domain session policy`() {
        val policy = DesktopRuntimeConfiguration(
            newItemsPerSession = 5,
            reviewItemsPerSession = 50
        ).toSessionPolicy()

        assertEquals(5, policy.newItemLimit)
        assertEquals(50, policy.reviewItemLimit)
    }

    @Test
    fun `active custom review value reaches the next session policy while remembered value does not override preset`() {
        val custom = DesktopRuntimeConfiguration(
            reviewItemsPerSession = 5,
            customReviewItemsPerSession = 5
        ).toSessionPolicy()
        val preset = DesktopRuntimeConfiguration(
            reviewItemsPerSession = 20,
            customReviewItemsPerSession = 5
        ).toSessionPolicy()

        assertEquals(5, custom.reviewItemLimit)
        assertEquals(20, preset.reviewItemLimit)
    }
}
