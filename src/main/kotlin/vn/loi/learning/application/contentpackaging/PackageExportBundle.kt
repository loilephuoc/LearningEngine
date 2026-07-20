package vn.loi.learning.application.contentpackaging

/**
 * Bộ dữ liệu export hoàn chỉnh trước khi ghi ra package vật lý.
 */
data class PackageExportBundle(
    val files: List<PackageExportFile>
 ) {

    val filePaths: List<String>
        get() = files.map { it.relativePath }

    val size: Int
        get() = files.size

    fun isEmpty(): Boolean =
        files.isEmpty()

    fun isNotEmpty(): Boolean =
        files.isNotEmpty()

    fun paths(): List<String> =
        filePaths.toList()

    fun file(relativePath: String): PackageExportFile? =
        files.firstOrNull { it.relativePath == relativePath }

    fun requireFiles(vararg relativePaths: String): List<PackageExportFile> =
        relativePaths.map { requireFile(it) }

    fun contentFiles(): List<PackageExportFile> =
        files.filterNot { it.relativePath == "manifest.json" }

    fun manifestFile(): PackageExportFile =
        requireFile("manifest.json")

    fun withoutManifest(): List<PackageExportFile> =
        contentFiles()

    fun contains(relativePath: String): Boolean =
        files.any { it.relativePath == relativePath }

    fun requireFile(relativePath: String): PackageExportFile =
        files.firstOrNull { it.relativePath == relativePath }
            ?: throw IllegalArgumentException("Missing export file: $relativePath")

    init {
        require(files.isNotEmpty()) {
            "Export bundle must contain at least one file."
        }
        require(filePaths.distinct().size == files.size) {
            "Export bundle must not contain duplicate file paths."
        }
    }
}
