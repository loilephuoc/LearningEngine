package vn.loi.learning.application.contentpackaging.browser

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

/**
 * Application Service xử lý các thao tác chỉnh sửa và xóa Content
 * từ Learning Browser.
 *
 * Mỗi method là một đơn vị công việc hoàn chỉnh: load → mutate → save.
 */
class ContentBrowserEditService(
    private val contentRepository: ContentRepository,
    private val contentLibraryRepository: ContentLibraryRepository? = null,
    private val installedPackageRepository: InstalledPackageRepository? = null
) {

    /**
     * Cập nhật các trường văn bản của một Content.
     *
     * Bất biến được bảo toàn:
     * - ContentId không thay đổi.
     * - ContentType không thay đổi.
     * - Media, Metadata (lesson/group/section/tags) không thay đổi.
     * - LearningItem không bị ảnh hưởng.
     *
     * @throws IllegalArgumentException nếu questionText trống, hoặc contentId không tồn tại.
     */
    fun updateTextFields(
        contentId: ContentId,
        questionText: String,
        answerText: String,
        pronunciation: String,
        partOfSpeech: String,
        exampleText: String,
        exampleTranslation: String
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

        val updatedCustomFields = updatePartOfSpeech(existing.customFields, partOfSpeech.trim())

        val updated = existing.copy(
            text = updatedText,
            customFields = updatedCustomFields
        )

        contentRepository.save(updated)
    }

    /**
     * Xóa một Content và tất cả LearningItem liên quan.
     * Cập nhật ContentLibrary và InstalledPackage nếu repositories được cung cấp.
     *
     * @throws IllegalArgumentException nếu contentId không tồn tại.
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

    /**
     * Cập nhật partOfSpeech trong customFields.
     *
     * Chiến lược: luôn dùng customField "partOfSpeech" (không thay đổi tags).
     * Nếu partOfSpeech blank hoặc bằng contentType.name → xóa custom field (dùng fallback).
     */
    private fun updatePartOfSpeech(
        existing: ContentCustomFields,
        partOfSpeech: String
    ): ContentCustomFields {
        val posFieldId = ContentFieldId("partOfSpeech")
        val existingOtherFields = existing.fields.filter { it.id != posFieldId }.toSet()

        return if (partOfSpeech.isBlank()) {
            // Xóa custom field, fallback về ContentType.name
            ContentCustomFields(existingOtherFields)
        } else {
            ContentCustomFields(
                existingOtherFields + ContentCustomField(id = posFieldId, value = partOfSpeech)
            )
        }
    }
}
