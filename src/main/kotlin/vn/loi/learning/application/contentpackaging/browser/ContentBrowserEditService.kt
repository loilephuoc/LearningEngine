package vn.loi.learning.application.contentpackaging.browser

import java.io.File
import java.util.UUID
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
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
    private val installedPackageRepository: InstalledPackageRepository? = null
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
     * Tạo một Content mới kèm theo text và media references.
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

        contentRepository.save(newContent)

        // Save LearningItem
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

        // Add to ContentLibrary if available
        contentLibraryRepository?.let { libRepo ->
            val instPkg = installedPackageRepository?.findById(installedPackageId)
            if (instPkg != null) {
                val lib = libRepo.findById(ContentLibraryId(instPkg.libraryId.value))
                if (lib != null) {
                    libRepo.save(lib.register(newContentId))
                }
            }
        }

        // Update InstalledPackage counts
        if (installedPackageRepository != null) {
            val instPkg = installedPackageRepository.findById(installedPackageId)
            if (instPkg != null) {
                val updatedPkg = InstalledPackage.reconstitute(
                    id = instPkg.id,
                    libraryId = instPkg.libraryId,
                    packageId = instPkg.packageId,
                    topicId = instPkg.topicId,
                    name = instPkg.name,
                    version = instPkg.version,
                    state = instPkg.state,
                    installedAt = instPkg.installedAt,
                    contentCount = instPkg.contentCount + 1,
                    learningItemCount = instPkg.learningItemCount + createdItemCount,
                    contentChecksum = instPkg.contentChecksum
                )
                installedPackageRepository.save(updatedPkg)
            }
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
