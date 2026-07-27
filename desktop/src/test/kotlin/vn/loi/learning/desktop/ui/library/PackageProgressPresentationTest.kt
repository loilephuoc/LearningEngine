package vn.loi.learning.desktop.ui.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertFalse(presentation.toString().contains("Topic ID"))
        assertFalse(presentation.toString().contains("Package ID"))
    }
}
