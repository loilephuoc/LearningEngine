package vn.loi.learning.desktop.tts.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LETypography

/**
 * Accessible numeric input component with unit label, +/- steppers, instant validation, and zero reset.
 */
@Composable
fun TtsNumericControl(
    label: String,
    value: Int,
    unit: String,
    min: Int = -50,
    max: Int = 50,
    step: Int = 5,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawText by remember(value) { mutableStateOf(if (value > 0) "+$value" else "$value") }
    var isError by remember { mutableStateOf(false) }

    Surface(
        color = LEColors.surface,
        shape = LERadius.xs,
        border = BorderStroke(1.dp, if (isError) LEColors.danger else LEColors.borderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = BatchTtsUiScale.controlSecondary,
                    fontWeight = FontWeight.Bold,
                    color = if (isError) LEColors.danger else LEColors.textMuted
                )
                if (value != 0) {
                    Text(
                        text = "Reset (0)",
                        style = BatchTtsUiScale.controlSecondary,
                        color = LEColors.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(LERadius.xs)
                            .clickable {
                                rawText = "0"
                                isError = false
                                onValueChange(0)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Decrement button
                Surface(
                    color = LEColors.surfaceElevated,
                    shape = LERadius.xs,
                    border = BorderStroke(1.dp, LEColors.borderSubtle),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(LERadius.xs)
                        .clickable(enabled = value > min) {
                            val newVal = (value - step).coerceAtLeast(min)
                            rawText = if (newVal > 0) "+$newVal" else "$newVal"
                            isError = false
                            onValueChange(newVal)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", style = BatchTtsUiScale.controlPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Numeric input field
                BasicTextField(
                    value = rawText,
                    onValueChange = { input ->
                        rawText = input
                        val clean = input.trim().replace("+", "")
                        val parsed = clean.toIntOrNull()
                        if (parsed != null && parsed in min..max) {
                            isError = false
                            onValueChange(parsed)
                        } else {
                            isError = true
                        }
                    },
                    textStyle = BatchTtsUiScale.controlPrimary.copy(
                        color = if (isError) LEColors.danger else LEColors.textPrimary,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Text(unit, style = BatchTtsUiScale.controlSecondary, color = LEColors.textMuted)

                // Increment button
                Surface(
                    color = LEColors.surfaceElevated,
                    shape = LERadius.xs,
                    border = BorderStroke(1.dp, LEColors.borderSubtle),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(LERadius.xs)
                        .clickable(enabled = value < max) {
                            val newVal = (value + step).coerceAtMost(max)
                            rawText = if (newVal > 0) "+$newVal" else "$newVal"
                            isError = false
                            onValueChange(newVal)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", style = BatchTtsUiScale.controlPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isError) {
                Text(
                    text = "Range: $min to +$max $unit",
                    style = BatchTtsUiScale.previewSource,
                    color = LEColors.danger,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
