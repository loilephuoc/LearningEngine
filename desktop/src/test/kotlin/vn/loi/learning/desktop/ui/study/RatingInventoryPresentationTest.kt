package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.session.RatingInventory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RatingInventoryPresentationTest {
    @Test
    fun `presentation maps shared inventory without recounting`() {
        val inventory = RatingInventory(1, 2, 3, 4, 5, 15)

        val presentation = RatingInventoryPresentationResolver.resolve(inventory)

        assertEquals(listOf(1, 2, 3, 4, 5, 15), presentation.items.map { it.count })
        assertEquals(RatingInventoryKind.entries, presentation.items.map { it.kind })
        assertEquals(
            RatingInventoryColorRole.entries,
            presentation.items.map { it.colorRole }
        )
        assertEquals(15, presentation.total)
    }

    @Test
    fun `inventory panel has six direct items and no duplicate visual header or collapse state`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt")
        ).substringAfter("private fun RatingInventoryPanel(")
            .substringBefore("private fun resolveRatingInventoryColor")

        assertFalse(source.contains("Rating inventory"))
        assertFalse(source.contains("expanded"))
        assertFalse(source.contains("clickable"))
        assertTrue(source.contains("Thống kê đánh giá hiện tại"))
        assertTrue(source.contains("presentation.items"))
        assertTrue(source.contains("chunked(3)"))
    }
}
