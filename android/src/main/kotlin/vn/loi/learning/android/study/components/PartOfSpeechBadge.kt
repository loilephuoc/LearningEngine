package vn.loi.learning.android.study.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.loi.learning.android.study.PartOfSpeechPresentation
import vn.loi.learning.android.ui.StudyPartOfSpeechColors

@Composable
internal fun PartOfSpeechBadge(
    presentation: PartOfSpeechPresentation,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    prominent: Boolean = false
) {
    val palette = StudyPartOfSpeechColors.palette
    val colors = palette[presentation.paletteIndex % palette.size]
    val textStyle = when {
        prominent -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        compact -> MaterialTheme.typography.labelSmall
        else -> MaterialTheme.typography.labelMedium
    }
    val paddingValues = when {
        prominent -> PaddingValues(horizontal = 14.dp, vertical = 5.dp)
        compact -> PaddingValues(horizontal = 7.dp, vertical = 2.dp)
        else -> PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.background,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Text(
            presentation.canonicalLabel,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(paddingValues),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            autoSize = if (compact) {
                TextAutoSize.StepBased(
                    minFontSize = 9.sp,
                    maxFontSize = textStyle.fontSize,
                    stepSize = 0.5.sp
                )
            } else {
                null
            }
        )
    }
}
