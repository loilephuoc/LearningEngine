package vn.loi.learning.adapter.jvm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageImportCliSmokeTest {

    @Test
    fun `command constant matches cli usage`() {
        assertTrue(
            PackageImportCommand
                .usage()
                .contains(
                    PackageImportCommand.NAME
                )
        )
    }

    @Test
    fun `usage contains exactly three placeholders`() {
        val usage =
            PackageImportCommand.usage()

        val placeholderCount =
            "<".toRegex()
                .findAll(usage)
                .count()

        assertEquals(
            3,
            placeholderCount
        )
    }

    @Test
    fun `usage ends with catalog placeholder`() {
        assertTrue(
            PackageImportCommand
                .usage()
                .trim()
                .endsWith(
                    "<catalog-id>"
                )
        )
    }
}