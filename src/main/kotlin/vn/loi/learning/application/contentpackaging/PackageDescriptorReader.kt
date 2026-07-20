package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * Đọc metadata logic của một package candidate.
 *
 * Application không biết cấu trúc file OPD3 hoặc filesystem JVM.
 * Adapter định dạng cụ thể chịu trách nhiệm đọc và xác thực metadata.
 */
fun interface PackageDescriptorReader {

    fun read(
        candidate: PackageScanCandidate
    ): PackageDescriptor
}
