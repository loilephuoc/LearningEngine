package vn.loi.learning.application.contentpackaging.browser

import java.io.File
import java.util.UUID
import vn.loi.learning.application.contentmedia.MediaReferencePolicy
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.sync.LocalSyncStateRepository
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.sync.protocol.*

data class PackageMediaReferenceRepairResult(
    val totalInspected: Int,
    val noImageSentinelsCleared: Int,
    val extensionsCanonicalized: Int,
    val repairedContentIds: Set<ContentId>
)

/**
 * Application Service xử lý các thao tác chỉnh sửa, tạo mới, xóa và quản lý media của Content
 * từ Learning Browser / Content Studio.
 */
class ContentBrowserEditService(
    private val contentRepository: ContentRepository,
    private val contentLibraryRepository: ContentLibraryRepository? = null,
    private val installedPackageRepository: InstalledPackageRepository? = null,
    private val contentPackageRepository: ContentPackageRepository? = null,
    private val transactionRunner: TransactionRunner? = null,
    private val studySessionRepository: StudySessionRepository? = null,
    private val mediaStorage: ContentMediaStorage? = null,
    private val localSyncStateRepository: LocalSyncStateRepository? = null,
    private val syncAccountProvider: (() -> SyncAccountId?)? = null,
    private val syncDeviceIdProvider: (() -> SyncDeviceId)? = null,
    private val imageOptimizer: vn.loi.learning.application.contentmedia.ContentImageOptimizer = vn.loi.learning.application.contentmedia.ContentImageOptimizer()
) {

    /**
     * Import một file media vào ContentMediaStorage của package.
     */
    fun importMediaAsset(
        packageName: String,
        sourceFile: File,
        mediaStorage: ContentMediaStorage
    ): String {
        require(sourceFile.exists() && sourceFile.isFile) { "Source file does not exist: ${sourceFile.absolutePath}" }
        val ext = sourceFile.extension.lowercase()
        val validExts = setOf("png", "jpg", "jpeg", "webp", "mp3", "wav", "aiff")
        require(ext in validExts) { "Unsupported media file extension: .$ext" }

        val isImage = ext in setOf("png", "jpg", "jpeg", "webp")
        val optimized = if (isImage) imageOptimizer.optimize(sourceFile) else null
        val bytes = optimized?.bytes ?: sourceFile.readBytes()
        val uniqueName = if (optimized != null) {
            "${UUID.randomUUID().toString().take(8)}_${sourceFile.nameWithoutExtension}.${optimized.extension}"
        } else {
            "${UUID.randomUUID().toString().take(8)}_${sourceFile.name}"
        }
        val asset = mediaStorage.store(packageName, uniqueName, bytes)
        return asset.relativePath
    }

    /**
     * Resolve the canonical writable ContentLibrary for the given InstalledPackage.
     *
     * Ownership resolution contract (mirrors PackageContentBrowserQueryService):
     * 1. Load InstalledPackage.packageId → ContentPackage.
     * 2. Read ContentPackage.libraryIds.
     * 3. Find an existing ContentLibrary for each libraryId.
     * 4. For the common single-library case, use the one that resolves.
     * 5. Fail explicitly if ContentPackage or ContentLibrary is missing.
     *
     * Does NOT use InstalledPackage.libraryId as a ContentLibraryId.
     */
    private fun resolveWritableContentLibrary(
        installedPackage: InstalledPackage
    ): ContentLibrary {
        val pkgRepo = contentPackageRepository
            ?: throw IllegalStateException(
                "ContentPackageRepository is required to resolve package ownership for create."
            )
        val libRepo = contentLibraryRepository
            ?: throw IllegalStateException(
                "ContentLibraryRepository is required to resolve package ownership for create."
            )

        val packageId = installedPackage.packageId
        val contentPackage = pkgRepo.findById(packageId)
            ?: throw IllegalStateException(
                "ContentPackage not found for packageId '${packageId.value}'. " +
                    "Cannot resolve writable ContentLibrary."
            )

        val resolvedLibraries = contentPackage.libraryIds
            .mapNotNull { libId -> libRepo.findById(libId) }
            .sortedBy { library -> library.id.value }

        if (resolvedLibraries.isEmpty()) {
            throw IllegalStateException(
                "No writable ContentLibrary found for package '${packageId.value}'. " +
                    "ContentPackage.libraryIds=${contentPackage.libraryIds.map { it.value }}. " +
                    "None of these libraries exist in the repository."
            )
        }

        // For the common single-library case, use the one resolved library.
        // For multiple libraries, use the first one — this mirrors the canonical read path
        // in PackageContentBrowserQueryService which collects all libraryIds from contentPackage.
        // The first libraryId in the set is used as the primary/default writable library.
        // Document: if domain ownership contracts change to designate a specific primary library,
        // update this selection logic accordingly.
        // Package data has no primary-library field. Sorting above makes this
        // single-membership choice stable instead of depending on Set iteration.
        return resolvedLibraries.first()
    }

    /**
     * Tạo một Content mới kèm theo text và media references.
     *
     * Ownership resolution: reads ContentPackage.libraryIds to find the canonical
     * writable ContentLibrary. Does NOT use InstalledPackage.libraryId as a ContentLibraryId.
     */
    fun createContent(
        installedPackageId: InstalledPackageId,
        questionText: String,
        answerText: String,
        pronunciation: String = "",
        partOfSpeech: String = "WORD",
        exampleText: String = "",
        exampleTranslation: String = "",
        imageRef: String? = null,
        questionAudioRef: String? = null,
        answerAudioRef: String? = null,
        exampleAudioRef: String? = null,
        translationAudioRef: String? = null,
        learningItemRepository: LearningItemRepository? = null
    ): Content {
        require(questionText.isNotBlank()) { "Question text must not be blank." }
        require(answerText.isNotBlank()) { "Answer text must not be blank." }

        // --- Phase 1: Resolve ownership BEFORE any repository writes ---
        val instPkgRepo = installedPackageRepository
            ?: throw IllegalStateException(
                "InstalledPackageRepository is required for createContent."
            )

        val instPkg = instPkgRepo.findById(installedPackageId)
            ?: throw IllegalStateException(
                "InstalledPackage not found for id '${installedPackageId.value}'."
            )

        // Resolve the canonical writable ContentLibrary from ContentPackage.libraryIds.
        // This is the same ownership path as PackageContentBrowserQueryService.
        val writableLibrary = resolveWritableContentLibrary(instPkg)

        // --- Phase 2: Construct domain objects ---
        val newContentId = ContentId("content_" + UUID.randomUUID().toString().replace("-", "").take(12))

        val posFieldId = ContentFieldId("partOfSpeech")
        val customFields = if (partOfSpeech.isNotBlank() && partOfSpeech != "WORD") {
            ContentCustomFields(setOf(ContentCustomField(posFieldId, partOfSpeech.trim())))
        } else {
            ContentCustomFields()
        }

        val contentText = ContentText(
            primaryText = questionText.trim(),
            translatedText = answerText.trim().takeIf { it.isNotBlank() },
            pronunciation = pronunciation.trim().takeIf { it.isNotBlank() },
            exampleText = exampleText.trim().takeIf { it.isNotBlank() },
            exampleTranslation = exampleTranslation.trim().takeIf { it.isNotBlank() }
        )

        val contentMedia = ContentMedia(
            image = MediaReferencePolicy.canonicalizeMediaReference(imageRef, mediaStorage),
            primaryAudio = questionAudioRef?.trim()?.takeIf { it.isNotBlank() },
            translatedAudio = answerAudioRef?.trim()?.takeIf { it.isNotBlank() },
            exampleAudio = exampleAudioRef?.trim()?.takeIf { it.isNotBlank() },
            exampleTranslatedAudio = translationAudioRef?.trim()?.takeIf { it.isNotBlank() }
        )

        val newContent = Content(
            id = newContentId,
            type = ContentType.WORD,
            text = contentText,
            media = contentMedia,
            customFields = customFields
        )

        val learningItems = if (learningItemRepository != null) {
            listOf(LearningItem(
                id = LearningItemId("item_" + UUID.randomUUID().toString().replace("-", "").take(12)),
                contentId = newContentId,
                mode = LearningMode.MEANING_RECOGNITION
            ))
        } else emptyList()

        requireNotNull(transactionRunner) {
            "Atomic transaction support is required for Content create."
        }.runInTransaction {
            contentRepository.save(newContent)
            learningItemRepository?.saveAll(learningItems)
            contentLibraryRepository!!.save(writableLibrary.register(newContentId))

            val currentPackage = requireNotNull(instPkgRepo.findById(installedPackageId)) {
                "InstalledPackage disappeared during Content create: ${installedPackageId.value}"
            }
            instPkgRepo.save(InstalledPackage.reconstitute(
                id = currentPackage.id,
                libraryId = currentPackage.libraryId,
                packageId = currentPackage.packageId,
                topicId = currentPackage.topicId,
                name = currentPackage.name,
                version = currentPackage.version,
                state = currentPackage.state,
                installedAt = currentPackage.installedAt,
                contentCount = currentPackage.contentCount + 1,
                learningItemCount = currentPackage.learningItemCount + learningItems.size,
                contentChecksum = currentPackage.contentChecksum
            ))
        }

        return newContent
    }

    /**
     * Cập nhật cả văn bản và phương tiện của một Content.
     */
    fun updateContent(
        contentId: ContentId,
        questionText: String,
        answerText: String,
        pronunciation: String,
        partOfSpeech: String,
        exampleText: String,
        exampleTranslation: String,
        imageRef: String? = null,
        questionAudioRef: String? = null,
        answerAudioRef: String? = null,
        exampleAudioRef: String? = null,
        translationAudioRef: String? = null
    ) {
        require(questionText.isNotBlank()) { "Question text must not be blank." }

        val existing = contentRepository.findById(contentId)
            ?: throw IllegalArgumentException("Content not found: ${contentId.value}")

        val normalizedQuestion = questionText.trim()
        val normalizedAnswer = answerText.trim().takeIf { it.isNotBlank() }
        val normalizedPronunciation = pronunciation.trim().takeIf { it.isNotBlank() }
        val normalizedExample = exampleText.trim().takeIf { it.isNotBlank() }
        val normalizedTranslation = exampleTranslation.trim().takeIf { it.isNotBlank() }

        val updatedText = existing.text.copy(
            primaryText = normalizedQuestion,
            translatedText = normalizedAnswer,
            pronunciation = normalizedPronunciation,
            exampleText = normalizedExample,
            exampleTranslation = normalizedTranslation
        )

        val updatedMedia = existing.media.copy(
            image = MediaReferencePolicy.canonicalizeMediaReference(imageRef, mediaStorage),
            primaryAudio = questionAudioRef?.trim()?.takeIf { it.isNotBlank() },
            translatedAudio = answerAudioRef?.trim()?.takeIf { it.isNotBlank() },
            exampleAudio = exampleAudioRef?.trim()?.takeIf { it.isNotBlank() },
            exampleTranslatedAudio = translationAudioRef?.trim()?.takeIf { it.isNotBlank() }
        )

        val updatedCustomFields = updatePartOfSpeech(existing.customFields, partOfSpeech.trim())

        val updated = existing.copy(
            text = updatedText,
            media = updatedMedia,
            customFields = updatedCustomFields
        )

        if (updated == existing) {
            return
        }

        val deltas = mutableListOf<ContentFieldDelta>()
        if (existing.text.primaryText != updatedText.primaryText) {
            deltas += ContentFieldDelta(
                field = ContentField.QUESTION,
                operation = DeltaOperation.SET,
                value = updatedText.primaryText
            )
        }
        if (existing.text.translatedText != updatedText.translatedText) {
            val value = updatedText.translatedText
            deltas += ContentFieldDelta(
                field = ContentField.ANSWER,
                operation = if (value != null) DeltaOperation.SET else DeltaOperation.REMOVE,
                value = value
            )
        }
        if (existing.text.exampleText != updatedText.exampleText) {
            val value = updatedText.exampleText
            deltas += ContentFieldDelta(
                field = ContentField.EXAMPLE,
                operation = if (value != null) DeltaOperation.SET else DeltaOperation.REMOVE,
                value = value
            )
        }
        if (existing.text.exampleTranslation != updatedText.exampleTranslation) {
            val value = updatedText.exampleTranslation
            deltas += ContentFieldDelta(
                field = ContentField.TRANSLATION,
                operation = if (value != null) DeltaOperation.SET else DeltaOperation.REMOVE,
                value = value
            )
        }

        val accountId = syncAccountProvider?.invoke()
        val outboundChanges = if (localSyncStateRepository != null && accountId != null && deltas.isNotEmpty()) {
            val deviceId = syncDeviceIdProvider?.invoke() ?: SyncDeviceId("desktop")
            deltas.map { delta ->
                val eventId = SyncEventId("sync_event_" + UUID.randomUUID().toString().replace("-", "").take(16))
                val idempotencyKey = IdempotencyKey("key_" + UUID.randomUUID().toString().replace("-", "").take(16))
                OutboundSyncChange(
                    accountId = accountId,
                    eventId = eventId,
                    idempotencyKey = idempotencyKey,
                    sourceDeviceId = deviceId,
                    entityId = SyncEntityId(contentId.value),
                    payloadVersion = 1,
                    delta = delta
                )
            }
        } else {
            emptyList()
        }

        val writeBlock = {
            contentRepository.save(updated)
            outboundChanges.forEach { change ->
                localSyncStateRepository?.enqueue(change)
            }
        }

        if (transactionRunner != null) {
            transactionRunner.runInTransaction { writeBlock() }
        } else {
            writeBlock()
        }
    }

    fun updateTextFields(
        contentId: ContentId,
        questionText: String,
        answerText: String,
        pronunciation: String,
        partOfSpeech: String,
        exampleText: String,
        exampleTranslation: String
    ) {
        val existing = contentRepository.findById(contentId)
            ?: throw IllegalArgumentException("Content not found: ${contentId.value}")
        updateContent(
            contentId = contentId,
            questionText = questionText,
            answerText = answerText,
            pronunciation = pronunciation,
            partOfSpeech = partOfSpeech,
            exampleText = exampleText,
            exampleTranslation = exampleTranslation,
            imageRef = existing.media.image,
            questionAudioRef = existing.media.primaryAudio,
            answerAudioRef = existing.media.translatedAudio,
            exampleAudioRef = existing.media.exampleAudio,
            translationAudioRef = existing.media.exampleTranslatedAudio
        )
    }

    /**
     * Replaces only the media image reference of an existing Content.
     * All text, metadata, custom fields, and audio references are preserved untouched.
     */
    fun replaceContentImage(
        contentId: ContentId,
        newImageRef: String?
    ): Content {
        val existing = contentRepository.findById(contentId)
            ?: throw IllegalArgumentException("Content not found: ${contentId.value}")
        val canonicalImage = MediaReferencePolicy.canonicalizeMediaReference(newImageRef, mediaStorage)
        val updated = existing.copy(
            media = existing.media.copy(image = canonicalImage)
        )
        contentRepository.save(updated)
        return updated
    }

    /**
     * Safely undoes an image reuse operation if and only if the Content's image reference
     * matches the expected applied reference, restoring the exact previous reference.
     */
    fun undoImageReuse(
        contentId: ContentId,
        expectedCurrentImageRef: String,
        restoreImageRef: String?
    ): Content {
        val existing = contentRepository.findById(contentId)
            ?: throw IllegalArgumentException("Content not found: ${contentId.value}")
        val expectedCanonical = MediaReferencePolicy.canonicalizeMediaReference(expectedCurrentImageRef, mediaStorage)
        val currentCanonical = existing.media.image
        if (currentCanonical != expectedCanonical) {
            throw IllegalStateException("Cannot undo because this item's image has changed since the reuse action.")
        }
        val canonicalRestore = MediaReferencePolicy.canonicalizeMediaReference(restoreImageRef, mediaStorage)
        val updated = existing.copy(
            media = existing.media.copy(image = canonicalRestore)
        )
        contentRepository.save(updated)
        return updated
    }

    /** Atomically updates only the canonical partOfSpeech custom field for the requested contents with a single POS value. */
    fun updatePartOfSpeechBatch(
        contentIds: Collection<ContentId>,
        partOfSpeech: String,
        markUserConfirmed: Boolean = true
    ): BatchPartOfSpeechResult =
        updatePartOfSpeechMultiBatch(contentIds.associateWith { partOfSpeech }, markUserConfirmed)

    /**
     * Atomically updates only the canonical partOfSpeech custom field for the requested contents
     * with individual POS values per ContentId, and atomically sets partOfSpeechReviewStatus = USER_CONFIRMED.
     */
    fun updatePartOfSpeechMultiBatch(
        updates: Map<ContentId, String>,
        markUserConfirmed: Boolean = true
    ): BatchPartOfSpeechResult {
        val requestedIds = updates.keys.distinct()
        require(requestedIds.isNotEmpty()) { "At least one ContentId is required for Batch POS." }
        val existing = contentRepository.findByIds(requestedIds)
        val existingById = existing.associateBy(Content::id)
        val sanitized = requestedIds.mapNotNull(existingById::get)
        require(sanitized.isNotEmpty()) { "No selected Content exists for Batch POS." }

        val posFieldId = ContentFieldId("partOfSpeech")
        val statusFieldId = ContentFieldId("partOfSpeechReviewStatus")
        val changed = mutableListOf<Content>()
        val updated = mutableListOf<Content>()

        for (content in sanitized) {
            val target = updates[content.id]?.trim().orEmpty()
            val currentPos = content.customFields[posFieldId]?.value.orEmpty().trim()

            if (currentPos != target) {
                val targetStatus = if (markUserConfirmed) "USER_CONFIRMED" else content.customFields[statusFieldId]?.value
                changed += content
                updated += content.copy(
                    customFields = updatePartOfSpeech(
                        existing = content.customFields,
                        partOfSpeech = target,
                        reviewStatus = targetStatus
                    )
                )
            }
        }

        if (updated.isNotEmpty()) {
            val transaction = requireNotNull(transactionRunner) {
               "Atomic transaction support is required for Batch POS."
            }
            try {
                transaction.runInTransaction { contentRepository.saveAll(updated) }
            } catch (failure: Throwable) {
                // File-backed runners roll back transaction members. This compensation preserves
                // all-or-nothing behavior for lightweight/non-transactional repository adapters.
                try {
                    contentRepository.saveAll(changed)
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
                throw failure
            }
        }
        return BatchPartOfSpeechResult(
            selectedCount = sanitized.size,
            changedCount = updated.size,
            unchangedCount = sanitized.size - updated.size,
            contentIds = sanitized.mapTo(linkedSetOf(), Content::id)
        )
    }

    /**
     * Atomically clears USER_CONFIRMED review authority for the requested contents,
     * allowing them to be evaluated by automatic POS analyzers again.
     */
    fun unlockPartOfSpeechReviewBatch(
        contentIds: Collection<ContentId>
    ): BatchPartOfSpeechResult {
        val requestedIds = contentIds.distinct()
        require(requestedIds.isNotEmpty()) { "At least one ContentId is required to unlock POS review." }
        val existing = contentRepository.findByIds(requestedIds)
        val existingById = existing.associateBy(Content::id)
        val sanitized = requestedIds.mapNotNull(existingById::get)
        require(sanitized.isNotEmpty()) { "No selected Content exists to unlock POS review." }

        val posFieldId = ContentFieldId("partOfSpeech")
        val statusFieldId = ContentFieldId("partOfSpeechReviewStatus")
        val changed = mutableListOf<Content>()
        val updated = mutableListOf<Content>()

        for (content in sanitized) {
            val currentStatus = content.customFields[statusFieldId]?.value.orEmpty().trim()
            if (currentStatus == "USER_CONFIRMED") {
                changed += content
                val currentPos = content.customFields[posFieldId]?.value.orEmpty()
                updated += content.copy(
                    customFields = updatePartOfSpeech(
                        existing = content.customFields,
                        partOfSpeech = currentPos,
                        reviewStatus = "UNREVIEWED"
                    )
                )
            }
        }

        if (updated.isNotEmpty()) {
            val transaction = requireNotNull(transactionRunner) {
                "Atomic transaction support is required to unlock POS review."
            }
            try {
                transaction.runInTransaction { contentRepository.saveAll(updated) }
            } catch (failure: Throwable) {
                try {
                    contentRepository.saveAll(changed)
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
                throw failure
            }
        }
        return BatchPartOfSpeechResult(
            selectedCount = sanitized.size,
            changedCount = updated.size,
            unchangedCount = sanitized.size - updated.size,
            contentIds = sanitized.mapTo(linkedSetOf(), Content::id)
        )
    }

    /**
     * Xóa một Content và tất cả LearningItem liên quan.
     */
    fun deleteContent(
        contentId: ContentId,
        learningItemRepository: LearningItemRepository,
        installedPackageId: InstalledPackageId? = null
    ): DeletedContentSnapshot {
        val content = contentRepository.findById(contentId)
            ?: throw IllegalArgumentException("Content not found: ${contentId.value}")
        val ownedItems = learningItemRepository.findByContentId(contentId)
        val ownedItemIds = ownedItems.mapTo(mutableSetOf()) { it.id }
        val blockingSession = studySessionRepository?.findAll()?.firstOrNull { session ->
            session.currentLearningItemId in ownedItemIds ||
                session.pendingReview?.learningItemId in ownedItemIds ||
                session.undoableReview?.learningItemId in ownedItemIds
        }
        check(blockingSession == null) {
            "Content cannot be deleted while one of its learning items is referenced by Study session ${blockingSession?.id?.value}."
        }
        val libraryIds = contentLibraryRepository
            ?.findAll()
            ?.filter { it.contains(contentId) }
            ?.map { it.id }
            ?.toSet()
            .orEmpty()
        val snapshot = DeletedContentSnapshot(
            content = content,
            learningItems = ownedItems.toList(),
            libraryIds = libraryIds,
            installedPackageId = installedPackageId,
            displayLabel = content.displayName
        )

        requireNotNull(transactionRunner) {
            "Atomic transaction support is required for Content delete."
        }.runInTransaction {
            learningItemRepository.deleteByContentIds(setOf(contentId))
            contentRepository.deleteById(contentId)
            contentLibraryRepository?.let { repository ->
                libraryIds.forEach { libraryId ->
                    val library = requireNotNull(repository.findById(libraryId)) {
                        "ContentLibrary ${libraryId.value} disappeared during delete."
                    }
                    repository.save(library.remove(contentId))
                }
            }
            reconcileInstalledPackageCounts(
                installedPackageId = installedPackageId,
                contentDelta = -1,
                learningItemDelta = -ownedItems.size
            )
        }
        return snapshot
    }

    fun preflightDeleteContents(
        contentIds: Collection<ContentId>,
        learningItemRepository: LearningItemRepository,
        installedPackageId: InstalledPackageId? = null
    ): BatchDeletePreflight {
        val requested = contentIds.toCollection(linkedSetOf())
        val contents = contentRepository.findByIds(requested).associateBy(Content::id)
        val sessions = studySessionRepository?.findAll().orEmpty()
        val snapshots = requested.mapNotNull { contentId ->
            val content = contents[contentId] ?: return@mapNotNull null
            val items = learningItemRepository.findByContentId(contentId).toList()
            val itemIds = items.mapTo(hashSetOf(), LearningItem::id)
            val blocker = sessions.firstOrNull { session ->
                session.currentLearningItemId in itemIds ||
                    session.pendingReview?.learningItemId in itemIds ||
                    session.undoableReview?.learningItemId in itemIds
            }
            BatchDeleteTarget(
                snapshot = DeletedContentSnapshot(
                    content = content,
                    learningItems = items,
                    libraryIds = contentLibraryRepository?.findAll()
                        ?.filter { it.contains(contentId) }?.mapTo(linkedSetOf()) { it.id }.orEmpty(),
                    installedPackageId = installedPackageId,
                    displayLabel = content.displayName
                ),
                blocker = blocker?.let {
                    BatchDeleteBlocker(contentId, "Referenced by active Study session ${it.id.value}.")
                }
            )
        }
        return BatchDeletePreflight(
            requestedCount = requested.size,
            staleContentIds = requested - contents.keys,
            targets = snapshots
        )
    }

    fun deleteContents(
        preflight: BatchDeletePreflight,
        learningItemRepository: LearningItemRepository
    ): BatchDeletedContentSnapshot {
        require(preflight.targets.isNotEmpty()) { "No existing Content is selected for deletion." }
        check(preflight.blockers.isEmpty()) {
            "Cannot delete selected items: ${preflight.blockers.joinToString { it.reason }}"
        }
        val snapshots = preflight.targets.map(BatchDeleteTarget::snapshot)
        // Revalidate canonical identities immediately before entering the transaction.
        snapshots.forEach { snapshot ->
            check(contentRepository.findById(snapshot.content.id) == snapshot.content) {
                "Content ${snapshot.content.id.value} changed after delete preflight."
            }
            snapshot.learningItems.forEach { item ->
                check(learningItemRepository.findById(item.id) == item) {
                    "LearningItem ${item.id.value} changed after delete preflight."
                }
            }
        }
        val transaction = requireNotNull(transactionRunner) {
            "Atomic transaction support is required for Batch Content delete."
        }
        val libraryIds = snapshots.flatMapTo(linkedSetOf()) { it.libraryIds }
        val librariesBefore = libraryIds.associateWith { contentLibraryRepository?.findById(it) }
        val packageBefore = snapshots.first().installedPackageId?.let { installedPackageRepository?.findById(it) }
        try {
            transaction.runInTransaction {
                val contentIds = snapshots.mapTo(linkedSetOf()) { it.content.id }
                learningItemRepository.deleteByContentIds(contentIds)
                contentRepository.deleteAllById(contentIds)
                val membershipByLibrary = snapshots.flatMap { snapshot ->
                    snapshot.libraryIds.map { it to snapshot.content.id }
                }.groupBy({ it.first }, { it.second })
                membershipByLibrary.forEach { (libraryId, removedIds) ->
                    val repository = requireNotNull(contentLibraryRepository)
                    val library = requireNotNull(repository.findById(libraryId)) {
                        "ContentLibrary ${libraryId.value} disappeared during batch delete."
                    }
                    repository.save(removedIds.fold(library) { current, id -> current.remove(id) })
                }
                reconcileInstalledPackageCounts(
                    installedPackageId = snapshots.first().installedPackageId,
                    contentDelta = -snapshots.size,
                    learningItemDelta = -snapshots.sumOf { it.learningItems.size }
                )
            }
        } catch (failure: Throwable) {
            try {
                transaction.runInTransaction {
                    contentRepository.saveAll(snapshots.map { it.content })
                    learningItemRepository.saveAll(snapshots.flatMap { it.learningItems })
                    librariesBefore.values.filterNotNull().forEach { contentLibraryRepository?.save(it) }
                    packageBefore?.let { installedPackageRepository?.save(it) }
                }
            } catch (rollbackFailure: Throwable) {
                failure.addSuppressed(rollbackFailure)
            }
            throw failure
        }
        return BatchDeletedContentSnapshot(snapshots)
    }

    fun restoreDeletedContent(
        snapshot: DeletedContentSnapshot,
        learningItemRepository: LearningItemRepository
    ) {
        val contentId = snapshot.content.id
        check(contentRepository.findById(contentId) == null) {
            "Cannot undo delete: Content ${contentId.value} already exists."
        }
        snapshot.learningItems.forEach { original ->
            val existing = learningItemRepository.findById(original.id)
            check(existing == null) {
                "Cannot undo delete: LearningItem ${original.id.value} already exists."
            }
        }
        val libraryRepository = contentLibraryRepository
        if (snapshot.libraryIds.isNotEmpty()) {
            checkNotNull(libraryRepository) { "Cannot undo delete: ContentLibraryRepository is unavailable." }
            snapshot.libraryIds.forEach { libraryId ->
                checkNotNull(libraryRepository.findById(libraryId)) {
                    "Cannot undo delete: ContentLibrary ${libraryId.value} is unavailable."
                }
            }
        }
        snapshot.installedPackageId?.let { packageId ->
            checkNotNull(installedPackageRepository?.findById(packageId)) {
                "Cannot undo delete: InstalledPackage ${packageId.value} is unavailable."
            }
        }

        requireNotNull(transactionRunner) {
            "Atomic transaction support is required for Undo Delete."
        }.runInTransaction {
            contentRepository.save(snapshot.content)
            learningItemRepository.saveAll(snapshot.learningItems)
            snapshot.libraryIds.forEach { libraryId ->
                val library = requireNotNull(libraryRepository?.findById(libraryId))
                libraryRepository.save(library.register(contentId))
            }
            reconcileInstalledPackageCounts(
                installedPackageId = snapshot.installedPackageId,
                contentDelta = 1,
                learningItemDelta = snapshot.learningItems.size
            )
        }
    }

    fun restoreDeletedContents(
        snapshot: BatchDeletedContentSnapshot,
        learningItemRepository: LearningItemRepository
    ) {
        require(snapshot.contents.isNotEmpty()) { "Batch Undo snapshot is empty." }
        snapshot.contents.forEach { deleted ->
            check(contentRepository.findById(deleted.content.id) == null) {
                "Cannot undo delete: Content ${deleted.content.id.value} already exists."
            }
            deleted.learningItems.forEach { item ->
                check(learningItemRepository.findById(item.id) == null) {
                    "Cannot undo delete: LearningItem ${item.id.value} already exists."
                }
            }
            deleted.libraryIds.forEach { libraryId ->
                checkNotNull(contentLibraryRepository?.findById(libraryId)) {
                    "Cannot undo delete: ContentLibrary ${libraryId.value} is unavailable."
                }
            }
        }
        snapshot.installedPackageId?.let { packageId ->
            checkNotNull(installedPackageRepository?.findById(packageId)) {
                "Cannot undo delete: InstalledPackage ${packageId.value} is unavailable."
            }
        }
        val transaction = requireNotNull(transactionRunner) {
            "Atomic transaction support is required for Batch Undo Delete."
        }
        val libraryIds = snapshot.contents.flatMapTo(linkedSetOf()) { it.libraryIds }
        val librariesBefore = libraryIds.associateWith { contentLibraryRepository?.findById(it) }
        val packageBefore = snapshot.installedPackageId?.let { installedPackageRepository?.findById(it) }
        try {
            transaction.runInTransaction {
                contentRepository.saveAll(snapshot.contents.map { it.content })
                learningItemRepository.saveAll(snapshot.contents.flatMap { it.learningItems })
                val membershipByLibrary = snapshot.contents.flatMap { deleted ->
                    deleted.libraryIds.map { it to deleted.content.id }
                }.groupBy({ it.first }, { it.second })
                membershipByLibrary.forEach { (libraryId, restoredIds) ->
                    val repository = requireNotNull(contentLibraryRepository)
                    val library = requireNotNull(repository.findById(libraryId))
                    repository.save(restoredIds.fold(library) { current, id -> current.register(id) })
                }
                reconcileInstalledPackageCounts(
                    installedPackageId = snapshot.installedPackageId,
                    contentDelta = snapshot.contents.size,
                    learningItemDelta = snapshot.contents.sumOf { it.learningItems.size }
                )
            }
        } catch (failure: Throwable) {
            try {
                transaction.runInTransaction {
                    val contentIds = snapshot.contentIds
                    learningItemRepository.deleteByContentIds(contentIds)
                    contentRepository.deleteAllById(contentIds)
                    librariesBefore.values.filterNotNull().forEach { contentLibraryRepository?.save(it) }
                    packageBefore?.let { installedPackageRepository?.save(it) }
                }
            } catch (rollbackFailure: Throwable) {
                failure.addSuppressed(rollbackFailure)
            }
            throw failure
        }
    }

    private fun reconcileInstalledPackageCounts(
        installedPackageId: InstalledPackageId?,
        contentDelta: Int,
        learningItemDelta: Int
    ) {
        if (installedPackageId == null) return
        val repository = installedPackageRepository ?: return
        val current = repository.findById(installedPackageId) ?: return
        repository.save(
            InstalledPackage.reconstitute(
                id = current.id,
                libraryId = current.libraryId,
                packageId = current.packageId,
                topicId = current.topicId,
                name = current.name,
                version = current.version,
                state = current.state,
                installedAt = current.installedAt,
                contentCount = (current.contentCount + contentDelta).coerceAtLeast(0),
                learningItemCount = (current.learningItemCount + learningItemDelta).coerceAtLeast(0),
                contentChecksum = current.contentChecksum
            )
        )
    }

    private fun updatePartOfSpeech(
        existing: ContentCustomFields,
        partOfSpeech: String,
        reviewStatus: String? = null
    ): ContentCustomFields {
        val posFieldId = ContentFieldId("partOfSpeech")
        val statusFieldId = ContentFieldId("partOfSpeechReviewStatus")
        var existingOtherFields = existing.fields.filter { it.id != posFieldId && it.id != statusFieldId }.toSet()

        if (partOfSpeech.isNotBlank()) {
            existingOtherFields = existingOtherFields + ContentCustomField(id = posFieldId, value = partOfSpeech)
        }
        if (reviewStatus != null && reviewStatus != "UNREVIEWED" && reviewStatus.isNotBlank()) {
            existingOtherFields = existingOtherFields + ContentCustomField(id = statusFieldId, value = reviewStatus)
        }

        return ContentCustomFields(existingOtherFields)
    }

    /**
     * Sửa đổi an toàn các tham chiếu media của package:
     * 1. Xóa các sentinel no_image (no_image.jpg, no_image.png...) -> null.
     * 2. Chuẩn hóa các đuôi mở rộng bị lệch nếu ContentMediaStorage tìm thấy file thực tế (.png -> .jpg).
     * Tuyệt đối không thay đổi question, answer, POS, audio, FSRS, ReviewEvents, LearningItems.
     */
    fun repairPackageMediaReferences(
        installedPackageId: InstalledPackageId,
        customMediaStorage: ContentMediaStorage? = null
    ): PackageMediaReferenceRepairResult {
        val storage = customMediaStorage ?: mediaStorage
        val instPkgRepo = installedPackageRepository
            ?: throw IllegalStateException("InstalledPackageRepository is required for repair.")
        val instPkg = instPkgRepo.findById(installedPackageId)
            ?: throw IllegalArgumentException("InstalledPackage not found: ${installedPackageId.value}")

        val pkgRepo = contentPackageRepository
            ?: throw IllegalStateException("ContentPackageRepository is required for repair.")
        val libRepo = contentLibraryRepository
            ?: throw IllegalStateException("ContentLibraryRepository is required for repair.")

        val contentPackage = pkgRepo.findById(instPkg.packageId)
            ?: throw IllegalArgumentException("ContentPackage not found: ${instPkg.packageId.value}")

        val ownedLibraries = contentPackage.libraryIds.mapNotNull { libRepo.findById(it) }
        val contentIds = ownedLibraries.flatMapTo(linkedSetOf()) { it.contentIds }
        val allContents = if (contentIds.isNotEmpty()) {
            contentRepository.findByIds(contentIds)
        } else {
            contentRepository.findAll().filter { it.id.value.startsWith(instPkg.packageId.value) || it.id.value.contains(instPkg.name.value) }
        }

        var noImageCount = 0
        var extCount = 0
        val repairedContents = mutableListOf<Content>()
        val repairedIds = mutableSetOf<ContentId>()

        allContents.forEach { content ->
            var changed = false
            var curMedia = content.media

            // 1. Check no_image sentinel
            if (!curMedia.image.isNullOrBlank() && MediaReferencePolicy.isNoImageSentinel(curMedia.image)) {
                curMedia = curMedia.copy(image = null)
                noImageCount++
                changed = true
            } else if (!curMedia.image.isNullOrBlank() && storage != null) {
                val canonical = MediaReferencePolicy.canonicalizeMediaReference(curMedia.image, storage)
                if (canonical != curMedia.image) {
                    curMedia = curMedia.copy(image = canonical)
                    extCount++
                    changed = true
                }
            }

            if (changed) {
                repairedContents.add(content.copy(media = curMedia))
                repairedIds.add(content.id)
            }
        }

        if (repairedContents.isNotEmpty()) {
            val tx = transactionRunner
            if (tx != null) {
                tx.runInTransaction { contentRepository.saveAll(repairedContents) }
            } else {
                contentRepository.saveAll(repairedContents)
            }
        }

        return PackageMediaReferenceRepairResult(
            totalInspected = allContents.size,
            noImageSentinelsCleared = noImageCount,
            extensionsCanonicalized = extCount,
            repairedContentIds = repairedIds
        )
    }
}

data class DeletedContentSnapshot(
    val content: Content,
    val learningItems: List<LearningItem>,
    val libraryIds: Set<ContentLibraryId>,
    val installedPackageId: InstalledPackageId?,
    val displayLabel: String
)

data class BatchDeleteBlocker(val contentId: ContentId, val reason: String)

data class BatchDeleteTarget(
    val snapshot: DeletedContentSnapshot,
    val blocker: BatchDeleteBlocker? = null
)

data class BatchDeletePreflight(
    val requestedCount: Int,
    val staleContentIds: Set<ContentId>,
    val targets: List<BatchDeleteTarget>
) {
    val blockers: List<BatchDeleteBlocker> get() = targets.mapNotNull(BatchDeleteTarget::blocker)
    val resolvableContentIds: Set<ContentId> get() = targets.mapTo(linkedSetOf()) { it.snapshot.content.id }
}

data class BatchDeletedContentSnapshot(val contents: List<DeletedContentSnapshot>) {
    val installedPackageId: InstalledPackageId? = contents.firstOrNull()?.installedPackageId
    val contentIds: Set<ContentId> = contents.mapTo(linkedSetOf()) { it.content.id }
    val displayLabel: String = "${contents.size} items"
}

data class BatchPartOfSpeechResult(
    val selectedCount: Int,
    val changedCount: Int,
    val unchangedCount: Int,
    val contentIds: Set<ContentId>
)
