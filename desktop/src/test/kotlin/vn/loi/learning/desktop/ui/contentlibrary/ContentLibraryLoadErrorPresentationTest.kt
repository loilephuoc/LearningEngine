package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ContentLibraryLoadErrorPresentationTest {
    @Test
    fun `load error preserves actionable repository detail`() {
        val presentation =
            resolveContentLibraryLoadErrorPresentation(
                "Content registry is unreadable."
            )

        assertEquals(
            "Content Library unavailable",
            presentation.title
        )
        assertEquals(
            "Content registry is unreadable.",
            presentation.message
        )
        assertEquals(
            "Retry",
            presentation.actionLabel
        )
        assertContains(
            presentation.guidance,
            "local content data"
        )
    }

    @Test
    fun `blank load error receives stable fallback wording`() {
        val presentation =
            resolveContentLibraryLoadErrorPresentation(
                "   "
            )

        assertEquals(
            "The content library could not be loaded.",
            presentation.message
        )
        assertContains(
            presentation.contentDescription,
            "then retry loading the library"
        )
    }

    @Test
    fun `semantic description announces error and recovery path`() {
        val presentation =
            resolveContentLibraryLoadErrorPresentation(
                "Package metadata is incompatible."
            )

        assertContains(
            presentation.contentDescription,
            "Content Library unavailable"
        )
        assertContains(
            presentation.contentDescription,
            "Package metadata is incompatible"
        )
        assertContains(
            presentation.contentDescription,
            "retry loading"
        )
    }
}
