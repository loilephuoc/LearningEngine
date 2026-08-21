package vn.loi.learning.android.library

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.android.R
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioState
import vn.loi.learning.android.platform.AndroidContentOperationState
import vn.loi.learning.android.ui.*

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
            is AndroidLibraryState.Root -> LazyColumn(Modifier.widthIn(max=840.dp).fillMaxWidth(),state=rememberLazyListState(),contentPadding=PaddingValues(bottom=LearningSpacing.extraLarge),verticalArrangement=Arrangement.spacedBy(LearningSpacing.small)) {
                item("top-bar") { LibraryTopBar(onImport, contentState is AndroidContentOperationState.Running) }
                item("search") { SearchField(state.query,onGlobalSearch,stringResource(R.string.library_search_packages_collections)) }
                item("filters") { LibraryFilterRow(state.filter,onFilter) }
                item("import-state") { ImportState(contentState,onImport) }
                if(state.allCollections.isEmpty() && state.allPackages.isEmpty()) item("empty") {
                    LearningEngineEmptyState(stringResource(R.string.library_empty_title), stringResource(R.string.library_empty_desc), actionLabel = stringResource(R.string.library_import_package), onAction = onImport)
                } else if(state.collections.isEmpty() && state.packages.isEmpty()) item("no-results") {
                    LearningEngineEmptyState(stringResource(R.string.library_empty_no_results), stringResource(R.string.library_empty_no_results_desc))
                }
                if(state.collections.isNotEmpty()) item("collections-heading") { LearningEngineSectionHeader(stringResource(R.string.library_section_collections)) }
                items(state.collections,key={"collection-${it.collectionId}"}) { collection -> CollectionCard(collection) { onOpenCollection(collection.collectionId) } }
                if(state.packages.isNotEmpty()) item("packages-heading") {
                    LearningEngineSectionHeader(if(state.selectedCollectionId==null) stringResource(R.string.library_section_packages) else stringResource(R.string.library_section_collection_packages))
                }
                items(state.packages,key={"package-${it.packageId}"}) { pkg ->
                    LibraryPackageCard(pkg) { onOpenPackage(pkg.packageId) }
                }
            }
            is AndroidLibraryState.PackageBrowser -> Column(Modifier.widthIn(max=1000.dp).fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                var showOperations by remember { mutableStateOf(false) }
                var confirmUninstall by remember { mutableStateOf(false) }
                ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,stringResource(R.string.action_back))};Text(androidDisplayTitle(state.pkg.name),style=MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f).semantics { contentDescription=state.pkg.name;heading() });Box{IconButton(onClick={showOperations=true}){Icon(Icons.Default.MoreVert,stringResource(R.string.library_package_details))};DropdownMenu(showOperations,{showOperations=false}){DropdownMenuItem({Text(stringResource(R.string.library_export))},{showOperations=false;onExport(state.pkg.id)});DropdownMenuItem({Text(stringResource(R.string.library_verify_package))},{showOperations=false;onVerify()});DropdownMenuItem({Text(stringResource(R.string.library_uninstall_action))},{showOperations=false;confirmUninstall=true},leadingIcon={Icon(Icons.Default.Delete,null)})}}}
                    Text("v${state.pkg.version} • ${state.allItems.size} items",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStudyPackage,modifier=Modifier.weight(1f).semantics { contentDescription="Study package ${state.pkg.name}" }){Icon(Icons.Default.School,null);Spacer(Modifier.width(8.dp));Text(stringResource(R.string.library_study))};OutlinedButton(onClick=onLessons){Text(stringResource(R.string.library_lessons))}}
                } }
                operationMessage?.let{Text(it,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite },color=MaterialTheme.colorScheme.primary)}
                if(confirmUninstall) AlertDialog(onDismissRequest={confirmUninstall=false},title={Text(stringResource(R.string.library_uninstall_confirm_title))},text={Text(stringResource(R.string.library_uninstall_confirm_message, androidDisplayTitle(state.pkg.name)))},confirmButton={Button(onClick={confirmUninstall=false;onUninstall(state.pkg.id)}){Text(stringResource(R.string.library_uninstall_action))}},dismissButton={TextButton(onClick={confirmUninstall=false}){Text(stringResource(R.string.action_cancel))}})
                SearchField(state.criteria.query,onSearch,stringResource(R.string.library_search_placeholder))
                Text(stringResource(R.string.library_results_count, state.visibleItems.size),style=MaterialTheme.typography.labelLarge,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                val selected=state.allItems.firstOrNull { it.contentId.value==state.selectedContentId }
                AnimatedContent(selected,contentKey={it?.contentId?.value},transitionSpec={fadeIn() togetherWith fadeOut()},label="item detail") { item ->
                    if(item!=null) if(state.draft==null) ItemDetail(item,onStudySelected,onEdit,resolveMedia) else Editor(state.draft,onDraft,onSave)
                }
                LazyColumn(state=rememberLazyListState(),contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    items(state.visibleItems,key={it.contentId.value}) { item -> ContentCard(item,{onSelect(item.contentId.value)},resolveMedia) }
                }
            }
            is AndroidLibraryState.Lessons -> Column(Modifier.widthIn(max=840.dp).fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,stringResource(R.string.action_back))};Text(stringResource(R.string.library_lessons_title, androidDisplayTitle(state.pkg.name)),style=MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis)}
                LazyColumn(state=rememberLazyListState(),contentPadding=PaddingValues(bottom=24.dp)){items(state.lessons,key={"${it.group}-${it.section}-${it.lesson}"}){lesson->ListItem(headlineContent={Text(lesson.lesson)},supportingContent={Text("${lesson.itemCount} items")},trailingContent={TextButton(onClick={onStudyLesson(lesson.lesson)}){Text(stringResource(R.string.library_study))}})}}
            }
            is AndroidLibraryState.StudyStarted -> LoadingPlaceholder("Starting Study")
        }
    }
}

