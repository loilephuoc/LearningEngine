package vn.loi.learning.android.reminder

import java.util.Random
import vn.loi.learning.application.contentpackaging.browser.LegacyExampleTranslationProjection
import vn.loi.learning.application.partofspeech.PartOfSpeechExtractor
import vn.loi.learning.application.partofspeech.normalizePronunciation
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningApplicationContext

data class AndroidReminderPackageInfo(
    val id: String,
    val name: String,
    val totalItemCount: Int
)

class AndroidVocabularyReminderCandidateSelector(
    private val context: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val difficultMarkers: AndroidVocabularyReminderDifficultMarkers? = null,
    private val resolveMedia: (String) -> String? = { null },
    private val now: () -> Long = System::currentTimeMillis,
    private val random: Random = Random(),
    private val recentLimit: Int = 10
) {
    private val recentContentIds = ArrayDeque<ContentId>()
    private var lastContextKey: String? = null

    fun getAvailablePackages(): List<AndroidReminderPackageInfo> {
        val libraryId = context.defaultLibraryId ?: return emptyList()
        val packages = context.installedPackageRepository?.findAll().orEmpty()
            .filter { it.libraryId == libraryId && it.state == PackageState.ACTIVE }
            .sortedBy { it.name.value }
        return packages.map { pkg ->
            val count = context.packageContentQuery?.getContentIdsForPackage(pkg.id)?.size ?: 0
            AndroidReminderPackageInfo(
                id = pkg.id.value,
                name = pkg.name.value,
                totalItemCount = count
            )
        }
    }

    fun getPackageName(packageId: String?): String? {
        if (packageId.isNullOrBlank()) return null
        val installedId = InstalledPackageId(packageId)
        return runCatching {
            context.installedPackages.query().firstOrNull { it.id == packageId }?.name
                ?: context.installedPackageRepository?.findById(installedId)?.name?.value
        }.getOrNull()
    }

    fun select(settings: AndroidVocabularyReminderSettings): AndroidVocabularyCandidateSelectionResult {
        if (!settings.enabled) {
            return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.DISABLED
            )
        }
        val packageIdStr = settings.selectedPackageId
            ?: return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.PACKAGE_NOT_SELECTED
            )
        val packageId = InstalledPackageId(packageIdStr)
        val installedPackage = context.installedPackageRepository?.findById(packageId)
            ?: return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE
            )
        if (installedPackage.state != PackageState.ACTIVE) {
            return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE
            )
        }

        val allContentIds = context.packageContentQuery?.getContentIdsForPackage(packageId)
            ?: return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE
            )
        if (allContentIds.isEmpty()) {
            return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.PACKAGE_EMPTY
            )
        }

        val contextKey = "$packageIdStr:${settings.selectionMode.name}"
        if (lastContextKey != contextKey) {
            lastContextKey = contextKey
            recentContentIds.clear()
        }

        val candidates = queryEligibleCandidates(packageId, installedPackage.name.value, allContentIds, settings.selectionMode)
        if (candidates.isEmpty()) {
            return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE
            )
        }

        val available = relaxRecentExclusions(candidates)
        val selected = available[random.nextInt(available.size)]
        remember(selected.contentId)
        return AndroidVocabularyCandidateSelectionResult.Selected(selected)
    }

    fun getReminderReviewQueue(
        packageIdStr: String,
        mode: AndroidVocabularyReminderSelectionMode,
        anchorContentIdStr: String?
    ): AndroidReminderReviewSession? {
        val packageId = InstalledPackageId(packageIdStr)
        val installedPackage = context.installedPackageRepository?.findById(packageId) ?: return null
        val allContentIds = context.packageContentQuery?.getContentIdsForPackage(packageId) ?: return null
        if (allContentIds.isEmpty()) return null

        val candidates = queryEligibleCandidates(packageId, installedPackage.name.value, allContentIds, mode)
        if (candidates.isEmpty()) {
            val anchorId = anchorContentIdStr?.let(::ContentId)
            val anchorContent = if (anchorId != null) context.contentRepository?.findById(anchorId) else null
            if (anchorContent != null) {
                val candidate = anchorContent.toCandidate(packageId, installedPackage.name.value)
                return AndroidReminderReviewSession(
                    packageId = packageIdStr,
                    mode = mode,
                    anchorContentId = anchorContent.id.value,
                    items = listOf(candidate),
                    initialIndex = 0
                )
            }
            return null
        }

        val anchorId = anchorContentIdStr?.let(::ContentId)
        val existingIndex = if (anchorId != null) candidates.indexOfFirst { it.contentId == anchorId } else -1

        val finalItems = if (existingIndex >= 0) {
            candidates
        } else if (anchorId != null) {
            val anchorContent = context.contentRepository?.findById(anchorId)
            if (anchorContent != null) {
                listOf(anchorContent.toCandidate(packageId, installedPackage.name.value)) + candidates
            } else {
                candidates
            }
        } else {
            candidates
        }

        val initialIndex = if (anchorId != null) {
            finalItems.indexOfFirst { it.contentId == anchorId }.coerceAtLeast(0)
        } else 0

        val resolvedAnchorId = if (anchorId != null) {
            finalItems.firstOrNull { it.contentId == anchorId }?.contentId?.value ?: finalItems.first().contentId.value
        } else {
            finalItems.first().contentId.value
        }

        return AndroidReminderReviewSession(
            packageId = packageIdStr,
            mode = mode,
            anchorContentId = resolvedAnchorId,
            items = finalItems,
            initialIndex = initialIndex
        )
    }

    private fun queryEligibleCandidates(
        packageId: InstalledPackageId,
        packageName: String,
        contentIds: Set<ContentId>,
        mode: AndroidVocabularyReminderSelectionMode
    ): List<AndroidVocabularyCandidate> {
        val scopedLearningItems = context.learningItemRepository?.findByContentIds(contentIds).orEmpty()
        val enabledItems = scopedLearningItems.filter { it.isEnabled }
        if (enabledItems.isEmpty()) return emptyList()

        val allStates = (context.memoryStateRepository as? vn.loi.learning.application.port.MemoryStateQuery)?.findAll(learnerId)
            ?: context.memoryStateRepository?.findAll().orEmpty()
        val stateSnapshot = allStates.filter { it.learnerId == learnerId }
        val statesByItemId = stateSnapshot.associateBy { it.learningItemId }
        val suspendedItemIds = stateSnapshot.filter { it.stage == LearningStage.SUSPENDED }.map { it.learningItemId }.toSet()
        val activeItems = enabledItems.filter { it.id !in suspendedItemIds }
        if (activeItems.isEmpty()) return emptyList()

        val itemsByContent = activeItems.groupBy { it.contentId }
        val momentNow = Moment(now())

        val eligibleContentIds: List<ContentId> = when (mode) {
            AndroidVocabularyReminderSelectionMode.AGAIN_HARD -> {
                val events = context.reviewEventRepository?.findAll(learnerId).orEmpty()
                val latestEventByItem = linkedMapOf<LearningItemId, ReviewRating>()
                events.forEach { latestEventByItem[it.learningItemId] = it.rating }
                activeItems.asSequence()
                    .filter { item ->
                        latestEventByItem[item.id] in setOf(ReviewRating.AGAIN, ReviewRating.HARD)
                    }
                    .distinctBy { it.contentId }
                    .map { it.contentId }
                    .toList()
            }
            AndroidVocabularyReminderSelectionMode.DUE -> {
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
            AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED -> {
                val states = stateSnapshot
                    .filter { it.reviewCount > 0 && it.lastReviewedAt != null }
                    .associateBy { it.learningItemId }
                val events = context.reviewEventRepository?.findAll(learnerId).orEmpty()
                val latestEventAt = linkedMapOf<LearningItemId, Moment>()
                events.forEach { latestEventAt[it.learningItemId] = it.reviewedAt }
                activeItems.asSequence()
                    .filter { it.id in states || it.id in latestEventAt }
                    .distinctBy { it.contentId }
                    .map { it.contentId }
                    .toList()
            }
            AndroidVocabularyReminderSelectionMode.RANDOM_ALL -> {
                activeItems.asSequence()
                    .distinctBy { it.contentId }
                    .map { it.contentId }
                    .toList()
            }
            AndroidVocabularyReminderSelectionMode.MARKED_DIFFICULT -> {
                val markedIds = difficultMarkers?.markedContentIds().orEmpty()
                contentIds.filter { it in markedIds }
            }
        }

        if (eligibleContentIds.isEmpty()) return emptyList()

        val contentsById = context.contentRepository?.findByIds(eligibleContentIds.toSet())
            ?.associateBy { it.id }.orEmpty()

        return eligibleContentIds.mapNotNull { cid ->
            contentsById[cid]?.takeIf { it.text.primaryText.isNotBlank() }?.toCandidate(packageId, packageName)
        }
    }

    private fun Content.toCandidate(
        packageId: InstalledPackageId,
        packageName: String
    ): AndroidVocabularyCandidate {
        val pronunciation = normalizePronunciation(text.pronunciation)
        val projectedExample = LegacyExampleTranslationProjection.project(
            text.exampleText,
            text.exampleTranslation
        )
        return AndroidVocabularyCandidate(
            contentId = id,
            packageId = packageId,
            packageName = packageName,
            primaryText = text.primaryText,
            answer = customFields[DEFINITION_FIELD]?.value?.trim()?.takeIf(String::isNotEmpty),
            translation = text.translatedText?.trim()?.takeIf(String::isNotEmpty),
            ipa = pronunciation.ipa,
            partOfSpeech = PartOfSpeechExtractor.primary(this)?.value ?: pronunciation.partOfSpeech,
            imageReference = media.image,
            primaryAudioReference = media.primaryAudio,
            answerAudioReference = media.translatedAudio,
            exampleAudioReference = media.exampleAudio,
            translationAudioReference = media.translatedAudio,
            exampleTranslationAudioReference = media.exampleTranslatedAudio,
            example = projectedExample.exampleText?.trim()?.takeIf(String::isNotEmpty),
            exampleTranslation = projectedExample.exampleTranslation?.trim()?.takeIf(String::isNotEmpty),
            lesson = metadata.lesson,
            section = metadata.section
        )
    }

    private fun relaxRecentExclusions(pool: List<AndroidVocabularyCandidate>): List<AndroidVocabularyCandidate> {
        if (recentLimit <= 0) return pool
        var available = pool.filterNot { it.contentId in recentContentIds }
        while (available.isEmpty() && recentContentIds.isNotEmpty()) {
            recentContentIds.removeFirst()
            available = pool.filterNot { it.contentId in recentContentIds }
        }
        return available.ifEmpty { pool }
    }

    private fun remember(contentId: ContentId) {
        if (recentLimit <= 0) return
        recentContentIds.remove(contentId)
        recentContentIds.addLast(contentId)
        while (recentContentIds.size > recentLimit) recentContentIds.removeFirst()
    }

    companion object {
        private val DEFINITION_FIELD = ContentFieldId("definition")
    }
}
