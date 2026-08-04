package vn.loi.learning.android.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(state: AndroidLibraryState, onOpenPackage: (String) -> Unit, onSearch: (String) -> Unit, onBack: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp)) {
        when (state) {
            AndroidLibraryState.Loading -> CircularProgressIndicator(Modifier.semantics { contentDescription="Loading library" })
            is AndroidLibraryState.Failed -> Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(state.message, color=MaterialTheme.colorScheme.error, modifier=Modifier.semantics { liveRegion=LiveRegionMode.Assertive })
                if(state.recoverable) Button(onClick=onRetry){Text("Retry")}
            }
            is AndroidLibraryState.Root -> LazyColumn(Modifier.widthIn(max=840.dp).fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                item { Text("Library", style=MaterialTheme.typography.headlineMedium) }
                if(state.tree.collections.isEmpty() && state.tree.installedPackages.isEmpty()) item { Text("No collections or installed packages") }
                items(state.tree.collections, key={"c-${it.collection.id.value}"}) { node ->
                    ListItem(headlineContent={Text(node.collection.name)}, supportingContent={Text("${node.assignedPackages.size} packages")})
                }
                items(state.tree.installedPackages, key={"p-${it.id.value}"}) { pkg ->
                    ListItem(headlineContent={Text(pkg.name)}, supportingContent={Text("v${pkg.version} • ${pkg.contentCount} items")},
                        modifier=Modifier.clickable { onOpenPackage(pkg.id.value) }.semantics { contentDescription="${pkg.name}, ${pkg.state}" })
                }
            }
            is AndroidLibraryState.PackageBrowser -> Column(Modifier.widthIn(max=1000.dp).fillMaxSize(), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row { TextButton(onClick=onBack){Text("Back")}; Text(state.pkg.name, style=MaterialTheme.typography.titleLarge) }
                OutlinedTextField(state.criteria.query, onSearch, label={Text("Search content")}, modifier=Modifier.fillMaxWidth())
                Text("${state.visibleItems.size} items")
                LazyColumn(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    items(state.visibleItems, key={it.contentId.value}) { item ->
                        ListItem(headlineContent={Text(item.questionText)}, supportingContent={Text("${item.answerText} • ${item.lesson}")},
                            trailingContent={Text(buildString { if(item.hasAudio) append("Audio "); if(item.hasImage) append("Image") })})
                    }
                }
            }
        }
    }
}
