package vn.loi.learning.android.library

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioState
import vn.loi.learning.android.ui.androidDisplayTitle
import vn.loi.learning.android.ui.*
import vn.loi.learning.android.platform.AndroidContentOperationState

@Composable
fun LibraryScreen(state: AndroidLibraryState, onOpenPackage: (String) -> Unit, onSearch: (String) -> Unit,
    onGlobalSearch:(String)->Unit, onOpenSearchResult:(String,String)->Unit, onSelect:(String)->Unit, onEdit:()->Unit, onDraft:(AndroidItemDraft)->Unit,
    onSave:()->Unit, onLessons:()->Unit, onStudyPackage:()->Unit, onStudyLesson:(String)->Unit, onStudySelected:()->Unit,
    onBack: () -> Unit, onRetry: () -> Unit, resolveMedia:(String)->String?={null},
    operationMessage:String?=null,onExport:(String)->Unit={},onVerify:()->Unit={},onUninstall:(String)->Unit={},
    contentState: AndroidContentOperationState = AndroidContentOperationState.Idle,
    onImport: () -> Unit = {},
    onFilter: (AndroidLibraryFilter) -> Unit = {}, onOpenCollection: (String) -> Unit = {},
    onSelectLearningPackage: (String) -> Unit = {}) {
    Box(Modifier.fillMaxSize().imePadding().padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.medium), contentAlignment=Alignment.TopCenter) {
        when (state) {
            AndroidLibraryState.Loading -> LoadingPlaceholder("Loading library")
            is AndroidLibraryState.Failed -> ErrorState(state,onRetry)
            is AndroidLibraryState.Root -> LazyColumn(Modifier.widthIn(max=840.dp).fillMaxWidth(),state=rememberLazyListState(),contentPadding=PaddingValues(bottom=LearningSpacing.extraLarge),verticalArrangement=Arrangement.spacedBy(LearningSpacing.medium)) {
                item("top-bar") { LibraryTopBar(onImport, contentState is AndroidContentOperationState.Running) }
                item("search") { SearchField(state.query,onGlobalSearch,"Search packages and collections") }
                item("filters") { LibraryFilterRow(state.filter,onFilter) }
                item("import-state") { ImportState(contentState,onImport) }
                if(state.allCollections.isEmpty() && state.allPackages.isEmpty()) item("empty") {
                    LearningEngineEmptyState("Your library is empty","Import a package to begin.",actionLabel="Import package",onAction=onImport)
                } else if(state.collections.isEmpty() && state.packages.isEmpty()) item("no-results") {
                    LearningEngineEmptyState("No results","Try a different package or collection name.")
                }
                if(state.collections.isNotEmpty()) item("collections-heading") { LearningEngineSectionHeader("Collections") }
                items(state.collections,key={"collection-${it.collectionId}"}) { collection -> CollectionCard(collection) { onOpenCollection(collection.collectionId) } }
                if(state.packages.isNotEmpty()) item("packages-heading") { LearningEngineSectionHeader(if(state.selectedCollectionId==null)"Packages" else "Collection packages") }
                items(state.packages,key={"package-${it.packageId}"}) { pkg ->
                    LibraryPackageCard(pkg, { onOpenPackage(pkg.packageId) }) { onSelectLearningPackage(pkg.packageId) }
                }
            }
            is AndroidLibraryState.PackageBrowser -> Column(Modifier.widthIn(max=1000.dp).fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                var showOperations by remember { mutableStateOf(false) }
                var confirmUninstall by remember { mutableStateOf(false) }
                ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")};Text(androidDisplayTitle(state.pkg.name),style=MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f).semantics { contentDescription=state.pkg.name;heading() });Box{IconButton(onClick={showOperations=true}){Icon(Icons.Default.MoreVert,"Package operations")};DropdownMenu(showOperations,{showOperations=false}){DropdownMenuItem({Text("Export")},{showOperations=false;onExport(state.pkg.id)});DropdownMenuItem({Text("Verify package file")},{showOperations=false;onVerify()});DropdownMenuItem({Text("Uninstall")},{showOperations=false;confirmUninstall=true},leadingIcon={Icon(Icons.Default.Delete,null)})}}}
                    Text("v${state.pkg.version} • ${state.allItems.size} items",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStudyPackage,modifier=Modifier.weight(1f).semantics { contentDescription="Study package ${state.pkg.name}" }){Icon(Icons.Default.School,null);Spacer(Modifier.width(8.dp));Text("Study")};OutlinedButton(onClick=onLessons){Text("Lessons")}}
                } }
                operationMessage?.let{Text(it,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite },color=MaterialTheme.colorScheme.primary)}
                if(confirmUninstall) AlertDialog(onDismissRequest={confirmUninstall=false},title={Text("Uninstall package?")},text={Text("This removes ${androidDisplayTitle(state.pkg.name)} and its local learning data.")},confirmButton={Button(onClick={confirmUninstall=false;onUninstall(state.pkg.id)}){Text("Uninstall")}},dismissButton={TextButton(onClick={confirmUninstall=false}){Text("Cancel")}})
                SearchField(state.criteria.query,onSearch,"Search content")
                Text("${state.visibleItems.size} results",style=MaterialTheme.typography.labelLarge,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                val selected=state.allItems.firstOrNull { it.contentId.value==state.selectedContentId }
                AnimatedContent(selected,contentKey={it?.contentId?.value},transitionSpec={fadeIn() togetherWith fadeOut()},label="item detail") { item ->
                    if(item!=null) if(state.draft==null) ItemDetail(item,onStudySelected,onEdit,resolveMedia) else Editor(state.draft,onDraft,onSave)
                }
                LazyColumn(state=rememberLazyListState(),contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    items(state.visibleItems,key={it.contentId.value}) { item -> ContentCard(item,{onSelect(item.contentId.value)},resolveMedia) }
                }
            }
            is AndroidLibraryState.Lessons -> Column(Modifier.widthIn(max=840.dp).fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")};Text("Lessons — ${androidDisplayTitle(state.pkg.name)}",style=MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis)}
                LazyColumn(state=rememberLazyListState(),contentPadding=PaddingValues(bottom=24.dp)){items(state.lessons,key={"${it.group}-${it.section}-${it.lesson}"}){lesson->ListItem(headlineContent={Text(lesson.lesson)},supportingContent={Text("${lesson.itemCount} items")},trailingContent={TextButton(onClick={onStudyLesson(lesson.lesson)}){Text("Study")}})}}
            }
            is AndroidLibraryState.StudyStarted -> LoadingPlaceholder("Starting Study")
        }
    }
}

