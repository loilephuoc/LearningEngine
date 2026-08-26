package vn.loi.learning.android.platform

import java.io.File
import java.nio.file.Files
import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.sqlite.LearningEngineDatabase
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteDatabaseFactory
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteDriverFactory

class AndroidSqliteDriverArchitectureTest {

    @Test
    fun `Android runtime construction uses custom SqliteDriverFactory and avoids JdbcSqliteDriver`() {
        val tempDir = Files.createTempDirectory("android-driver-test")
        try {
            var driverCreated = false
            val mockAndroidFactory = SqliteDriverFactory { file ->
                driverCreated = true
                vn.loi.learning.infrastructure.persistence.sqlite.JvmSqliteDriverFactory.createDriver(file)
            }

            SqliteDatabaseFactory.defaultDriverFactory = mockAndroidFactory

            val context = LearningApplicationFactory.createPersisted(
                persistenceDirectory = tempDir,
                reconcilePartOfSpeechRegistryOnCreate = false,
                sqliteDriverFactory = mockAndroidFactory
            )

            assertTrue("Expected custom platform driver factory to be invoked", driverCreated)
            assertNotNull(context.contentLibraryRepository)
            assertNotNull(context.contentRepository)
            assertNotNull(context.learningItemRepository)
            assertNotNull(context.memoryStateRepository)
        } finally {
            SqliteDatabaseFactory.defaultDriverFactory = null
            tempDir.toFile().deleteRecursively()
        }
    }
}
