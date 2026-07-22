package vn.loi.learning.desktop.ui.localization

import vn.loi.learning.desktop.runtime.DesktopLocale
import vn.loi.learning.desktop.runtime.DesktopThemePreference
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.study.LearningContentRendererStrings
import vn.loi.learning.desktop.ui.study.StudyWorkspaceStrings

data class DesktopStrings(
    val navigation: Map<NavigationDestination, String>,
    val settingsTitle: String,
    val settingsSubtitle: String,
    val applicationSection: String,
    val theme: String,
    val language: String,
    val aboutAndSupport: String,
    val aboutButton: String,
    val runtimeInformation: String,
    val close: String,
    val startup: String,
    val exportDiagnostics: String,
    val diagnosticsExported: String,
    val diagnosticsExportFailure: String,
    val recovery: String,
    val createBackup: String,
    val restoreBackup: String,
    val restoreWarning: String,
    val confirmRestore: String,
    val backupCreated: String,
    val restoreCompleted: String,
    val recoveryFailure: String,
    val onboardingTitle: String,
    val onboardingMessage: String,
    val installSample: String,
    val skipSample: String,
    val learningContent: LearningContentRendererStrings,
    val studyWorkspace: StudyWorkspaceStrings,
    val themeNames: Map<DesktopThemePreference, String>,
    val languageNames: Map<DesktopLocale, String>
) {
    fun destination(destination: NavigationDestination): String =
        requireNotNull(navigation[destination])

    fun theme(preference: DesktopThemePreference): String =
        requireNotNull(themeNames[preference])

    fun language(locale: DesktopLocale): String =
        requireNotNull(languageNames[locale])

    fun diagnosticsExportedTo(path: String): String = "$diagnosticsExported: $path"

    fun diagnosticsExportFailed(reason: String): String = "$diagnosticsExportFailure: $reason"
    fun backupCreatedAt(path: String) = "$backupCreated: $path"
    fun restoreCompletedFrom(path: String) = "$restoreCompleted: $path"
    fun recoveryFailed(reason: String) = "$recoveryFailure: $reason"
}

object DesktopLocalization {
    fun strings(locale: DesktopLocale): DesktopStrings =
        when (locale) {
            DesktopLocale.ENGLISH -> english
            DesktopLocale.VIETNAMESE -> vietnamese
        }

    private val english =
        DesktopStrings(
            navigation = mapOf(
                NavigationDestination.DASHBOARD to "Home",
                NavigationDestination.STUDY to "Learn",
                NavigationDestination.STATISTICS to "Statistics",
                NavigationDestination.REVIEW_HISTORY to "Review",
                NavigationDestination.CONTENT_LIBRARY to "Library",
                NavigationDestination.SETTINGS to "Settings"
            ),
            settingsTitle = "Settings",
            settingsSubtitle = "Current Learning Engine configuration",
            applicationSection = "Desktop Application",
            theme = "Theme",
            language = "Language",
            aboutAndSupport = "About and Support",
            aboutButton = "About Learning Engine",
            runtimeInformation = "Runtime information",
            close = "Close",
            startup = "Starting Learning Engine",
            exportDiagnostics = "Export diagnostics",
            diagnosticsExported = "Diagnostics exported",
            diagnosticsExportFailure = "Diagnostics export failed",
            recovery = "Backup and restore",
            createBackup = "Create backup",
            restoreBackup = "Restore backup",
            restoreWarning = "Restoring replaces all current durable data. A safety backup is created first. The application closes after a successful restore.",
            confirmRestore = "Replace data and restore",
            backupCreated = "Backup created",
            restoreCompleted = "Restore completed",
            recoveryFailure = "Recovery operation failed",
            onboardingTitle = "Welcome to Learning Engine",
            onboardingMessage = "Start with a small retrieval-practice sample or continue with an empty library and import your own OPD3 package.",
            installSample = "Install starter sample",
            skipSample = "Continue with empty library",
            learningContent = LearningContentRendererStrings(
                "Answer unavailable", "Image unavailable", "Audio unavailable or unsupported",
                "Learning content image", "Learning content audio", "Play audio", "Stop audio",
                "Answer", "Example"
            ),
            studyWorkspace = StudyWorkspaceStrings.ENGLISH,
            themeNames = mapOf(
                DesktopThemePreference.LIGHT to "Light",
                DesktopThemePreference.DARK to "Dark",
                DesktopThemePreference.SYSTEM to "System"
            ),
            languageNames = mapOf(
                DesktopLocale.ENGLISH to "English",
                DesktopLocale.VIETNAMESE to "Vietnamese"
            )
        )

