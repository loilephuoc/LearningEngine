package vn.loi.learning.application.contentpackaging

open class MissingRequiredPackageEntryException(
    val entryName: String,
    message: String = "Missing package file: $entryName"
) : PackageImportException(message) {

    init {
        require(entryName.isNotBlank()) {
            "Missing package entry name must not be blank."
        }
    }
}
