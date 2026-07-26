package vn.loi.learning.desktop.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import vn.loi.learning.desktop.ui.designsystem.LEColors

/**
 * Reusable Canvas-based audio waveform composable.
 * Generates deterministic amplitudes from asset reference hash when raw PCM sample data is unavailable.
 */
@Composable
fun LEWaveform(
    audioRef: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    height: Dp = 24.dp
) {
    val amplitudes = remember(audioRef, barCount) {
        generateDeterministicAmplitudes(audioRef, barCount)
    }

    val activeColor = LEColors.waveformActive
    val inactiveColor = LEColors.waveformInactive

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 2.dp)
    ) {
        if (amplitudes.isEmpty()) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = (canvasWidth / barCount) * 0.55f
        val gap = (canvasWidth - (barWidth * barCount)) / (barCount + 1)

        for (i in 0 until barCount) {
            val amplitude = amplitudes[i]
            val barHeight = (canvasHeight * amplitude).coerceIn(4f, canvasHeight)
            val x = gap + i * (barWidth + gap)
            val y = (canvasHeight - barHeight) / 2f

            // Dynamic active highlight when playing
            val color = if (isPlaying && i < (barCount * 0.45f)) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

private fun generateDeterministicAmplitudes(seedString: String?, count: Int): FloatArray {
    val hash = abs(seedString?.hashCode() ?: 42)
    val result = FloatArray(count)
    for (i in 0 until count) {
        val val1 = (hash * (i + 1) * 31) % 100
        val val2 = (hash * (i + 7) * 17) % 100
        val combined = (val1 + val2) / 200f
        result[i] = 0.2f + (combined * 0.75f)
    }
    return result
}
