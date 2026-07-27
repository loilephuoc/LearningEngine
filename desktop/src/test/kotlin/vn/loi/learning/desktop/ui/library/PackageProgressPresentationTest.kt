package vn.loi.learning.desktop.ui.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.packageprogress.PackageLearningProgress
import vn.loi.learning.domain.library.model.InstalledPackageId

class PackageProgressPresentationTest {
    @Test
    fun `projects package progress without conflating unseen new and started stages`() {
        val presentation = PackageLearningProgress(
            installedPackageId = InstalledPackageId("installed"),
            totalLessonCount = 990,
            totalLearningItemCount = 4_950,
            unseenItemCount = 4_000,
            newStateItemCount = 500,
            startedItemCount = 450,
            masteredItemCount = 100,
            dueItemCount = 75,
            suspendedItemCount = 25,
            completionPercent = 2,
            startedPercent = 9,
            lessons = emptyList()
        ).toPresentation()

        assertEquals(4_000, presentation.unseenItemCount)
        assertEquals(500, presentation.newStateItemCount)
        assertEquals(450, presentation.startedItemCount)
        assertEquals(100, presentation.masteredItemCount)
        assertEquals(75, presentation.dueItemCount)
        assertEquals(25, presentation.suspendedItemCount)
        assertEquals(2, presentation.completionPercent)
        assertEquals(9, presentation.startedPercent)
        assertEquals(350, presentation.learningItemCount)
        assertEquals(
            listOf("Tổng từ", "Chưa học", "Đang học", "Cần ôn hôm nay", "Đã học", "Đã thành thạo"),
            packageMetrics(presentation).map { it.label }
        )
        assertEquals(listOf(4_950, 4_000, 350, 75, 450, 100), packageMetrics(presentation).map { it.value })
        assertTrue(presentation.masteredItemCount <= presentation.startedItemCount)
        assertEquals("Tạm ngưng 25", suspendedProgressLabel(presentation))
        assertFalse(presentation.toString().contains("Topic ID"))
        assertFalse(presentation.toString().contains("Package ID"))
    }

    @Test
    fun `zero and unavailable presentation never fabricate progress or rating data`() {
        val zero = PackageLearningProgress.empty(InstalledPackageId("empty")).toPresentation()

        assertEquals(listOf(0, 0, 0, 0, 0, 0), packageMetrics(zero).map { it.value })
        assertNull(suspendedProgressLabel(zero))
        assertEquals("0%", formatVietnameseProgress(0, 4_950))
        assertEquals("Chưa có dữ liệu đánh giá", PACKAGE_RATING_UNAVAILABLE_LABEL)
        assertEquals(PackageProgressPresentation.Unavailable, PackageProgressPresentation.Unavailable)
    }

    @Test
    fun `Vietnamese count and progress formatting preserves small non-zero ratios`() {
        assertEquals("4.950", formatVietnameseCount(4_950))
        assertEquals("0,5%", formatVietnameseProgress(23, 4_950))
        assertEquals("0%", formatVietnameseProgress(0, 4_950))
        assertEquals("100%", formatVietnameseProgress(4_950, 4_950))
    }
}
