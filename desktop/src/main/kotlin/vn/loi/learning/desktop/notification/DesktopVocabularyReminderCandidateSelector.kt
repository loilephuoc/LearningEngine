package vn.loi.learning.desktop.notification

import java.time.Clock
import kotlin.random.Random
import vn.loi.learning.application.partofspeech.PartOfSpeechExtractor
import vn.loi.learning.application.partofspeech.normalizePronunciation
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

fun interface DesktopVocabularyCandidateChooser {
    fun chooseIndex(candidateCount: Int): Int
}

class RandomDesktopVocabularyCandidateChooser(
    private val random: Random = Random.Default
) : DesktopVocabularyCandidateChooser {
    override fun chooseIndex(candidateCount: Int): Int = random.nextInt(candidateCount)
}

class DesktopVocabularyReminderCandidateSelector(
    private val installedPackages: DesktopInstalledPackageReadSource,
    private val packageContents: DesktopPackageContentReadSource,
    private val contents: DesktopContentReadSource,
    private val learningItems: DesktopLearningItemReadSource,
    private val memoryStates: DesktopMemoryStateReadSource,
    private val contentLearningStates: DesktopContentLearningStateReadSource,
    private val learnerId: LearnerId,
    private val clock: Clock,
    private val markedContent: DesktopVocabularyReminderMarkedReadSource = DesktopVocabularyReminderMarkedReadSource { false },
    private val chooser: DesktopVocabularyCandidateChooser = RandomDesktopVocabularyCandidateChooser(),
    private val recentLimit: Int = DEFAULT_RECENT_LIMIT
) : DesktopVocabularyReminderSelectionSource {
    private data class SelectionContext(
        val packageId: InstalledPackageId,
        val mode: DesktopVocabularyReminderSelectionMode
    )

    private var context: SelectionContext? = null
    private val recentContentIds = ArrayDeque<ContentId>()

    init {
        require(recentLimit >= 0) { "Recent candidate limit must not be negative." }
    }

    override fun select(settings: DesktopVocabularyReminderSettings): DesktopVocabularyCandidateSelectionResult {
        if (!settings.enabled) return noCandidate(DesktopVocabularyCandidateSelectionResult.Reason.DISABLED)
        val packageId = settings.selectedPackageId
            ?: return noCandidate(DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_NOT_SELECTED)
        reconcileContext(packageId, settings.selectionMode)

        val installedPackage = installedPackages.findById(packageId)
            ?: return noCandidate(DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE)
        val contentIds = packageContents.findContentIds(packageId)
            ?: return noCandidate(DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE)
        if (contentIds.isEmpty()) {
            return noCandidate(DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_EMPTY)
        }

        val itemsByContent = learningItems.findByContentIds(contentIds).groupBy(LearningItem::contentId)
        val statesByItem = memoryStates.findAll(learnerId).associateBy(MemoryState::learningItemId)
        val learningStateByContent = contentLearningStates.resolveAll(learnerId, contentIds)
        val now = Moment(clock.millis())
        val eligibleIds = contentIds.asSequence()
            .filter { contentId ->
                val enabledItems = itemsByContent[contentId].orEmpty().filter(LearningItem::isEnabled)
                enabledItems.isNotEmpty() &&
                    !enabledItems.isSuspendedOnly(statesByItem) &&
                    qualifies(
                        settings.selectionMode,
                        enabledItems,
                        statesByItem,
                        learningStateByContent[contentId],
                        now
                    )
            }
            .toSet()
        val eligibleContents = contents.findByIds(eligibleIds)
            .asSequence()
            .filter { it.id in eligibleIds && it.text.primaryText.isNotBlank() }
            .sortedBy { it.id.value }
            .toList()
        if (eligibleContents.isEmpty()) {
            return noCandidate(DesktopVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE)
        }

        val available = relaxRecentExclusions(eligibleContents)
        val index = chooser.chooseIndex(available.size)
        require(index in available.indices) { "Candidate chooser returned an out-of-range index." }
        val selected = available[index]
        remember(selected.id)
        return DesktopVocabularyCandidateSelectionResult.Selected(
            selected.toCandidate(packageId, installedPackage.name)
        )
    }

    private fun qualifies(
        mode: DesktopVocabularyReminderSelectionMode,
        enabledItems: List<LearningItem>,
        statesByItem: Map<vn.loi.learning.domain.study.learning.model.LearningItemId, MemoryState>,
        learningState: vn.loi.learning.application.study.ContentLearningState?,
        now: Moment
    ): Boolean = when (mode) {
        DesktopVocabularyReminderSelectionMode.AGAIN_HARD ->
            learningState?.latestEffectiveRating in setOf(ReviewRating.AGAIN, ReviewRating.HARD)
        DesktopVocabularyReminderSelectionMode.DUE ->
            learningState?.isLearned == true && enabledItems.any { item ->
                statesByItem[item.id]?.let { state -> state.reviewCount > 0 && state.isDue(now) } == true
            }
        DesktopVocabularyReminderSelectionMode.RANDOM_LEARNED -> learningState?.isLearned == true
        DesktopVocabularyReminderSelectionMode.RANDOM_ALL -> true
        DesktopVocabularyReminderSelectionMode.MARKED_DIFFICULT ->
            enabledItems.firstOrNull()?.contentId?.let(markedContent::isMarked) == true
    }

    private fun List<LearningItem>.isSuspendedOnly(
        statesByItem: Map<vn.loi.learning.domain.study.learning.model.LearningItemId, MemoryState>
    ): Boolean = all { item -> statesByItem[item.id]?.stage == LearningStage.SUSPENDED }

    private fun relaxRecentExclusions(pool: List<Content>): List<Content> {
        if (recentLimit == 0) return pool
        var available = pool.filterNot { it.id in recentContentIds }
        while (available.isEmpty() && recentContentIds.isNotEmpty()) {
            recentContentIds.removeFirst()
            available = pool.filterNot { it.id in recentContentIds }
        }
        return available.ifEmpty { pool }
    }

    private fun remember(contentId: ContentId) {
        if (recentLimit == 0) return
        recentContentIds.remove(contentId)
        recentContentIds.addLast(contentId)
        while (recentContentIds.size > recentLimit) recentContentIds.removeFirst()
    }

    private fun reconcileContext(
        packageId: InstalledPackageId,
        mode: DesktopVocabularyReminderSelectionMode
    ) {
        val next = SelectionContext(packageId, mode)
        if (context != next) {
            context = next
            recentContentIds.clear()
        }
    }

    private fun Content.toCandidate(
        packageId: InstalledPackageId,
        packageName: String
    ): DesktopVocabularyCandidate {
        val pronunciation = normalizePronunciation(text.pronunciation)
        return DesktopVocabularyCandidate(
            contentId = id,
            installedPackageId = packageId,
            packageDisplayName = packageName,
            primaryText = text.primaryText,
            answer = customFields[DEFINITION_FIELD]?.value?.trim()?.takeIf(String::isNotEmpty),
            translation = text.translatedText?.trim()?.takeIf(String::isNotEmpty),
            ipa = pronunciation.ipa,
            partOfSpeech = PartOfSpeechExtractor.primary(this)?.value ?: pronunciation.partOfSpeech,
            imageReference = media.image,
            primaryAudioReference = media.primaryAudio,
            translatedAudioReference = media.translatedAudio,
            lesson = metadata.lesson,
            section = metadata.section
        )
    }

    private fun noCandidate(reason: DesktopVocabularyCandidateSelectionResult.Reason) =
        DesktopVocabularyCandidateSelectionResult.NoCandidate(reason)

    companion object {
        const val DEFAULT_RECENT_LIMIT = 10
        private val DEFINITION_FIELD = ContentFieldId("definition")
    }
}
