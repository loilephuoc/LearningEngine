package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.infrastructure.contentpackaging.BundlePackageReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3ArchiveReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3EntryReader
import vn.loi.learning.infrastructure.contentpackaging.JvmPackageContentImporter
import vn.loi.learning.infrastructure.contentpackaging.PackageBundleImporter
import vn.loi.learning.infrastructure.contentpackaging.PackageContentImporterCompat

class BundlePackageCompatibilityIntegrationTest {

    @Test
    fun `compat importer can be constructed`() {
        val importer =
            PackageContentImporterCompat(
                bundleImporter =
                    PackageBundleImporter(
                        BundlePackageReader(
                            JvmOpd3ArchiveReader(),
                            JvmOpd3EntryReader()
                        )
                    ),
                legacyImporter =
                    JvmPackageContentImporter(
                        archiveReader =
                            JvmOpd3ArchiveReader(),
                        entryReader =
                            JvmOpd3EntryReader()
                    )
            )

        assertEquals(
            PackageContentImporterCompat::class,
            importer::class
        )
    }
}