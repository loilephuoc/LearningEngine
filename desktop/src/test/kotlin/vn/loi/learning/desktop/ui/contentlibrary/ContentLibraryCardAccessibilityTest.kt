package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentLibraryCardAccessibilityTest {
    @Test
    fun `library card exposes normalized counts`() {
        val accessibility =
            resolveLibraryCardAccessibility(
                ContentLibraryItem(
                    id = "library-1",
                    name = "Biology",
                    contentCount = 2,
                    learningItemCount = 1,
                    collections = emptyList()
                )
            )

        assertEquals(
            "Biology. 2 contents. 1 learning item. 0 collections.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `library card clamps negative counts and falls back for blank name`() {
        val accessibility =
            resolveLibraryCardAccessibility(
                ContentLibraryItem(
                    id = "library-2",
                    name = " ",
                    contentCount = -2,
                    learningItemCount = -1,
                    collections = emptyList()
                )
            )

        assertEquals(
            "Unnamed library. 0 contents. 0 learning items. 0 collections.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `collection card distinguishes empty and populated attachment states`() {
        val empty =
            resolveCollectionCardAccessibility(
                ContentLibraryCollectionItem(
                    id = "collection-1",
                    libraryId = "library-1",
                    name = "Core",
                    attachedPackages = emptyList()
                )
            )

        val populated =
            resolveCollectionCardAccessibility(
                ContentLibraryCollectionItem(
                    id = "collection-2",
                    libraryId = "library-1",
                    name = "Advanced",
                    attachedPackages =
                        listOf(
                            ContentLibraryAttachedPackageItem(
                                id = "package-1",
                                name = "Package",
                                version = "1.0",
                                format = "OPD3"
                            )
                        )
                )
            )

        assertEquals(
            "Core. No packages attached.",
            empty.contentDescription
        )
        assertEquals(
            "Advanced. 1 package attached.",
            populated.contentDescription
        )
    }

    @Test
    fun `attached package omits blank optional metadata`() {
        val accessibility =
            resolveAttachedPackageCardAccessibility(
                ContentLibraryAttachedPackageItem(
                    id = "package-1",
                    name = " ",
                    version = "",
                    format = " "
                )
            )

        assertEquals(
            "Unnamed package. Attached package.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `installed package exposes version format and library count`() {
        val accessibility =
            resolveInstalledPackageCardAccessibility(
                ContentLibraryPackageItem(
                    id = "package-2",
                    name = "Medical Physics",
                    version = "2.1",
                    format = "OPD3",
                    libraryCount = 2
                )
            )

        assertEquals(
            "Medical Physics. Version 2.1. Format OPD3. 2 libraries.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank installed package metadata receives stable fallbacks`() {
        val accessibility =
            resolveInstalledPackageCardAccessibility(
                ContentLibraryPackageItem(
                    id = "package-3",
                    name = "",
                    version = " ",
                    format = "",
                    libraryCount = -1
                )
            )

        assertEquals(
            "Unnamed package. Version Unavailable. Format Unavailable. 0 libraries.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `property is exposed as one label and value unit`() {
        val accessibility =
            resolveContentLibraryPropertyAccessibility(
                label = "Package ID",
                value = "pkg-42"
            )

        assertEquals(
            "Package ID: pkg-42.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank property receives stable fallbacks`() {
        val accessibility =
            resolveContentLibraryPropertyAccessibility(
                label = " ",
                value = ""
            )

        assertEquals(
            "Property",
            accessibility.label
        )
        assertEquals(
            "Unavailable",
            accessibility.value
        )
    }
}
