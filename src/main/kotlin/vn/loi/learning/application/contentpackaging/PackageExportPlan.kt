package vn.loi.learning.application.contentpackaging

/**
 * Kế hoạch export hoàn chỉnh trước khi Infrastructure ghi ra package.
 */
data class PackageExportPlan(
    val files: List<PackageExportFile>
) : Iterable<PackageExportFile> {

    val fileByPath: Map<String, PackageExportFile> =
        files.associateBy { it.relativePath }

    val relativePaths: Set<String> =
        fileByPath.keys

    val size: Int
        get() = files.size

    init {
        require(files.isNotEmpty()) {
            "Package export plan must contain at least one file."
        }
        require(fileByPath.size == files.size) {
            "Package export plan must not contain duplicate relative paths."
        }
    }

    fun isEmpty(): Boolean =
        files.isEmpty()

    fun isNotEmpty(): Boolean =
        files.isNotEmpty()

    fun relativePathSequence(): Sequence<String> =
        files.asSequence().map { it.relativePath }

    fun fileSequence(): Sequence<PackageExportFile> =
        files.asSequence()

    override fun iterator(): Iterator<PackageExportFile> =
        files.iterator()

    fun toList(): List<PackageExportFile> =
        files.toList()

    fun requireFile(relativePath: String): PackageExportFile =
        get(relativePath)
            ?: throw IllegalArgumentException("Missing export file: $relativePath")

    fun contains(relativePath: String): Boolean =
        relativePaths.contains(relativePath)

    operator fun get(relativePath: String): PackageExportFile? =
        fileByPath[relativePath]
}
