package vn.loi.learning.android.autoplay

import java.util.Random
import vn.loi.learning.android.study.resolveIntroductionPartOfSpeech
import vn.loi.learning.application.contentpackaging.browser.LegacyExampleTranslationProjection
import vn.loi.learning.application.session.LearnEntryScope
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningApplicationContext

class AutoPlayContentSelector(
    private val context: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val resolveMedia: (String) -> String? = { null },
    private val now: () -> Long = System::currentTimeMillis
) {
    fun currentScope(): LearnEntryScope? {
        val libraryId = context.defaultLibraryId ?: return null
        val activePackageId = context.domainLibraryRepository?.findById(libraryId)?.activePackageId ?: return null
        val pkg = context.installedPackageRepository?.findById(activePackageId)
            ?.takeIf { it.libraryId == libraryId && it.state == PackageState.ACTIVE }
            ?: return null
        return LearnEntryScope(learnerId, pkg.id, pkg.topicId)
    }

    fun getPackageName(): String? {
        val scope = currentScope() ?: return null
        return runCatching {
            context.installedPackages.query().firstOrNull { it.id == scope.installedPackageId.value }?.name
        }.getOrNull()
    }

    fun countItemsForSource(source: AutoPlaySource): Int {
        return selectItems(source).size
    }

    fun selectItems(source: AutoPlaySource, randomSeed: Long? = null): List<AutoPlayItem> {
        val scope = currentScope() ?: return emptyList()
        val contentIds = context.packageContentQuery?.getContentIdsForPackage(scope.installedPackageId) ?: return emptyList()
        if (contentIds.isEmpty()) return emptyList()

        val scopedLearningItems = context.learningItemRepository?.findByContentIds(contentIds) ?: return emptyList()
        val enabledItems = scopedLearningItems.filter { it.isEnabled }
        if (enabledItems.isEmpty()) return emptyList()

        val allStates = context.memoryStateRepository?.findAll().orEmpty()
        val stateSnapshot = allStates.filter { it.learnerId == learnerId }
        val statesByItemId = stateSnapshot.associateBy { it.learningItemId }
        val suspendedItemIds = stateSnapshot.filter { it.stage == LearningStage.SUSPENDED }.map { it.learningItemId }.toSet()
        val activeItems = enabledItems.filter { it.id !in suspendedItemIds }
        if (activeItems.isEmpty()) return emptyList()

        val contentIdsToFetch = activeItems.map { it.contentId }.toSet()
        val contentsById: Map<ContentId, Content> = context.contentRepository?.findByIds(contentIdsToFetch)?.associateBy { it.id }.orEmpty()

        val momentNow = Moment(now())

        val selectedContentIds: List<ContentId> = when (source) {
            AutoPlaySource.DUE -> {
                activeItems.asSequence()
                    .filter { item ->
                        statesByItemId[item.id]?.let { state ->
                            state.reviewCount > 0 && state.isDue(momentNow)
                        } == true
                    }
                    .distinctBy { it.contentId }
                    .sortedWith(
                        compareBy<LearningItem>(
                            { statesByItemId[it.id]?.dueAt?.epochMillis ?: Long.MAX_VALUE },
                            { it.contentId.value }
                        )
                    )
                    .map { it.contentId }
                    .toList()
            }
            AutoPlaySource.AGAIN_HARD -> {
                val events = context.reviewEventRepository?.findAll(learnerId).orEmpty()
                val scopedById = activeItems.associateBy { it.id }
                val latestByContent = linkedMapOf<ContentId, ReviewEvent>()
                events.forEach { event ->
                    scopedById[event.learningItemId]?.let { latestByContent[it.contentId] = event }
                }
                latestByContent.entries.asSequence()
                    .filter { it.value.rating == ReviewRating.AGAIN || it.value.rating == ReviewRating.HARD }
                    .mapNotNull { (contentId, event) ->
                        val representative = activeItems.asSequence()
                            .filter { it.contentId == contentId }
                            .sortedBy { it.id.value }
                            .firstOrNull() ?: return@mapNotNull null
                        DifficultSelection(representative, event.rating, event.reviewedAt, statesByItemId[representative.id])
                    }
                    .sortedWith(
                        compareBy<DifficultSelection>(
                            { if (it.rating == ReviewRating.AGAIN) 0 else 1 },
                            { it.state?.isDue(momentNow) != true },
                            { it.reviewedAt.epochMillis },
                            { it.item.id.value }
                        )
                    )
                    .map { it.item.contentId }
                    .toList()
            }
            AutoPlaySource.LEARNED -> {
                val states = stateSnapshot
                    .filter { it.reviewCount > 0 && it.lastReviewedAt != null }
                    .associateBy { it.learningItemId }
                val events = context.reviewEventRepository?.findAll(learnerId).orEmpty()
                val latestEventAt = linkedMapOf<LearningItemId, Moment>()
                events.forEach { latestEventAt[it.learningItemId] = it.reviewedAt }
                activeItems.asSequence()
                    .filter { it.id in states || it.id in latestEventAt }
                    .distinctBy { it.contentId }
                    .sortedWith(
                        compareBy<LearningItem>(
                            { states[it.id]?.isDue(momentNow) != true },
                            { states[it.id]?.lastReviewedAt?.epochMillis ?: latestEventAt[it.id]?.epochMillis ?: Long.MAX_VALUE },
                            { it.id.value }
                        )
                    )
                    .map { it.contentId }
                    .toList()
            }
            AutoPlaySource.RANDOM_LEARNED -> {
                val states = stateSnapshot
                    .filter { it.reviewCount > 0 && it.lastReviewedAt != null }
                    .associateBy { it.learningItemId }
                val events = context.reviewEventRepository?.findAll(learnerId).orEmpty()
                val latestEventAt = linkedMapOf<LearningItemId, Moment>()
                events.forEach { latestEventAt[it.learningItemId] = it.reviewedAt }
                val learned = activeItems.asSequence()
                    .filter { it.id in states || it.id in latestEventAt }
                    .distinctBy { it.contentId }
                    .map { it.contentId }
                    .toList()
                val rng = if (randomSeed != null) Random(randomSeed) else Random()
                learned.shuffled(rng)
            }
            AutoPlaySource.RANDOM_ALL -> {
                val allDistinct = activeItems.asSequence()
                    .distinctBy { it.contentId }
                    .map { it.contentId }
                    .toList()
                val rng = if (randomSeed != null) Random(randomSeed) else Random()
                allDistinct.shuffled(rng)
            }
        }

        return selectedContentIds.mapNotNull { cid ->
            contentsById[cid]?.let(::mapContentToAutoPlayItem)
        }
    }

    private fun mapContentToAutoPlayItem(content: Content): AutoPlayItem {
        val projectedExample = LegacyExampleTranslationProjection.project(
            content.text.exampleText,
            content.text.exampleTranslation
        )
        return AutoPlayItem(
            contentId = content.id,
            headword = content.text.primaryText,
            ipa = content.text.pronunciation?.takeIf { it.isNotBlank() },
            partOfSpeech = resolveIntroductionPartOfSpeech(content),
            vietnameseMeaning = content.text.translatedText.orEmpty(),
            englishExample = projectedExample.exampleText?.takeIf { it.isNotBlank() },
            vietnameseExample = projectedExample.exampleTranslation?.takeIf { it.isNotBlank() },
            wordAudioPath = content.media.primaryAudio?.let(resolveMedia),
            meaningAudioPath = content.media.translatedAudio?.let(resolveMedia),
            exampleAudioPath = content.media.exampleAudio?.let(resolveMedia),
            exampleTranslatedAudioPath = content.media.exampleTranslatedAudio?.let(resolveMedia),
            imagePath = content.media.image?.let(resolveMedia)
        )
    }

    private data class DifficultSelection(
        val item: LearningItem,
        val rating: ReviewRating,
        val reviewedAt: Moment,
        val state: MemoryState?
    )
}
