package vn.loi.learning.infrastructure.sync

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.domain.sync.model.ContentDeltaOperation
import vn.loi.learning.domain.sync.model.ContentDeltaRecord
import vn.loi.learning.domain.sync.model.MediaSyncItem
import vn.loi.learning.domain.sync.model.MediaSyncManifest
import vn.loi.learning.domain.sync.model.SyncConflict
import vn.loi.learning.domain.sync.model.SyncConflictType
import vn.loi.learning.domain.sync.model.SyncPreviewReport
import vn.loi.learning.domain.sync.model.SyncResultSummary
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord
import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord

class SyncEngine(
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val memoryStateRepository: MemoryStateRepository,
    private val reviewEventRepository: ReviewEventRepository,
    private val installedPackageRepository: InstalledPackageRepository,
    private val contentLibraryRepository: ContentLibraryRepository? = null,
    private val contentPackageRepository: ContentPackageRepository? = null,
    private val mediaStorage: ContentMediaStorage? = null,
    private val transactionRunner: TransactionRunner
) {
    fun isPackageBaselined(packageId: String): Boolean {
        val all = installedPackageRepository.findAll()
        return all.any { it.packageId.value == packageId || it.id.value == packageId }
    }

    fun getMediaHashesForPackages(packageIds: Set<String>? = null): Set<String> {
        if (mediaStorage == null) return emptySet()
        val allContents = if (packageIds != null) {
            val targetContentIds = resolveContentIdsForPackages(packageIds)
            if (targetContentIds.isNotEmpty()) contentRepository.findAll().filter { it.id in targetContentIds }
            else contentRepository.findAll()
        } else contentRepository.findAll()

        val hashes = mutableSetOf<String>()
        allContents.forEach { content ->
            listOfNotNull(
                content.media.primaryAudio,
                content.media.translatedAudio,
                content.media.image,
                content.media.exampleAudio,
                content.media.exampleTranslatedAudio
            ).forEach { ref ->
                val resolved = mediaStorage.resolve(ref)
                if (resolved != null && Files.isRegularFile(resolved)) {
                    hashes += PortableSyncPackageService.sha256(resolved)
                }
            }
        }
        return hashes
    }

    fun exportSyncPackage(
        target: Path,
        sourcePlatform: String,
        sourceDeviceId: String? = null,
        specificPackageIds: Set<String>? = null,
        knownRemoteMediaHashes: Set<String> = emptySet(),
        includeReviewEvents: Boolean = true
    ): Path {
        val installedPackages = installedPackageRepository.findAll()
            .filter { specificPackageIds == null || it.packageId.value in specificPackageIds || it.id.value in specificPackageIds }

        val targetPackageIds = if (specificPackageIds != null) {
            installedPackages.map { it.packageId.value }.ifEmpty { specificPackageIds.toList() }.distinct()
        } else {
            installedPackages.map { it.packageId.value }.distinct()
        }

        val allContents = contentRepository.findAll()
        val scopedContents = if (specificPackageIds != null) {
            val targetContentIds = resolveContentIdsForPackages(specificPackageIds)
            if (targetContentIds.isNotEmpty()) allContents.filter { it.id in targetContentIds } else allContents
        } else allContents

        val scopedContentIdSet = scopedContents.map { it.id }.toSet()

        // Collect content deltas
        val contentDeltas = scopedContents.map { content ->
            ContentDeltaRecord(
                contentId = content.id.value,
                operation = ContentDeltaOperation.UPSERT,
                type = content.type.name,
                primaryText = content.text.primaryText,
                translatedText = content.text.translatedText,
                pronunciation = content.text.pronunciation,
                exampleText = content.text.exampleText,
                exampleTranslation = content.text.exampleTranslation,
                primaryAudio = content.media.primaryAudio,
                translatedAudio = content.media.translatedAudio,
                image = content.media.image,
                exampleAudio = content.media.exampleAudio,
                exampleTranslatedAudio = content.media.exampleTranslatedAudio,
                customFields = content.customFields.fields.associate { it.id.value to it.value },
                title = content.metadata.title,
                tags = content.metadata.tags,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        }

        // Collect media references
        val mediaFilesToPackage = mutableMapOf<String, Path>()
        val mediaSyncItems = mutableListOf<MediaSyncItem>()

        if (mediaStorage != null) {
            scopedContents.forEach { content ->
                listOfNotNull(
                    content.media.primaryAudio,
                    content.media.translatedAudio,
                    content.media.image,
                    content.media.exampleAudio,
                    content.media.exampleTranslatedAudio
                ).forEach { ref ->
                    val resolvedPath = mediaStorage.resolve(ref)
                    if (resolvedPath != null && Files.isRegularFile(resolvedPath)) {
                        val sha = PortableSyncPackageService.sha256(resolvedPath)
                        val size = Files.size(resolvedPath)
                        val isKnownRemote = sha in knownRemoteMediaHashes
                        mediaSyncItems += MediaSyncItem(
                            relativePath = ref,
                            sizeBytes = size,
                            sha256 = sha,
                            isContainedInSyncPackage = !isKnownRemote
                        )
                        if (!isKnownRemote) {
                            mediaFilesToPackage[ref] = resolvedPath
                        }
                    }
                }
            }
        }

        // Collect review events for scoped items
        val reviewEventRecords = if (includeReviewEvents) {
            val allReviewEvents = reviewEventRepository.findAll()
            val filteredEvents = if (specificPackageIds != null) {
                val scopedItemIds = learningItemRepository.findAll()
                    .filter { it.contentId in scopedContentIdSet }
                    .map { it.id }
                    .toSet()
                allReviewEvents.filter { it.learningItemId in scopedItemIds }
            } else allReviewEvents

            filteredEvents.map { event ->
                ReviewEventRecord(
                    schemaVersion = ReviewEventRecord.CURRENT_SCHEMA_VERSION,
                    id = event.id.value,
                    rating = event.rating.name,
                    reviewedAtEpochMillis = event.reviewedAt.epochMillis,
                    responseTimeMillis = event.responseTime?.millis,
                    stateBefore = toMemoryStateRecord(event.stateBefore),
                    stateAfter = toMemoryStateRecord(event.stateAfter),
                    source = event.source.name
                )
            }
        } else emptyList()

        return PortableSyncPackageService.writeSyncPackage(
            target = target,
            sourcePlatform = sourcePlatform,
            sourceDeviceId = sourceDeviceId,
            packageIds = targetPackageIds,
            contentDeltas = contentDeltas,
            reviewEvents = reviewEventRecords,
            mediaManifest = MediaSyncManifest(mediaSyncItems.distinctBy { it.relativePath }),
            mediaFiles = mediaFilesToPackage
        )
    }

    fun previewSyncPackage(source: Path): StagedSyncPayload {
        val tempDir = Files.createTempDirectory(".sync-preview-")
        try {
            return PortableSyncPackageService.readAndValidateSyncPackage(source, tempDir)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    fun generatePreviewReport(source: Path): SyncPreviewReport {
        val tempDir = Files.createTempDirectory(".sync-preview-report-")
        try {
            val payload = PortableSyncPackageService.readAndValidateSyncPackage(source, tempDir)
            val targetPackageIds = payload.manifest.packageIds
            val installedLocalPackages = installedPackageRepository.findAll()
            val installedPackageIds = (installedLocalPackages.map { it.packageId.value } +
                installedLocalPackages.map { it.id.value }).toSet()

            val missingBaseline = targetPackageIds.filter { it !in installedPackageIds }
            val requiresFullBackup = missingBaseline.isNotEmpty()

            val conflicts = mutableListOf<SyncConflict>()
            payload.contentDeltas.forEach { delta ->
                val existing = contentRepository.findById(ContentId(delta.contentId))
                if (existing != null) {
                    val isTextDifferent = existing.text.primaryText != delta.primaryText ||
                        existing.text.translatedText != delta.translatedText ||
                        existing.text.exampleText != delta.exampleText ||
                        existing.text.exampleTranslation != delta.exampleTranslation
                    if (isTextDifferent && existing.text.primaryText != delta.primaryText) {
                        conflicts += SyncConflict(
                            conflictType = SyncConflictType.CONTENT_FIELD_COLLISION,
                            entityId = delta.contentId,
                            fieldName = "primaryText",
                            localValueSummary = existing.text.primaryText,
                            incomingValueSummary = delta.primaryText,
                            resolutionApplied = ConflictResolutionStrategy.MERGE_FIELD_LEVEL,
                            detail = "Local text differs from incoming change."
                        )
                    }
                }
            }

            val existingEvents = reviewEventRepository.findAll().map { it.id.value }.toSet()
            val deduplicatedReviewCount = payload.reviewEvents.count { it.id in existingEvents }
            val newReviewCount = payload.reviewEvents.size - deduplicatedReviewCount

            val mediaReusedCount = payload.mediaManifest.items.count { !it.isContainedInSyncPackage }
            val newMediaBytes = payload.mediaFiles.values.sumOf { Files.size(it) }

            val warnings = mutableListOf<String>()
            if (requiresFullBackup) {
                warnings += "Gói (${missingBaseline.joinToString()}) chưa tồn tại trên thiết bị này. Khuyến nghị thực hiện Sao lưu toàn diện (.lebak) để thiết lập baseline."
            }

            return SyncPreviewReport(
                syncPackageId = payload.manifest.syncPackageId,
                sourcePlatform = payload.manifest.sourcePlatform,
                createdAtUtc = payload.manifest.createdAtUtc,
                packagesAffected = targetPackageIds,
                contentChangesCount = payload.contentDeltas.size,
                newMediaCount = payload.mediaFiles.size,
                newMediaBytes = newMediaBytes,
                existingMediaReusedCount = mediaReusedCount,
                reviewEventsCount = newReviewCount,
                reviewEventsDeduplicatedCount = deduplicatedReviewCount,
                conflicts = conflicts,
                missingBaselinePackageIds = missingBaseline,
                requiresFullBackup = requiresFullBackup,
                warnings = warnings
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    fun importSyncPackage(
        source: Path,
        conflictStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.MERGE_FIELD_LEVEL
    ): SyncResultSummary {
        val tempStaging = Files.createTempDirectory(".sync-import-")
        try {
            val payload = PortableSyncPackageService.readAndValidateSyncPackage(source, tempStaging)
            return transactionRunner.runInTransaction {
                applySyncPayloadLocked(payload, conflictStrategy)
            }
        } finally {
            tempStaging.toFile().deleteRecursively()
        }
    }

    private fun applySyncPayloadLocked(
        payload: StagedSyncPayload,
        conflictStrategy: ConflictResolutionStrategy
    ): SyncResultSummary {
        val conflicts = mutableListOf<SyncConflict>()
        var contentAppliedCount = 0
        var mediaAddedCount = 0
        var reviewMergedCount = 0
        var reviewDeduplicatedCount = 0

        // 1. Copy Media Assets into ContentMediaStorage
        if (mediaStorage != null) {
            payload.mediaFiles.forEach { (relPath, stagedPath) ->
                val resolved = mediaStorage.resolve(relPath)
                val needsCopy = resolved == null || !Files.exists(resolved) ||
                    PortableSyncPackageService.sha256(resolved) != PortableSyncPackageService.sha256(stagedPath)
                if (needsCopy) {
                    val packageName = if ('/' in relPath) relPath.substringBefore('/') else "shared"
                    val fileName = if ('/' in relPath) relPath.substringAfter('/') else relPath
                    mediaStorage.storeStream(packageName, fileName, stagedPath)
                    mediaAddedCount++
                }
            }
        }

        // 2. Merge Content Deltas
        payload.contentDeltas.forEach { delta ->
            val contentId = ContentId(delta.contentId)
            val existing = contentRepository.findById(contentId)
            val customFieldsSet = delta.customFields.map { ContentCustomField(ContentFieldId(it.key), it.value) }.toSet()

            if (existing == null) {
                // New Content
                val newContent = Content(
                    id = contentId,
                    type = ContentType.valueOf(delta.type),
                    text = ContentText(
                        primaryText = delta.primaryText,
                        translatedText = delta.translatedText,
                        pronunciation = delta.pronunciation,
                        exampleText = delta.exampleText,
                        exampleTranslation = delta.exampleTranslation
                    ),
                    media = ContentMedia(
                        primaryAudio = delta.primaryAudio,
                        translatedAudio = delta.translatedAudio,
                        image = delta.image,
                        exampleAudio = delta.exampleAudio,
                        exampleTranslatedAudio = delta.exampleTranslatedAudio
                    ),
                    metadata = ContentMetadata(
                        title = delta.title,
                        tags = delta.tags
                    ),
                    customFields = ContentCustomFields(customFieldsSet)
                )
                contentRepository.save(newContent)
                // Also create default LearningItem if none exists
                val defaultItemId = LearningItemId("item-" + delta.contentId)
                if (learningItemRepository.findById(defaultItemId) == null) {
                    learningItemRepository.save(
                        LearningItem(
                            id = defaultItemId,
                            contentId = contentId,
                            mode = LearningMode.MEANING_RECOGNITION
                        )
                    )
                }
                contentAppliedCount++
            } else {
                // Existing Content: Check for differences and merge
                val isTextDifferent = existing.text.primaryText != delta.primaryText ||
                    existing.text.translatedText != delta.translatedText ||
                    existing.text.pronunciation != delta.pronunciation ||
                    existing.text.exampleText != delta.exampleText ||
                    existing.text.exampleTranslation != delta.exampleTranslation

                val isMediaDifferent = existing.media.primaryAudio != delta.primaryAudio ||
                    existing.media.translatedAudio != delta.translatedAudio ||
                    existing.media.image != delta.image ||
                    existing.media.exampleAudio != delta.exampleAudio ||
                    existing.media.exampleTranslatedAudio != delta.exampleTranslatedAudio

                val existingMap = existing.customFields.fields.associate { it.id.value to it.value }
                val isCustomDifferent = existingMap != delta.customFields

                if (isTextDifferent || isMediaDifferent || isCustomDifferent) {
                    val mergedMap = existingMap + delta.customFields
                    val mergedCustomFields = ContentCustomFields(
                        mergedMap.map { ContentCustomField(ContentFieldId(it.key), it.value) }.toSet()
                    )

                    val mergedContent = when (conflictStrategy) {
                        ConflictResolutionStrategy.PRESERVE_LOCAL -> existing
                        ConflictResolutionStrategy.APPLY_INCOMING -> {
                            existing.copy(
                                text = ContentText(
                                    primaryText = delta.primaryText.ifBlank { existing.text.primaryText },
                                    translatedText = delta.translatedText ?: existing.text.translatedText,
                                    pronunciation = delta.pronunciation ?: existing.text.pronunciation,
                                    exampleText = delta.exampleText ?: existing.text.exampleText,
                                    exampleTranslation = delta.exampleTranslation ?: existing.text.exampleTranslation
                                ),
                                media = ContentMedia(
                                    primaryAudio = delta.primaryAudio ?: existing.media.primaryAudio,
                                    translatedAudio = delta.translatedAudio ?: existing.media.translatedAudio,
                                    image = delta.image ?: existing.media.image,
                                    exampleAudio = delta.exampleAudio ?: existing.media.exampleAudio,
                                    exampleTranslatedAudio = delta.exampleTranslatedAudio ?: existing.media.exampleTranslatedAudio
                                ),
                                customFields = mergedCustomFields,
                                metadata = existing.metadata.copy(
                                    title = delta.title ?: existing.metadata.title,
                                    tags = existing.metadata.tags + delta.tags
                                )
                            )
                        }
                        ConflictResolutionStrategy.MERGE_FIELD_LEVEL -> {
                            existing.copy(
                                text = ContentText(
                                    primaryText = if (delta.primaryText.isNotBlank()) delta.primaryText else existing.text.primaryText,
                                    translatedText = delta.translatedText ?: existing.text.translatedText,
                                    pronunciation = delta.pronunciation ?: existing.text.pronunciation,
                                    exampleText = delta.exampleText ?: existing.text.exampleText,
                                    exampleTranslation = delta.exampleTranslation ?: existing.text.exampleTranslation
                                ),
                                media = ContentMedia(
                                    primaryAudio = delta.primaryAudio ?: existing.media.primaryAudio,
                                    translatedAudio = delta.translatedAudio ?: existing.media.translatedAudio,
                                    image = delta.image ?: existing.media.image,
                                    exampleAudio = delta.exampleAudio ?: existing.media.exampleAudio,
                                    exampleTranslatedAudio = delta.exampleTranslatedAudio ?: existing.media.exampleTranslatedAudio
                                ),
                                customFields = mergedCustomFields,
                                metadata = existing.metadata.copy(
                                    title = delta.title ?: existing.metadata.title,
                                    tags = existing.metadata.tags + delta.tags
                                )
                            )
                        }
                    }

                    if (mergedContent != existing) {
                        contentRepository.save(mergedContent)
                        contentAppliedCount++
                    }

                    if (isTextDifferent && existing.text.primaryText != delta.primaryText) {
                        conflicts += SyncConflict(
                            conflictType = SyncConflictType.CONTENT_FIELD_COLLISION,
                            entityId = delta.contentId,
                            fieldName = "primaryText",
                            localValueSummary = existing.text.primaryText,
                            incomingValueSummary = delta.primaryText,
                            resolutionApplied = conflictStrategy,
                            detail = "Field primaryText merged according to $conflictStrategy"
                        )
                    }
                }
            }
        }

        // 3. Merge Review Events
        val existingEvents = reviewEventRepository.findAll()
        val existingEventIds = existingEvents.map { it.id.value }.toSet()

        val learnerItemsToReconcile = mutableSetOf<Pair<LearnerId, LearningItemId>>()

        payload.reviewEvents.forEach { rec ->
            if (rec.id in existingEventIds) {
                reviewDeduplicatedCount++
            } else {
                val stateBefore = fromMemoryStateRecord(rec.stateBefore)
                val stateAfter = fromMemoryStateRecord(rec.stateAfter)
                val event = ReviewEvent(
                    id = ReviewEventId(rec.id),
                    rating = ReviewRating.valueOf(rec.rating),
                    reviewedAt = Moment(rec.reviewedAtEpochMillis),
                    responseTime = rec.responseTimeMillis?.let(::TimeSpan),
                    stateBefore = stateBefore,
                    stateAfter = stateAfter,
                    source = runCatching { RatingSource.valueOf(rec.source) }.getOrDefault(RatingSource.STANDARD_REVIEW)
                )
                reviewEventRepository.append(event)
                reviewMergedCount++
                learnerItemsToReconcile += (event.learnerId to event.learningItemId)
            }
        }

        // 4. Reconcile MemoryState for affected items to reflect latest timeline
        learnerItemsToReconcile.forEach { (learnerId, itemId) ->
            val allItemEvents = reviewEventRepository.findAll(learnerId, itemId)
                .sortedBy { it.reviewedAt.epochMillis }
            val latestEvent = allItemEvents.lastOrNull()
            if (latestEvent != null) {
                val currentMemory = memoryStateRepository.find(learnerId, itemId)
                if (currentMemory == null || latestEvent.reviewedAt >= currentMemory.lastReviewedAt ?: Moment(0)) {
                    memoryStateRepository.save(latestEvent.stateAfter)
                }
            }
        }

        return SyncResultSummary(
            syncPackageId = payload.manifest.syncPackageId,
            sourcePlatform = payload.manifest.sourcePlatform,
            contentDeltasApplied = contentAppliedCount,
            mediaAssetsAdded = mediaAddedCount,
            reviewEventsMerged = reviewMergedCount,
            reviewEventsDeduplicated = reviewDeduplicatedCount,
            conflicts = conflicts,
            success = true,
            message = "Sync applied: $contentAppliedCount content updates, $mediaAddedCount media assets, $reviewMergedCount review events merged ($reviewDeduplicatedCount deduplicated)."
        )
    }

    private fun resolveContentIdsForPackages(packageIds: Set<String>): Set<ContentId> {
        val targetContentIds = mutableSetOf<ContentId>()
        val installed = installedPackageRepository.findAll()
            .filter { it.packageId.value in packageIds || it.id.value in packageIds }
        val packageIdValues = (installed.map { it.packageId.value } + packageIds).toSet()

        contentPackageRepository?.findAll()
            ?.filter { it.id.value in packageIdValues }
            ?.forEach { cp ->
                cp.libraryIds.forEach { libId ->
                    contentLibraryRepository?.findById(libId)?.contentIds?.let { targetContentIds.addAll(it) }
                }
            }

        if (targetContentIds.isEmpty()) {
            contentRepository.findAll().forEach { content ->
                val mediaRef = content.media.primaryAudio ?: content.media.image
                if (mediaRef != null && packageIdValues.any { mediaRef.startsWith(it) }) {
                    targetContentIds.add(content.id)
                }
            }
        }
        return targetContentIds
    }

    private fun toMemoryStateRecord(state: MemoryState): MemoryStateRecord = MemoryStateRecord(
        schemaVersion = MemoryStateRecord.CURRENT_SCHEMA_VERSION,
        learnerId = state.learnerId.value,
        learningItemId = state.learningItemId.value,
        stage = state.stage.name,
        difficulty = state.difficulty,
        stabilityDays = state.stabilityDays,
        dueAtEpochMillis = state.dueAt.epochMillis,
        lastReviewedAtEpochMillis = state.lastReviewedAt?.epochMillis,
        reviewCount = state.reviewCount,
        lapseCount = state.lapseCount
    )

    private fun fromMemoryStateRecord(rec: MemoryStateRecord): MemoryState = MemoryState(
        learnerId = LearnerId(rec.learnerId),
        learningItemId = LearningItemId(rec.learningItemId),
        stage = LearningStage.valueOf(rec.stage),
        difficulty = rec.difficulty,
        stabilityDays = rec.stabilityDays,
        dueAt = Moment(rec.dueAtEpochMillis),
        lastReviewedAt = rec.lastReviewedAtEpochMillis?.let(::Moment),
        reviewCount = rec.reviewCount,
        lapseCount = rec.lapseCount
    )
}
