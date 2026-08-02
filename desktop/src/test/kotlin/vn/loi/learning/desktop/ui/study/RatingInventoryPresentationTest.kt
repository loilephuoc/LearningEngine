package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.session.RatingInventory

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
}
