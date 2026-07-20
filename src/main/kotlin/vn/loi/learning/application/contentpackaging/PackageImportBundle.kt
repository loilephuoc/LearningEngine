package vn.loi.learning.application.contentpackaging

data class PackageImportBundle(
    val files: Map<String, String>
) {

    init {
        val missingFiles = REQUIRED_FILES.filterNot(files::containsKey)

        require(missingFiles.isEmpty()) {
            "Missing required package files: ${missingFiles.joinToString()}"
        }
    }

    fun requireFile(name: String): String =
        files[name] ?: throw IllegalArgumentException(
            "Missing package file: $name"
        )

    fun metadataJson(): String =
        requireFile(METADATA_FILE)

    fun contentsJson(): String =
        requireFile(CONTENTS_FILE)

    fun learningItemsJson(): String =
        requireFile(LEARNING_ITEMS_FILE)

    fun manifestJson(): String =
        requireFile(MANIFEST_FILE)

    companion object {
        const val METADATA_FILE = "metadata.json"
        const val CONTENTS_FILE = "contents.json"
        const val LEARNING_ITEMS_FILE = "learning-items.json"
        const val MANIFEST_FILE = "manifest.json"

        val REQUIRED_FILES: Set<String> = setOf(
            METADATA_FILE,
            CONTENTS_FILE,
            LEARNING_ITEMS_FILE,
            MANIFEST_FILE
        )
    }
}