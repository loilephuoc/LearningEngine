package vn.loi.learning.domain.content.packaging.model

/**
 * Khai báo một package khác mà package hiện tại cần để hoạt động.
 *
 * Domain chỉ lưu yêu cầu logic. Việc kiểm tra package đã được cài đặt
 * và so sánh version thuộc Application.
 */
data class PackageDependency(
    val packageName: String,
    val minimumVersion: String? =
        null,
    val maximumVersion: String? =
        null
) {

    init {
        require(packageName.isNotBlank()) {
            "Dependency package name must not be blank."
        }

        require(
            minimumVersion == null ||
                    minimumVersion.isNotBlank()
        ) {
            "Dependency minimum version must be null or non-blank."
        }

        require(
            maximumVersion == null ||
                    maximumVersion.isNotBlank()
        ) {
            "Dependency maximum version must be null or non-blank."
        }
    }
}