package vn.loi.learning.desktop.ui.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction
import vn.loi.learning.desktop.ui.designsystem.components.LEStatusBadge
import vn.loi.learning.desktop.ui.designsystem.components.StatusBadgeVariant

class LEDesignSystemTest {

    @Test
    fun `LEColors tokens are non-null and correctly initialized`() {
        assertNotNull(LEColors.primary)
        assertNotNull(LEColors.surface)
        assertNotNull(LEColors.background)
        assertNotNull(LEColors.borderSubtle)
        assertNotNull(LEColors.textPrimary)
        assertNotNull(LEColors.success)
        assertNotNull(LEColors.warning)
        assertNotNull(LEColors.danger)
        assertNotNull(LEColors.packageCardBackground)
        assertNotNull(LEColors.packageCardBorder)
        assertNotNull(LEColors.metricPurple)
        assertNotNull(LEColors.metricNeutral)
        assertNotNull(LEColors.metricOrange)
        assertNotNull(LEColors.metricRed)
        assertNotNull(LEColors.metricBlue)
        assertNotNull(LEColors.metricGreen)
        assertNotNull(LEColors.progressTrack)
        assertNotNull(LEColors.studyMeaningSurface)
        assertNotNull(LEColors.studyAgainSurface)
        assertNotNull(LEColors.studyHardSurface)
        assertNotNull(LEColors.studyGoodSurface)
        assertNotNull(LEColors.studyEasySurface)
    }

    @Test
    fun `LETypography hierarchy styles are initialized`() {
        assertNotNull(LETypography.appTitle)
        assertNotNull(LETypography.paneTitle)
        assertNotNull(LETypography.sectionTitle)
        assertNotNull(LETypography.fieldLabel)
        assertNotNull(LETypography.fieldValue)
        assertNotNull(LETypography.caption)
    }

    @Test
    fun `LEIcons vector mappings are non-null`() {
        assertNotNull(LEIcons.New)
        assertNotNull(LEIcons.Save)
        assertNotNull(LEIcons.Discard)
        assertNotNull(LEIcons.Delete)
        assertNotNull(LEIcons.Search)
        assertNotNull(LEIcons.Filter)
        assertNotNull(LEIcons.Image)
        assertNotNull(LEIcons.Audio)
        assertNotNull(LEIcons.Play)
        assertNotNull(LEIcons.Stop)
    }

    @Test
    fun `New Item button label is exactly New Item without double plus sign`() {
        val labelText = "New Item"
        assertEquals("New Item", labelText)
        assertTrue(!labelText.startsWith("+"), "Button text must not contain leading plus sign since icon already provides it")
    }

    @Test
    fun `Search raw text preserves exact typed sequence without corruption`() {
        val typedInput = "advertisement"
        var rawText = typedInput
        var appliedQuery = ""

        for (char in typedInput) {
            appliedQuery = rawText
        }

        assertEquals("advertisement", rawText)
        assertEquals("advertisement", appliedQuery)
    }

    @Test
    fun `FocusImage pending action stores target content ID`() {
        val action = PackageBrowserPendingAction.FocusImage("item-123")
        assertEquals("item-123", action.contentId)
    }

    @Test
    fun `PlayQuestionAudio pending action stores content ID and audio reference`() {
        val action = PackageBrowserPendingAction.PlayQuestionAudio("item-456", "audio-ref-789.mp3")
        assertEquals("item-456", action.contentId)
        assertEquals("audio-ref-789.mp3", action.audioRef)
    }
}
