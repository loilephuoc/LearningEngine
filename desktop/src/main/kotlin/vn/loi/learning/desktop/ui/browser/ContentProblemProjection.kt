package vn.loi.learning.desktop.ui.browser

import java.nio.file.Files
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.partofspeech.PartOfSpeechNormalizer
import vn.loi.learning.application.port.ContentMediaStorage

enum class ContentProblem(val label: String) {
    MISSING_IMAGE("Missing Image"),
    MISSING_QUESTION_AUDIO("Missing Question Audio"),
    MISSING_ANSWER_AUDIO("Missing Answer Audio"),
    MISSING_EXAMPLE_AUDIO("Missing Example Audio"),
    MISSING_TRANSLATION_AUDIO("Missing Translation Audio"),
    MISSING_ANY_AUDIO("Missing Any Audio"),
    MISSING_POS("Missing POS"),
    UNKNOWN_POS("Unknown/Custom POS"),
    INCOMPLETE_REQUIRED_TEXT("Incomplete")
}

enum class ContentProblemFilter(val label: String) {
    NONE("All Content"),
    ALL_PROBLEMS("All Problems"),
    MISSING_IMAGE("Missing Image"),
    MISSING_QUESTION_AUDIO("Missing Question Audio"),
    MISSING_ANSWER_AUDIO("Missing Answer Audio"),
    MISSING_EXAMPLE_AUDIO("Missing Example Audio"),
    MISSING_TRANSLATION_AUDIO("Missing Translation Audio"),
    MISSING_ANY_AUDIO("Missing Any Audio"),
    MISSING_POS("Missing POS"),
    UNKNOWN_POS("Unknown/Custom POS"),
    INCOMPLETE_REQUIRED_TEXT("Incomplete");

    val problem: ContentProblem?
        get() = ContentProblem.entries.firstOrNull { it.name == name }
}

fun interface MediaReferenceAvailability {
    fun exists(reference: String): Boolean
}

data class ContentProblemProjection(
    val byContentId: Map<String, Set<ContentProblem>> = emptyMap()
) {
    val problematicContentCount: Int get() = byContentId.count { it.value.isNotEmpty() }
    fun count(problem: ContentProblem): Int = byContentId.count { problem in it.value }
    fun problemsFor(contentId: String): Set<ContentProblem> = byContentId[contentId].orEmpty()
    fun matches(contentId: String, filter: ContentProblemFilter): Boolean = when (filter) {
        ContentProblemFilter.NONE -> true
        ContentProblemFilter.ALL_PROBLEMS -> problemsFor(contentId).isNotEmpty()
        else -> filter.problem in problemsFor(contentId)
    }
}

data class SelectedMediaCheckSummary(
    val selectedItemCount: Int,
    val counts: Map<ContentProblem, Int>
) {
    fun count(problem: ContentProblem): Int = counts[problem] ?: 0
}

fun ContentProblemProjection.summarize(contentIds: Set<String>): SelectedMediaCheckSummary =
    SelectedMediaCheckSummary(
        selectedItemCount = contentIds.size,
        counts = ContentProblem.entries.associateWith { problem ->
            contentIds.count { problem in problemsFor(it) }
        }
    )

object ContentProblemDetector {
    fun detect(item: PackageContentBrowserItem, media: MediaReferenceAvailability): Set<ContentProblem> =
        buildSet {
            missingBrokenMedia(item.imageRef, media)?.let { add(ContentProblem.MISSING_IMAGE) }

            val missingQAudio = isMissingAudio(item.questionAudioRef, media)
            val missingAAudio = isMissingAudio(item.answerAudioRef, media)
            val missingEAudio = isMissingAudio(item.exampleAudioRef, media)
            val missingTAudio = isMissingAudio(item.translationAudioRef, media)

            if (missingQAudio) add(ContentProblem.MISSING_QUESTION_AUDIO)
            if (missingAAudio) add(ContentProblem.MISSING_ANSWER_AUDIO)
            if (missingEAudio) add(ContentProblem.MISSING_EXAMPLE_AUDIO)
            if (missingTAudio) add(ContentProblem.MISSING_TRANSLATION_AUDIO)
            if (missingQAudio || missingAAudio || missingEAudio || missingTAudio) {
                add(ContentProblem.MISSING_ANY_AUDIO)
            }

            if (item.partOfSpeech.isNullOrBlank()) {
                add(ContentProblem.MISSING_POS)
            } else {
                val canonical = PartOfSpeechNormalizer.canonicalize(item.partOfSpeech)
                if (canonical == null || !canonical.known) {
                    add(ContentProblem.UNKNOWN_POS)
                }
            }

            if (item.questionText.isBlank() || item.answerText.isBlank()) {
                add(ContentProblem.INCOMPLETE_REQUIRED_TEXT)
            }
        }

    fun project(
        items: List<PackageContentBrowserItem>,
        media: MediaReferenceAvailability
    ): ContentProblemProjection = ContentProblemProjection(
        items.associate { it.contentId.value to detect(it, media) }
    )

    private fun isMissingAudio(reference: String?, media: MediaReferenceAvailability): Boolean {
        if (reference.isNullOrBlank()) return true
        return !media.exists(reference)
    }

    private fun missingBrokenMedia(reference: String?, media: MediaReferenceAvailability): Unit? =
        reference?.takeIf(String::isNotBlank)?.takeUnless(media::exists)?.let { Unit }
}

internal fun projectContentProblems(
    items: List<PackageContentBrowserItem>,
    mediaStorage: ContentMediaStorage?
): ContentProblemProjection {
    val availability = MediaReferenceAvailability { reference ->
        mediaStorage?.resolve(reference)?.let { Files.isRegularFile(it) } == true
    }
    return ContentProblemDetector.project(items, availability)
}
