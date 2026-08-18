package vn.loi.learning.desktop.ui.browser.imagereuse

/**
 * Scope for filtering target vocabulary items during Image Reuse Review.
 */
enum class ImageReuseScope(val label: String, val description: String) {
    MISSING_IMAGES_ONLY(
        label = "Missing images only",
        description = "Only scan target items that currently do not have an image"
    ),
    ALL_MATCHING_ITEMS(
        label = "All matching items",
        description = "Scan all target items that match candidate questions, allowing image replacement"
    )
}

/**
 * Explicit user intent for the target item's image in Image Reuse Review.
 */
sealed interface TargetImageIntent {
    /** Keep current target image untouched on navigation, or copy source candidate if user clicks Use Source Image & Next. */
    data object Unchanged : TargetImageIntent

    /** User explicitly replaced target image with a newly selected / dropped image. */
    data class Replace(val imageRef: String) : TargetImageIntent

    /** User explicitly removed target image. Target image must become null. */
    data object Remove : TargetImageIntent

    /** User explicitly chose to reuse a specific source image. */
    data class ReuseSource(val imageRef: String) : TargetImageIntent
}

/**
 * In-memory draft for Target item editing in Image Reuse Review.
 */
data class ImageReuseTargetDraft(
    val targetContentId: String,
    val question: String,
    val answer: String,
    val translation: String,
    val exampleText: String,
    val partOfSpeech: String,
    val pronunciation: String = "",
    val imageIntent: TargetImageIntent = TargetImageIntent.Unchanged
) {
    fun isDirty(target: ImageReuseTargetItem): Boolean =
        question != target.question ||
        answer != target.answer ||
        translation != target.translation ||
        exampleText != target.exampleText ||
        partOfSpeech != target.partOfSpeech ||
        imageIntent !is TargetImageIntent.Unchanged
}

/**
 * A reusable image candidate discovered from an installed source package.
 * All fields are read-only and provided for semantic context.
 */
data class ImageReuseSourceCandidate(
    val sourcePackageId: String,
    val sourcePackageName: String,
    val sourceContentId: String,
    val question: String,
    val answer: String,
    val translation: String,
    val exampleText: String,
    val partOfSpeech: String,
    val imageRef: String
)

/**
 * A target package vocabulary item paired with candidate images discovered from source packages.
 */
data class ImageReuseTargetItem(
    val targetContentId: String,
    val targetLesson: String,
    val question: String,
    val answer: String,
    val translation: String,
    val exampleText: String,
    val partOfSpeech: String,
    val currentImageRef: String?,
    val pronunciation: String = "",
    val candidates: List<ImageReuseSourceCandidate>
)

/**
 * Package option displayed in the Image Reuse Review setup stage.
 */
data class ImageReusePackageOption(
    val id: String,
    val name: String,
    val version: String = ""
)

/**
 * Aligned comparison row pairing Target and Source values for a specific semantic field.
 */
data class ImageReuseFieldComparisonRow(
    val fieldName: String,
    val targetValue: String,
    val sourceValue: String
)

object ImageReuseComparisonProjection {
    /**
     * Canonical comparison sequence: Question, Answer, Translation, Example, POS.
     * Guarantees both sides share the exact same row structure and field order.
     */
    fun createRows(
        target: ImageReuseTargetItem,
        candidate: ImageReuseSourceCandidate
    ): List<ImageReuseFieldComparisonRow> = listOf(
        ImageReuseFieldComparisonRow("Question", target.question, candidate.question),
        ImageReuseFieldComparisonRow("Answer", target.answer, candidate.answer),
        ImageReuseFieldComparisonRow("Translation", target.translation, candidate.translation),
        ImageReuseFieldComparisonRow("Example", target.exampleText, candidate.exampleText),
        ImageReuseFieldComparisonRow("POS", target.partOfSpeech, candidate.partOfSpeech)
    )
}

/**
 * Entry recorded in the session-local undo stack after a successful Image Reuse acceptance.
 */
data class ImageReuseUndoEntry(
    val targetContentId: String,
    val targetIndex: Int,
    val candidateIndex: Int,
    val beforeImageRef: String?,
    val appliedImageRef: String
)

