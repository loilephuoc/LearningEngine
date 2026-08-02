package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TypingFieldMeasuredLayoutTest {
    @Test
    fun `actual measured label inner and action bounds remain inside outer minimum`() =
        assertMeasuredBounds(viewportWidthDp = 520)

    @Test
    fun `wide viewport does not reduce actual typing height`() {
        val compact = measure(viewportWidthDp = 520)
        val wide = measure(viewportWidthDp = 1_200)

        val wideOuter = assertNotNull(wide.outer)
        val compactOuter = assertNotNull(compact.outer)
        assertTrue(wideOuter.height >= compactOuter.height)
        assertEquals(wide.metrics.outerMinimumHeightDp.toFloat(), wideOuter.height)
    }

    private fun assertMeasuredBounds(viewportWidthDp: Int) {
        val measured = measure(viewportWidthDp)
        val outer = assertNotNull(measured.outer)
        val label = assertNotNull(measured.label)
        val inner = assertNotNull(measured.inner)
        val action = assertNotNull(measured.trailingAction)

        assertTrue(outer.height >= measured.metrics.outerMinimumHeightDp)
        assertTrue(inner.height >= measured.metrics.lineBoxHeightDp)
        assertTrue(outer.containsBounds(label))
        assertTrue(outer.containsBounds(inner))
        assertTrue(outer.containsBounds(action))
    }

    private fun measure(viewportWidthDp: Int): Measurement {
        val metrics = TypingFieldLayoutMetricsResolver.resolve(viewportWidthDp)
        var bounds = TypingFieldMeasuredBounds()
        runComposeUiTest {
            setContent {
                TypingFieldMeasuredLayout(
                    metrics = metrics,
                    horizontalInset = 16.dp,
                    modifier = Modifier.width(viewportWidthDp.dp),
                    audit = TypingFieldBoundsAudit(
                        onOuter = { bounds = bounds.copy(outer = it) },
                        onLabel = { bounds = bounds.copy(label = it) },
                        onInner = { bounds = bounds.copy(inner = it) },
                        onTrailingAction = { bounds = bounds.copy(trailingAction = it) }
                    ),
                    label = { Text("Câu trả lời của bạn", modifier = it) },
                    inner = { Text("gjpqy", modifier = it) },
                    trailingAction = { Box(it) }
                )
            }
            waitForIdle()
        }
        return Measurement(metrics, bounds.outer, bounds.label, bounds.inner, bounds.trailingAction)
    }

    private data class Measurement(
        val metrics: TypingFieldLayoutMetrics,
        val outer: Rect?,
        val label: Rect?,
        val inner: Rect?,
        val trailingAction: Rect?
    )

    private fun Rect.containsBounds(other: Rect): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom
}
