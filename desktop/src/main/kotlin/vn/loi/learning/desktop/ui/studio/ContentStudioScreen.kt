package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader

@Composable
fun ContentStudioScreen(
    uiState: PackageContentBrowserUiState,
    onClose: () -> Unit,
    onSelectRow: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLessonFilterChanged: (String) -> Unit,
    onMediaFilterChanged: (BrowserMediaFilter) -> Unit,
    onSortChanged: (BrowserSortOption) -> Unit,
    onResetFilters: () -> Unit,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
    onEditContent: (() -> Unit)? = null,
    onSaveEdit: (() -> Unit)? = null,
    onDiscardEdit: (() -> Unit)? = null,
    onUpdateDraftQuestion: ((String) -> Unit)? = null,
    onUpdateDraftAnswer: ((String) -> Unit)? = null,
    onUpdateDraftPronunciation: ((String) -> Unit)? = null,
    onUpdateDraftPartOfSpeech: ((String) -> Unit)? = null,
    onUpdateDraftExampleText: ((String) -> Unit)? = null,
    onUpdateDraftExampleTranslation: ((String) -> Unit)? = null,
    onDoubleClickRow: ((String) -> Unit)? = null,
    onRequestDelete: (() -> Unit)? = null,
    onConfirmDelete: (() -> Unit)? = null,
    onDismissDelete: (() -> Unit)? = null,
    onConfirmSaveAndProceed: (() -> Unit)? = null,
    onConfirmDiscardAndProceed: (() -> Unit)? = null,
    onCancelUnsavedDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            ContentExplorerPane(
                modifier = Modifier.weight(0.25f),
                uiState = uiState,
                onClose = onClose,
                onSelectRow = onSelectRow,
                onQueryChanged = onQueryChanged,
                onClearQuery = onClearQuery,
                onLessonFilterChanged = onLessonFilterChanged,
                onMediaFilterChanged = onMediaFilterChanged,
                onSortChanged = onSortChanged,
                onResetFilters = onResetFilters,
                onDoubleClickRow = onDoubleClickRow
            )
            ContentEditorPane(
                modifier = Modifier.weight(0.5f),
                uiState = uiState,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                thumbnailLoader = thumbnailLoader,
                onEditContent = onEditContent,
                onSaveEdit = onSaveEdit,
                onDiscardEdit = onDiscardEdit,
                onUpdateDraftQuestion = onUpdateDraftQuestion,
                onUpdateDraftAnswer = onUpdateDraftAnswer,
                onUpdateDraftPronunciation = onUpdateDraftPronunciation,
                onUpdateDraftPartOfSpeech = onUpdateDraftPartOfSpeech,
                onUpdateDraftExampleText = onUpdateDraftExampleText,
                onUpdateDraftExampleTranslation = onUpdateDraftExampleTranslation,
                onRequestDelete = onRequestDelete,
                onConfirmDelete = onConfirmDelete,
                onDismissDelete = onDismissDelete,
                onConfirmSaveAndProceed = onConfirmSaveAndProceed,
                onConfirmDiscardAndProceed = onConfirmDiscardAndProceed,
                onCancelUnsavedDialog = onCancelUnsavedDialog
            )
            MediaInspectorPane(
                modifier = Modifier.weight(0.25f),
                uiState = uiState,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                thumbnailLoader = thumbnailLoader
            )
        }
    }
}
