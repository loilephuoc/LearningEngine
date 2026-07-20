package vn.loi.learning.application.contentmedia

data class ContentMediaAsset(
    val packageName: String,
    val fileName: String,
    val relativePath: String
) {

    init {
        require(packageName.isNotBlank()) {
            "Package name must not be blank."
        }

        require(fileName.isNotBlank()) {
            "Media file name must not be blank."
        }

        require(relativePath.isNotBlank()) {
            "Media relative path must not be blank."
        }
    }
}