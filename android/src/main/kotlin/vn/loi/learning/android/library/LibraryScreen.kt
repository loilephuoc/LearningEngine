package vn.loi.learning.android.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(state: AndroidLibraryState, onOpenPackage: (String) -> Unit, onSearch: (String) -> Unit,
    onGlobalSearch:(String)->Unit, onOpenSearchResult:(String,String)->Unit, onSelect:(String)->Unit, onEdit:()->Unit, onDraft:(AndroidItemDraft)->Unit,
    onSave:()->Unit, onLessons:()->Unit, onStudyPackage:()->Unit, onStudyLesson:(String)->Unit, onStudySelected:()->Unit,
    onBack: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp), contentAlignment=Alignment.TopCenter) {
        when (state) {
            AndroidLibraryState.Loading -> LoadingPlaceholder("Loading library")
            is AndroidLibraryState.Failed -> ElevatedCard(Modifier.widthIn(max=600.dp).fillMaxWidth()) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Library unavailable",style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics { heading() })
                    Text(state.message,color=MaterialTheme.colorScheme.error,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Assertive })
                    if(state.recoverable) Button(onClick=onRetry,modifier=Modifier.defaultMinSize(minHeight=48.dp)){Text("Retry")}
                }
            }
            is AndroidLibraryState.Root -> LazyColumn(Modifier.widthIn(max=840.dp).fillMaxWidth(), state=rememberLazyListState(), contentPadding=PaddingValues(bottom=24.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                item { Text("Library", style=MaterialTheme.typography.headlineMedium, modifier=Modifier.semantics { heading() }) }
                item { SearchField(state.query,onGlobalSearch,"Search library") }
                if(state.query.isNotBlank() && state.results.isEmpty()) item { EmptyState("No results", "Try a different word or phrase.") }
                items(state.results,key={"s-${it.packageId}-${it.item.contentId.value}"}) { result ->
                    ListItem(headlineContent={Text(result.item.questionText)}, supportingContent={Text("${result.item.answerText} • ${result.packageName} • ${result.item.lesson}")},
                        modifier=Modifier.clickable { onOpenSearchResult(result.packageId,result.item.contentId.value) })
                }
                if(state.tree.collections.isEmpty() && state.tree.installedPackages.isEmpty()) item { EmptyState("Your library is empty", "Import a package from Home to begin.") }
                items(state.tree.collections, key={"c-${it.collection.id.value}"}) { node ->
                    ListItem(headlineContent={Text(node.collection.name)}, supportingContent={Text("${node.assignedPackages.size} packages")})
                }
                items(state.tree.installedPackages, key={"p-${it.id.value}"}) { pkg ->
                    ListItem(headlineContent={Text(pkg.name)}, supportingContent={Text("v${pkg.version} • ${pkg.contentCount} items")},
                        modifier=Modifier.clickable { onOpenPackage(pkg.id.value) }.semantics { contentDescription="${pkg.name}, ${pkg.state}" })
                }
            }
            is AndroidLibraryState.PackageBrowser -> Column(Modifier.widthIn(max=1000.dp).fillMaxSize(), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row { TextButton(onClick=onBack){Text("Back")}; Text(state.pkg.name, style=MaterialTheme.typography.titleLarge) }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStudyPackage){Text("Study package")};OutlinedButton(onClick=onLessons){Text("Lessons")}}
                SearchField(state.criteria.query,onSearch,"Search content")
                Text("${state.visibleItems.size} items",style=MaterialTheme.typography.labelLarge)
                val selected=state.allItems.firstOrNull { it.contentId.value==state.selectedContentId }
                AnimatedContent(targetState=selected,contentKey={it?.contentId?.value},transitionSpec={fadeIn() togetherWith fadeOut()},label="library selection") { item ->
                  if(item!=null) {
                    if(state.draft==null) ElevatedCard(Modifier.fillMaxWidth().animateContentSize()) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(item.questionText,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics { heading() });Text(item.answerText);Text(item.pronunciation);Text(item.partOfSpeech)
                        item.exampleText?.let{Text(it)}; item.exampleTranslation?.let{Text(it)}
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStudySelected){Text("Study")};OutlinedButton(onClick=onEdit){Text("Edit")}}
                    }} else Editor(state.draft,onDraft,onSave)
                  }
                }
                LazyColumn(state=rememberLazyListState(),contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    items(state.visibleItems, key={it.contentId.value}) { item ->
                        ListItem(headlineContent={Text(item.questionText)}, supportingContent={Text("${item.answerText} • ${item.lesson}")}, modifier=Modifier.clickable{onSelect(item.contentId.value)},
                            trailingContent={Text(buildString { if(item.hasAudio) append("Audio "); if(item.hasImage) append("Image") })})
                    }
                }
            }
            is AndroidLibraryState.Lessons -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row { TextButton(onClick=onBack){Text("Back")};Text("Lessons — ${state.pkg.name}",style=MaterialTheme.typography.titleLarge) }
                LazyColumn(state=rememberLazyListState(),contentPadding=PaddingValues(bottom=24.dp)) { items(state.lessons,key={"${it.group}-${it.section}-${it.lesson}"}) { lesson ->
                    ListItem(headlineContent={Text(lesson.lesson)},supportingContent={Text("${lesson.itemCount} items")},
                        trailingContent={TextButton(onClick={onStudyLesson(lesson.lesson)}){Text("Study")}})
                }}
            }
            is AndroidLibraryState.StudyStarted -> LoadingPlaceholder("Starting Study")
        }
    }
}

@Composable
private fun SearchField(value:String,onValueChange:(String)->Unit,label:String) {
    val keyboard=LocalSoftwareKeyboardController.current
    OutlinedTextField(value,onValueChange,label={Text(label)},singleLine=true,
        keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={keyboard?.hide()}),
        modifier=Modifier.fillMaxWidth().defaultMinSize(minHeight=56.dp))
}

@Composable
private fun LoadingPlaceholder(label:String)=ElevatedCard(Modifier.widthIn(max=480.dp).fillMaxWidth()) {
    Row(Modifier.padding(24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(Modifier.size(28.dp).semantics { contentDescription=label },strokeWidth=3.dp)
        Text(label,style=MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EmptyState(title:String,detail:String)=Surface(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,tonalElevation=1.dp) {
    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        Text(detail,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun Editor(draft:AndroidItemDraft,onDraft:(AndroidItemDraft)->Unit,onSave:()->Unit)=Column(Modifier.fillMaxWidth().padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    fun update(value:String,field:Int)=onDraft(when(field){0->draft.copy(question=value);1->draft.copy(answer=value);2->draft.copy(pronunciation=value);3->draft.copy(partOfSpeech=value);4->draft.copy(example=value);else->draft.copy(exampleTranslation=value)})
    listOf("Word" to draft.question,"Meaning" to draft.answer,"Pronunciation" to draft.pronunciation,"Part of speech" to draft.partOfSpeech,"Example" to draft.example,"Example translation" to draft.exampleTranslation).forEachIndexed { i,(label,value)->OutlinedTextField(value,{update(it,i)},label={Text(label)},modifier=Modifier.fillMaxWidth()) }
    Button(onClick=onSave,enabled=draft.question.isNotBlank()){Text("Save")}
}
