package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentLibraryDialogAccessibilityTest {
    @Test
    fun `create dialog announces target library and input purpose`() {
        val accessibility =
            resolveCreateCollectionDialogAccessibility(
                "Languages"
            )

        assertEquals(
            "Create Collection dialog. Library: Languages. Enter a collection name.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `rename and delete dialogs normalize blank collection names`() {
        assertEquals(
            "Rename Collection dialog. Current name: Unnamed collection. Enter a new collection name.",
            resolveRenameCollectionDialogAccessibility(
                " "
            ).contentDescription
        )

        assertEquals(
            "Delete Collection dialog. Collection: Unnamed collection. This action cannot be undone.",
            resolveDeleteCollectionDialogAccessibility(
                ""
            ).contentDescription
        )
    }

    @Test
    fun `detach dialog identifies package collection and non destructive scope`() {
        val accessibility =
            resolveDetachPackageDialogAccessibility(
                packageName = "Physics Pack",
                collectionName = "Mechanics"
            )

        assertEquals(
            "Detach Package dialog. Package: Physics Pack. Collection: Mechanics. The installed package will not be deleted.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `attach dialog pluralizes and clamps available package count`() {
        assertEquals(
            "Attach Package dialog. Collection: Core. 1 package available. Select one package.",
            resolveAttachPackageDialogAccessibility(
                collectionName = "Core",
                availablePackageCount = 1
            ).contentDescription
        )

        assertEquals(
            "Attach Package dialog. Collection: Unnamed collection. 0 packages available. Select one package.",
            resolveAttachPackageDialogAccessibility(
                collectionName = " ",
                availablePackageCount = -2
            ).contentDescription
        )
    }

    @Test
    fun `attach package option exposes selected state without relying on checkmark`() {
        val selected =
            resolveAttachPackageOptionAccessibility(
                packageName = "Biology",
                selected = true
            )

        val unselected =
            resolveAttachPackageOptionAccessibility(
                packageName = " ",
                selected = false
            )

        assertTrue(selected.selected)
        assertEquals(
            "Biology. Selected package.",
            selected.contentDescription
        )

        assertFalse(unselected.selected)
        assertEquals(
            "Unnamed package. Package option.",
            unselected.contentDescription
        )
    }
}
