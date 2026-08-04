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
fun LibraryScreen(state: AndroidLibraryState, onOpenPackage: (String) -> Unit, onSearch: (String) -> Unit,
    onGlobalSearch:(String)->Unit, onOpenSearchResult:(String,String)->Unit, onSelect:(String)->Unit, onEdit:()->Unit, onDraft:(AndroidItemDraft)->Unit,
    onSave:()->Unit, onLessons:()->Unit, onStudyPackage:()->Unit, onStudyLesson:(String)->Unit, onStudySelected:()->Unit,
    onBack: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp)) {
        when (state) {
            AndroidLibraryState.Loading -> CircularProgressIndicator(Modifier.semantics { contentDescription="Loading library" })
            is AndroidLibraryState.Failed -> Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(state.message, color=MaterialTheme.colorScheme.error, modifier=Modifier.semantics { liveRegion=LiveRegionMode.Assertive })
                if(state.recoverable) Button(onClick=onRetry){Text("Retry")}
            }
            is AndroidLibraryState.Root -> LazyColumn(Modifier.widthIn(max=840.dp).fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                item { Text("Library", style=MaterialTheme.typography.headlineMedium) }
                item { OutlinedTextField(state.query,onGlobalSearch,label={Text("Search library")},modifier=Modifier.fillMaxWidth()) }
                if(state.query.isNotBlank() && state.results.isEmpty()) item { Text("No results") }
                items(state.results,key={"s-${it.packageId}-${it.item.contentId.value}"}) { result ->
                    ListItem(headlineContent={Text(result.item.questionText)}, supportingContent={Text("${result.item.answerText} • ${result.packageName} • ${result.item.lesson}")},
                        modifier=Modifier.clickable { onOpenSearchResult(result.packageId,result.item.contentId.value) })
                }
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
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStudyPackage){Text("Study package")};OutlinedButton(onClick=onLessons){Text("Lessons")}}
                OutlinedTextField(state.criteria.query, onSearch, label={Text("Search content")}, modifier=Modifier.fillMaxWidth())
                Text("${state.visibleItems.size} items")
                val selected=state.allItems.firstOrNull { it.contentId.value==state.selectedContentId }
                if(selected!=null) {
                    if(state.draft==null) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(selected.questionText,style=MaterialTheme.typography.headlineSmall);Text(selected.answerText);Text(selected.pronunciation);Text(selected.partOfSpeech)
                        selected.exampleText?.let{Text(it)}; selected.exampleTranslation?.let{Text(it)}
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStudySelected){Text("Study")};OutlinedButton(onClick=onEdit){Text("Edit")}}
                    }} else Editor(state.draft,onDraft,onSave)
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    items(state.visibleItems, key={it.contentId.value}) { item ->
                        ListItem(headlineContent={Text(item.questionText)}, supportingContent={Text("${item.answerText} • ${item.lesson}")}, modifier=Modifier.clickable{onSelect(item.contentId.value)},
                            trailingContent={Text(buildString { if(item.hasAudio) append("Audio "); if(item.hasImage) append("Image") })})
                    }
                }
            }
            is AndroidLibraryState.Lessons -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row { TextButton(onClick=onBack){Text("Back")};Text("Lessons — ${state.pkg.name}",style=MaterialTheme.typography.titleLarge) }
                LazyColumn { items(state.lessons,key={"${it.group}-${it.section}-${it.lesson}"}) { lesson ->
                    ListItem(headlineContent={Text(lesson.lesson)},supportingContent={Text("${lesson.itemCount} items")},
                        trailingContent={TextButton(onClick={onStudyLesson(lesson.lesson)}){Text("Study")}})
                }}
            }
            is AndroidLibraryState.StudyStarted -> CircularProgressIndicator()
        }
    }
}

@Composable private fun Editor(draft:AndroidItemDraft,onDraft:(AndroidItemDraft)->Unit,onSave:()->Unit)=Column(Modifier.fillMaxWidth().padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    fun update(value:String,field:Int)=onDraft(when(field){0->draft.copy(question=value);1->draft.copy(answer=value);2->draft.copy(pronunciation=value);3->draft.copy(partOfSpeech=value);4->draft.copy(example=value);else->draft.copy(exampleTranslation=value)})
    listOf("Word" to draft.question,"Meaning" to draft.answer,"Pronunciation" to draft.pronunciation,"Part of speech" to draft.partOfSpeech,"Example" to draft.example,"Example translation" to draft.exampleTranslation).forEachIndexed { i,(label,value)->OutlinedTextField(value,{update(it,i)},label={Text(label)},modifier=Modifier.fillMaxWidth()) }
    Button(onClick=onSave,enabled=draft.question.isNotBlank()){Text("Save")}
}
