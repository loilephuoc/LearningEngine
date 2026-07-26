package vn.loi.learning.desktop.ui.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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
}
