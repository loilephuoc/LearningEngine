package vn.loi.learning.desktop.ui.settings

import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences

data class StudyPresentationSettingsState(
    val active: StudyPresentationPreferences,
    val draft: StudyPresentationPreferences = active
) {
    fun edit(updated: StudyPresentationPreferences) = copy(draft = updated)

    fun applyTo(configuration: DesktopRuntimeConfiguration) =
        configuration.copy(studyPresentation = draft)
}

data class StudyPresentationSettingsPresentation(
    val controlsEnabled: Boolean,
    val guidance: String,
    val showEnglishPreview: Boolean,
    val showVietnamesePreview: Boolean,
    val autoplayPreview: Boolean
)

fun resolveStudyPresentationSettings(
    preferences: StudyPresentationPreferences
): StudyPresentationSettingsPresentation =
    when (preferences.controlMode) {
        StudyPresentationControlMode.ADAPTIVE ->
            StudyPresentationSettingsPresentation(
                controlsEnabled = false,
                guidance = "Product Brain quyết định nội dung và âm thanh phù hợp với tiến trình học.",
                showEnglishPreview = true,
                showVietnamesePreview = true,
                autoplayPreview = false
            )

        StudyPresentationControlMode.PREFERENCE_GUIDED ->
            StudyPresentationSettingsPresentation(
                controlsEnabled = true,
                guidance = "Product Brain thích ứng trong phạm vi các tùy chọn bạn cho phép.",
                showEnglishPreview = preferences.showEnglish,
                showVietnamesePreview = preferences.showVietnamese,
                autoplayPreview = false
            )

        StudyPresentationControlMode.MANUAL ->
            StudyPresentationSettingsPresentation(
                controlsEnabled = true,
                guidance = "Các tùy chọn của bạn trực tiếp điều khiển nội dung và tự phát âm thanh.",
                showEnglishPreview = preferences.showEnglish,
                showVietnamesePreview = preferences.showVietnamese,
                autoplayPreview = false
            )
    }

fun StudyPresentationControlMode.settingsLabel(): String =
    when (this) {
        StudyPresentationControlMode.ADAPTIVE -> "Thích ứng"
        StudyPresentationControlMode.PREFERENCE_GUIDED -> "Theo tùy chọn"
        StudyPresentationControlMode.MANUAL -> "Thủ công"
    }