@Composable
private fun LibraryTopBar(onImport: () -> Unit, importRunning: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Library", style = LearningTextRole.screenTitle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() })
            Text("Packages and collections", style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = onImport, enabled = !importRunning,
            shape = LearningEngineShapes.medium, modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)) {
            Icon(Icons.Default.Add, null, Modifier.size(LearningIconSize.inline)); Spacer(Modifier.width(6.dp)); Text("Import")
        }
    }
}

@Composable
private fun LibraryFilterRow(selected: AndroidLibraryFilter, onFilter: (AndroidLibraryFilter) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        AndroidLibraryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilter(filter) },
                label = { Text(filter.name.lowercase().replaceFirstChar(Char::uppercase)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics {
                    stateDescription = if (selected == filter) "Selected" else "Not selected"
                }
            )
        }
    }
}

@Composable
private fun CollectionCard(collection: AndroidLibraryCollectionItem, onOpen: () -> Unit) {
    LibrarySurfaceCard(Modifier.fillMaxWidth().clickable(onClick = onOpen).semantics(mergeDescendants = true) {
        contentDescription = "${collection.title}, ${collection.packageCount} packages"
        role = Role.Button
    }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
            Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(collection.title, style = MaterialTheme.typography.titleMedium)
                Text("${collection.packageCount} package(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, "Open collection")
        }
    }
}

