package vn.loi.learning.adapter.jvm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainPackageImportRoutingTest {

    @Test
    fun `package import command name remains stable`() {
        assertEquals(
            "package-import",
            PackageImportCommand.NAME
        )
    }

    @Test
    fun `usage starts with command name`() {
        assertTrue(
            PackageImportCommand
                .usage()
                .startsWith(
                    "Usage: package-import"
                )
        )
    }

    @Test
    fun `usage documents persistence directory`() {
        assertTrue(
            PackageImportCommand
                .usage()
                .contains(
                    "<persistence-directory>"
                )
        )
    }

    @Test
    fun `usage documents package directory`() {
        assertTrue(
            PackageImportCommand
                .usage()
                .contains(
                    "<package-directory>"
                )
        )
    }

    @Test
    fun `usage documents catalog id`() {
        assertTrue(
            PackageImportCommand
                .usage()
                .contains(
                    "<catalog-id>"
                )
        )
    }
}