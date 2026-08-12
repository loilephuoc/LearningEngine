package vn.loi.learning.desktop.ui.designsystem.responsive

enum class DesktopContentWidthClass { NARROW, MEDIUM, WIDE }

data class DesktopResponsivePolicy(
    val widthClass: DesktopContentWidthClass,
    val stackPrimaryContent: Boolean,
    val metricColumns: Int
)

object DesktopResponsivePolicyResolver {
    const val NARROW_MAX_WIDTH_DP = 619
    const val MEDIUM_MAX_WIDTH_DP = 999

    fun resolve(availableWidthDp: Int): DesktopResponsivePolicy = when {
        availableWidthDp <= NARROW_MAX_WIDTH_DP ->
            DesktopResponsivePolicy(DesktopContentWidthClass.NARROW, true, 1)
        availableWidthDp <= MEDIUM_MAX_WIDTH_DP ->
            DesktopResponsivePolicy(DesktopContentWidthClass.MEDIUM, true, 2)
        else -> DesktopResponsivePolicy(DesktopContentWidthClass.WIDE, false, 4)
    }
}