@Composable
private fun LibraryPackageCard(pkg: AndroidLibraryPackageItem, onOpen: () -> Unit, onSelectLearningPackage: () -> Unit) {
    val description = "${pkg.title}, version ${pkg.version}, ${pkg.contentCount} contents, ${pkg.status.lowercase()}"
    LibrarySurfaceCard(Modifier.fillMaxWidth().defaultMinSize(minHeight = LearningSpacing.touchTarget)) {
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen)
            .semantics(mergeDescendants = true) { contentDescription = description; role = Role.Button },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(40.dp), MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                Text(androidDisplayTitle(pkg.title), style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text("v${pkg.version} · ${pkg.contentCount} contents", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                    LearningEngineStatusBadge(if (pkg.isUsable) "Available" else pkg.status.lowercase().replaceFirstChar(Char::uppercase), LearningStatusTone.INFO)
                    if(pkg.isActivePackage) Text("Current learning package", style = MaterialTheme.typography.labelMedium)
                }
            }
            Icon(Icons.Default.ChevronRight, "Open package")
        }
        if (pkg.isUsable && !pkg.isActivePackage) {
            TextButton(onClick = onSelectLearningPackage) { Text("Use for Study") }
        }
        }
    }
}

@Composable
private fun ImportState(state: AndroidContentOperationState, onRetry: () -> Unit) {
    when (state) {
        is AndroidContentOperationState.Running -> LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = "Importing package" })
        is AndroidContentOperationState.Succeeded -> Text(state.detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        is AndroidContentOperationState.Failed -> LearningEngineErrorState("Import failed", state.failure.message, onRetry = onRetry)
        AndroidContentOperationState.Idle -> Unit
    }
}

@Composable private fun ContentCard(item:vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem,onClick:()->Unit,resolveMedia:(String)->String?) = ElevatedCard(Modifier.fillMaxWidth().clickable(onClick=onClick)) { Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    item.imageRef?.let{MediaThumbnail(it,resolveMedia,Modifier.size(72.dp))} ?: Icon(Icons.Default.ImageNotSupported,"No image",Modifier.size(48.dp))
    Column(Modifier.weight(1f)){Text(item.questionText,style=MaterialTheme.typography.titleMedium);Text(item.answerText);Text("${item.partOfSpeech} • ${item.lesson}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    Row{if(item.hasAudio)Icon(Icons.AutoMirrored.Filled.VolumeUp,"Audio available",Modifier.semantics { stateDescription="Available" });if(item.hasImage)Icon(Icons.Default.Image,"Image available")}
} }

@Composable private fun ItemDetail(item:vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem,onStudy:()->Unit,onEdit:()->Unit,resolveMedia:(String)->String?) = ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(item.questionText,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics { heading() });Text(item.answerText);if(item.pronunciation.isNotBlank())Text(item.pronunciation);if(item.partOfSpeech.isNotBlank())Text(item.partOfSpeech)
    item.imageRef?.let{MediaThumbnail(it,resolveMedia,Modifier.fillMaxWidth().heightIn(max=280.dp))};item.exampleText?.let{Text(it)};item.exampleTranslation?.let{Text(it)}
    val controller=remember{AndroidAudioController()};DisposableEffect(Unit){onDispose{controller.close()}};var audio by remember(item.contentId){mutableStateOf<AndroidAudioState>(AndroidAudioState.Idle)}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStudy){Text("Study this item")};OutlinedButton(onClick=onEdit){Text("Edit")};if(item.hasAudio)IconButton(onClick={audio=controller.replay(item.audioRef?.let(resolveMedia)){audio=it}},modifier=Modifier.semantics { stateDescription=audio.javaClass.simpleName }){Icon(Icons.AutoMirrored.Filled.VolumeUp,"Play audio")}}
} }

