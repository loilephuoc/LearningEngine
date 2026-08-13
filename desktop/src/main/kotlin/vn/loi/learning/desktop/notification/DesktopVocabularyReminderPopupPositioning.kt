package vn.loi.learning.desktop.notification

import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.Rectangle
import java.awt.Toolkit
import kotlin.math.roundToInt

data class DesktopVocabularyReminderMonitorGeometry(
    val deviceBounds: Rectangle,
    val deviceInsets: Insets,
    val scaleX: Double,
    val scaleY: Double
) {
    init {
        require(scaleX > 0.0 && scaleY > 0.0)
    }
}

data class DesktopVocabularyReminderPopupPlacement(
    val xDp: Int,
    val yDp: Int,
    val widthDp: Int,
    val heightDp: Int
)

object DesktopVocabularyReminderPopupPositioning {
    fun bottomRight(
        geometry: DesktopVocabularyReminderMonitorGeometry,
        requestedWidthDp: Int,
        requestedHeightDp: Int,
        marginDp: Int = DEFAULT_MARGIN_DP
    ): DesktopVocabularyReminderPopupPlacement {
        val bounds = geometry.deviceBounds.toLogical(geometry.scaleX, geometry.scaleY)
        val insets = geometry.deviceInsets.toLogical(geometry.scaleX, geometry.scaleY)
        val usableLeft = bounds.x + insets.left
        val usableTop = bounds.y + insets.top
        val usableRight = bounds.x + bounds.width - insets.right
        val usableBottom = bounds.y + bounds.height - insets.bottom
        val width = requestedWidthDp.coerceAtMost((usableRight - usableLeft).coerceAtLeast(1))
        val height = requestedHeightDp.coerceAtMost((usableBottom - usableTop).coerceAtLeast(1))
        return DesktopVocabularyReminderPopupPlacement(
            xDp = (usableRight - width - marginDp).coerceAtLeast(usableLeft),
            yDp = (usableBottom - height - marginDp).coerceAtLeast(usableTop),
            widthDp = width,
            heightDp = height
        )
    }

    fun resolveGeometry(configuration: GraphicsConfiguration?): DesktopVocabularyReminderMonitorGeometry {
        val resolved = configuration
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        val transform = resolved.defaultTransform
        val scaleX = transform.scaleX.takeIf { it > 0.0 } ?: 1.0
        val scaleY = transform.scaleY.takeIf { it > 0.0 } ?: 1.0
        val logicalBounds = resolved.bounds
        val logicalInsets = Toolkit.getDefaultToolkit().getScreenInsets(resolved)
        return DesktopVocabularyReminderMonitorGeometry(
            deviceBounds = logicalBounds.toDevice(scaleX, scaleY),
            deviceInsets = logicalInsets.toDevice(scaleX, scaleY),
            scaleX = scaleX,
            scaleY = scaleY
        )
    }

    private fun Rectangle.toLogical(scaleX: Double, scaleY: Double) = Rectangle(
        (x / scaleX).roundToInt(),
        (y / scaleY).roundToInt(),
        (width / scaleX).roundToInt(),
        (height / scaleY).roundToInt()
    )

    private fun Insets.toLogical(scaleX: Double, scaleY: Double) = Insets(
        (top / scaleY).roundToInt(),
        (left / scaleX).roundToInt(),
        (bottom / scaleY).roundToInt(),
        (right / scaleX).roundToInt()
    )

    private fun Rectangle.toDevice(scaleX: Double, scaleY: Double) = Rectangle(
        (x * scaleX).roundToInt(),
        (y * scaleY).roundToInt(),
        (width * scaleX).roundToInt(),
        (height * scaleY).roundToInt()
    )

    private fun Insets.toDevice(scaleX: Double, scaleY: Double) = Insets(
        (top * scaleY).roundToInt(),
        (left * scaleX).roundToInt(),
        (bottom * scaleY).roundToInt(),
        (right * scaleX).roundToInt()
    )

    const val DEFAULT_MARGIN_DP = 16
}