@Composable
private fun LibraryTopBar(onImport: () -> Unit, importRunning: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.library_title), style = LearningTextRole.screenTitle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.library_subtitle), style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = onImport, enabled = !importRunning,
            shape = LearningEngineShapes.medium, modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)) {
            Icon(Icons.Default.Add, null, Modifier.size(LearningIconSize.inline)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.library_import_package))
        }
    }
}

@Composable
private fun LibraryFilterRow(selected: AndroidLibraryFilter, onFilter: (AndroidLibraryFilter) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        AndroidLibraryFilter.entries.forEach { filter ->
            val label = when (filter) {
                AndroidLibraryFilter.ALL -> stringResource(R.string.library_tab_all)
                AndroidLibraryFilter.COLLECTIONS -> stringResource(R.string.library_tab_collections)
                AndroidLibraryFilter.PACKAGES -> stringResource(R.string.library_tab_packages)
            }
            FilterChip(
                selected = selected == filter,
                onClick = { onFilter(filter) },
                label = { Text(label) },
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
private fun LibraryPackageCard(pkg: AndroidLibraryPackageItem, onOpen: () -> Unit) {
    val activeSemantics = if (pkg.isActivePackage) stringResource(R.string.library_active_semantics) else ""
    val contentDesc = if (pkg.isActivePackage) {
        "${pkg.title}, $activeSemantics, ${pkg.contentCount} contents"
    } else {
        "${pkg.title}, ${pkg.contentCount} contents"
    }

    val containerColor = if (pkg.isActivePackage) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val cardBorder = if (pkg.isActivePackage) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LearningSpacing.touchTarget)
            .clickable(onClick = onOpen)
            .semantics(mergeDescendants = true) {
                contentDescription = contentDesc
                role = Role.Button
                if (pkg.isActivePackage) {
                    stateDescription = activeSemantics
                }
            },
        shape = LearningEngineShapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (pkg.isActivePackage) 2.dp else LearningElevation.card
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = if (pkg.isActivePackage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = androidDisplayTitle(pkg.title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.library_content_count, pkg.contentCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pkg.isActivePackage) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.library_active_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStudy){Text(stringResource(R.string.library_study_this_item))};OutlinedButton(onClick=onEdit){Text(stringResource(R.string.library_edit))};if(item.hasAudio)IconButton(onClick={audio=controller.replay(item.audioRef?.let(resolveMedia)){audio=it}},modifier=Modifier.semantics { stateDescription=audio.javaClass.simpleName }){Icon(Icons.AutoMirrored.Filled.VolumeUp,stringResource(R.string.library_listen_to_word))}}
} }

