package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class BundlePackageContentImporterTest {

    @Test
    fun `malformed required JSON reports exact entry and preserves parser message`() {
        val validBundle = packageBundle()

        PackageImportBundle.REQUIRED_FILES.forEach { entryName ->
            val exception =
                assertFailsWith<InvalidPackageJsonException> {
                    BundlePackageContentImporter().importContent(
                        PackageImportBundle(
                            validBundle.files +
                                (entryName to "{")
                        )
                    )
                }

            assertEquals(entryName, exception.entryName)
            assertEquals(
                exception.cause?.message,
                exception.message
            )
        }
    }

    @Test
    fun `imports exported contents and learning items`() {
        val content =
            testContent()

        val learningItem =
            testLearningItem(
                content
            )

        val bundle =
            packageBundle(
                contents =
                    listOf(
                        content
                    ),
                learningItems =
                    listOf(
                        learningItem
                    )
            )

        val imported =
            BundlePackageContentImporter()
                .importContent(
                    bundle
                )

        assertEquals(
            listOf(
                content
            ),
            imported.contents
        )

        assertEquals(
            listOf(
                learningItem
            ),
            imported.learningItems
        )

        assertEquals(
            1,
            imported.libraries.size
        )

        val library =
            imported.libraries.single()

        assertEquals(
            "test",
            library.name
        )

        assertEquals(
            setOf(
                content.id
            ),
            library.contentIds
        )

        assertEquals(
            emptyList(),
            imported.warnings
        )
    }

    @Test
    fun `accepts backward compatible manifest without schema version`() {
        val bundle =
            packageBundle(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
            )

        val imported =
            BundlePackageContentImporter()
                .importContent(
                    bundle
                )

        assertEquals(
            emptyList(),
            imported.contents
        )

        assertEquals(
            emptyList(),
            imported.learningItems
        )
    }

    @Test
    fun `accepts opd3 format without case sensitivity`() {
        val bundle =
            packageBundle(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "opd3",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
            )

        val imported =
            BundlePackageContentImporter()
                .importContent(
                    bundle
                )

        assertEquals(
            emptyList(),
            imported.contents
        )
    }

    @Test
    fun `accepts legacy metadata when only name is present`() {
        val imported =
            BundlePackageContentImporter()
                .importContent(
                    packageBundle(
                        metadata =
                            """
                            {
                              "name": "test"
                            }
                            """.trimIndent()
                    )
                )

        assertEquals(
            emptyList(),
            imported.contents
        )
    }

    @Test
    fun `rejects metadata name that differs from manifest`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                BundlePackageContentImporter()
                    .importContent(
                        packageBundle(
                            metadata =
                                """
                                {
                                  "name": "other-package",
                                  "version": "1.0",
                                  "format": "OPD3"
                                }
                                """.trimIndent()
                        )
                    )
            }

        assertEquals(
            "Package metadata name 'other-package' does not match manifest name 'test'.",
            exception.message
        )
    }

    @Test
    fun `rejects metadata version that differs from manifest`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                BundlePackageContentImporter()
                    .importContent(
                        packageBundle(
                            metadata =
                                """
                                {
                                  "name": "test",
                                  "version": "2.0",
                                  "format": "OPD3"
                                }
                                """.trimIndent()
                        )
                    )
            }

        assertEquals(
            "Package metadata version '2.0' does not match manifest version '1.0'.",
            exception.message
        )
    }

    @Test
    fun `accepts metadata format with different casing`() {
        val imported =
            BundlePackageContentImporter()
                .importContent(
                    packageBundle(
                        metadata =
                            """
                            {
                              "name": "test",
                              "version": "1.0",
                              "format": "opd3"
                            }
                            """.trimIndent()
                    )
                )

        assertEquals(
            emptyList(),
            imported.contents
        )
    }

    @Test
    fun `rejects blank manifest name`() {
        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": " ",
                      "version": "1.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Package manifest name must not be blank.",
            exception.message
        )
    }

    @Test
    fun `rejects blank manifest version`() {
        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": " ",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Package manifest version must not be blank.",
            exception.message
        )
    }

    @Test
    fun `rejects unsupported manifest format`() {
        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "ZIP",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Unsupported package manifest format: ZIP.",
            exception.message
        )
    }

    @Test
    fun `rejects non-positive manifest schema version`() {
        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "schemaVersion": 0,
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Package manifest schema version must be positive.",
            exception.message
        )
    }

    @Test
    fun `rejects manifest schema newer than engine`() {
        val unsupportedSchemaVersion =
            PackageDescriptor.CURRENT_SCHEMA_VERSION +
                    1

        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "schemaVersion": $unsupportedSchemaVersion,
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Unsupported package manifest schema version: $unsupportedSchemaVersion. Current engine schema version is ${PackageDescriptor.CURRENT_SCHEMA_VERSION}.",
            exception.message
        )
    }

    @Test
    fun `rejects negative content count`() {
        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "contentCount": -1,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Package manifest content count must not be negative.",
            exception.message
        )
    }

    @Test
    fun `rejects negative learning item count`() {
        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": -1
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Package manifest learning item count must not be negative.",
            exception.message
        )
    }

    @Test
    fun `rejects duplicate manifest dependencies`() {
        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0,
                      "dependencies": [
                        {
                          "packageName": "shared"
                        },
                        {
                          "packageName": "shared",
                          "minimumVersion": "2.0"
                        }
                      ]
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Package manifest dependencies must have unique package names: shared.",
            exception.message
        )
    }

    @Test
    fun `rejects self dependency in manifest`() {
        val exception =
            assertManifestFailure(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0,
                      "dependencies": [
                        {
                          "packageName": "test"
                        }
                      ]
                    }
                    """.trimIndent()
            )

        assertEquals(
            "Package manifest must not depend on itself.",
            exception.message
        )
    }

    @Test
    fun `rejects manifest content count mismatch`() {
        val content =
            testContent()

        val bundle =
            packageBundle(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent(),
                contents =
                    listOf(
                        content
                    )
            )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                BundlePackageContentImporter()
                    .importContent(
                        bundle
                    )
            }

        assertEquals(
            "Manifest content count 0 does not match imported content count 1.",
            exception.message
        )
    }

    @Test
    fun `rejects manifest learning item count mismatch`() {
        val content =
            testContent()

        val learningItem =
            testLearningItem(
                content
            )

        val bundle =
            packageBundle(
                manifest =
                    """
                    {
                      "name": "test",
                      "version": "1.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent(),
                learningItems =
                    listOf(
                        learningItem
                    )
            )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                BundlePackageContentImporter()
                    .importContent(
                        bundle
                    )
            }

        assertEquals(
            "Manifest learning item count 0 does not match imported learning item count 1.",
            exception.message
        )
    }

    private fun assertManifestFailure(
        manifest: String
    ): IllegalArgumentException =
        assertFailsWith {
            BundlePackageContentImporter()
                .importContent(
                    packageBundle(
                        manifest =
                            manifest
                    )
                )
        }

    private fun packageBundle(
        manifest: String? =
            null,
        metadata: String =
            """
            {
              "name": "test",
              "version": "1.0",
              "format": "OPD3"
            }
            """.trimIndent(),
        contents: List<Content> =
            emptyList(),
        learningItems: List<LearningItem> =
            emptyList()
    ): PackageImportBundle {
        val resolvedManifest =
            manifest
                ?: """
               {
                 "name": "test",
                 "version": "1.0",
                 "format": "OPD3",
                 "schemaVersion": ${PackageDescriptor.CURRENT_SCHEMA_VERSION},
                 "contentCount": ${contents.size},
                 "learningItemCount": ${learningItems.size}
               }
               """.trimIndent()

        return PackageImportBundle(
            files =
                mapOf(
                    PackageImportBundle.METADATA_FILE to
                            metadata,
                    PackageImportBundle.CONTENTS_FILE to
                            PackageExportContentsSerializer()
                                .serialize(
                                    contents
                                ),
                    PackageImportBundle.LEARNING_ITEMS_FILE to
                            PackageExportLearningItemsSerializer()
                                .serialize(
                                    learningItems
                                ),
                    PackageImportBundle.MANIFEST_FILE to
                            resolvedManifest
                )
        )
    }

    private fun testContent(): Content =
        Content(
            id =
                ContentId(
                    "content-1"
                ),
            type =
                ContentType.WORD,
            text =
                ContentText(
                    primaryText =
                        "apple",
                    translatedText =
                        "quả táo",
                    pronunciation =
                        "/ˈæp.əl/"
                ),
            media =
                ContentMedia(
                    primaryAudio =
                        "audio/apple.mp3",
                    image =
                        "images/apple.png"
                ),
            metadata =
                ContentMetadata(
                    title =
                        "Apple",
                    group =
                        "Vocabulary",
                    section =
                        "Food",
                    lesson =
                        "Lesson 1",
                    tags =
                        setOf(
                            "fruit"
                        ),
                    source =
                        "test"
                )
        )

    private fun testLearningItem(
        content: Content
    ): LearningItem =
        LearningItem(
            id =
                LearningItemId(
                    "learning-item-1"
                ),
            contentId =
                content.id,
            mode =
                LearningMode.MEANING_RECOGNITION,
            isEnabled =
                true
        )
}
