package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentLibraryActionAccessibilityTest {
    @Test
    fun `global actions explain their result`() {
        assertEquals(
            "Refresh Content Library data.",
            resolveContentLibraryActionAccessibility(
                ContentLibraryAction.Refresh
            ).contentDescription
        )

        assertEquals(
            "Import a package directory into Content Library.",
            resolveContentLibraryActionAccessibility(
                ContentLibraryAction.ImportPackage
            ).contentDescription
        )

        assertEquals(
            "Retry loading Content Library data.",
            resolveContentLibraryActionAccessibility(
                ContentLibraryAction.RetryLoad
            ).contentDescription
        )
    }

    @Test
    fun `library actions include normalized target context`() {
        assertEquals(
            "Open library Biology and browse its lessons.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.OpenLibrary,
                targetName = "  Biology  "
            ).contentDescription
        )

        assertEquals(
            "Create a collection in library Biology.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.CreateCollection,
                targetName = "Biology"
            ).contentDescription
        )
    }

    @Test
    fun `collection actions include target and destructive scope`() {
        assertEquals(
            "Attach an installed package to collection Core.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.AttachPackage,
                targetName = "Core"
            ).contentDescription
        )

        assertEquals(
            "Rename collection Core.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.RenameCollection,
                targetName = "Core"
            ).contentDescription
        )

        assertEquals(
            "Delete collection Core. This action requires confirmation.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.DeleteCollection,
                targetName = "Core"
            ).contentDescription
        )
    }

    @Test
    fun `detach action announces package and confirmation boundary`() {
        assertEquals(
            "Detach package Vocabulary from its collection. This action requires confirmation.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.DetachPackage,
                targetName = "Vocabulary"
            ).contentDescription
        )
    }

    @Test
    fun `blank targets receive stable fallbacks`() {
        assertEquals(
            "Open library Unnamed library and browse its lessons.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.OpenLibrary,
                targetName = " "
            ).contentDescription
        )

        assertEquals(
            "Delete collection Unnamed collection. This action requires confirmation.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.DeleteCollection
            ).contentDescription
        )

        assertEquals(
            "Detach package Unnamed package from its collection. This action requires confirmation.",
            resolveContentLibraryActionAccessibility(
                action = ContentLibraryAction.DetachPackage,
                targetName = ""
            ).contentDescription
        )
    }
}
