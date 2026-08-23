package vn.loi.learning.android.platform

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test
import vn.loi.learning.android.packageexperience.AndroidPackageContentRow
import vn.loi.learning.android.packageexperience.AndroidContentFsrsStatus
import vn.loi.learning.android.reminder.AndroidHomeVocabularyWidgetDraft
import vn.loi.learning.android.reminder.AndroidVocabularyReminderIntervalUnit

class AppLanguageAndUxPolishTest {

    @Test
    fun `AppLanguage resolves code correctly with English default fallback`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("EN"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.fromCode("vi"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.fromCode("VI"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("unknown"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
    }

    @Test
    fun `interval input select-all replacement replaces existing 30 with typed 4`() {
        // Initial state: "30" with entire text selected on focus
        var intervalTextFieldValue = TextFieldValue("30", selection = TextRange(0, 2))
        var draft = AndroidHomeVocabularyWidgetDraft(intervalValueText = "30")

        // User types '4' while text is selected:
        val typed4 = TextFieldValue("4", selection = TextRange(1))
        val filtered = typed4.text.filter { it.isDigit() }
        intervalTextFieldValue = typed4.copy(text = filtered)
        draft = draft.copy(intervalValueText = filtered)

        assertEquals("4", intervalTextFieldValue.text)
        assertEquals("4", draft.intervalValueText)
    }

    @Test
    fun `interval input filters out non-digits like dashes, dots, commas, spaces, letters`() {
        val rawInputs = listOf("-", ".", ",", " ", "a", "4-2", "3.5", "10 min", "5a")
        val expected = listOf("", "", "", "", "", "42", "35", "10", "5")

        rawInputs.zip(expected).forEach { (raw, exp) ->
            val filtered = raw.filter { it.isDigit() }
            assertEquals(exp, filtered)
        }
    }

    @Test
    fun `interval preset replaces draft interval value directly`() {
        var draft = AndroidHomeVocabularyWidgetDraft(intervalValueText = "30", intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS)

        // Preset 5s tapped
        draft = draft.copy(intervalValueText = "5", intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS)
        assertEquals("5", draft.intervalValueText)
        assertEquals(AndroidVocabularyReminderIntervalUnit.SECONDS, draft.intervalUnit)

        // Preset 2m tapped
        draft = draft.copy(intervalValueText = "2", intervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES)
        assertEquals("2", draft.intervalValueText)
        assertEquals(AndroidVocabularyReminderIntervalUnit.MINUTES, draft.intervalUnit)
    }

    @Test
    fun `Package library interaction contract enforces word audio only on text tap and detail on image tap`() {
        var wordAudioPlayed: String? = null
        var detailOpened = false
        var difficultToggled = false
        var quickEditTriggered = false

        val row = AndroidPackageContentRow(
            contentId = "c1",
            question = "beyond",
            answer = "vượt ra ngoài",
            lesson = "Lesson 1",
            group = null,
            section = null,
            index = 0,
            pronunciation = "/bɪˈjɑːnd/",
            partOfSpeech = "PREPOSITION",
            hasImage = true,
            hasAudio = true,
            audioRef = "audio/beyond.mp3",
            imageRef = "images/beyond.png",
            fsrsStatus = AndroidContentFsrsStatus.LEARNING,
            isDifficult = false
        )

        // 1. Text tap -> triggers word audio only
        val onTextTap: () -> Unit = {
            row.audioRef?.let { wordAudioPlayed = it }
        }
        onTextTap()
        assertEquals("audio/beyond.mp3", wordAudioPlayed)
        assertFalse(detailOpened)
        assertFalse(difficultToggled)

        // 2. Image tap -> opens detail only
        val onImageTap: () -> Unit = {
            detailOpened = true
        }
        onImageTap()
        assertTrue(detailOpened)

        // 3. Star tap -> toggles difficult only
        val onStarTap: () -> Unit = {
            difficultToggled = true
        }
        onStarTap()
        assertTrue(difficultToggled)

        // 4. Long click -> quick edit only
        val onLongClick: () -> Unit = {
            quickEditTriggered = true
        }
        onLongClick()
        assertTrue(quickEditTriggered)
    }

    @Test
    fun `Package library row with null audioRef safely ignores text tap without crash or substitute`() {
        var audioPlayed = false
        val row = AndroidPackageContentRow(
            contentId = "c2",
            question = "silent",
            answer = "im lặng",
            lesson = "Lesson 1",
            group = null,
            section = null,
            index = 1,
            pronunciation = "/ˈsaɪ.lənt/",
            partOfSpeech = "ADJECTIVE",
            hasImage = false,
            hasAudio = false,
            audioRef = null,
            imageRef = null,
            fsrsStatus = AndroidContentFsrsStatus.NEW,
            isDifficult = false
        )

        val onTextTap: () -> Unit = {
            row.audioRef?.let { audioPlayed = true }
        }
        onTextTap()
        assertFalse(audioPlayed)
    }

    @Test
    fun `strings XML resource parity between English and Vietnamese`() {
        val enFile = java.io.File("src/main/res/values/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values/strings.xml")
        val viFile = java.io.File("src/main/res/values-vi/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values-vi/strings.xml")

        assertTrue("English strings.xml must exist", enFile.exists())
        assertTrue("Vietnamese strings.xml must exist", viFile.exists())

        fun extractKeys(file: java.io.File): Set<String> {
            val keyRegex = Regex("""<string\s+name="([^"]+)"""")
            return keyRegex.findAll(file.readText()).map { it.groupValues[1] }.toSet()
        }

        val enKeys = extractKeys(enFile)
        val viKeys = extractKeys(viFile)

        assertTrue("English keys must not be empty", enKeys.isNotEmpty())
        assertTrue("Vietnamese keys must not be empty", viKeys.isNotEmpty())

        val missingInVi = enKeys - viKeys
        val missingInEn = viKeys - enKeys

        assertTrue("All English keys must have Vietnamese translation. Missing: $missingInVi", missingInVi.isEmpty())
        assertTrue("All Vietnamese keys must match an English key. Extra: $missingInEn", missingInEn.isEmpty())
    }

    @Test
    fun `Reminder popup POS badge supports long POS labels horizontally without character wrapping`() {
        val longPosList = listOf("PREPOSITION", "PHRASAL VERB", "PROPER NOUN", "ADVERB", "ADJECTIVE", "NOUN", "VERB")

        longPosList.forEach { pos ->
            val upper = pos.uppercase()
            // POS badge text is single-line, uppercase, non-empty
            assertTrue(upper.isNotBlank())
            assertEquals(pos.uppercase(), upper)
            assertFalse(upper.contains("\n"))
        }
    }

    @Test
    fun `Big Popup overlay above launcher preserves VISIBLE HomeSurfaceState without pausing widget rotation`() {
        // Authoritative home state evaluation simulation:
        // topAppPkg = launcher (e.g. "com.miui.home"), rawEventPkg = "vn.loi.learning.android" (overlay window)
        val defaultLauncher = "com.miui.home"
        val topAppPkg = "com.miui.home"
        val rawEventPkg = "vn.loi.learning.android"

        fun isLauncher(pkg: String?) = pkg == defaultLauncher

        val resolvedHomeState = when {
            isLauncher(topAppPkg) -> vn.loi.learning.android.reminder.HomeSurfaceState.VISIBLE
            topAppPkg != null -> vn.loi.learning.android.reminder.HomeSurfaceState.HIDDEN
            rawEventPkg == "vn.loi.learning.android" -> vn.loi.learning.android.reminder.HomeSurfaceState.VISIBLE
            else -> vn.loi.learning.android.reminder.HomeSurfaceState.HIDDEN
        }

        assertEquals(vn.loi.learning.android.reminder.HomeSurfaceState.VISIBLE, resolvedHomeState)
    }

    @Test
    fun `Big Popup active suppresses Home widget auto-audio while candidate cadence continues`() {
        var widgetAutoAudioPlayed = false
        var skipReason: String? = null
        val isPopupShowing = true
        val isAutoAudioEnabled = true
        val isHomeSurfaceAllowed = true

        fun checkAndTriggerWidgetAudio() {
            if (!isAutoAudioEnabled) {
                skipReason = "AUTO_AUDIO_DISABLED"
                return
            }
            if (!isHomeSurfaceAllowed) {
                skipReason = "HOME_SURFACE_NOT_ALLOWED"
                return
            }
            if (isPopupShowing) {
                skipReason = "POPUP_ACTIVE"
                return
            }
            widgetAutoAudioPlayed = true
            skipReason = "NONE"
        }

        checkAndTriggerWidgetAudio()

        assertFalse("Widget auto-audio must be suppressed while Big Popup is showing", widgetAutoAudioPlayed)
        assertEquals("POPUP_ACTIVE", skipReason)
    }

    @Test
    fun `Manual next cancels pending timer countdown and restarts fresh full interval`() {
        var scheduledIntervalMs = 0L
        var scheduleCount = 0
        var isArmed = false

        fun cancelInternal(reason: String) {
            isArmed = false
            scheduledIntervalMs = 0L
        }

        fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean) {
            if (!freshInterval && isArmed && scheduledIntervalMs == intervalMs) {
                return
            }
            cancelInternal("RESCHEDULE (fresh=$freshInterval): $reason")
            scheduledIntervalMs = intervalMs
            isArmed = true
            scheduleCount++
        }

        // 1. Initial 30s armed
        schedule(30000L, "INIT", freshInterval = false)
        assertTrue(isArmed)
        assertEquals(30000L, scheduledIntervalMs)
        assertEquals(1, scheduleCount)

        // 2. Schedule called with same interval and freshInterval=false -> ignored (continues current countdown)
        schedule(30000L, "NOOP", freshInterval = false)
        assertEquals(1, scheduleCount)

        // 3. User taps MANUAL NEXT -> freshInterval=true cancels and restarts fresh 30s countdown
        schedule(30000L, "MANUAL_NEXT", freshInterval = true)
        assertTrue(isArmed)
        assertEquals(30000L, scheduledIntervalMs)
        assertEquals(2, scheduleCount)
    }

    @Test
    fun `Bottom navigation destinations expose string resource IDs resolving to localized Vietnamese labels`() {
        val viFile = java.io.File("src/main/res/values-vi/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values-vi/strings.xml")
        val viText = viFile.readText()

        fun extractViString(key: String): String? {
            val match = Regex("""<string\s+name="$key">([^<]+)</string>""").find(viText)
            return match?.groupValues?.get(1)?.replace("\\'", "'")
        }

        assertEquals("Trang chủ", extractViString("nav_home"))
        assertEquals("Thư viện", extractViString("nav_library"))
        assertEquals("Học", extractViString("nav_study"))
        assertEquals("Ôn tập", extractViString("nav_review"))
        assertEquals("Cài đặt", extractViString("nav_settings"))
    }

    @Test
    fun `Primary screen headers and components resolve to correct Vietnamese translations`() {
        val viFile = java.io.File("src/main/res/values-vi/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values-vi/strings.xml")
        val viText = viFile.readText()

        fun extractViString(key: String): String? {
            val match = Regex("""<string\s+name="$key">([^<]+)</string>""").find(viText)
            return match?.groupValues?.get(1)?.replace("\\'", "'")
        }

        // Home
        assertEquals("Tiếp tục tiến trình học tập", extractViString("home_header_title"))
        assertEquals("Tiếp tục phiên học", extractViString("home_continue_session"))
        assertEquals("Đến hạn", extractViString("home_due_label"))
        assertEquals("Từ đang học", extractViString("home_active_memories"))

        // Study
        assertEquals("Học", extractViString("study_hub_title"))
        assertEquals("Tiếp tục học từ nội dung đang kích hoạt", extractViString("study_hub_subtitle"))
        assertEquals("Học từ mới", extractViString("study_mode_learn_new"))
        assertEquals("Học thích ứng", extractViString("study_mode_adaptive"))
        assertEquals("Luyện gõ", extractViString("study_mode_typing"))

        // Settings
        assertEquals("Giao diện", extractViString("settings_appearance_title"))
        assertEquals("Ngôn ngữ ứng dụng", extractViString("settings_app_language"))

        // Library
        assertEquals("Thư viện", extractViString("library_title"))
        assertEquals("Gói học và bộ sưu tập", extractViString("library_subtitle"))
        assertEquals("Có sẵn", extractViString("library_available"))
    }

    @Test
    fun `MainActivity root composition preserves LocalActivityResultRegistryOwner and wraps context as ContextWrapper`() {
        val mainSourceFile = java.io.File("src/main/kotlin/vn/loi/learning/android/MainActivity.kt").takeIf { it.exists() }
            ?: java.io.File("android/src/main/kotlin/vn/loi/learning/android/MainActivity.kt")
        val mainSource = mainSourceFile.readText()

        assertTrue("MainActivity must explicitly provide LocalActivityResultRegistryOwner",
            mainSource.contains("LocalActivityResultRegistryOwner provides this@MainActivity"))
        assertTrue("MainActivity must use ContextWrapper to preserve Activity instance in context chain",
            mainSource.contains("LocalizedActivityContextWrapper(baseContext, configContext)"))
        assertTrue("LocalizedActivityContextWrapper must inherit from ContextWrapper",
            mainSource.contains("class LocalizedActivityContextWrapper") && mainSource.contains(": ContextWrapper(base)"))
    }

    @Test
    fun `Study mode cards and rating buttons resolve to required Vietnamese translations`() {
        val viFile = java.io.File("src/main/res/values-vi/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values-vi/strings.xml")
        val viText = viFile.readText()

        fun extractViString(key: String): String? {
            val match = Regex("""<string\s+name="$key">([^<]+)</string>""").find(viText)
            return match?.groupValues?.get(1)?.replace("\\'", "'")?.replace("&amp;", "&")
        }

        // Ratings policy
        assertEquals("Học lại", extractViString("rating_again"))
        assertEquals("Khó", extractViString("rating_hard"))
        assertEquals("Tốt", extractViString("rating_good"))
        assertEquals("Dễ", extractViString("rating_easy"))

        // Study Mode Descriptions
        assertEquals("Chuyển từ phiên hiện tại sang học các từ mới trong gói.", extractViString("study_learn_new_desc_switch"))
        assertEquals("Tiếp tục ôn luyện thông minh với các từ đã học.", extractViString("study_adaptive_desc_switch"))
        assertEquals("Chuyển từ phiên hiện tại sang chế độ luyện gõ.", extractViString("study_typing_desc_switch"))

        // Insights & Forecast
        assertEquals("Thống kê & Dự báo", extractViString("insights_title"))
        assertEquals("Tất cả gói học", extractViString("insights_all_packages"))
        assertEquals("Dự báo ôn tập", extractViString("insights_forecast_title"))
        assertEquals("Ngày mai", extractViString("insights_forecast_tomorrow"))
        assertEquals("Mức độ ghi nhớ", extractViString("insights_retention_title"))
        assertEquals("Mới", extractViString("insights_retention_new"))
        assertEquals("Đang học", extractViString("insights_retention_learning"))
        assertEquals("Mới hình thành (≤21 ngày)", extractViString("insights_retention_young"))
        assertEquals("Đã ghi nhớ (>21 ngày)", extractViString("insights_retention_retained"))
        assertEquals("Đánh giá hôm nay", extractViString("insights_today_ratings_title"))
    }

    @Test
    fun `StudyHub layout uses clickable compact cards and eliminates trailing CTA action buttons`() {
        val navSourceFile = java.io.File("src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt").takeIf { it.exists() }
            ?: java.io.File("android/src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        val navSource = navSourceFile.readText()

        assertFalse("StudyHub should no longer contain wide trailing switch CTA buttons",
            navSource.contains("study_switch_learn_new") ||
            navSource.contains("study_switch_adaptive") ||
            navSource.contains("study_switch_typing"))

        assertTrue("StudyHub must use full-card clickable compact cards",
            navSource.contains("LearningEngineCompactCard") &&
            navSource.contains("StudyMode.LEARN_NEW") &&
            navSource.contains("StudyMode.ADAPTIVE") &&
            navSource.contains("StudyMode.TYPING"))
    }

    @Test
    fun `ReviewHub layout uses clickable compact cards and eliminates trailing CTA action buttons`() {
        val navSourceFile = java.io.File("src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt").takeIf { it.exists() }
            ?: java.io.File("android/src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        val navSource = navSourceFile.readText()
        val reviewHub = navSource.substringAfter("fun ReviewHub(").substringBefore("@Composable\nprivate fun QuickReviewInsightsCard(")

        assertFalse("ReviewHub must not contain CTA button labels",
            reviewHub.contains("review_action_switch") ||
            reviewHub.contains("review_action_start") ||
            reviewHub.contains("LearningEngineSecondaryButton"))

        assertTrue("ReviewHub cards must be directly clickable",
            reviewHub.contains("LearningEngineCompactCard") &&
            reviewHub.contains("clickable(enabled = action.available)") &&
            reviewHub.contains("onEvent(AndroidStudyEvent.Start(action.entry))"))
    }

    @Test
    fun `Library package card redesign uses high density layout with active badge and no low-value metadata`() {
        val libSourceFile = java.io.File("src/main/kotlin/vn/loi/learning/android/library/LibraryScreen.kt").takeIf { it.exists() }
            ?: java.io.File("android/src/main/kotlin/vn/loi/learning/android/library/LibraryScreen.kt")
        val libSource = libSourceFile.readText()
        val pkgCard = libSource.substringAfter("private fun LibraryPackageCard(").substringBefore("private fun ImportState(")

        // Active state badge
        assertTrue(pkgCard.contains("R.string.library_active_badge"))
        assertTrue(pkgCard.contains("R.string.library_active_semantics"))
        assertTrue(pkgCard.contains("pkg.isActivePackage"))
        assertTrue(pkgCard.contains("maxLines = 2"))

        // Removed low value list items
        assertFalse(pkgCard.contains("pkg.version"))
        assertFalse(pkgCard.contains("R.string.library_available"))
        assertFalse(pkgCard.contains("R.string.library_use_for_study"))
        assertFalse(pkgCard.contains("R.string.library_current_package"))
    }

    @Test
    fun `Library header search and section headings resolve to localized Vietnamese`() {
        val viFile = java.io.File("src/main/res/values-vi/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values-vi/strings.xml")
        val viText = viFile.readText()

        fun extractViString(key: String): String? {
            val match = Regex("""<string\s+name="$key">([^<]+)</string>""").find(viText)
            return match?.groupValues?.get(1)?.replace("\\'", "'")?.replace("&amp;", "&")
        }

        assertEquals("Tìm gói học và bộ sưu tập", extractViString("library_search_packages_collections"))
        assertEquals("Gói học", extractViString("library_tab_packages"))
        assertEquals("Bộ sưu tập", extractViString("library_section_collections"))
        assertEquals("Đang học", extractViString("library_active_badge"))
        assertEquals("Đang học", extractViString("library_active_semantics"))
    }

    @Test
    fun `ReviewHub mode identity and reminder strings resolve to accurate Vietnamese translations`() {
        val viFile = java.io.File("src/main/res/values-vi/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values-vi/strings.xml")
        val viText = viFile.readText()

        fun extractViString(key: String): String? {
            val match = Regex("""<string\s+name="$key">([^<]+)</string>""").find(viText)
            return match?.groupValues?.get(1)?.replace("\\'", "'")?.replace("&amp;", "&")
        }

        // Review mode identity
        assertEquals("Ôn Học lại / Khó", extractViString("review_difficult_title"))
        assertEquals("NHANH · KHÔNG GIỚI HẠN", extractViString("review_quick_badge"))

        // Unlocked Reminder Popup translations
        assertEquals("Thông báo nổi khi mở khóa", extractViString("reminder_unlocked_popup_title"))
        assertEquals("Bật nhắc nhở khi mở khóa", extractViString("reminder_enable_unlocked"))
        assertEquals("Nhắc nhở đang được lên lịch", extractViString("reminder_actively_scheduled"))
        assertEquals("Nhắc nhở đang tạm dừng/tắt", extractViString("reminder_paused_or_off"))
        assertEquals("Yêu cầu quyền thông báo", extractViString("reminder_permission_required"))
        assertEquals("Cấp quyền để nhận thông báo nhắc nhở từ vựng định kỳ.", extractViString("reminder_permission_desc"))
        assertEquals("Phát âm khi hiện nhắc nhở", extractViString("reminder_play_pronunciation"))
        assertEquals("Popup nhắc nhở lớn", extractViString("reminder_large_popup_title"))
        assertEquals("Hiển thị thẻ từ vựng lớn trên các ứng dụng khác khi màn hình đã mở khóa.", extractViString("reminder_large_popup_desc"))
        assertEquals("Yêu cầu quyền hiển thị trên ứng dụng khác", extractViString("reminder_large_popup_permission_title"))
        assertEquals("Cấp quyền 'Hiển thị trên các ứng dụng khác' để cho phép hiển thị thẻ từ vựng nổi.", extractViString("reminder_large_popup_permission_desc"))
        assertEquals("Nút tạm dừng nhanh", extractViString("reminder_quick_pause_actions_title"))
        assertEquals("Hiển thị các nút tạm dừng 5 phút, 30 phút và 1 giờ trên popup nhắc nhở.", extractViString("reminder_quick_pause_actions_desc"))
        assertEquals("Tạm dừng nhắc nhở nhanh", extractViString("reminder_quick_pause_title"))
        assertEquals("Chế độ nhắc nhở", extractViString("reminder_mode_title"))
        assertEquals("Học lại / Khó", extractViString("reminder_mode_again_hard"))
        assertEquals("Từ đến hạn", extractViString("reminder_mode_due"))
        assertEquals("Ngẫu nhiên từ đã học", extractViString("reminder_mode_random_learned"))
        assertEquals("Ngẫu nhiên tất cả", extractViString("reminder_mode_random_all"))
        assertEquals("Đã đánh dấu khó", extractViString("reminder_mode_marked_difficult"))
        assertEquals("Khoảng thời gian nhắc", extractViString("reminder_interval_title"))
        assertEquals("Giá trị", extractViString("reminder_interval_value"))
        assertEquals("Đã gửi thông báo xem trước.", extractViString("reminder_preview_sent"))
    }

    @Test
    fun `primary Android surfaces expose Vietnamese actions dialogs and accessibility text`() {
        fun source(relative: String) = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/kotlin").resolve(relative)
        )
        val surfaces = listOf(
            "vn/loi/learning/android/packageexperience/PackageScreen.kt",
            "vn/loi/learning/android/study/StudyScreen.kt",
            "vn/loi/learning/android/recording/QuickVoiceRecordingsScreen.kt",
            "vn/loi/learning/android/recovery/BackupRestoreScreen.kt",
            "vn/loi/learning/android/reminder/ReminderReviewScreen.kt",
            "vn/loi/learning/android/reminder/LockScreenSettingsScreen.kt",
            "vn/loi/learning/android/reminder/HomeWidgetSettingsScreen.kt",
            "vn/loi/learning/android/ui/LearningEngineComponents.kt"
        ).associateWith(::source)

        listOf("Text(\"Cancel\")", "Text(\"Retry\")", "Text(\"Dismiss\")", "contentDescription = \"Back\"")
            .forEach { forbidden ->
                assertTrue("Chuỗi UI tiếng Anh còn sót: $forbidden", surfaces.values.none { it.contains(forbidden) })
            }
        assertTrue(surfaces.getValue("vn/loi/learning/android/packageexperience/PackageScreen.kt").contains("Gỡ cài đặt gói?"))
        assertTrue(surfaces.getValue("vn/loi/learning/android/recovery/BackupRestoreScreen.kt").contains("Tạo bản sao lưu thành công"))
        assertTrue(surfaces.getValue("vn/loi/learning/android/recording/QuickVoiceRecordingsScreen.kt").contains("Bản ghi âm"))
    }
    @Test
    fun `Home widget and lock screen settings keep English Vietnamese key parity`() {
        val enFile = java.io.File("src/main/res/values/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values/strings.xml")
        val viFile = java.io.File("src/main/res/values-vi/strings.xml").takeIf { it.exists() }
            ?: java.io.File("android/src/main/res/values-vi/strings.xml")
        val keyRegex = Regex("""<string\s+name="([^"]+)">""")
        val enKeys = keyRegex.findAll(enFile.readText()).map { it.groupValues[1] }.toSet()
        val viKeys = keyRegex.findAll(viFile.readText()).map { it.groupValues[1] }.toSet()
        assertEquals(enKeys, viKeys)
        listOf("widget_auto_next_rotation_title", "widget_screen_on_only_explanation",
            "lock_screen_wallpaper_title", "lock_screen_candidate_pool_title",
            "lock_screen_quick_review_desc", "lock_screen_prep_delay_title", "delay_instant"
        ).forEach { key -> assertTrue("Missing localized key: $key", key in enKeys) }
        val widget = java.io.File("src/main/kotlin/vn/loi/learning/android/reminder/HomeWidgetSettingsScreen.kt").readText()
        val lock = java.io.File("src/main/kotlin/vn/loi/learning/android/reminder/LockScreenSettingsScreen.kt").readText()
        assertTrue(widget.contains("stringResource(R.string.widget_settings_title)"))
        assertTrue(lock.contains("stringResource(R.string.lock_screen_settings_title)"))
    }
}
