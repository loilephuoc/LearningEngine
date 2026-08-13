package vn.loi.learning.android.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal const val REVIEW_NAVIGATION_SWIPE_THRESHOLD_DP = 72
internal const val REVIEW_NAVIGATION_INTENT_RATIO = 1.35f

internal enum class ReviewNavigationGesture { NONE, PREVIOUS, NEXT }

internal fun resolveReviewNavigationGesture(delta: Offset, thresholdPx: Float): ReviewNavigationGesture {
    val absX = abs(delta.x)
    val absY = abs(delta.y)
    if (absX >= thresholdPx && absX > absY * REVIEW_NAVIGATION_INTENT_RATIO) {
        return if (delta.x > 0f) ReviewNavigationGesture.PREVIOUS else ReviewNavigationGesture.NEXT
    }
    return if (delta.y <= -thresholdPx && absY > absX * REVIEW_NAVIGATION_INTENT_RATIO) {
        ReviewNavigationGesture.NEXT
    } else ReviewNavigationGesture.NONE
}

@Composable
internal fun ReviewImageNavigationOverlay(
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    gesturesEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    imageContent: @Composable BoxScope.() -> Unit
) {
    var drag = Offset.Zero
    val gestureModifier = if (gesturesEnabled && (canPrevious || canNext)) {
        Modifier.pointerInput(canPrevious, canNext) {
            val threshold = REVIEW_NAVIGATION_SWIPE_THRESHOLD_DP.dp.toPx()
            detectDragGestures(
                onDragStart = { drag = Offset.Zero },
                onDrag = { change, amount ->
                    drag += amount
                    if (abs(drag.x) > threshold || -drag.y > threshold) change.consume()
                },
                onDragEnd = {
                    when (resolveReviewNavigationGesture(drag, threshold)) {
                        ReviewNavigationGesture.PREVIOUS -> if (canPrevious) onPrevious()
                        ReviewNavigationGesture.NEXT -> if (canNext) onNext()
                        ReviewNavigationGesture.NONE -> Unit
                    }
                    drag = Offset.Zero
                },
                onDragCancel = { drag = Offset.Zero }
            )
        }
    } else Modifier
    Box(modifier.then(gestureModifier)) {
        imageContent()
        NavigationEdgeButton(false, canPrevious, onPrevious, Modifier.align(Alignment.CenterStart))
        NavigationEdgeButton(true, canNext, onNext, Modifier.align(Alignment.CenterEnd))
    }
}

internal fun Modifier.reviewNavigationGestures(
    enabled: Boolean,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Modifier = if (!enabled || (!canPrevious && !canNext)) this else pointerInput(canPrevious, canNext) {
    val threshold = REVIEW_NAVIGATION_SWIPE_THRESHOLD_DP.dp.toPx()
    var drag = Offset.Zero
    detectDragGestures(
        onDragStart = { drag = Offset.Zero },
        onDrag = { change, amount ->
            drag += amount
            if (abs(drag.x) > threshold || -drag.y > threshold) change.consume()
        },
        onDragEnd = {
            when (resolveReviewNavigationGesture(drag, threshold)) {
                ReviewNavigationGesture.PREVIOUS -> if (canPrevious) onPrevious()
                ReviewNavigationGesture.NEXT -> if (canNext) onNext()
                ReviewNavigationGesture.NONE -> Unit
            }
            drag = Offset.Zero
        },
        onDragCancel = { drag = Offset.Zero }
    )
}

@Composable
private fun NavigationEdgeButton(next: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (!enabled) {
        Box(
            modifier.size(48.dp).background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.38f), CircleShape
            ).semantics {
                contentDescription = if (next) "Next item" else "Previous item"
                disabled()
            },
            contentAlignment = Alignment.Center
        ) {
            Text(if (next) "›" else "‹", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        }
        return
    }
    IconButton(
        onClick = onClick,
        enabled = true,
        modifier = modifier.size(48.dp).background(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.68f), CircleShape
        ).semantics {
            contentDescription = if (next) "Next item" else "Previous item"
        }
    ) {
        Text(if (next) "›" else "‹", style = MaterialTheme.typography.headlineMedium)
    }
}
