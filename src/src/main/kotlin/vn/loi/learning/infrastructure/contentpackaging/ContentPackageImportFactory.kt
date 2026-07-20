package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.DefaultPackageInstaller
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageDescriptorReader
import vn.loi.learning.application.contentpackaging.PackageIdGenerator
import vn.loi.learning.application.contentpackaging.PackageInstaller
import vn.loi.learning.application.contentpackaging.PackageScanner

object ContentPackageImportFactory {

    fun createScanner(
        directory: Path
    ): PackageScanner =
        JvmDirectoryPackageScanner(directory)

    fun createDescriptorReader(): PackageDescriptorReader =
        JvmOpd3PackageDescriptorReader(
            archiveReader = JvmOpd3ArchiveReader(),
            entryReader = JvmOpd3EntryReader()
        )

    fun createIdGenerator(): PackageIdGenerator =
        Sha256PackageIdGenerator()

    fun createInstaller(): PackageInstaller =
        DefaultPackageInstaller(
            descriptorReader = createDescriptorReader(),
            packageIdGenerator = createIdGenerator()
        )

    fun createContentImporter(): PackageContentImporter =
        JvmPackageContentImporter(
            archiveReader = JvmOpd3ArchiveReader(),
            entryReader = JvmOpd3EntryReader()
        )
}
