package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

/** Supported fit modes for Content Studio Hero Image */
enum class HeroFitMode {
    FIT,
    FIT_WIDTH,
    FIT_HEIGHT
}

/** Helper function to resolve image file reference for StudioHeroImage */
private fun resolveHeroImageFile(reference: String, storage: ContentMediaStorage?): File? {
    return try {
        val path = storage?.resolve(reference)
        if (path != null) {
            val file = path.toFile()
            if (file.exists()) return file
        }
        val direct = File(reference)
        if (direct.exists()) direct else null
    } catch (_: Exception) {
        null
    }
}

/** Copy a real image file to the OS clipboard (same idea as Ctrl+C in File Explorer). */
private fun copyFileToSystemClipboard(file: File): Boolean {
    if (!file.exists() || !file.isFile) return false

    return try {
        val files = listOf(file)
        val transferable = object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> =
                arrayOf(DataFlavor.javaFileListFlavor)

            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
                flavor == DataFlavor.javaFileListFlavor

            override fun getTransferData(flavor: DataFlavor): Any {
                if (!isDataFlavorSupported(flavor)) {
                    throw UnsupportedFlavorException(flavor)
                }
                return files
            }
        }

        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
        true
    } catch (_: Exception) {
        false
    }
}

/** Internal state of loaded Hero Image */
sealed interface HeroImageState {
    data object Loading : HeroImageState
    data class Success(val bitmap: ImageBitmap, val width: Int, val height: Int) : HeroImageState
    data object Unavailable : HeroImageState
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.editorImageDropTarget(
    onFileDropped: (File) -> Unit,
    onDragOverChanged: (Boolean) -> Unit,
    onError: (String) -> Unit
): Modifier {
    val target = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) { onDragOverChanged(true) }
            override fun onEntered(event: DragAndDropEvent) { onDragOverChanged(true) }
            override fun onExited(event: DragAndDropEvent) { onDragOverChanged(false) }
            override fun onEnded(event: DragAndDropEvent) { onDragOverChanged(false) }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                onDragOverChanged(false)
                return try {
                    val transferable = event.awtTransferable
                    val files = DragDropUtils.extractFiles(transferable)
                    val file = files.firstOrNull()
                    if (file != null && file.exists() && file.isFile) {
                        if (DragDropUtils.isSupportedImage(file)) {
                            onFileDropped(file)
                            true
                        } else {
                            onError("Audio file dropped on image slot. Use an audio slot instead.")
                            false
                        }
                    } else false
                } catch (_: Exception) { false }
            }
        }
    }
    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { true },
        target = target
    )
}

/**
 * Dedicated large-image renderer for Content Studio Hero Viewer.
 * Bypasses list-thumbnail (72dp) restrictions and displays full-resolution image.
 */
