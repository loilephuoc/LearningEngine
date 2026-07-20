package vn.loi.learning.adapter.jvm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PackageImportCommandTest {

    @Test
    fun `command name is stable`() {
        assertEquals(
            "package-import",
            PackageImportCommand.NAME
        )
    }

    @Test
    fun `rejects missing arguments with usage message`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PackageImportCommand.execute(
                    emptyList()
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                PackageImportCommand.usage()
            )
        )
    }

    @Test
    fun `rejects extra arguments with usage message`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PackageImportCommand.execute(
                    listOf(
                        "persistence",
                        "packages",
                        "catalog",
                        "extra"
                    )
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                PackageImportCommand.usage()
            )
        )
    }

    @Test
    fun `rejects blank catalog id`() {
        assertFailsWith<IllegalArgumentException> {
            PackageImportCommand.execute(
                listOf(
                    "persistence",
                    "packages",
                    " "
                )
            )
        }
    }

    @Test
    fun `usage describes all required arguments`() {
        val usage =
            PackageImportCommand.usage()

        assertTrue(
            usage.contains(
                "<persistence-directory>"
            )
        )

        assertTrue(
            usage.contains(
                "<package-directory>"
            )
        )

        assertTrue(
            usage.contains(
                "<catalog-id>"
            )
        )
    }
}