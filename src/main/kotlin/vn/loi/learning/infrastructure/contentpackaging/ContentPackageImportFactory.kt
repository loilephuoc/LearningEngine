package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.DefaultPackageInstaller
import vn.loi.learning.application.contentpackaging.LegacyPackageImportService
import vn.loi.learning.application.contentpackaging.LegacyPackageImportWorkflow
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageDescriptorReader
import vn.loi.learning.application.contentpackaging.PackageIdGenerator
import vn.loi.learning.application.contentpackaging.PackageInstaller
import vn.loi.learning.application.contentpackaging.PackageRegistrationOperation
import vn.loi.learning.application.contentpackaging.PackageScanner
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.infrastructure.contentmedia.ImportedMediaVerifier
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.contentmedia.LegacyContentMediaPathMapper
import vn.loi.learning.infrastructure.contentmedia.LegacyOpd3MediaArchiveReader
import vn.loi.learning.infrastructure.contentmedia.LegacyOpd3MediaExtractor
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

object ContentPackageImportFactory {

    fun createScanner(
        directory: Path
    ): PackageScanner =
        JvmDirectoryPackageScanner(
            directory
        )

    fun createLegacyScanner(
        directory: Path
    ): JvmLegacyPackageScanner =
        JvmLegacyPackageScanner(
            directory
        )

    fun createDescriptorReader(): PackageDescriptorReader =
        JvmOpd3PackageDescriptorReader(
            archiveReader =
                JvmOpd3ArchiveReader(),
            entryReader =
                JvmOpd3EntryReader()
        )

    fun createIdGenerator(): PackageIdGenerator =
        Sha256PackageIdGenerator()

    fun createInstaller(): PackageInstaller =
        DefaultPackageInstaller(
            descriptorReader =
                createDescriptorReader(),
            packageIdGenerator =
                createIdGenerator()
        )

    fun createContentImporter(): PackageContentImporter {
        val archiveReader =
            JvmOpd3ArchiveReader()

        val entryReader =
            JvmOpd3EntryReader()

        return createContentImporter(
            archiveReader = archiveReader,
            entryReader = entryReader,
            legacyImporter =
                JvmPackageContentImporter(
                    archiveReader = archiveReader,
                    entryReader = entryReader
                )
        )
    }

    fun createContentImporter(
        mediaDirectory: Path
    ): PackageContentImporter {
        val archiveReader =
            JvmOpd3ArchiveReader()

        val entryReader =
            JvmOpd3EntryReader()

        val mediaStorage =
            JvmContentMediaStorage(
                mediaDirectory
            )

        val legacyImporter =
            JvmPackageContentImporter(
                archiveReader = archiveReader,
                entryReader = entryReader,
                mediaExtractor =
                    LegacyOpd3MediaExtractor(
                        archiveReader =
                            LegacyOpd3MediaArchiveReader(),
                        mediaStorage =
                            mediaStorage
                    ),
                importedMediaVerifier =
                    ImportedMediaVerifier(
                        mediaStorage
                    )
            )

        return createContentImporter(
            archiveReader = archiveReader,
            entryReader = entryReader,
            legacyImporter = legacyImporter
        )
    }

    fun createLegacyImporter(
        mediaDirectory: Path
    ): LegacyOpd3PackageImporter {
        val mediaStorage =
            JvmContentMediaStorage(
                mediaDirectory
            )

        return LegacyOpd3PackageImporter(
            jsonImporter =
                LegacyJsonImporter(),
            mediaExtractor =
                LegacyOpd3MediaExtractor(
                    archiveReader =
                        LegacyOpd3MediaArchiveReader(),
                    mediaStorage =
                        mediaStorage
                ),
            mediaPathMapper =
                LegacyContentMediaPathMapper(),
            mediaVerifier =
                ImportedMediaVerifier(
                    mediaStorage
                )
        )
    }

    fun createLegacyImportWorkflow(
        sourceDirectory: Path,
        mediaDirectory: Path,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        packageRegistrationOperation: PackageRegistrationOperation,
        transactionRunner: TransactionRunner
    ): LegacyPackageImportWorkflow =
        LegacyPackageImportWorkflow(
            packageScanner =
                createLegacyScanner(
                    sourceDirectory
                ),
            packageImportService =
                LegacyPackageImportService(
                    packageContentImporter =
                        createLegacyImporter(
                            mediaDirectory
                        ),
                    packageIdGenerator =
                        createIdGenerator(),
                    contentRepository =
                        contentRepository,
                    learningItemRepository =
                        learningItemRepository,
                    packageRegistrationOperation =
                        packageRegistrationOperation,
                    transactionRunner =
                        transactionRunner
                )
        )

    private fun createContentImporter(
        archiveReader: Opd3ArchiveReader,
        entryReader: Opd3EntryReader,
        legacyImporter: JvmPackageContentImporter
    ): PackageContentImporter {
        val bundleImporter =
            PackageBundleImporter(
                bundleReader =
                    BundlePackageReader(
                        archiveReader =
                            archiveReader,
                        entryReader =
                            entryReader
                    )
            )

        return PackageContentImporterCompat(
            bundleImporter =
                bundleImporter,
            legacyImporter =
                legacyImporter
        )
    }
}