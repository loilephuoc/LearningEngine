package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

@Immutable
internal data class TypingFieldMeasuredBounds(
    val outer: Rect? = null,
    val label: Rect? = null,
    val inner: Rect? = null,
    val trailingAction: Rect? = null
)

internal class TypingFieldBoundsAudit(
    val onOuter: (Rect) -> Unit,
    val onLabel: (Rect) -> Unit,
    val onInner: (Rect) -> Unit,
    val onTrailingAction: (Rect) -> Unit
)

@Composable
internal fun TypingFieldMeasuredLayout(
    metrics: TypingFieldLayoutMetrics,
    horizontalInset: Dp,
    modifier: Modifier = Modifier,
    audit: TypingFieldBoundsAudit? = null,
    label: @Composable (Modifier) -> Unit,
    inner: @Composable (Modifier) -> Unit,
    trailingAction: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = metrics.outerMinimumHeightDp.dp)
            .then(audit?.let { Modifier.onGloballyPositioned { c -> it.onOuter(c.boundsInRoot()) } } ?: Modifier)
            .padding(
                start = horizontalInset,
                top = metrics.topInsetDp.dp,
                end = horizontalInset,
                bottom = metrics.bottomInsetDp.dp
            )
    ) {
        label(
            Modifier
                .heightIn(min = metrics.labelHeightDp.dp)
                .then(audit?.let { Modifier.onGloballyPositioned { c -> it.onLabel(c.boundsInRoot()) } } ?: Modifier)
        )
        Spacer(Modifier.height(metrics.labelToInputGapDp.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxOf(metrics.lineBoxHeightDp, metrics.trailingActionDiameterDp).dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            inner(
                Modifier
                    .weight(1f)
                    .heightIn(min = metrics.lineBoxHeightDp.dp)
                    .then(audit?.let { Modifier.onGloballyPositioned { c -> it.onInner(c.boundsInRoot()) } } ?: Modifier)
            )
            trailingAction(
                Modifier
                    .size(metrics.trailingActionDiameterDp.dp)
                    .then(audit?.let { Modifier.onGloballyPositioned { c -> it.onTrailingAction(c.boundsInRoot()) } } ?: Modifier)
            )
        }
    }
}
