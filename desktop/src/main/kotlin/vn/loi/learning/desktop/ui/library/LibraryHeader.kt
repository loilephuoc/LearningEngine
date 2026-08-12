package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.library.query.LibraryStatistics
import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopContentWidthClass
import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopResponsivePolicyResolver

@Composable
fun LibraryHeader(
    libraryName: String,
    statistics: LibraryStatistics,
    onImport: () -> Unit = {},
    onCreateCollection: () -> Unit = {},
    isImporting: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val policy = DesktopResponsivePolicyResolver.resolve(maxWidth.value.toInt())
            val heading: @Composable () -> Unit = {
            Column {
                Text(
                    text = libraryName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Central Content Catalog & Library Hierarchy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
            val actions: @Composable (Modifier) -> Unit = { actionsModifier ->
            FlowRow(
                modifier = actionsModifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImport,
                    enabled = !isImporting
                ) {
                    Text("Import Package")
                }
                Button(onClick = onCreateCollection) {
                    Text("+ New Collection")
                }
            }
            }
            if (policy.widthClass == DesktopContentWidthClass.WIDE) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    heading()
                    actions(Modifier)
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    heading()
                    actions(Modifier.fillMaxWidth())
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val policy = DesktopResponsivePolicyResolver.resolve(maxWidth.value.toInt())
            if (policy.widthClass == DesktopContentWidthClass.NARROW) {
                CompactLibraryStatistics(statistics)
            } else FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 3) {
                val cardModifier = Modifier.weight(1f)
            StatCard(
                title = "Total Packages",
                value = statistics.totalInstalledPackagesCount.toString(),
                subtitle = "${statistics.activePackagesCount} active, ${statistics.archivedPackagesCount} archived",
                modifier = cardModifier
            )
            StatCard(
                title = "Collections",
                value = statistics.activeCollectionsCount.toString(),
                subtitle = "${statistics.deletedCollectionsCount} deleted",
                modifier = cardModifier
            )
            StatCard(
                title = "Active Content",
                value = statistics.totalActiveContentCount.toString(),
                subtitle = "${statistics.totalActiveLearningItemCount} items",
                modifier = cardModifier
            )
            }
        }
    }
}

@Composable
private fun CompactLibraryStatistics(statistics: LibraryStatistics) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactMetric(statistics.totalInstalledPackagesCount.toString(), "Packages")
            CompactMetric(statistics.activeCollectionsCount.toString(), "Collections")
            CompactMetric(statistics.totalActiveContentCount.toString(), "Active")
        }
    }
}

@Composable
private fun CompactMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
