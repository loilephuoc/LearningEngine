package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ContentLibraryEmptyPresentationTest {
    @Test
    fun `empty content library exposes an actionable first import`() {
        val presentation =
            resolveContentLibraryEmptyPresentation()

        assertEquals(
            "No content libraries",
            presentation.title
        )
        assertEquals(
            "Import First Package",
            presentation.actionLabel
        )
        assertContains(
            presentation.description,
            ".opd3"
        )
        assertContains(
            presentation.description,
            ".pkg"
        )
    }

    @Test
    fun `screen reader description explains state and recovery action`() {
        val presentation =
            resolveContentLibraryEmptyPresentation()

        assertContains(
            presentation.contentDescription,
            "Content Library is empty"
        )
        assertContains(
            presentation.contentDescription,
            "create your first content library"
        )
    }
}
