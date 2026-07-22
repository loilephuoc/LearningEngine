package vn.loi.learning.desktop.runtime

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopRuntimeDirectoryResolverTest {
    @Test
    fun `resolves Windows directories from local app data`() {
        val directories =
            resolve(
                operatingSystem = DesktopOperatingSystem.WINDOWS,
                environment = mapOf("LOCALAPPDATA" to "C:\\Users\\learner\\AppData\\Local")
            )

        val root = Path.of("C:\\Users\\learner\\AppData\\Local\\LearningEngine")
        assertEquals(root.resolve("data"), directories.data)
        assertEquals(root.resolve("config"), directories.config)
        assertEquals(root.resolve("cache"), directories.cache)
        assertEquals(root.resolve("logs"), directories.logs)
        assertEquals(Path.of("C:\\Temp\\LearningEngine"), directories.temp)
        assertFalse(directories.legacyDataInUse)
    }

    @Test
    fun `resolves Linux XDG directories independently`() {
        val directories =
            resolve(
                operatingSystem = DesktopOperatingSystem.LINUX,
                environment =
                    mapOf(
                        "XDG_DATA_HOME" to "C:\\xdg\\data",
                        "XDG_CONFIG_HOME" to "C:\\xdg\\config",
                        "XDG_CACHE_HOME" to "C:\\xdg\\cache",
                        "XDG_STATE_HOME" to "C:\\xdg\\state"
                    )
            )

        assertEquals(Path.of("C:\\xdg\\data\\LearningEngine"), directories.data)
        assertEquals(Path.of("C:\\xdg\\config\\LearningEngine"), directories.config)
        assertEquals(Path.of("C:\\xdg\\cache\\LearningEngine"), directories.cache)
        assertEquals(Path.of("C:\\xdg\\state\\LearningEngine\\logs"), directories.logs)
        assertEquals(Path.of("C:\\Temp\\LearningEngine"), directories.temp)
    }

    @Test
    fun `resolves macOS directories from user library`() {
        val directories =
            resolve(
                operatingSystem = DesktopOperatingSystem.MACOS
            )

        assertEquals(
            Path.of("C:\\Users\\learner\\Library\\Application Support\\LearningEngine\\data"),
            directories.data
        )
        assertEquals(
            Path.of("C:\\Users\\learner\\Library\\Caches\\LearningEngine"),
            directories.cache
        )
        assertEquals(
            Path.of("C:\\Users\\learner\\Library\\Logs\\LearningEngine"),
            directories.logs
        )
    }

    @Test
    fun `keeps existing legacy data in place without migrating it`() {
        val directories =
            resolve(
                operatingSystem = DesktopOperatingSystem.LINUX,
                legacyDataExists = true
            )

        assertEquals(
            Path.of("C:\\Users\\learner\\.learning-engine\\data"),
            directories.data
        )
        assertTrue(directories.legacyDataInUse)
        assertEquals(
            Path.of("C:\\Users\\learner\\.config\\LearningEngine"),
            directories.config
        )
    }

    @Test
    fun `detects supported operating system families`() {
        assertEquals(
            DesktopOperatingSystem.WINDOWS,
            DesktopRuntimeDirectoryResolver.detectOperatingSystem("Windows 11")
        )
        assertEquals(
            DesktopOperatingSystem.MACOS,
            DesktopRuntimeDirectoryResolver.detectOperatingSystem("Mac OS X")
        )
        assertEquals(
            DesktopOperatingSystem.LINUX,
            DesktopRuntimeDirectoryResolver.detectOperatingSystem("Linux")
        )
    }

    private fun resolve(
        operatingSystem: DesktopOperatingSystem,
        environment: Map<String, String> = emptyMap(),
        legacyDataExists: Boolean = false
    ): DesktopRuntimeDirectories =
        DesktopRuntimeDirectoryResolver.resolve(
            operatingSystem = operatingSystem,
            userHome = Path.of("C:\\Users\\learner"),
            temporaryRoot = Path.of("C:\\Temp"),
            environment = environment,
            legacyDataExists = legacyDataExists
        )
}
