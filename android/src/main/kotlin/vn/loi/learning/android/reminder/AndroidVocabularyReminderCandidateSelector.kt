package vn.loi.learning.android.reminder

import android.util.Log
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
    private val recentLimit: Int = 10,
    private val shuffleBagStore: LockScreenShuffleBagStore? = null
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

        val candidates = queryEligibleCandidates(packageId, installedPackage.name.value, allContentIds, settings.selectionMode.name)
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

    private val lockScreenRecentContentIds = ArrayDeque<ContentId>()
    private var lastLockScreenContextKey: String? = null
    private val lockScreenRecentLimit: Int = 8

    fun selectLockScreen(settings: AndroidLockScreenVocabularySettings): AndroidVocabularyCandidateSelectionResult {
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

        val candidates = queryEligibleCandidates(packageId, installedPackage.name.value, allContentIds, settings.selectionMode.name)
        if (candidates.isEmpty()) {
            return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE
            )
        }

        val store = shuffleBagStore
        if (store == null) {
            val contextKey = "$packageIdStr:${settings.selectionMode.name}"
            if (lastLockScreenContextKey != contextKey) {
                lastLockScreenContextKey = contextKey
                lockScreenRecentContentIds.clear()
            }
            val available = relaxLockScreenRecentExclusions(candidates)
            val selected = available[random.nextInt(available.size)]
            rememberLockScreen(selected.contentId)
            return AndroidVocabularyCandidateSelectionResult.Selected(selected)
        }

        val candidateMap = candidates.associateBy { it.contentId.value }
        val eligibleIds = candidates.map { it.contentId.value }
        val contextKey = "$packageIdStr:${settings.selectionMode.name}"

        var bagState = store.load(contextKey)

        fun replenishBag(lastPresented: String?): LockScreenShuffleBagState {
            val shuffled = eligibleIds.shuffled(kotlin.random.Random(random.nextLong())).toMutableList()
            if (shuffled.size > 1 && shuffled[0] == lastPresented) {
                val swapIndex = if (shuffled.size > 2) 1 else shuffled.lastIndex
                val tmp = shuffled[0]
                shuffled[0] = shuffled[swapIndex]
                shuffled[swapIndex] = tmp
            }
            val newVersion = (bagState?.cycleVersion ?: 0) + 1
            return LockScreenShuffleBagState(
                cycleVersion = newVersion,
                orderedCandidateIds = shuffled,
                currentIndex = 0,
                lastPresentedCandidateId = lastPresented
            )
        }

        if (bagState == null || bagState.currentIndex >= bagState.orderedCandidateIds.size || bagState.orderedCandidateIds.isEmpty()) {
            bagState = replenishBag(bagState?.lastPresentedCandidateId)
        }

        var candidate: AndroidVocabularyCandidate? = null
        var bagIndex = bagState.currentIndex

        while (bagIndex < bagState.orderedCandidateIds.size) {
            val candId = bagState.orderedCandidateIds[bagIndex]
            val matched = candidateMap[candId]
            bagIndex++
            if (matched != null) {
                candidate = matched
                break
            }
        }

        if (candidate == null) {
            bagState = replenishBag(bagState.lastPresentedCandidateId)
            bagIndex = 0
            while (bagIndex < bagState.orderedCandidateIds.size) {
                val candId = bagState.orderedCandidateIds[bagIndex]
                val matched = candidateMap[candId]
                bagIndex++
                if (matched != null) {
                    candidate = matched
                    break
                }
            }
        }

        if (candidate == null) {
            return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE
            )
        }

        val oldIndex = bagState.currentIndex
        val updatedState = bagState.copy(
            currentIndex = bagIndex,
            lastPresentedCandidateId = candidate.contentId.value
        )
        store.save(contextKey, updatedState)

        Log.i(
            "ShuffleBagAdvance",
            "[ShuffleBagAdvance] cycleVersion=${updatedState.cycleVersion} packageId=$packageIdStr selectionMode=${settings.selectionMode.name} oldIndex=$oldIndex newIndex=$bagIndex candidateId=${candidate.contentId.value} trigger=SCREEN_OFF"
        )

        return AndroidVocabularyCandidateSelectionResult.Selected(candidate)
    }

    private val homeWidgetRecentContentIds = ArrayDeque<ContentId>()
    private var lastHomeWidgetContextKey: String? = null
    private val homeWidgetRecentLimit: Int = 8

    fun resolveCandidate(packageIdStr: String, contentIdStr: String): AndroidVocabularyCandidate? {
        val packageId = InstalledPackageId(packageIdStr)
        val installedPackage = context.installedPackageRepository?.findById(packageId) ?: return null
        if (installedPackage.state != PackageState.ACTIVE) return null
        val contentId = ContentId(contentIdStr)
        val content = context.contentRepository?.findById(contentId) ?: return null
        return content.toCandidate(packageId, installedPackage.name.value)
    }

    fun selectHomeWidget(settings: AndroidHomeVocabularyWidgetSettings): AndroidVocabularyCandidateSelectionResult {
        val packageIdStr = settings.selectedPackageId
            ?: getAvailablePackages().firstOrNull()?.id
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

        val candidates = queryEligibleCandidates(packageId, installedPackage.name.value, allContentIds, settings.selectionMode.name)
        if (candidates.isEmpty()) {
            return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE
            )
        }

        val store = shuffleBagStore
        val candidateMap = candidates.associateBy { it.contentId.value }
        val eligibleIds = candidates.map { it.contentId.value }
        val contextKey = "HOME_WIDGET:$packageIdStr:${settings.selectionMode.name}"

        if (store == null) {
            if (lastHomeWidgetContextKey != contextKey) {
                lastHomeWidgetContextKey = contextKey
                homeWidgetRecentContentIds.clear()
            }
            val filtered = candidates.filter { it.contentId !in homeWidgetRecentContentIds }
            val available = filtered.ifEmpty {
                homeWidgetRecentContentIds.clear()
                candidates
            }
            val selected = available[random.nextInt(available.size)]
            homeWidgetRecentContentIds.addLast(selected.contentId)
            while (homeWidgetRecentContentIds.size > homeWidgetRecentLimit) {
                homeWidgetRecentContentIds.removeFirst()
            }
            return AndroidVocabularyCandidateSelectionResult.Selected(selected)
        }

        var bagState = store.load(contextKey)

        fun replenishBag(lastPresented: String?): LockScreenShuffleBagState {
            val shuffled = eligibleIds.shuffled(kotlin.random.Random(random.nextLong())).toMutableList()
            if (shuffled.size > 1 && shuffled[0] == lastPresented) {
                val swapIndex = if (shuffled.size > 2) 1 else shuffled.lastIndex
                val tmp = shuffled[0]
                shuffled[0] = shuffled[swapIndex]
                shuffled[swapIndex] = tmp
            }
            val newVersion = (bagState?.cycleVersion ?: 0) + 1
            return LockScreenShuffleBagState(
                cycleVersion = newVersion,
                orderedCandidateIds = shuffled,
                currentIndex = 0,
                lastPresentedCandidateId = lastPresented
            )
        }

        if (bagState == null || bagState.currentIndex >= bagState.orderedCandidateIds.size || bagState.orderedCandidateIds.isEmpty()) {
            bagState = replenishBag(bagState?.lastPresentedCandidateId)
        }

        var candidate: AndroidVocabularyCandidate? = null
        var bagIndex = bagState.currentIndex

        while (bagIndex < bagState.orderedCandidateIds.size) {
            val candId = bagState.orderedCandidateIds[bagIndex]
            val matched = candidateMap[candId]
            bagIndex++
            if (matched != null) {
                candidate = matched
                break
            }
        }

        if (candidate == null) {
            bagState = replenishBag(bagState.lastPresentedCandidateId)
            bagIndex = 0
            while (bagIndex < bagState.orderedCandidateIds.size) {
                val candId = bagState.orderedCandidateIds[bagIndex]
                val matched = candidateMap[candId]
                bagIndex++
                if (matched != null) {
                    candidate = matched
                    break
                }
            }
        }

        if (candidate == null) {
            return AndroidVocabularyCandidateSelectionResult.NoCandidate(
                AndroidVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE
            )
        }

        val oldIndex = bagState.currentIndex
        val updatedState = bagState.copy(
            currentIndex = bagIndex,
            lastPresentedCandidateId = candidate.contentId.value
        )
        store.save(contextKey, updatedState)

        Log.i(
            "HomeWidgetCandidate",
            "[HomeWidgetCandidate] action=ADVANCE cycleVersion=${updatedState.cycleVersion} packageId=$packageIdStr selectionMode=${settings.selectionMode.name} oldIndex=$oldIndex newIndex=$bagIndex candidateId=${candidate.contentId.value}"
        )

        return AndroidVocabularyCandidateSelectionResult.Selected(candidate)
    }

    fun getReminderReviewQueue(
        packageIdStr: String,
        mode: AndroidVocabularyReminderSelectionMode,
        anchorContentIdStr: String?
    ): AndroidReminderReviewSession? {
        return getReviewQueue(packageIdStr, mode.name, anchorContentIdStr)
    }

    fun getReviewQueue(
        packageIdStr: String,
        modeStr: String,
        anchorContentIdStr: String?
    ): AndroidReminderReviewSession? {
        val packageId = InstalledPackageId(packageIdStr)
        val installedPackage = context.installedPackageRepository?.findById(packageId) ?: return null
        val allContentIds = context.packageContentQuery?.getContentIdsForPackage(packageId) ?: return null
        if (allContentIds.isEmpty()) return null

        val candidates = queryEligibleCandidates(packageId, installedPackage.name.value, allContentIds, modeStr)
        val reminderMode = runCatching {
            AndroidVocabularyReminderSelectionMode.valueOf(modeStr)
        }.getOrDefault(AndroidVocabularyReminderSelectionMode.AGAIN_HARD)

        if (candidates.isEmpty()) {
            val anchorId = anchorContentIdStr?.let(::ContentId)
            val anchorContent = if (anchorId != null) context.contentRepository?.findById(anchorId) else null
            if (anchorContent != null) {
                val candidate = anchorContent.toCandidate(packageId, installedPackage.name.value)
                return AndroidReminderReviewSession(
                    packageId = packageIdStr,
                    mode = reminderMode,
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
            mode = reminderMode,
            anchorContentId = resolvedAnchorId,
            items = finalItems,
            initialIndex = initialIndex
        )
    }

    private fun queryEligibleCandidates(
        packageId: InstalledPackageId,
        packageName: String,
        contentIds: Set<ContentId>,
        modeName: String
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

        val momentNow = Moment(now())

        val eligibleContentIds: List<ContentId> = when (modeName) {
            "AGAIN_HARD" -> {
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
            "DUE" -> {
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
            "NEW_UNSEEN" -> {
                val reviewedItemIds = buildSet {
                    addAll(context.reviewEventRepository?.findAll(learnerId).orEmpty().map { it.learningItemId })
                    addAll(stateSnapshot.filter { it.reviewCount > 0 || it.lastReviewedAt != null }.map { it.learningItemId })
                }
                activeItems.groupBy { it.contentId }.entries.asSequence()
                    .filter { (_, items) ->
                        items.isNotEmpty() && items.none { it.id in reviewedItemIds }
                    }
                    .map { it.key }
                    .toList()
            }
            "RANDOM_LEARNED" -> {
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
            "RANDOM_ALL" -> {
                activeItems.asSequence()
                    .distinctBy { it.contentId }
                    .map { it.contentId }
                    .toList()
            }
            "MARKED_DIFFICULT" -> {
                val markedIds = difficultMarkers?.markedContentIds().orEmpty()
                contentIds.filter { it in markedIds }
            }
            else -> emptyList()
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

    private fun relaxLockScreenRecentExclusions(pool: List<AndroidVocabularyCandidate>): List<AndroidVocabularyCandidate> {
        if (lockScreenRecentLimit <= 0) return pool
        var available = pool.filterNot { it.contentId in lockScreenRecentContentIds }
        while (available.isEmpty() && lockScreenRecentContentIds.isNotEmpty()) {
            lockScreenRecentContentIds.removeFirst()
            available = pool.filterNot { it.contentId in lockScreenRecentContentIds }
        }
        return available.ifEmpty { pool }
    }

    private fun rememberLockScreen(contentId: ContentId) {
        if (lockScreenRecentLimit <= 0) return
        lockScreenRecentContentIds.remove(contentId)
        lockScreenRecentContentIds.addLast(contentId)
        while (lockScreenRecentContentIds.size > lockScreenRecentLimit) lockScreenRecentContentIds.removeFirst()
    }

    companion object {
        private val DEFINITION_FIELD = ContentFieldId("definition")
    }
}
