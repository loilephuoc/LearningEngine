package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopSampleContentInstallerIntegrationTest {
    @Test
    fun `starter sample imports through production OPD3 persistence and survives restart`() {
        val directory = Files.createTempDirectory("desktop-sample-content-test")
        try {
            val app1 = LearningApplicationFactory.createPersisted(directory)
            DesktopSampleContentInstaller.install(app1)
            app1.close()
            val restarted = LearningApplicationFactory.createPersisted(directory)
            try {
                assertEquals(1, restarted.installedPackages.query().size)
                assertEquals("Learning Engine Starter", restarted.installedPackages.query().single().name)
                assertEquals(2, restarted.contentLibraries.query().single().learningItemCount)
                assertEquals(true, restarted.learningItemRepository?.findAll()?.any { it.id.value == "starter-item-spacing" })
            } finally {
                restarted.close()
            }
        } finally { directory.toFile().deleteRecursively() }
    }
}
