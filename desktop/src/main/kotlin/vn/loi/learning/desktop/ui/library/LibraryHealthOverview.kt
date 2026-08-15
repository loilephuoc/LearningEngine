package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.contentlibrary.LibraryHealthOverviewState
import vn.loi.learning.desktop.ui.contentlibrary.toPresentation

@Composable
fun LibraryHealthOverview(
    state: LibraryHealthOverviewState,
    onCheck: () -> Unit,
    onOpenReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = "Library Health. ${state.results.size} packages checked. " +
        "${state.healthyPackages} healthy. ${state.warningPackages} with warnings. " +
        "${state.errorPackages} with errors. ${state.failedPackages} checks failed."
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Library Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Read-only integrity overview for installed packages", style = MaterialTheme.typography.bodySmall)
                }
                if (state.hasResult) {
                    OutlinedButton(onClick = onCheck, enabled = !state.scanning) {
                        Text(if (state.scanning) "Checking…" else "Check Again")
                    }
                } else {
                    Button(onClick = onCheck, enabled = !state.scanning) {
                        Text(if (state.scanning) "Checking…" else "Check Library Health")
                    }
                }
            }

            when {
                state.scanning && !state.hasResult -> Text("Checking installed packages…")
                state.failure != null -> Text("Library health check failed: ${state.failure}", color = MaterialTheme.colorScheme.error)
                state.hasResult && state.results.isEmpty() -> Text("No installed packages to check.")
                state.hasResult -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                            contentDescription = summary
                        },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("${state.results.size} packages checked", fontWeight = FontWeight.SemiBold)
                        Text("Healthy ${state.healthyPackages} · Warnings ${state.warningPackages} · Errors ${state.errorPackages}")
                        if (state.failedPackages > 0) Text("Check failed ${state.failedPackages}")
                        Text("Last scan: ${state.scannedAt}", style = MaterialTheme.typography.bodySmall)
                    }
                    state.results.forEach { result ->
                        val presentation = result.report?.toPresentation()
                        val status = presentation?.statusText ?: "Check failed"
                        val findings = presentation?.summaryText ?: requireNotNull(result.failure)
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = presentation?.accessibilityDescription
                                        ?: "${result.packageName}. Check failed. ${result.failure}"
                                }
                                .then(
                                    if (result.report != null) Modifier.clickable { onOpenReport(result.packageId) }
                                    else Modifier
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(result.packageName, fontWeight = FontWeight.SemiBold)
                                    Text(findings, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(status, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
