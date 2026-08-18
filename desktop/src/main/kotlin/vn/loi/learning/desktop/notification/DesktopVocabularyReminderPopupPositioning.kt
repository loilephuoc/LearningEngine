package vn.loi.learning.desktop.notification

import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.Rectangle
import java.awt.Toolkit
import kotlin.math.roundToInt

data class DesktopReminderMonitor(
    val id: String,
    val displayName: String,
    val bounds: Rectangle,
    val usableBounds: Rectangle,
    val scaleX: Double,
    val scaleY: Double,
    val isPrimary: Boolean
)

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
    const val DEFAULT_MARGIN_DP = 16

    fun enumerateMonitors(): List<DesktopReminderMonitor> {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val defaultDevice = runCatching { ge.defaultScreenDevice }.getOrNull()
        val devices = runCatching { ge.screenDevices }.getOrNull()
        if (devices.isNullOrEmpty()) {
            val geom = resolveGeometry(null)
            val logicalWidth = (geom.deviceBounds.width / geom.scaleX).roundToInt()
            val logicalHeight = (geom.deviceBounds.height / geom.scaleY).roundToInt()
            val bounds = Rectangle(0, 0, logicalWidth, logicalHeight)
            val insets = geom.deviceInsets.toLogical(geom.scaleX, geom.scaleY)
            val usable = Rectangle(
                insets.left,
                insets.top,
                (logicalWidth - insets.left - insets.right).coerceAtLeast(100),
                (logicalHeight - insets.top - insets.bottom).coerceAtLeast(100)
            )
            return listOf(
                DesktopReminderMonitor(
                    id = "primary",
                    displayName = "Primary (${logicalWidth}x${logicalHeight})",
                    bounds = bounds,
                    usableBounds = usable,
                    scaleX = geom.scaleX,
                    scaleY = geom.scaleY,
                    isPrimary = true
                )
            )
        }
        return devices.mapIndexed { index, device ->
            val config = device.defaultConfiguration
            val bounds = config.bounds
            val insets = runCatching { Toolkit.getDefaultToolkit().getScreenInsets(config) }.getOrDefault(Insets(0, 0, 0, 0))
            val usableBounds = Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                (bounds.width - insets.left - insets.right).coerceAtLeast(100),
                (bounds.height - insets.top - insets.bottom).coerceAtLeast(100)
            )
            val transform = config.defaultTransform
            val scaleX = transform.scaleX.takeIf { it > 0.0 } ?: 1.0
            val scaleY = transform.scaleY.takeIf { it > 0.0 } ?: 1.0
            val isPrimary = (device == defaultDevice) || (index == 0 && bounds.x == 0 && bounds.y == 0)
            val name = if (isPrimary) "Primary (${bounds.width}x${bounds.height})" else "Display ${index + 1} (${bounds.width}x${bounds.height})"
            val idStr = runCatching { device.iDstring }.getOrNull()?.takeIf { it.isNotBlank() } ?: "display-$index"
            DesktopReminderMonitor(
                id = idStr,
                displayName = name,
                bounds = bounds,
                usableBounds = usableBounds,
                scaleX = scaleX,
                scaleY = scaleY,
                isPrimary = isPrimary
            )
        }
    }

    fun resolveMonitor(monitorId: String?, monitors: List<DesktopReminderMonitor> = enumerateMonitors()): DesktopReminderMonitor {
        if (monitors.isEmpty()) return enumerateMonitors().first()
        if (monitorId != null) {
            monitors.firstOrNull { it.id == monitorId }?.let { return it }
        }
        return monitors.firstOrNull { it.isPrimary } ?: monitors.first()
    }

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

    fun bottomRight(
        monitor: DesktopReminderMonitor,
        requestedWidthDp: Int,
        requestedHeightDp: Int,
        marginDp: Int = DEFAULT_MARGIN_DP
    ): DesktopVocabularyReminderPopupPlacement {
        val usable = monitor.usableBounds
        val width = requestedWidthDp.coerceAtMost(usable.width.coerceAtLeast(1))
        val height = requestedHeightDp.coerceAtMost(usable.height.coerceAtLeast(1))
        return DesktopVocabularyReminderPopupPlacement(
            xDp = (usable.x + usable.width - width - marginDp).coerceAtLeast(usable.x),
            yDp = (usable.y + usable.height - height - marginDp).coerceAtLeast(usable.y),
            widthDp = width,
            heightDp = height
        )
    }

    fun resolveCustomPosition(
        monitor: DesktopReminderMonitor,
        normalizedX: Double,
        normalizedY: Double,
        requestedWidthDp: Int,
        requestedHeightDp: Int,
        marginDp: Int = DEFAULT_MARGIN_DP
    ): DesktopVocabularyReminderPopupPlacement {
        val usable = monitor.usableBounds
        val width = requestedWidthDp.coerceAtMost(usable.width.coerceAtLeast(1))
        val height = requestedHeightDp.coerceAtMost(usable.height.coerceAtLeast(1))
        val maxX = (usable.width - width).coerceAtLeast(0)
        val maxY = (usable.height - height).coerceAtLeast(0)
        val rawX = (usable.x + normalizedX.coerceIn(0.0, 1.0) * maxX).roundToInt()
        val rawY = (usable.y + normalizedY.coerceIn(0.0, 1.0) * maxY).roundToInt()
        return DesktopVocabularyReminderPopupPlacement(
            xDp = rawX.coerceIn(usable.x, usable.x + maxX),
            yDp = rawY.coerceIn(usable.y, usable.y + maxY),
            widthDp = width,
            heightDp = height
        )
    }

    fun calculateNormalizedPosition(
        logicalX: Int,
        logicalY: Int,
        widthDp: Int,
        heightDp: Int,
        monitors: List<DesktopReminderMonitor> = enumerateMonitors()
    ): Pair<String, Pair<Double, Double>> {
        val centerX = logicalX + widthDp / 2
        val centerY = logicalY + heightDp / 2
        val targetMonitor = monitors.firstOrNull { it.bounds.contains(centerX, centerY) }
            ?: monitors.maxByOrNull { m ->
                val overlapX = maxOf(0, minOf(logicalX + widthDp, m.bounds.x + m.bounds.width) - maxOf(logicalX, m.bounds.x))
                val overlapY = maxOf(0, minOf(logicalY + heightDp, m.bounds.y + m.bounds.height) - maxOf(logicalY, m.bounds.y))
                overlapX * overlapY
            }
            ?: resolveMonitor(null, monitors)

        val usable = targetMonitor.usableBounds
        val clampedX = logicalX.coerceIn(usable.x, (usable.x + usable.width - widthDp).coerceAtLeast(usable.x))
        val clampedY = logicalY.coerceIn(usable.y, (usable.y + usable.height - heightDp).coerceAtLeast(usable.y))

        val maxX = (usable.width - widthDp).coerceAtLeast(1)
        val maxY = (usable.height - heightDp).coerceAtLeast(1)

        val normX = ((clampedX - usable.x).toDouble() / maxX).coerceIn(0.0, 1.0)
        val normY = ((clampedY - usable.y).toDouble() / maxY).coerceIn(0.0, 1.0)

        return targetMonitor.id to (normX to normY)
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
}
