package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import vn.loi.learning.desktop.ui.designsystem.LEBorder
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LESpacing

@Composable
fun DiscoveryFrontSurface(
    model: FocusedVocabularyAnswerModel,
    strings: LearningContentRendererStrings,
    onRevealAnswer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibilityLabel = "New learning item discovery. English answer hidden. Vietnamese meaning: ${model.vietnameseMeaning.ifBlank { "Reveal answer to discover" }}."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LESpacing.md)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LESpacing.lg)
    ) {
        Text(
            text = "Khám phá từ mới",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        if (model.imagePath != null) {
            VocabularyImageBlock(
                imagePath = model.imagePath,
                imageDescription = strings.imageDescription
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = LERadius.md,
            color = LEColors.surfaceElevated,
            border = LEBorder.subtle
        ) {
            Column(
                modifier = Modifier.padding(LESpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                Text(
                    text = "NGHĨA TIẾNG VIỆT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = model.vietnameseMeaning.ifBlank { "Xem đáp án để khám phá từ mới" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }

        Button(
            onClick = onRevealAnswer,
            shape = LERadius.md,
            modifier = Modifier.padding(top = LESpacing.sm)
        ) {
            Text(
                text = "Xem đáp án (Space / Enter)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
