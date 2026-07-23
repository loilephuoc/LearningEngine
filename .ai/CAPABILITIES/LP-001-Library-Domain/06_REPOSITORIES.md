# LP-001: Library Domain — Repositories Specification

## 1. `LibraryRepository` (Interface Port)

```kotlin
package vn.loi.learning.domain.library.repository

import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Repository interface định nghĩa hợp đồng lưu trữ và truy vấn cho Aggregate Root Library.
 * Không chứa bất kỳ mã triển khai persistence hoặc database nào.
 */
interface LibraryRepository {
    fun findById(id: LibraryId): Library?
    fun save(library: Library)
    fun existsById(id: LibraryId): Boolean
}
```

---

## 2. `InstalledPackageRepository` (Interface Port)

```kotlin
package vn.loi.learning.domain.library.repository

import vn.loi.learning.domain.content.model.PackageId
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
```

---

## 3. `CollectionRepository` (Interface Port)

```kotlin
package vn.loi.learning.domain.library.repository

import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Repository interface quản lý bộ sưu tập (Collection) của người học.
 */
interface CollectionRepository {
    fun findById(id: CollectionId): Collection?
    fun findByName(libraryId: LibraryId, name: CollectionName): Collection?
    fun findAllByLibraryId(libraryId: LibraryId): List<Collection>
    fun save(collection: Collection)
    fun delete(id: CollectionId)
    fun existsByName(libraryId: LibraryId, name: CollectionName): Boolean
}
```
