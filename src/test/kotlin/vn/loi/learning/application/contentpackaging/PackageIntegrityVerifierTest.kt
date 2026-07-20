package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PackageIntegrityVerifierTest {

    private val hasher =
        Sha256PackageIntegrityHasher()

    private val verifier =
        PackageIntegrityVerifier(
            hasher
        )

    @Test
    fun `accepts matching file hashes`() {
        val bundle =
            createBundle(
                metadata =
                    """{"name":"Demo"}"""
            )

        verifier.verify(
            bundle =
                bundle,
            manifest =
                createManifest(
                    bundle
                )
        )
    }

    @Test
    fun `rejects modified package file`() {
        val originalBundle =
            createBundle(
                metadata =
                    """{"name":"Demo"}"""
            )

        val manifest =
            createManifest(
                originalBundle
            )

        val modifiedBundle =
            createBundle(
                metadata =
                    """{"name":"Modified"}"""
            )

        val exception =
            assertFailsWith<InvalidPackageIntegrityException> {
                verifier.verify(
                    bundle =
                        modifiedBundle,
                    manifest =
                        manifest
                )
            }

        assertEquals(
            PackageImportBundle.METADATA_FILE,
            exception.relativePath
        )
    }

    private fun createBundle(
        metadata: String
    ): PackageImportBundle =
        PackageImportBundle(
            files =
                mapOf(
                    PackageImportBundle.METADATA_FILE to
                            metadata,
                    PackageImportBundle.CONTENTS_FILE to
                            """{"contents":[]}""",
                    PackageImportBundle.LEARNING_ITEMS_FILE to
                            """{"learningItems":[]}""",
                    PackageImportBundle.MANIFEST_FILE to
                            """{"name":"Demo","version":"1.0.0","format":"OPD3","contentCount":0,"learningItemCount":0}"""
                )
        )

    private fun createManifest(
        bundle: PackageImportBundle
    ): PackageExportManifestJson =
        PackageExportManifestJson(
            name =
                "Demo",
            version =
                "1.0.0",
            format =
                "OPD3",
            contentCount =
                0,
            learningItemCount =
                0,
            hashAlgorithm =
                PackageExportManifestFactory.HASH_ALGORITHM,
            fileHashes =
                bundle.files
                    .filterKeys { relativePath ->
                        relativePath !=
                                PackageImportBundle.MANIFEST_FILE
                    }
                    .mapValues {
                            (
                                _,
                                content
                            ) ->
                        hasher.hash(
                            content
                        )
                    }
        )
}