@Composable
fun StudioHeroImage(
    reference: String?,
    contentMediaStorage: ContentMediaStorage?,
    zoomPercent: Int = 100,
    fitMode: HeroFitMode = HeroFitMode.FIT,
    onStateChanged: ((HeroImageState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by produceState<HeroImageState>(HeroImageState.Loading, reference, contentMediaStorage) {
        value = if (reference.isNullOrBlank()) {
            HeroImageState.Unavailable
        } else {
            withContext(Dispatchers.IO) {
                val targetFile = resolveHeroImageFile(reference, contentMediaStorage)
                if (targetFile != null && targetFile.exists() && targetFile.isFile) {
                    try {
                        val bytes = targetFile.readBytes()
                        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                        HeroImageState.Success(
                            bitmap = skiaImage.toComposeImageBitmap(),
                            width = skiaImage.width,
                            height = skiaImage.height
                        )
                    } catch (_: Exception) {
                        HeroImageState.Unavailable
                    }
                } else {
                    HeroImageState.Unavailable
                }
            }
        }
    }

    LaunchedEffect(state) {
        onStateChanged?.invoke(state)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is HeroImageState.Loading -> {
                CircularProgressIndicator(
                    color = LEColors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            is HeroImageState.Success -> {
                val scaleFactor = zoomPercent / 100f
                val contentScale = when (fitMode) {
                    HeroFitMode.FIT_WIDTH -> ContentScale.FillWidth
                    HeroFitMode.FIT_HEIGHT -> ContentScale.FillHeight
                    HeroFitMode.FIT -> ContentScale.Fit
                }

                Image(
                    bitmap = s.bitmap,
                    contentDescription = "Content Hero Image",
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (fitMode == HeroFitMode.FIT) scaleFactor else 1f,
                            scaleY = if (fitMode == HeroFitMode.FIT) scaleFactor else 1f
                        )
                )
            }
            is HeroImageState.Unavailable -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                ) {
                    Icon(
                        imageVector = LEIcons.Image,
                        contentDescription = null,
                        tint = LEColors.textMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Image unavailable",
                        style = LETypography.caption,
                        color = LEColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ContentEditorPane(
    uiState: PackageContentBrowserUiState,
    isCreatingNewItem: Boolean = false,
    playbackCoordinator: PlaybackCoordinator? = null,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
    contentMediaStorage: ContentMediaStorage? = null,
    onEditContent: (() -> Unit)? = null,
    onSaveEdit: (() -> Unit)? = null,
    onDiscardEdit: (() -> Unit)? = null,
    onUpdateDraftQuestion: ((String) -> Unit)? = null,
    onUpdateDraftAnswer: ((String) -> Unit)? = null,
    onUpdateDraftPronunciation: ((String) -> Unit)? = null,
    onUpdateDraftPartOfSpeech: ((String) -> Unit)? = null,
    onUpdateDraftExampleText: ((String) -> Unit)? = null,
    onUpdateDraftExampleTranslation: ((String) -> Unit)? = null,
    onUpdateDraftImageRef: ((String?) -> Unit)? = null,
    onImportMediaFile: ((File, String) -> Unit)? = null,
    onRequestDelete: (() -> Unit)? = null,
    onConfirmDelete: (() -> Unit)? = null,
    onDismissDelete: (() -> Unit)? = null,
    onConfirmSaveAndProceed: (() -> Unit)? = null,
    onConfirmDiscardAndProceed: (() -> Unit)? = null,
    onCancelUnsavedDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCreating = uiState.isCreatingNewItem || isCreatingNewItem
    val activeDraft = if (isCreating || uiState.editingContentId != null) uiState.draftEdits else null
    val persistedItem = uiState.selectedItemInView ?: uiState.selectedItemAnywhere
    val selectedItem = persistedItem
    val draft = activeDraft
    val isEditing = (uiState.editingContentId != null && uiState.editingContentId == uiState.selectedContentId) || isCreating

    val activeQuestionAudioRef = if (activeDraft != null) activeDraft.questionAudioRef else persistedItem?.questionAudioRef
    val activeAnswerAudioRef = if (activeDraft != null) activeDraft.answerAudioRef else persistedItem?.answerAudioRef
    val activeExampleAudioRef = if (activeDraft != null) activeDraft.exampleAudioRef else persistedItem?.exampleAudioRef
    val activeTranslationAudioRef = if (activeDraft != null) activeDraft.translationAudioRef else persistedItem?.translationAudioRef
    val activeImageRef = if (activeDraft != null) activeDraft.imageRef else persistedItem?.imageRef
    val scrollState = rememberScrollState()

    var imageZoomLevel by remember { mutableStateOf(100) }
    var fitMode by remember { mutableStateOf(HeroFitMode.FIT) }
    var isFullscreenImageOpen by remember { mutableStateOf(false) }
    var heroState by remember { mutableStateOf<HeroImageState>(HeroImageState.Loading) }

    if (selectedItem == null && !isCreatingNewItem) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(LESpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                Text(
                    text = "Select a content item from the Explorer",
                    style = LETypography.paneTitle
                )
                Text(
                    text = "Choose an item on the left pane to view or edit its fields and media.",
                    style = LETypography.secondaryMetadata
                )
            }
        }
        return
    }

    // Focus requesters for ordered keyboard navigation
    val questionFocusRequester = remember { FocusRequester() }
    val answerFocusRequester = remember { FocusRequester() }
    val ipaFocusRequester = remember { FocusRequester() }
    val posFocusRequester = remember { FocusRequester() }
    val exampleFocusRequester = remember { FocusRequester() }
    val translationFocusRequester = remember { FocusRequester() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isNarrow = maxWidth < 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LESpacing.lg, vertical = LESpacing.md)
        ) {
            // Top Editor Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LESpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                ) {
                    Icon(
                        imageVector = LEIcons.Settings,
                        contentDescription = null,
                        tint = LEColors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isCreatingNewItem) "Creating New Content Item"
                        else if (isEditing) "Editing Item #${selectedItem?.index ?: 0} of ${uiState.allItems.size}"
                        else "Viewing Item #${selectedItem?.index ?: 0} of ${uiState.allItems.size}",
                        style = LETypography.fieldValueEmphasized
                    )
                    LEStatusBadge(
                        variant = StatusBadgeVariant.Present,
                        customText = if (isEditing) "Editing" else "Active"
                    )
                }
            }

            // Scrollable Form & Image Hero Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                if (isEditing && (draft != null || isCreatingNewItem)) {
                    LaunchedEffect(isEditing) {
                        questionFocusRequester.requestFocus()
                    }

                    val currentQuestion = draft?.questionText ?: ""
                    val currentAnswer = draft?.answerText ?: ""
                    val currentPronunciation = draft?.pronunciation ?: ""
                    val currentPos = draft?.partOfSpeech ?: "WORD"
                    val currentExample = draft?.exampleText ?: ""
                    val currentTranslation = draft?.exampleTranslation ?: ""

                    // PLE-020 Adaptive Visibility: Empty optional fields collapse into "+ Add ..." actions.
                    var isIpaRevealed by remember(selectedItem?.contentId?.value, isCreating) {
                        mutableStateOf(currentPronunciation.isNotBlank())
                    }
                    var isExampleRevealed by remember(selectedItem?.contentId?.value, isCreating) {
                        mutableStateOf(currentExample.isNotBlank())
                    }
                    var isTranslationRevealed by remember(selectedItem?.contentId?.value, isCreating) {
                        mutableStateOf(currentTranslation.isNotBlank())
                    }

                    // ROW 1: QUESTION & ANSWER
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                            EditorFieldCard(
                                label = "Question",
                                value = currentQuestion,
                                onValueChange = { onUpdateDraftQuestion?.invoke(it) },
                                isRequired = true,
                                audioRef = activeQuestionAudioRef,
                                playbackCoordinator = playbackCoordinator,
                                onFallbackPlay = onPlayAudio,
                                onFallbackStop = onStopAudio,
                                focusRequester = questionFocusRequester,
                                nextFocusRequester = answerFocusRequester
                            )
                            EditorFieldCard(
                                label = "Answer",
                                value = currentAnswer,
                                onValueChange = { onUpdateDraftAnswer?.invoke(it) },
                                isRequired = true,
                                audioRef = activeAnswerAudioRef,
                                playbackCoordinator = playbackCoordinator,
                                onFallbackPlay = onPlayAudio,
                                onFallbackStop = onStopAudio,
                                focusRequester = answerFocusRequester,
                                nextFocusRequester = if (isIpaRevealed) ipaFocusRequester else posFocusRequester
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EditorFieldCard(
                                label = "Question",
                                value = currentQuestion,
                                onValueChange = { onUpdateDraftQuestion?.invoke(it) },
                                isRequired = true,
                                audioRef = activeQuestionAudioRef,
                                playbackCoordinator = playbackCoordinator,
                                onFallbackPlay = onPlayAudio,
                                onFallbackStop = onStopAudio,
                                focusRequester = questionFocusRequester,
                                nextFocusRequester = answerFocusRequester,
                                modifier = Modifier.weight(1f)
                            )
                            EditorFieldCard(
                                label = "Answer",
                                value = currentAnswer,
                                onValueChange = { onUpdateDraftAnswer?.invoke(it) },
                                isRequired = true,
                                audioRef = activeAnswerAudioRef,
                                playbackCoordinator = playbackCoordinator,
                                onFallbackPlay = onPlayAudio,
                                onFallbackStop = onStopAudio,
                                focusRequester = answerFocusRequester,
                                nextFocusRequester = if (isIpaRevealed) ipaFocusRequester else posFocusRequester,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ROW 2: EXAMPLE & TRANSLATION (Side by Side at 50% / 50%)
                    if (isExampleRevealed && isTranslationRevealed) {
                        if (isNarrow) {
                            Column(verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                                EditorFieldCard(
                                    label = "Example (English)",
                                    value = currentExample,
                                    onValueChange = { onUpdateDraftExampleText?.invoke(it) },
                                    audioRef = activeExampleAudioRef,
                                    playbackCoordinator = playbackCoordinator,
                                    onFallbackPlay = onPlayAudio,
                                    onFallbackStop = onStopAudio,
                                    focusRequester = exampleFocusRequester,
                                    nextFocusRequester = translationFocusRequester,
                                    minLines = 2,
                                    maxLines = 3
                                )
                                EditorFieldCard(
                                    label = "Translation (Vietnamese)",
                                    value = currentTranslation,
                                    onValueChange = { onUpdateDraftExampleTranslation?.invoke(it) },
                                    audioRef = activeTranslationAudioRef,
                                    playbackCoordinator = playbackCoordinator,
                                    onFallbackPlay = onPlayAudio,
                                    onFallbackStop = onStopAudio,
                                    focusRequester = translationFocusRequester,
                                    minLines = 2,
                                    maxLines = 3
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                EditorFieldCard(
                                    label = "Example (English)",
                                    value = currentExample,
                                    onValueChange = { onUpdateDraftExampleText?.invoke(it) },
                                    audioRef = activeExampleAudioRef,
                                    playbackCoordinator = playbackCoordinator,
                                    onFallbackPlay = onPlayAudio,
                                    onFallbackStop = onStopAudio,
                                    focusRequester = exampleFocusRequester,
                                    nextFocusRequester = translationFocusRequester,
                                    minLines = 2,
                                    maxLines = 3,
                                    modifier = Modifier.weight(1f)
                                )
                                EditorFieldCard(
                                    label = "Translation (Vietnamese)",
                                    value = currentTranslation,
                                    onValueChange = { onUpdateDraftExampleTranslation?.invoke(it) },
                                    audioRef = activeTranslationAudioRef,
                                    playbackCoordinator = playbackCoordinator,
                                    onFallbackPlay = onPlayAudio,
                                    onFallbackStop = onStopAudio,
                                    focusRequester = translationFocusRequester,
                                    minLines = 2,
                                    maxLines = 3,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else if (isExampleRevealed) {
                        EditorFieldCard(
                            label = "Example (English)",
                            value = currentExample,
                            onValueChange = { onUpdateDraftExampleText?.invoke(it) },
                            audioRef = activeExampleAudioRef,
                            playbackCoordinator = playbackCoordinator,
                            onFallbackPlay = onPlayAudio,
                            onFallbackStop = onStopAudio,
                            focusRequester = exampleFocusRequester,
                            minLines = 2,
                            maxLines = 3
                        )
                    } else if (isTranslationRevealed) {
                        EditorFieldCard(
                            label = "Translation (Vietnamese)",
                            value = currentTranslation,
                            onValueChange = { onUpdateDraftExampleTranslation?.invoke(it) },
                            audioRef = activeTranslationAudioRef,
                            playbackCoordinator = playbackCoordinator,
                            onFallbackPlay = onPlayAudio,
                            onFallbackStop = onStopAudio,
                            focusRequester = translationFocusRequester,
                            minLines = 2,
                            maxLines = 3
                        )
                    }

                    // ROW 3: IPA (70%) & POS (30%) True Compact Metadata Row with Symmetrical Height
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                            if (isIpaRevealed) {
                                CompactMetadataFieldCard(
                                    label = "IPA",
                                    value = currentPronunciation,
                                    onValueChange = { onUpdateDraftPronunciation?.invoke(it) },
                                    focusRequester = ipaFocusRequester,
                                    nextFocusRequester = posFocusRequester,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            PosDropdownSelector(
                                selectedPos = currentPos,
                                onPosSelected = { onUpdateDraftPartOfSpeech?.invoke(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isIpaRevealed) {
                                CompactMetadataFieldCard(
                                    label = "IPA",
                                    value = currentPronunciation,
                                    onValueChange = { onUpdateDraftPronunciation?.invoke(it) },
                                    focusRequester = ipaFocusRequester,
                                    nextFocusRequester = posFocusRequester,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            PosDropdownSelector(
                                selectedPos = currentPos,
                                onPosSelected = { onUpdateDraftPartOfSpeech?.invoke(it) },
                                modifier = if (isIpaRevealed) Modifier.weight(1f) else Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Toolbar for revealing currently hidden optional fields
                    if (!isIpaRevealed || !isExampleRevealed || !isTranslationRevealed) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.xs)
                        ) {
                            Text(
                                text = "Add optional fields:",
                                style = LETypography.caption,
                                color = LEColors.textMuted
                            )

                            if (!isIpaRevealed) {
                                LESecondaryButton(
                                    text = "+ Add IPA",
                                    onClick = { isIpaRevealed = true }
                                )
                            }

                            if (!isExampleRevealed) {
                                LESecondaryButton(
                                    text = "+ Add Example",
                                    onClick = { isExampleRevealed = true }
                                )
                            }

                            if (!isTranslationRevealed) {
                                LESecondaryButton(
                                    text = "+ Add Translation",
                                    onClick = { isTranslationRevealed = true }
                                )
                            }
                        }
                    }
                } else if (persistedItem != null) {
                    val hasIpa = persistedItem.pronunciation.isNotBlank()
                    val hasExample = !persistedItem.exampleText.isNullOrBlank()
                    val hasTranslation = !persistedItem.exampleTranslation.isNullOrBlank()

                    // ROW 1: QUESTION & ANSWER
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                            LEFieldCard(
                                label = "Question",
                                value = persistedItem.questionText,
                                isRequired = true,
                                audioRef = activeQuestionAudioRef,
                                playbackCoordinator = playbackCoordinator,
                                onFallbackPlay = onPlayAudio,
                                onFallbackStop = onStopAudio,
                                isTitle = true
                            )
                            LEFieldCard(
                                label = "Answer",
                                value = persistedItem.answerText,
                                isRequired = true,
                                audioRef = activeAnswerAudioRef,
                                playbackCoordinator = playbackCoordinator,
                                onFallbackPlay = onPlayAudio,
                                onFallbackStop = onStopAudio
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LEFieldCard(
                                label = "Question",
                                value = persistedItem.questionText,
                                isRequired = true,
                                audioRef = activeQuestionAudioRef,
                                playbackCoordinator = playbackCoordinator,
                                onFallbackPlay = onPlayAudio,
                                onFallbackStop = onStopAudio,
                                isTitle = true,
                                modifier = Modifier.weight(1f)
                            )
                            LEFieldCard(
                                label = "Answer",
                                value = persistedItem.answerText,
                                isRequired = true,
                                audioRef = activeAnswerAudioRef,
                                playbackCoordinator = playbackCoordinator,
                                onFallbackPlay = onPlayAudio,
                                onFallbackStop = onStopAudio,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ROW 2: EXAMPLE & TRANSLATION
                    if (hasExample && hasTranslation) {
                        if (isNarrow) {
                            Column(verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                                LEFieldCard(
                                    label = "Example (English)",
                                    value = persistedItem.exampleText.orEmpty(),
                                    audioRef = activeExampleAudioRef,
                                    playbackCoordinator = playbackCoordinator,
                                    onFallbackPlay = onPlayAudio,
                                    onFallbackStop = onStopAudio
                                )
                                LEFieldCard(
                                    label = "Translation (Vietnamese)",
                                    value = persistedItem.exampleTranslation.orEmpty(),
                                    audioRef = activeTranslationAudioRef,
                                    playbackCoordinator = playbackCoordinator,
                                    onFallbackPlay = onPlayAudio,
                                    onFallbackStop = onStopAudio
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LEFieldCard(
                                    label = "Example (English)",
                                    value = persistedItem.exampleText.orEmpty(),
                                    audioRef = activeExampleAudioRef,
                                    playbackCoordinator = playbackCoordinator,
                                    onFallbackPlay = onPlayAudio,
                                    onFallbackStop = onStopAudio,
                                    modifier = Modifier.weight(1f)
                                )
                                LEFieldCard(
                                    label = "Translation (Vietnamese)",
                                    value = persistedItem.exampleTranslation.orEmpty(),
                                    audioRef = activeTranslationAudioRef,
                                    playbackCoordinator = playbackCoordinator,
                                    onFallbackPlay = onPlayAudio,
                                    onFallbackStop = onStopAudio,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else if (hasExample) {
                        LEFieldCard(
                            label = "Example (English)",
                            value = persistedItem.exampleText.orEmpty(),
                            audioRef = activeExampleAudioRef,
                            playbackCoordinator = playbackCoordinator,
                            onFallbackPlay = onPlayAudio,
                            onFallbackStop = onStopAudio
                        )
                    } else if (hasTranslation) {
                        LEFieldCard(
                            label = "Translation (Vietnamese)",
                            value = persistedItem.exampleTranslation.orEmpty(),
                            audioRef = activeTranslationAudioRef,
                            playbackCoordinator = playbackCoordinator,
                            onFallbackPlay = onPlayAudio,
                            onFallbackStop = onStopAudio
                        )
                    }

                    // ROW 3: IPA (70%) & POS (30%)
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                            if (hasIpa) {
                                CompactMetadataViewCard(
                                    label = "IPA",
                                    value = "[${persistedItem.pronunciation}]"
                                )
                            }
                            CompactMetadataViewCard(
                                label = "POS (Part of Speech)",
                                value = persistedItem.partOfSpeech.ifBlank { "WORD" }
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (hasIpa) {
                                CompactMetadataViewCard(
                                    label = "IPA",
                                    value = "[${persistedItem.pronunciation}]",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            CompactMetadataViewCard(
                                label = "POS (Part of Speech)",
                                value = persistedItem.partOfSpeech.ifBlank { "WORD" },
                                modifier = if (hasIpa) Modifier.weight(1f) else Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ROW 4: HERO IMAGE CONTAINER (Adaptive position right after Example/Translation)
                Text(
                    text = "Image",
                    style = LETypography.fieldLabel,
                    color = LEColors.textSecondary
                )

                var isHeroDragOver by remember { mutableStateOf(false) }
                var copiedHeroImageFile by remember(activeImageRef) { mutableStateOf(false) }
                var copiedHeroImageName by remember(activeImageRef) { mutableStateOf(false) }

                LaunchedEffect(copiedHeroImageFile) {
                    if (copiedHeroImageFile) {
                        kotlinx.coroutines.delay(1200)
                        copiedHeroImageFile = false
                    }
                }

                LaunchedEffect(copiedHeroImageName) {
                    if (copiedHeroImageName) {
                        kotlinx.coroutines.delay(1200)
                        copiedHeroImageName = false
                    }
                }

                LECard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .editorImageDropTarget(
                            onFileDropped = { file ->
                                if (onImportMediaFile != null) onImportMediaFile(file, "image")
                                else onUpdateDraftImageRef?.invoke(file.name)
                            },
                            onDragOverChanged = { isHeroDragOver = it },
                            onError = {}
                        )
                ) {
                    val imageRef = activeImageRef
                    if (imageRef != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 380.dp)
                                .border(
                                    width = if (isHeroDragOver) 2.dp else 1.dp,
                                    color = if (isHeroDragOver) LEColors.primary else LEColors.borderSubtle,
                                    shape = LERadius.sm
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            StudioHeroImage(
                                reference = imageRef,
                                contentMediaStorage = contentMediaStorage,
                                zoomPercent = imageZoomLevel,
                                fitMode = fitMode,
                                onStateChanged = { heroState = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Image Controls Toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = LESpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LESecondaryButton(
                                    text = "Open Fullscreen",
                                    onClick = { isFullscreenImageOpen = true },
                                    icon = LEIcons.Fullscreen
                                )
                                LEPrimaryButton(
                                    text = if (copiedHeroImageFile) "Copied" else "Copy Image",
                                    onClick = {
                                        val file = resolveHeroImageFile(imageRef, contentMediaStorage)
                                        copiedHeroImageFile = file != null && copyFileToSystemClipboard(file)
                                    },
                                    icon = null
                                )
                                LESecondaryButton(
                                    text = if (copiedHeroImageName) "Copied" else "Copy Name",
                                    onClick = {
                                        // Copy only the image file name as plain text, without extension.
                                        // Prefer the resolved physical file so converted .jpg assets stay accurate.
                                        val resolvedFile = resolveHeroImageFile(imageRef, contentMediaStorage)
                                        val fileNameWithExtension = resolvedFile?.name
                                            ?: imageRef.substringAfterLast('/').substringAfterLast('\\')
                                        val fileNameWithoutExtension = fileNameWithExtension.substringBeforeLast(
                                            delimiter = '.',
                                            missingDelimiterValue = fileNameWithExtension
                                        )

                                        if (fileNameWithoutExtension.isNotBlank()) {
                                            Toolkit.getDefaultToolkit()
                                                .systemClipboard
                                                .setContents(StringSelection(fileNameWithoutExtension), null)
                                            copiedHeroImageName = true
                                        }
                                    },
                                    icon = null
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LEIconButton(
                                    icon = LEIcons.ZoomOut,
                                    onClick = {
                                        fitMode = HeroFitMode.FIT
                                        if (imageZoomLevel > 50) imageZoomLevel -= 25
                                    },
                                    contentDescription = "Zoom out"
                                )
                                Text(
                                    text = if (fitMode == HeroFitMode.FIT_WIDTH) "Fit Width"
                                    else if (fitMode == HeroFitMode.FIT_HEIGHT) "Fit Height"
                                    else "$imageZoomLevel%",
                                    style = LETypography.statusText,
                                    modifier = Modifier.padding(horizontal = LESpacing.xs)
                                )
                                LEIconButton(
                                    icon = LEIcons.ZoomIn,
                                    onClick = {
                                        fitMode = HeroFitMode.FIT
                                        if (imageZoomLevel < 250) imageZoomLevel += 25
                                    },
                                    contentDescription = "Zoom in"
                                )
                                LESecondaryButton(
                                    text = "Fit Width",
                                    onClick = { fitMode = HeroFitMode.FIT_WIDTH }
                                )
                                LESecondaryButton(
                                    text = "Fit Height",
                                    onClick = { fitMode = HeroFitMode.FIT_HEIGHT }
                                )
                                if (heroState is HeroImageState.Success) {
                                    val s = heroState as HeroImageState.Success
                                    Text(
                                        text = "${s.width} × ${s.height}",
                                        style = LETypography.caption,
                                        color = LEColors.textMuted,
                                        modifier = Modifier.padding(start = LESpacing.sm)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(if (isHeroDragOver) LEColors.primary.copy(alpha = 0.08f) else LEColors.surfaceElevated)
                                .border(
                                    width = if (isHeroDragOver) 2.dp else 1.dp,
                                    color = if (isHeroDragOver) LEColors.primary else LEColors.borderSubtle,
                                    shape = LERadius.sm
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isHeroDragOver) "Drop image here to attach" else "No Image Attached",
                                    style = LETypography.fieldValue,
                                    color = if (isHeroDragOver) LEColors.primary else LEColors.textMuted
                                )
                                Text(
                                    text = "Drag & drop image file from Explorer or Desktop",
                                    style = LETypography.caption,
                                    color = LEColors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isFullscreenImageOpen && activeImageRef != null) {
        Dialog(onDismissRequest = { isFullscreenImageOpen = false }) {
            Surface(
                shape = LERadius.lg,
                color = LEColors.surface,
                tonalElevation = LEElevation.modal,
                modifier = Modifier.fillMaxSize(0.9f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(LESpacing.md),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StudioHeroImage(
                            reference = activeImageRef,
                            contentMediaStorage = contentMediaStorage,
                            zoomPercent = 100,
                            fitMode = HeroFitMode.FIT,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = LESpacing.sm),
                        horizontalArrangement = Arrangement.End
                    ) {
                        LEPrimaryButton(text = "Close", onClick = { isFullscreenImageOpen = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMetadataFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null
) {
    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.md, vertical = LESpacing.xs)
        ) {
            Text(text = label, style = LETypography.fieldLabel, color = LEColors.textSecondary)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    maxLines = 1,
                    textStyle = LETypography.fieldValue.copy(color = LEColors.primaryText),
                    cursorBrush = SolidColor(LEColors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                        .let { if (nextFocusRequester != null) it.focusProperties { next = nextFocusRequester } else it }
                )
            }
        }
    }
}

@Composable
private fun CompactMetadataViewCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.md, vertical = LESpacing.xs)
        ) {
            Text(text = label, style = LETypography.fieldLabel, color = LEColors.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = value, style = LETypography.fieldValue)
            }
        }
    }
}

@Composable
private fun EditorFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    audioRef: String? = null,
    playbackCoordinator: PlaybackCoordinator? = null,
    onFallbackPlay: ((String) -> Unit)? = null,
    onFallbackStop: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    minLines: Int = 1,
    maxLines: Int = if (minLines > 1) 4 else 1,
    singleLine: Boolean = false
) {
    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.md, vertical = LESpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = LESpacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = label, style = LETypography.fieldLabel, color = LEColors.textSecondary)
                    if (isRequired) {
                        Text(text = " *", style = LETypography.fieldLabel, color = LEColors.danger)
                    }
                }
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine || minLines == 1,
                    minLines = if (singleLine) 1 else minLines,
                    maxLines = if (singleLine) 1 else maxLines,
                    textStyle = LETypography.fieldValue,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LEColors.surface,
                        unfocusedContainerColor = LEColors.surface,
                        disabledContainerColor = LEColors.surface,
                        focusedIndicatorColor = LEColors.borderFocus,
                        unfocusedIndicatorColor = LEColors.borderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                        .let { if (nextFocusRequester != null) it.focusProperties { next = nextFocusRequester } else it }
                )
            }

            if (audioRef != null) {
                AudioStateButton(
                    audioRef = audioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onFallbackPlay,
                    onFallbackStop = onFallbackStop
                )
            }
        }
    }
}

@Composable
private fun PosDropdownSelector(
    selectedPos: String,
    onPosSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val posOptions = listOf("WORD", "NOUN", "VERB", "ADJECTIVE", "ADVERB", "PHRASE")

    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat),
        modifier = modifier
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = LESpacing.md, vertical = LESpacing.xs)
            ) {
                Text(text = "POS (Part of Speech)", style = LETypography.fieldLabel, color = LEColors.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedPos, style = LETypography.fieldValue)
                    Text("v", style = LETypography.caption, color = LEColors.textMuted)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                posOptions.forEach { pos ->
                    DropdownMenuItem(
                        text = { Text(pos, style = LETypography.fieldValue) },
                        onClick = {
                            onPosSelected(pos)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
