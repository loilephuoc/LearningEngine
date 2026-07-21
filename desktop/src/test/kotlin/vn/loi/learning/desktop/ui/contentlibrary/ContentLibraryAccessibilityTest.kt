package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentLibraryAccessibilityTest {
    @Test
    fun `header exposes normalized singular and plural counts`() {
        val accessibility =
            resolveContentLibraryHeaderAccessibility(
                libraryCount = 1,
                collectionCount = 2,
                packageCount = 0
            )

        assertEquals(
            "1 library · 2 collections · 0 installed packages",
            accessibility.summary
        )
        assertEquals(
            "Content Library. 1 library. 2 collections. 0 installed packages.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `header clamps negative counts`() {
        val accessibility =
            resolveContentLibraryHeaderAccessibility(
                libraryCount = -1,
                collectionCount = -2,
                packageCount = -3
            )

        assertEquals(
            "0 libraries · 0 collections · 0 installed packages",
            accessibility.summary
        )
    }

    @Test
    fun `import status and error use distinct announcements`() {
        assertEquals(
            "Import status. Package imported successfully.",
            resolveContentLibraryMessageAccessibility(
                message = "Package imported successfully.",
                isError = false
            ).contentDescription
        )

        assertEquals(
            "Import error. Invalid package.",
            resolveContentLibraryMessageAccessibility(
                message = "Invalid package.",
                isError = true
            ).contentDescription
        )
    }

    @Test
    fun `blank import messages receive stable fallbacks`() {
        assertEquals(
            "Import completed.",
            resolveContentLibraryMessageAccessibility(
                message = " ",
                isError = false
            ).message
        )

        assertEquals(
            "An unknown import error occurred.",
            resolveContentLibraryMessageAccessibility(
                message = "",
                isError = true
            ).message
        )
    }

    @Test
    fun `section title has stable semantic wording`() {
        assertEquals(
            "Installed Packages section.",
            resolveContentLibrarySectionContentDescription(
                "Installed Packages"
            )
        )

        assertEquals(
            "Untitled section section.",
            resolveContentLibrarySectionContentDescription(
                " "
            )
        )
    }
}
