package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.LegacyTopicPairDiscoveryService

class LegacyTopicPairDiscoveryIntegrationTest {

    @Test
    fun `folder discovery returns one validated legacy topic pair`() {
        val directory =
            Files.createTempDirectory(
                "legacy-topic-discovery"
            )

        try {
            val jsonFile =
                directory.resolve(
                    "Vocabulary.json"
                )
            val packageFile =
                directory.resolve(
                    "Vocabulary.pkg"
                )

            Files.writeString(
                jsonFile,
                "[]"
            )
            Files.write(
                packageFile,
                "OPD3".toByteArray(
                    Charsets.US_ASCII
                )
            )

            val result =
                LegacyTopicPairDiscoveryService(
                    JvmLegacyTopicFolderReader()
                ).discover(
                    directory.toString()
                )

            assertTrue(result.isValid)
            assertEquals(
                emptyList(),
                result.diagnostics
            )
            assertEquals(1, result.pairs.size)
            assertEquals(
                "Vocabulary",
                result.pairs.single().logicalTopicName
            )
            assertEquals(
                jsonFile.toString(),
                result.pairs.single().jsonSource
            )
            assertEquals(
                packageFile.toString(),
                result.pairs.single().packageSource
            )
        } finally {
            Files.walk(
                directory
            ).use { paths ->
                paths
                    .sorted(
                        Comparator.reverseOrder()
                    )
                    .forEach(
                        Files::deleteIfExists
                    )
            }
        }
    }
}