/**
 * Lifecycle stages of an Image Reuse Review session.
 */
sealed interface ImageReuseReviewStage {
    /**
     * Initial configuration stage: select source packages and review scope.
     */
    data class Setup(
        val availableSourcePackages: List<ImageReusePackageOption>,
        val selectedSourcePackageIds: Set<String> = emptySet(),
        val scope: ImageReuseScope = ImageReuseScope.MISSING_IMAGES_ONLY,
        val isScanning: Boolean = false,
        val scanError: String? = null
    ) : ImageReuseReviewStage

    /**
     * Active candidate review stage: inspect one candidate for one target item at a time.
     */
    data class Review(
        val targetItems: List<ImageReuseTargetItem>,
        val currentTargetIndex: Int = 0,
        val currentCandidateIndex: Int = 0,
        val isApplying: Boolean = false,
        val applyError: String? = null,
        val appliedCount: Int = 0,
        val undoStack: List<ImageReuseUndoEntry> = emptyList(),
        val allowedSourcePackageIds: Set<String> = emptySet(),
        val targetDraft: ImageReuseTargetDraft? = null
    ) : ImageReuseReviewStage {
        init {
            if (allowedSourcePackageIds.isNotEmpty()) {
                targetItems.flatMap { it.candidates }.forEach { candidate ->
                    require(candidate.sourcePackageId in allowedSourcePackageIds) {
                        "Invariant violation: Candidate ${candidate.sourceContentId} from package '${candidate.sourcePackageName}' (${candidate.sourcePackageId}) is not in allowed source package IDs: $allowedSourcePackageIds"
                    }
                }
            }
        }

        val currentTarget: ImageReuseTargetItem?
            get() = targetItems.getOrNull(currentTargetIndex)

        val currentCandidate: ImageReuseSourceCandidate?
            get() = currentTarget?.candidates?.getOrNull(currentCandidateIndex)

        val effectiveTargetDraft: ImageReuseTargetDraft
            get() = targetDraft?.takeIf { it.targetContentId == currentTarget?.targetContentId }
                ?: currentTarget?.let {
                    ImageReuseTargetDraft(
                        targetContentId = it.targetContentId,
                        question = it.question,
                        answer = it.answer,
                        translation = it.translation,
                        exampleText = it.exampleText,
                        partOfSpeech = it.partOfSpeech,
                        pronunciation = it.pronunciation,
                        imageIntent = TargetImageIntent.Unchanged
                    )
                } ?: ImageReuseTargetDraft("", "", "", "", "", "")

        val isTargetDraftDirty: Boolean
            get() {
                val target = currentTarget ?: return false
                val draft = effectiveTargetDraft
                return draft.question != target.question ||
                    draft.answer != target.answer ||
                    draft.translation != target.translation ||
                    draft.exampleText != target.exampleText ||
                    draft.partOfSpeech != target.partOfSpeech ||
                    draft.imageIntent !is TargetImageIntent.Unchanged
            }

        val effectiveTargetImageRef: String?
            get() = when (val intent = effectiveTargetDraft.imageIntent) {
                is TargetImageIntent.Unchanged -> currentTarget?.currentImageRef
                is TargetImageIntent.Replace -> intent.imageRef
                is TargetImageIntent.Remove -> null
                is TargetImageIntent.ReuseSource -> intent.imageRef
            }

        val totalTargets: Int
            get() = targetItems.size

        val totalCandidatesForCurrentTarget: Int
            get() = currentTarget?.candidates?.size ?: 0

        val canGoPrevious: Boolean
            get() = currentTargetIndex > 0 && !isApplying

        val canUndo: Boolean
            get() = undoStack.isNotEmpty() && !isApplying
    }

    /**
     * Completed review stage summarizing reviewed items and applied changes.
     */
    data class Complete(
        val totalReviewedTargets: Int,
        val totalAppliedCount: Int
    ) : ImageReuseReviewStage
}

/**
 * Top-level state of the Image Reuse Review dialog.
 */
data class ImageReuseReviewDialogState(
    val visible: Boolean = false,
    val targetPackageId: String = "",
    val targetPackageName: String = "",
    val stage: ImageReuseReviewStage? = null
)
