package vn.loi.learning.desktop.ui.localization

import vn.loi.learning.desktop.runtime.DesktopLocale
import vn.loi.learning.desktop.runtime.DesktopThemePreference
import vn.loi.learning.desktop.ui.navigation.NavigationDestination

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
