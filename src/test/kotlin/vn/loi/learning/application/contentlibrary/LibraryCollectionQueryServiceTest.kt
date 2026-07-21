package vn.loi.learning.application.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryCollectionRepository

class LibraryCollectionQueryServiceTest {

    private val libraryId =
        ContentLibraryId(
            "library-main"
        )

    private val otherLibraryId =
        ContentLibraryId(
            "library-other"
        )

    @Test
    fun `query returns collections for requested library ordered by name`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        repository.saveAll(
            listOf(
                createCollection(
                    collectionId =
                        LibraryCollectionId(
                            "collection-vocabulary"
                        ),
                    libraryId = libraryId,
                    name = "Vocabulary",
                    packageIds =
                        setOf(
                            PackageId(
                                "package-vocabulary"
                            )
                        )
                ),
                createCollection(
                    collectionId =
                        LibraryCollectionId(
                            "collection-conversations"
                        ),
                    libraryId = libraryId,
                    name = "Conversations",
                    packageIds =
                        setOf(
                            PackageId(
                                "package-conversations-2"
                            ),
                            PackageId(
                                "package-conversations-1"
                            )
                        )
                ),
                createCollection(
                    collectionId =
                        LibraryCollectionId(
                            "collection-other"
                        ),
                    libraryId = otherLibraryId,
                    name = "Other",
                    packageIds = emptySet()
                )
            )
        )

        val result =
            createService(
                repository
            ).query(
                libraryId
            )

        assertEquals(
            2,
            result.size
        )

        assertEquals(
            LibraryCollectionItem(
                id = "collection-conversations",
                libraryId = "library-main",
                name = "Conversations",
                packageIds =
                    listOf(
                        "package-conversations-1",
                        "package-conversations-2"
                    )
            ),
            result[0]
        )

        assertEquals(
            2,
            result[0].packageCount
        )

        assertEquals(
            LibraryCollectionItem(
                id = "collection-vocabulary",
                libraryId = "library-main",
                name = "Vocabulary",
                packageIds =
                    listOf(
                        "package-vocabulary"
                    )
            ),
            result[1]
        )

        assertEquals(
            1,
            result[1].packageCount
        )
    }

    @Test
    fun `query uses collection id as deterministic tie breaker`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        repository.saveAll(
            listOf(
                createCollection(
                    collectionId =
                        LibraryCollectionId(
                            "collection-b"
                        ),
                    libraryId = libraryId,
                    name = "English",
                    packageIds = emptySet()
                ),
                createCollection(
                    collectionId =
                        LibraryCollectionId(
                            "collection-a"
                        ),
                    libraryId = libraryId,
                    name = "English",
                    packageIds = emptySet()
                )
            )
        )

        val result =
            createService(
                repository
            ).query(
                libraryId
            )

        assertEquals(
            listOf(
                "collection-a",
                "collection-b"
            ),
            result.map { item ->
                item.id
            }
        )
    }

    @Test
    fun `query returns package ids in deterministic order`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        repository.save(
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-main"
                    ),
                libraryId = libraryId,
                name = "Main",
                packageIds =
                    setOf(
                        PackageId(
                            "package-z"
                        ),
                        PackageId(
                            "package-a"
                        ),
                        PackageId(
                            "package-m"
                        )
                    )
            )
        )

        val result =
            createService(
                repository
            ).query(
                libraryId
            )

        assertEquals(
            listOf(
                "package-a",
                "package-m",
                "package-z"
            ),
            result.single().packageIds
        )
    }

    @Test
    fun `query returns empty list when library has no collections`() {
        val result =
            createService(
                InMemoryLibraryCollectionRepository()
            ).query(
                libraryId
            )

        assertEquals(
            emptyList(),
            result
        )
    }

    private fun createCollection(
        collectionId: LibraryCollectionId,
        libraryId: ContentLibraryId,
        name: String,
        packageIds: Set<PackageId>
    ): LibraryCollection =
        LibraryCollection(
            id = collectionId,
            libraryId = libraryId,
            descriptor =
                LibraryCollectionDescriptor(
                    name = name
                ),
            packageIds = packageIds
        )

    private fun createService(
        repository: InMemoryLibraryCollectionRepository
    ): LibraryCollectionQueryService =
        LibraryCollectionQueryService(
            libraryCollectionRepository = repository
        )
}