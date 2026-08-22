package vn.loi.learning.android.reminder

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidQuickReviewWidgetTest {
    @Test fun `each widget instance advances deterministically and resets reveal`() {
        val first = QuickReviewWidgetState("pkg", "one", 0, revealed = true)
        assertEquals(QuickReviewWidgetState("pkg", null, 1, false), advanceQuickReviewWidget(first, 3))
        assertEquals(QuickReviewWidgetState("pkg", null, 0, false), advanceQuickReviewWidget(first.copy(index = 2), 3))
        assertEquals(first.copy(contentId = null, index = 0, revealed = false), advanceQuickReviewWidget(first, 0))
    }

    @Test fun `dedicated provider is independently declared as a large resizable home widget`() {
        val manifest = source("AndroidManifest.xml")
        val info = source("res/xml/quick_review_widget_info.xml")
        assertTrue(manifest.contains("AndroidQuickReviewWidgetProvider"))
        assertTrue(manifest.contains("@xml/quick_review_widget_info"))
        assertTrue(info.contains("android:targetCellHeight=\"4\""))
        assertTrue(info.contains("android:resizeMode=\"horizontal|vertical\""))
        assertTrue(info.contains("android:updatePeriodMillis=\"0\""))
        assertFalse(manifest.substringAfter("AndroidQuickReviewWidgetProvider").substringBefore("</receiver>").contains("HomeVocabularyWidgetRuntimeService"))
    }

    @Test fun `widget presentation is Vietnamese readable and exposes reveal next and ratings`() {
        val layout = source("res/layout/quick_review_widget.xml")
        listOf("ÔN NHANH", "34sp", "23sp", "Hiện đáp án", "Tiếp theo", "Học lại", "Khó", "Tốt", "Dễ", "48dp")
            .forEach { assertTrue(layout.contains(it), it) }
        assertFalse(layout.contains("Reveal answer"))
        assertFalse(layout.contains("Next"))
    }

    @Test fun `coordinator uses canonical active package queue and canonical rating bridge`() {
        val coordinator = source("kotlin/vn/loi/learning/android/reminder/AndroidQuickReviewWidgetCoordinator.kt")
        assertTrue(coordinator.contains("domainLibraryRepository?.findById(libraryId)?.activePackageId"))
        assertTrue(coordinator.contains("selector.getReviewQueue(packageId, AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED.name"))
        assertFalse(coordinator.contains("AndroidVocabularyReminderSelectionMode.RANDOM_ALL.name"))
        assertTrue(coordinator.contains("ratingBridge.submitRating(contentId, rating)"))
        assertTrue(coordinator.contains("state.contentId != expectedContentId"))
        assertTrue(coordinator.contains("!state.revealed"))
        assertFalse(coordinator.contains("startSession"))
        assertFalse(coordinator.contains("StudyQueue"))
        assertFalse(coordinator.contains("daily"))
        assertFalse(coordinator.contains("scheduleAtFixedRate"))
    }

    @Test fun `passive render does not invoke rating or mutate study state`() {
        val coordinator = source("kotlin/vn/loi/learning/android/reminder/AndroidQuickReviewWidgetCoordinator.kt")
        val render = coordinator.substringAfter("private fun render(id: Int)").substringBefore("private fun bind(")
        assertFalse(render.contains("submitRating"))
        assertFalse(render.contains("engine.review"))
        assertFalse(render.contains("StudySession"))
        assertTrue(render.contains("manager.updateAppWidget"))
    }

    @Test fun `widget deletion cleans only instance scoped state`() {
        val coordinator = source("kotlin/vn/loi/learning/android/reminder/AndroidQuickReviewWidgetCoordinator.kt")
        assertTrue(coordinator.contains("ids.forEach { remove(key(it)) }"))
        assertTrue(coordinator.contains("quick_review_widget_instances"))
        assertFalse(coordinator.contains("clear()"))
    }

    private fun source(relative: String): String = Files.readString(Path.of("src/main").resolve(relative))
}
