package vn.loi.learning.desktop.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SearchOptionGroup(
    presentation: SearchOptionGroupPresentation,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics { contentDescription = presentation.contentDescription },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = presentation.heading,
            modifier = Modifier.semantics { heading() }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presentation.options.forEach { option ->
                FilterChip(
                    selected = option.selected,
                    onClick = { onOptionSelected(option.label) },
                    label = { Text(option.label) },
                    modifier = Modifier.semantics {
                        contentDescription = option.contentDescription
                    }
                )
            }
        }
    }
}
