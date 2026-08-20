package vn.loi.learning.android.packageexperience

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter

// ─── Canonical package header UI model ───────────────────────────────────────
// Fields: only what InstalledPackageSummary (navigation tree authority) provides.
// NOT included: artwork, lesson count, difficulty, author, duration, progress.
data class AndroidPackageHeaderModel(
    val packageId: String,
    val title: String,
    val version: String,
    val contentCount: Int,
    val state: String,            // from PackageState.name
    val isActivePackage: Boolean  // canonical: id == navigation tree activePackageId
)

// ─── Primary CTA model ───────────────────────────────────────────────────────
sealed interface AndroidPackageCta {
    /** Active session exists inside this package → Continue Learning. */
    data class ContinueLearning(val sessionId: String) : AndroidPackageCta
    /** Active scope exists for this package but no live session → Continue Package. */
    data object ContinuePackage : AndroidPackageCta
    /** Package has content but no active session/scope → Study Package. */
    data object StudyPackage : AndroidPackageCta
    /** Package has zero content → no CTA. */
    data object NoContent : AndroidPackageCta
}

// ─── FSRS filter & status models ─────────────────────────────────────────────
enum class AndroidFsrsFilter(val label: String) {
    ALL("All"),
    DUE("Due"),
    OVERDUE("Overdue"),
    LEARNING("Learning"),
    NEW("New"),
    REVIEW("Review")
}

enum class AndroidContentFsrsStatus(val label: String) {
    NEW("New"),
    LEARNING("Learning"),
    REVIEW("Review"),
    DUE("Due"),
    OVERDUE("Overdue")
}

data class AndroidPackageFilterSpec(
    val query: String = "",
    val fsrsFilter: AndroidFsrsFilter = AndroidFsrsFilter.ALL,
    val difficultOnly: Boolean = false,
    val selectedLesson: String? = null,
    val mediaFilter: BrowserMediaFilter = BrowserMediaFilter.ALL
) {
    val isFiltered: Boolean
        get() = query.isNotBlank() || fsrsFilter != AndroidFsrsFilter.ALL || difficultOnly ||
            selectedLesson != null || mediaFilter != BrowserMediaFilter.ALL
}

// ─── Package content row UI model ────────────────────────────────────────────
// Thin projection for lazy list — no media bytes loaded at row level.
// searchableText is pre-computed by PackageContentBrowserQueryService (NFKC normalized, lowercase).
data class AndroidPackageContentRow(
    val contentId: String,
    val question: String,
    val answer: String,
    val lesson: String,
    val group: String?,
    val section: String?,
    val pronunciation: String,
    val partOfSpeech: String,
    val hasImage: Boolean,
    val hasAudio: Boolean,
    val imageRef: String?,
    val audioRef: String?,
    val example: String? = null,
    val translation: String? = null,
    val index: Int,
    val searchableText: String = "",
    val fsrsStatus: AndroidContentFsrsStatus = AndroidContentFsrsStatus.NEW,
    val fsrsStageFilter: AndroidFsrsFilter = when (fsrsStatus) {
        AndroidContentFsrsStatus.NEW -> AndroidFsrsFilter.NEW
        AndroidContentFsrsStatus.LEARNING -> AndroidFsrsFilter.LEARNING
        else -> AndroidFsrsFilter.REVIEW
    },
    val isDue: Boolean = fsrsStatus == AndroidContentFsrsStatus.DUE || fsrsStatus == AndroidContentFsrsStatus.OVERDUE,
    val isOverdue: Boolean = fsrsStatus == AndroidContentFsrsStatus.OVERDUE,
    val isDifficult: Boolean = false
)

data class AndroidPackageQuickEditDraft(
    val contentId: String,
    val question: String,
    val answer: String,
    val pronunciation: String,
    val partOfSpeech: String,
    val example: String = "",
    val translation: String = ""
)

internal fun PackageContentBrowserItem.toRow(
    fsrsStatus: AndroidContentFsrsStatus = AndroidContentFsrsStatus.NEW,
    fsrsStageFilter: AndroidFsrsFilter = AndroidFsrsFilter.NEW,
    isDue: Boolean = false,
    isOverdue: Boolean = false,
    isDifficult: Boolean = false
) = AndroidPackageContentRow(
    contentId = contentId.value,
    question = questionText,
    answer = answerText,
    lesson = lesson,
    group = group,
    section = section,
    pronunciation = pronunciation,
    partOfSpeech = partOfSpeech,
    hasImage = hasImage,
    hasAudio = hasAudio,
    imageRef = imageRef,
    audioRef = audioRef,
    example = exampleText,
    translation = exampleTranslation,
    index = index,
    searchableText = searchableText,
    fsrsStatus = fsrsStatus,
    fsrsStageFilter = fsrsStageFilter,
    isDue = isDue,
    isOverdue = isOverdue,
    isDifficult = isDifficult
)

// ─── Package content state ────────────────────────────────────────────────────
sealed interface AndroidPackageContentState {
    data object Loading : AndroidPackageContentState
    data class Content(
        val header: AndroidPackageHeaderModel,
        val cta: AndroidPackageCta,
        val allRows: List<AndroidPackageContentRow>,
        val visibleRows: List<AndroidPackageContentRow>,
        val query: String = "",
        val filterSpec: AndroidPackageFilterSpec = AndroidPackageFilterSpec(query = query),
        val availableLessons: List<String> = emptyList(),
        val generation: Long = 0L
    ) : AndroidPackageContentState
    data class Empty(
        val header: AndroidPackageHeaderModel,
        val cta: AndroidPackageCta = AndroidPackageCta.NoContent,
        val generation: Long = 0L
    ) : AndroidPackageContentState
    data class Failure(val message: String, val recoverable: Boolean = true) : AndroidPackageContentState
}

// ─── Package operation state ──────────────────────────────────────────────────
sealed interface AndroidPackageOperationState {
    data object Idle : AndroidPackageOperationState
    data class Pending(val message: String) : AndroidPackageOperationState
    data class Succeeded(val message: String) : AndroidPackageOperationState
    data class Failed(val message: String) : AndroidPackageOperationState
}
