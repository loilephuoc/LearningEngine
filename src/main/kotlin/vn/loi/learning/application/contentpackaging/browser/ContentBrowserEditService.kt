package vn.loi.learning.application.contentpackaging.browser

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText

/**
 * Application Service xử lý các thao tác chỉnh sửa và xóa Content
 * từ Learning Browser.
 *
 * Không chạm vào LearningItem, MediaAsset, PackageDescriptor, hay ContentLibrary.
 * Mỗi method là một đơn vị công việc hoàn chỉnh: load → mutate → save.
 */
class ContentBrowserEditService(
    private val contentRepository: ContentRepository
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
     *
     * @throws IllegalArgumentException nếu contentId không tồn tại.
     */
    fun deleteContent(
        contentId: ContentId,
        learningItemRepository: vn.loi.learning.application.port.LearningItemRepository
    ) {
        contentRepository.findById(contentId)
            ?: throw IllegalArgumentException("Content not found: ${contentId.value}")

        learningItemRepository.deleteByContentIds(setOf(contentId))
        contentRepository.deleteById(contentId)
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
