package vn.loi.learning.domain.library.repository

import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageState

/**
 * Repository interface quản lý lưu trữ và truy vấn các gói InstalledPackage.
 */
interface InstalledPackageRepository {
    fun findById(id: InstalledPackageId): InstalledPackage?
    fun findByPackageId(packageId: PackageId): InstalledPackage?
    fun findByTopicId(topicId: TopicId): InstalledPackage?
    fun findAllByState(state: PackageState): List<InstalledPackage>
    fun findAll(): List<InstalledPackage>
    fun save(installedPackage: InstalledPackage)
    fun delete(id: InstalledPackageId)
}