@Composable private fun MediaThumbnail(reference:String,resolveMedia:(String)->String?,modifier:Modifier=Modifier){val bitmap by produceState<android.graphics.Bitmap?>(null,reference){value=withContext(Dispatchers.IO){val path=resolveMedia(reference)?:return@withContext null;val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(path,bounds);var sample=1;while(bounds.outWidth/sample>320||bounds.outHeight/sample>320)sample*=2;BitmapFactory.decodeFile(path,BitmapFactory.Options().apply{inSampleSize=sample})}};if(bitmap==null)Surface(modifier,shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceVariant){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.ImageNotSupported,"Image unavailable")}}else Image(bitmap!!.asImageBitmap(),"Content image",modifier,contentScale=ContentScale.Crop)}
@Composable private fun SearchField(value:String,onValueChange:(String)->Unit,label:String){val keyboard=LocalSoftwareKeyboardController.current;TextField(value,onValueChange,placeholder={Text(label)},leadingIcon={Icon(Icons.Default.Search,null,Modifier.size(LearningIconSize.action))},singleLine=true,shape=LearningEngineShapes.medium,colors=TextFieldDefaults.colors(focusedContainerColor=MaterialTheme.colorScheme.surfaceVariant,unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant,disabledContainerColor=MaterialTheme.colorScheme.surfaceVariant,focusedIndicatorColor=androidx.compose.ui.graphics.Color.Transparent,unfocusedIndicatorColor=androidx.compose.ui.graphics.Color.Transparent,disabledIndicatorColor=androidx.compose.ui.graphics.Color.Transparent,cursorColor=MaterialTheme.colorScheme.primary),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={keyboard?.hide()}),trailingIcon={if(value.isNotEmpty())IconButton(onClick={onValueChange("")}){Icon(Icons.Default.Clear,stringResource(R.string.library_clear_search))}},modifier=Modifier.fillMaxWidth().defaultMinSize(minHeight=LearningSpacing.touchTarget))}

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
@Composable private fun ErrorState(state:AndroidLibraryState.Failed,onRetry:()->Unit)=ElevatedCard(Modifier.widthIn(max=600.dp).fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(stringResource(R.string.library_unavailable),style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics { heading() });Text(state.message,color=MaterialTheme.colorScheme.error,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Assertive });if(state.recoverable)Button(onClick=onRetry){Text(stringResource(R.string.action_retry))}}}
@Composable
private fun Editor(draft:AndroidItemDraft,onDraft:(AndroidItemDraft)->Unit,onSave:()->Unit) {
    fun update(value:String,field:Int) = onDraft(when(field){0->draft.copy(question=value);1->draft.copy(answer=value);2->draft.copy(pronunciation=value);3->draft.copy(partOfSpeech=value);4->draft.copy(example=value);else->draft.copy(exampleTranslation=value)})
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        listOf("Word" to draft.question,"Meaning" to draft.answer,"Pronunciation" to draft.pronunciation,"Part of speech" to draft.partOfSpeech,"Example" to draft.example,"Example translation" to draft.exampleTranslation).forEachIndexed { index, field ->
            OutlinedTextField(field.second,{update(it,index)},label={Text(field.first)},modifier=Modifier.fillMaxWidth())
        }
        Button(onClick=onSave,enabled=draft.question.isNotBlank()){Text(stringResource(R.string.action_save))}
    }
}
