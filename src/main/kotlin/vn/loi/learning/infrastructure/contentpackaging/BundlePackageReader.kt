package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.MissingRequiredPackageEntryException
import vn.loi.learning.application.contentpackaging.PackageImportBundle

class BundlePackageReader(
    private val archiveReader: Opd3ArchiveReader,
    private val entryReader: Opd3EntryReader,
    private val archiveStructureValidator:
    Opd3ArchiveStructureValidator =
        Opd3ArchiveStructureValidator()
) {

    fun read(packagePath: Path): PackageImportBundle {
        val files = archiveReader.open(packagePath).use { archive ->
            archiveStructureValidator.validate(
                archive
            )

            PackageImportBundle.REQUIRED_FILES.associateWith { fileName ->
                entryReader.readText(archive, fileName)
                    ?: throw MissingRequiredPackageEntryException(
                        fileName
                    )
            }
        }

        return PackageImportBundle(files)
    }
}
