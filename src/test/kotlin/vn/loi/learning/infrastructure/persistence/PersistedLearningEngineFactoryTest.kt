package vn.loi.learning.infrastructure.persistence

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

class PersistedLearningEngineFactoryTest {

    @Test
    fun `creates persisted learning engine`() {
        val directory =
            Files.createTempDirectory(
                "persisted-learning-engine-factory-test"
            )

        try {
            val engine =
                PersistedLearningEngineFactory.create(
                    persistenceDirectory = directory,
                    contentRepository =
                        InMemoryContentRepository(),
                    learningItemRepository =
                        InMemoryLearningItemRepository()
                )

            assertNotNull(engine)
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }
}