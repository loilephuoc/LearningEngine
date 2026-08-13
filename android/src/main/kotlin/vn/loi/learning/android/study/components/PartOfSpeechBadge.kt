package vn.loi.learning.android.study.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.study.PartOfSpeechPresentation
import vn.loi.learning.android.ui.StudyPartOfSpeechColors

@Composable
internal fun PartOfSpeechBadge(
    presentation: PartOfSpeechPresentation,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = StudyPartOfSpeechColors.palette
    val colors = palette[presentation.paletteIndex % palette.size]
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.background,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Text(
            presentation.canonicalLabel,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = if (compact) 7.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp
            )
        )
    }
}
