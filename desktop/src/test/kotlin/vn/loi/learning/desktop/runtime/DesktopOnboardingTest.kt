package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopOnboardingTest {
    @Test
    fun `empty first run requires onboarding and completion survives restart`() {
        val root = Files.createTempDirectory("onboarding-first-run-test")
        try {
            val data = Files.createDirectories(root.resolve("data"))
            val config = Files.createDirectories(root.resolve("config"))
            val first = DesktopOnboardingSession.open(data, config)
            assertEquals(DesktopOnboardingState.REQUIRED, first.initial)
            first.complete()
            assertEquals(DesktopOnboardingState.COMPLETED, DesktopOnboardingSession.open(data, config).initial)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `existing user data skips onboarding without writing marker`() {
        val root = Files.createTempDirectory("onboarding-existing-data-test")
        try {
            val data = Files.createDirectories(root.resolve("data"))
            val config = Files.createDirectories(root.resolve("config"))
            Files.writeString(data.resolve("contents.json"), "[]")
            assertEquals(DesktopOnboardingState.NOT_REQUIRED, DesktopOnboardingSession.open(data, config).initial)
            assertFalse(Files.exists(config.resolve(DesktopOnboardingSession.FILE_NAME)))
        } finally { root.toFile().deleteRecursively() }
    }
}