@Composable private fun MediaThumbnail(reference:String,resolveMedia:(String)->String?,modifier:Modifier=Modifier){val bitmap by produceState<android.graphics.Bitmap?>(null,reference){value=withContext(Dispatchers.IO){val path=resolveMedia(reference)?:return@withContext null;val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(path,bounds);var sample=1;while(bounds.outWidth/sample>320||bounds.outHeight/sample>320)sample*=2;BitmapFactory.decodeFile(path,BitmapFactory.Options().apply{inSampleSize=sample})}};if(bitmap==null)Surface(modifier,shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceVariant){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.ImageNotSupported,"Image unavailable")}}else Image(bitmap!!.asImageBitmap(),"Content image",modifier,contentScale=ContentScale.Crop)}
@Composable private fun SearchField(value:String,onValueChange:(String)->Unit,label:String){val keyboard=LocalSoftwareKeyboardController.current;TextField(value,onValueChange,placeholder={Text(label)},leadingIcon={Icon(Icons.Default.Search,null,Modifier.size(LearningIconSize.action))},singleLine=true,shape=LearningEngineShapes.medium,colors=TextFieldDefaults.colors(focusedContainerColor=MaterialTheme.colorScheme.surfaceVariant,unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant,disabledContainerColor=MaterialTheme.colorScheme.surfaceVariant,focusedIndicatorColor=androidx.compose.ui.graphics.Color.Transparent,unfocusedIndicatorColor=androidx.compose.ui.graphics.Color.Transparent,disabledIndicatorColor=androidx.compose.ui.graphics.Color.Transparent,cursorColor=MaterialTheme.colorScheme.primary),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={keyboard?.hide()}),trailingIcon={if(value.isNotEmpty())IconButton(onClick={onValueChange("")}){Icon(Icons.Default.Clear,"Clear search")}},modifier=Modifier.fillMaxWidth().defaultMinSize(minHeight=LearningSpacing.touchTarget))}

@Composable
private fun LibrarySurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    ElevatedCard(
        modifier = modifier,
        shape = LearningEngineShapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = LearningElevation.card)
    ) {
        Column(
            Modifier.padding(LearningSpacing.large),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.small),
            content = content
        )
    }
@Composable private fun LoadingPlaceholder(label:String)=ElevatedCard(Modifier.widthIn(max=480.dp).fillMaxWidth()){Row(Modifier.padding(24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){CircularProgressIndicator(Modifier.size(28.dp).semantics { contentDescription=label },strokeWidth=3.dp);Text(label,style=MaterialTheme.typography.titleMedium)}}
@Composable private fun EmptyState(title:String,detail:String)=Surface(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,tonalElevation=1.dp){Column(Modifier.padding(20.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(detail,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun ErrorState(state:AndroidLibraryState.Failed,onRetry:()->Unit)=ElevatedCard(Modifier.widthIn(max=600.dp).fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Library unavailable",style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics { heading() });Text(state.message,color=MaterialTheme.colorScheme.error,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Assertive });if(state.recoverable)Button(onClick=onRetry){Text("Retry")}}}
@Composable
private fun Editor(draft:AndroidItemDraft,onDraft:(AndroidItemDraft)->Unit,onSave:()->Unit) {
    fun update(value:String,field:Int) = onDraft(when(field){0->draft.copy(question=value);1->draft.copy(answer=value);2->draft.copy(pronunciation=value);3->draft.copy(partOfSpeech=value);4->draft.copy(example=value);else->draft.copy(exampleTranslation=value)})
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        listOf("Word" to draft.question,"Meaning" to draft.answer,"Pronunciation" to draft.pronunciation,"Part of speech" to draft.partOfSpeech,"Example" to draft.example,"Example translation" to draft.exampleTranslation).forEachIndexed { index, field ->
            OutlinedTextField(field.second,{update(it,index)},label={Text(field.first)},modifier=Modifier.fillMaxWidth())
        }
        Button(onClick=onSave,enabled=draft.question.isNotBlank()){Text("Save")}
    }
}
