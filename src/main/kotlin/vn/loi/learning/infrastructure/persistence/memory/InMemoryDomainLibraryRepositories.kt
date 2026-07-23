package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository

class InMemoryLibraryRepository : LibraryRepository {
    private val storage = mutableMapOf<LibraryId, Library>()

    override fun findById(id: LibraryId): Library? = storage[id]
    override fun save(library: Library) { storage[library.id] = library }
    override fun existsById(id: LibraryId): Boolean = storage.containsKey(id)
}

class InMemoryInstalledPackageRepository : InstalledPackageRepository {
    private val storage = mutableMapOf<InstalledPackageId, InstalledPackage>()

    override fun findById(id: InstalledPackageId): InstalledPackage? = storage[id]

    override fun findByPackageId(packageId: PackageId): InstalledPackage? =
        storage.values.firstOrNull { it.packageId == packageId && it.state != PackageState.REMOVED }

    override fun findByTopicId(topicId: TopicId): InstalledPackage? =
        storage.values.firstOrNull { it.topicId == topicId && it.state != PackageState.REMOVED }

    override fun findAllByState(state: PackageState): List<InstalledPackage> =
        storage.values.filter { it.state == state }

    override fun findAll(): List<InstalledPackage> = storage.values.toList()

    override fun findAllByLibraryId(libraryId: LibraryId): List<InstalledPackage> =
        storage.values.filter { it.libraryId == libraryId }

    override fun findAllByLibraryIdAndState(libraryId: LibraryId, state: PackageState): List<InstalledPackage> =
        storage.values.filter { it.libraryId == libraryId && it.state == state }

    override fun save(installedPackage: InstalledPackage) {
        storage[installedPackage.id] = installedPackage
    }

    override fun delete(id: InstalledPackageId) {
        storage.remove(id)
    }
}

class InMemoryCollectionRepository : CollectionRepository {
    private val storage = mutableMapOf<CollectionId, Collection>()

    override fun findById(id: CollectionId): Collection? = storage[id]

    override fun findByName(libraryId: LibraryId, name: CollectionName): Collection? =
        storage.values.firstOrNull { it.libraryId == libraryId && it.name == name }

    override fun findAllByLibraryId(libraryId: LibraryId): List<Collection> =
        storage.values.filter { it.libraryId == libraryId }

    override fun findAllByLibraryIdAndState(libraryId: LibraryId, state: CollectionState): List<Collection> =
        storage.values.filter { it.libraryId == libraryId && it.state == state }

    override fun save(collection: Collection) {
        storage[collection.id] = collection
    }

    override fun delete(id: CollectionId) {
        storage.remove(id)
    }

    override fun existsByName(libraryId: LibraryId, name: CollectionName): Boolean =
        findByName(libraryId, name) != null
}
