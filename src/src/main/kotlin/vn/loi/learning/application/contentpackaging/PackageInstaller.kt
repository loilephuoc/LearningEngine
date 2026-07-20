package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.ContentPackage

/**
 * Cài đặt một package candidate thành ContentPackage hợp lệ.
 *
 * Việc đọc định dạng cụ thể và xác thực dữ liệu được thực hiện
 * bởi adapter triển khai contract này.
 */
fun interface PackageInstaller {

    fun install(
        candidate: PackageScanCandidate
    ): ContentPackage
}