    private val vietnamese =
        DesktopStrings(
            navigation = mapOf(
                NavigationDestination.DASHBOARD to "Trang chủ",
                NavigationDestination.STUDY to "Học",
                NavigationDestination.STATISTICS to "Thống kê",
                NavigationDestination.REVIEW_HISTORY to "Ôn tập",
                NavigationDestination.CONTENT_LIBRARY to "Thư viện",
                NavigationDestination.SETTINGS to "Cài đặt"
            ),
            settingsTitle = "Cài đặt",
            settingsSubtitle = "Cấu hình Learning Engine hiện tại",
            applicationSection = "Ứng dụng máy tính",
            theme = "Giao diện",
            language = "Ngôn ngữ",
            aboutAndSupport = "Giới thiệu và hỗ trợ",
            aboutButton = "Giới thiệu Learning Engine",
            runtimeInformation = "Thông tin môi trường chạy",
            close = "Đóng",
            startup = "Đang khởi động Learning Engine",
            exportDiagnostics = "Xuất thông tin chẩn đoán",
            diagnosticsExported = "Đã xuất thông tin chẩn đoán",
            diagnosticsExportFailure = "Không thể xuất thông tin chẩn đoán",
            recovery = "Sao lưu và khôi phục",
            createBackup = "Tạo bản sao lưu",
            restoreBackup = "Khôi phục bản sao lưu",
            restoreWarning = "Khôi phục sẽ thay thế toàn bộ dữ liệu bền vững hiện tại. Ứng dụng tạo bản sao lưu an toàn trước và đóng sau khi khôi phục thành công.",
            confirmRestore = "Thay thế dữ liệu và khôi phục",
            backupCreated = "Đã tạo bản sao lưu",
            restoreCompleted = "Đã khôi phục",
            recoveryFailure = "Thao tác khôi phục thất bại",
            onboardingTitle = "Chào mừng đến Learning Engine",
            onboardingMessage = "Bắt đầu với nội dung gợi nhớ mẫu hoặc tiếp tục với thư viện trống và nhập gói OPD3 của bạn.",
            installSample = "Cài nội dung mẫu",
            skipSample = "Tiếp tục với thư viện trống",
            learningContent = LearningContentRendererStrings(
                "Không có câu trả lời", "Không thể hiển thị hình ảnh",
                "Không thể phát âm thanh hoặc định dạng không được hỗ trợ",
                "Hình ảnh nội dung học", "Âm thanh nội dung học", "Phát âm thanh", "Dừng âm thanh",
                "Câu trả lời", "Ví dụ"
            ),
            studyWorkspace = StudyWorkspaceStrings(
                labels = mapOf(
                    vn.loi.learning.desktop.ui.study.StudyActionControl.RETRY_LOAD to "Thử lại",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.START_STUDY to "Bắt đầu học",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.START_GENERAL_STUDY to "Bắt đầu phiên học chung",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.REVEAL_ANSWER to "Hiện câu trả lời",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.REVIEW_AGAIN to "Lại",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.REVIEW_HARD to "Khó",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.REVIEW_GOOD to "Tốt",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.REVIEW_EASY to "Dễ",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.UNDO_LATEST to "Hoàn tác đánh giá gần nhất",
                    vn.loi.learning.desktop.ui.study.StudyActionControl.PAUSE_WORKSPACE to "Tạm dừng"
                ),
                shortcutTemplate = { label, shortcut -> "$label. Phím tắt: $shortcut." }
            ),
            themeNames = mapOf(
                DesktopThemePreference.LIGHT to "Sáng",
                DesktopThemePreference.DARK to "Tối",
                DesktopThemePreference.SYSTEM to "Hệ thống"
            ),
            languageNames = mapOf(
                DesktopLocale.ENGLISH to "Tiếng Anh",
                DesktopLocale.VIETNAMESE to "Tiếng Việt"
            )
        )
}
