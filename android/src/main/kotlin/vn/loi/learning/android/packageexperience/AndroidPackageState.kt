package vn.loi.learning.android.packageexperience

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem

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
    val hasImage: Boolean,
    val hasAudio: Boolean,
    val index: Int,
    val searchableText: String = ""
)

internal fun PackageContentBrowserItem.toRow() = AndroidPackageContentRow(
    contentId = contentId.value,
    question = questionText,
    answer = answerText,
    lesson = lesson,
    group = group,
    section = section,
    hasImage = hasImage,
    hasAudio = hasAudio,
    index = index,
    searchableText = searchableText
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
