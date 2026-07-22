package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopWindowPlacementTest {
    @Test
    fun `missing state starts centered and saves for restart`() {
        val directory = Files.createTempDirectory("window-placement-restart-test")
        try {
            val file = directory.resolve(DesktopWindowPlacement.FILE_NAME)
            val first = DesktopWindowPlacementSession.open(file)
            assertEquals(DesktopWindowPlacement(), first.initial)

            val saved =
                DesktopWindowPlacement(
                    width = 1440,
                    height = 900,
                    x = 120,
                    y = 80,
                    maximized = true
                )
            assertTrue(first.save(saved))

            val restarted = DesktopWindowPlacementSession.open(file)
            assertEquals(saved, restarted.initial)
            assertFalse(restarted.invalidStatePreserved)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `corrupt state falls back safely and is never overwritten`() {
        val directory = Files.createTempDirectory("window-placement-corrupt-test")
        try {
            val file = directory.resolve(DesktopWindowPlacement.FILE_NAME)
            val bytes = "schema.version=1\nwidth=10\nprivate=do-not-log\n".toByteArray()
            Files.write(file, bytes)

            val session = DesktopWindowPlacementSession.open(file)
            assertEquals(DesktopWindowPlacement(), session.initial)
            assertTrue(session.invalidStatePreserved)
            assertFalse(session.save(DesktopWindowPlacement(width = 1400)))
            assertContentEquals(bytes, Files.readAllBytes(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects unsafe dimensions coordinates and partial positions`() {
        assertFailsWith<IllegalArgumentException> {
            DesktopWindowPlacement(width = 799)
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopWindowPlacement(x = 1, y = null)
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopWindowPlacement(x = 40_000, y = 0)
        }
    }
}
