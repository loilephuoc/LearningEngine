package vn.loi.learning.application.contentpackaging.browser

import java.io.File
import java.util.UUID
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
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

/**
 * Application Service xử lý các thao tác chỉnh sửa, tạo mới, xóa và quản lý media của Content
 * từ Learning Browser / Content Studio.
 */
class ContentBrowserEditService(
    private val contentRepository: ContentRepository,
    private val contentLibraryRepository: ContentLibraryRepository? = null,
    private val installedPackageRepository: InstalledPackageRepository? = null,
    private val contentPackageRepository: ContentPackageRepository? = null
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

        val bytes = sourceFile.readBytes()
        val uniqueName = "${UUID.randomUUID().toString().take(8)}_${sourceFile.name}"
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
            image = imageRef?.trim()?.takeIf { it.isNotBlank() },
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

        // --- Phase 3: Persist in correct order ---

        // 3a. Persist Content
        contentRepository.save(newContent)

        // 3b. Persist LearningItem
        var createdItemCount = 0
        if (learningItemRepository != null) {
            val itemId = LearningItemId("item_" + UUID.randomUUID().toString().replace("-", "").take(12))
            val learningItem = LearningItem(
                id = itemId,
                contentId = newContentId,
                mode = LearningMode.MEANING_RECOGNITION
            )
            learningItemRepository.save(learningItem)
            createdItemCount = 1
        }

        // 3c. Register new ContentId in the canonical writable ContentLibrary.
        //     Use the updatedLibrary instance (not the original pre-register instance).
        val updatedLibrary = writableLibrary.register(newContentId)
        contentLibraryRepository!!.save(updatedLibrary)

        // 3d. Update InstalledPackage counts.
        //     Re-load instPkg in case it was modified (defensive); use updated instance for save.
        val freshInstPkg = instPkgRepo.findById(installedPackageId) ?: instPkg
        val updatedPkg = InstalledPackage.reconstitute(
            id = freshInstPkg.id,
            libraryId = freshInstPkg.libraryId,
            packageId = freshInstPkg.packageId,
            topicId = freshInstPkg.topicId,
            name = freshInstPkg.name,
            version = freshInstPkg.version,
            state = freshInstPkg.state,
            installedAt = freshInstPkg.installedAt,
            contentCount = freshInstPkg.contentCount + 1,
            learningItemCount = freshInstPkg.learningItemCount + createdItemCount,
            contentChecksum = freshInstPkg.contentChecksum
        )
        instPkgRepo.save(updatedPkg)

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

        val updatedText = existing.text.copy(
            primaryText = questionText.trim(),
            translatedText = answerText.trim().takeIf { it.isNotBlank() },
            pronunciation = pronunciation.trim().takeIf { it.isNotBlank() },
            exampleText = exampleText.trim().takeIf { it.isNotBlank() },
            exampleTranslation = exampleTranslation.trim().takeIf { it.isNotBlank() }
        )

        val updatedMedia = existing.media.copy(
            image = imageRef?.trim()?.takeIf { it.isNotBlank() },
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

        contentRepository.save(updated)
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
        updateContent(
            contentId = contentId,
            questionText = questionText,
            answerText = answerText,
            pronunciation = pronunciation,
            partOfSpeech = partOfSpeech,
            exampleText = exampleText,
            exampleTranslation = exampleTranslation
        )
    }

    /**
     * Xóa một Content và tất cả LearningItem liên quan.
     */
    fun deleteContent(
        contentId: ContentId,
        learningItemRepository: LearningItemRepository,
        installedPackageId: InstalledPackageId? = null
    ) {
        contentRepository.findById(contentId)
            ?: throw IllegalArgumentException("Content not found: ${contentId.value}")

        val ownedItems = learningItemRepository.findAllEnabled().filter { it.contentId == contentId }
        val deletedItemCount = ownedItems.size

        learningItemRepository.deleteByContentIds(setOf(contentId))
        contentRepository.deleteById(contentId)

        contentLibraryRepository?.let { libRepo ->
            val libraries = libRepo.findAll().filter { it.contains(contentId) }
            for (lib in libraries) {
                libRepo.save(lib.remove(contentId))
            }
        }

        if (installedPackageRepository != null && installedPackageId != null) {
            val instPkg = installedPackageRepository.findById(installedPackageId)
            if (instPkg != null) {
                val updated = InstalledPackage.reconstitute(
                    id = instPkg.id,
                    libraryId = instPkg.libraryId,
                    packageId = instPkg.packageId,
                    topicId = instPkg.topicId,
                    name = instPkg.name,
                    version = instPkg.version,
                    state = instPkg.state,
                    installedAt = instPkg.installedAt,
                    contentCount = (instPkg.contentCount - 1).coerceAtLeast(0),
                    learningItemCount = (instPkg.learningItemCount - deletedItemCount).coerceAtLeast(0),
                    contentChecksum = instPkg.contentChecksum
                )
                installedPackageRepository.save(updated)
            }
        }
    }

    private fun updatePartOfSpeech(
        existing: ContentCustomFields,
        partOfSpeech: String
    ): ContentCustomFields {
        val posFieldId = ContentFieldId("partOfSpeech")
        val existingOtherFields = existing.fields.filter { it.id != posFieldId }.toSet()

        return if (partOfSpeech.isBlank()) {
            ContentCustomFields(existingOtherFields)
        } else {
            ContentCustomFields(
                existingOtherFields + ContentCustomField(id = posFieldId, value = partOfSpeech)
            )
        }
    }
}
