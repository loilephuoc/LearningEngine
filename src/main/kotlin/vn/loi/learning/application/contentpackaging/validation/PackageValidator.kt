package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageValidator(
    private val metadataValidator: PackageMetadataValidator =
        PackageMetadataValidator(),
    private val contentIntegrityValidator: PackageContentIntegrityValidator =
        PackageContentIntegrityValidator(),
    private val learningItemReferenceValidator: PackageLearningItemReferenceValidator =
        PackageLearningItemReferenceValidator(),
    private val duplicateContentValidator: DuplicateContentValidator =
        DuplicateContentValidator(),
    private val packageWarningValidator: PackageWarningValidator =
        PackageWarningValidator()
) {

    fun validate(
        descriptor: PackageDescriptor,
        importedContent: ImportedPackageContent
    ): PackageValidationReport {
        val issues =
            buildList {
                addAll(
                    metadataValidator
                        .validate(
                            descriptor
                        )
                        .issues
                )

                addAll(
                    contentIntegrityValidator
                        .validate(
                            importedContent
                        )
                        .issues
                )

                addAll(
                    learningItemReferenceValidator
                        .validate(
                            importedContent
                        )
                        .issues
                )

                addAll(
                    duplicateContentValidator
                        .validate(
                            importedContent
                        )
                        .issues
                )

                addAll(
                    packageWarningValidator
                        .validate(
                            importedContent
                        )
                        .issues
                )
            }

        return PackageValidationReport(
            issues =
                issues
        )
    }
}