package vn.loi.learning.infrastructure.persistence

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

class PersistedLearningEngineFactoryIsolationTest {

    @Test
    fun `creates independent persistence files for different directories`() {

        val firstDirectory =
            Files.createTempDirectory(
                "engine-a"
            )

        val secondDirectory =
            Files.createTempDirectory(
                "engine-b"
            )

        try {
            PersistedLearningEngineFactory.create(
                persistenceDirectory = firstDirectory,
                contentRepository =
                    InMemoryContentRepository(),
                learningItemRepository =
                    InMemoryLearningItemRepository()
            )

            PersistedLearningEngineFactory.create(
                persistenceDirectory = secondDirectory,
                contentRepository =
                    InMemoryContentRepository(),
                learningItemRepository =
                    InMemoryLearningItemRepository()
            )

            assertNotEquals(
                firstDirectory,
                secondDirectory
            )

            assertTrue(
                Files.exists(firstDirectory)
            )

            assertTrue(
                Files.exists(secondDirectory)
            )
        } finally {
            firstDirectory
                .toFile()
                .deleteRecursively()

            secondDirectory
                .toFile()
                .deleteRecursively()
        }
    }
}