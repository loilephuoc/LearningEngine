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
}
