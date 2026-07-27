package vn.loi.learning.application.contentpackaging.validation

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

class InstalledContentConflictValidatorTest {

    @Test
    fun `new identifiers produce valid report`() {
        val validator =
            InstalledContentConflictValidator(
                contentRepository =
                    InMemoryContentRepository(),
                learningItemRepository =
                    InMemoryLearningItemRepository()
            )

        val content =
            createContent(
                "content-1"
            )

        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            content
                        ),
                    learningItems =
                        listOf(
                            createLearningItem(
                                id =
                                    "item-1",
                                contentId =
                                    content.id
                            )
                        )
                )
            )

        assertTrue(
            report.isValid
        )

        assertTrue(
            report.issues.isEmpty()
        )
    }

    @Test
    fun `installed content and learning item identifiers produce errors`() {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val content =
            createContent(
                "content-1"
            )

        val learningItem =
            createLearningItem(
                id =
                    "item-1",
                contentId =
                    content.id
            )

        contentRepository.save(
            content
        )

        learningItemRepository.save(
            learningItem
        )

        val validator =
            InstalledContentConflictValidator(
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository
            )

        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            content
                        ),
                    learningItems =
                        listOf(
                            learningItem
                        )
                )
            )

        assertEquals(
            listOf(
                "CONTENT_ID_ALREADY_INSTALLED",
                "LEARNING_ITEM_ID_ALREADY_INSTALLED"
            ),
            report.errors.map { issue ->
                issue.code
            }
        )
    }

    @Test
    fun `duplicate identifiers inside package produce one installed conflict each`() {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val content =
            createContent(
                "content-1"
            )

        val learningItem =
            createLearningItem(
                id =
                    "item-1",
                contentId =
                    content.id
            )

        contentRepository.save(
            content
        )

        learningItemRepository.save(
            learningItem
        )

        val validator =
            InstalledContentConflictValidator(
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository
            )

        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            content,
                            content
                        ),
                    learningItems =
                        listOf(
                            learningItem,
                            learningItem
                        )
                )
            )

        assertEquals(
            2,
            report.errors.size
        )
    }

    @Test
    fun `canonical repository present and empty ignores complete orphan graph`() {
        val fixture = authorityFixture(installedState = null)

        val codes = fixture.validator.validate(fixture.imported).errors.map { it.code }

        assertTrue(codes.isEmpty())
    }

    @Test
    fun `canonical repository with only removed package ignores orphan identifiers and fingerprints`() {
        val fixture = authorityFixture(installedState = PackageState.REMOVED)
        val sameFingerprint = createContent("replacement-content")
        val imported = ImportedPackageContent(
            contents = listOf(fixture.content, sameFingerprint),
            learningItems = listOf(
                fixture.learningItem,
                createLearningItem("replacement-item", sameFingerprint.id)
            )
        )

        val codes = fixture.validator.validate(imported).errors.map { it.code }

        assertFalse(codes.any {
            it == "CONTENT_ID_ALREADY_INSTALLED" ||
                it == "LEARNING_ITEM_ID_ALREADY_INSTALLED" ||
                it == "CONTENT_ALREADY_INSTALLED" ||
                it == "LEARNING_ITEM_ALREADY_INSTALLED"
        })
    }

    @Test
    fun `active installed owner makes matching content and item live conflicts`() {
        val fixture = authorityFixture(installedState = PackageState.ACTIVE)

        val codes = fixture.validator.validate(fixture.imported).errors.map { it.code }

        assertEquals(
            listOf("CONTENT_ID_ALREADY_INSTALLED", "LEARNING_ITEM_ID_ALREADY_INSTALLED"),
            codes
        )
    }

    @Test
    fun `archived installed owner makes matching content and item live conflicts`() {
        val fixture = authorityFixture(installedState = PackageState.ARCHIVED)

        val codes = fixture.validator.validate(fixture.imported).errors.map { it.code }

        assertEquals(
            listOf("CONTENT_ID_ALREADY_INSTALLED", "LEARNING_ITEM_ID_ALREADY_INSTALLED"),
            codes
        )
    }

    @Test
    fun `empty canonical repository never falls back to orphan content package`() {
        val fixture = authorityFixture(installedState = null)

        assertTrue(fixture.validator.validate(fixture.imported).isValid)
    }

    @Test
    fun `absent canonical repository retains deterministic legacy content package fallback`() {
        val fixture = authorityFixture(installedState = null, canonicalRepositoryPresent = false)

        val codes = fixture.validator.validate(fixture.imported).errors.map { it.code }

        assertEquals(
            listOf("CONTENT_ID_ALREADY_INSTALLED", "LEARNING_ITEM_ID_ALREADY_INSTALLED"),
            codes
        )
    }

    private fun authorityFixture(
        installedState: PackageState?,
        canonicalRepositoryPresent: Boolean = true
    ): AuthorityFixture {
        val contentRepository = InMemoryContentRepository()
        val learningItemRepository = InMemoryLearningItemRepository()
        val contentPackageRepository = InMemoryContentPackageRepository()
        val contentLibraryRepository = InMemoryContentLibraryRepository()
        val installedPackageRepository = InMemoryInstalledPackageRepository()
        val packageId = PackageId("package-a")
        val libraryId = ContentLibraryId("library-a")
        val topicId = TopicId("topic-a")
        val content = createContent("content-1")
        val learningItem = createLearningItem("item-1", content.id)

        contentRepository.save(content)
        learningItemRepository.save(learningItem)
        contentLibraryRepository.save(
            ContentLibrary(libraryId, LibraryDescriptor("Library A"), setOf(content.id))
        )
        contentPackageRepository.save(
            ContentPackage(
                packageId,
                PackageDescriptor("Package A", "1.0", "OPD3"),
                setOf(libraryId),
                topicId
            )
        )
        if (installedState != null) {
            installedPackageRepository.save(
                InstalledPackage.reconstitute(
                    id = InstalledPackageId("installed-a"),
                    libraryId = LibraryId("default-library"),
                    packageId = packageId,
                    topicId = topicId,
                    name = PackageName("Package A"),
                    version = PackageVersion("1.0"),
                    state = installedState,
                    installedAt = Instant.EPOCH,
                    contentCount = 1,
                    learningItemCount = 1
                )
            )
        }

        return AuthorityFixture(
            validator = InstalledContentConflictValidator(
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
                installedPackageRepository = installedPackageRepository.takeIf {
                    canonicalRepositoryPresent
                },
                contentPackageRepository = contentPackageRepository,
                contentLibraryRepository = contentLibraryRepository
            ),
            content = content,
            learningItem = learningItem,
            imported = ImportedPackageContent(listOf(content), listOf(learningItem))
        )
    }

    private data class AuthorityFixture(
        val validator: InstalledContentConflictValidator,
        val content: Content,
        val learningItem: LearningItem,
        val imported: ImportedPackageContent
    )

    private fun createContent(
        id: String
    ): Content =
        Content(
            id =
                ContentId(
                    id
                ),
            type =
                ContentType.WORD,
            text =
                ContentText(
                    primaryText =
                        "Hello"
                )
        )

    private fun createLearningItem(
        id: String,
        contentId: ContentId
    ): LearningItem =
        LearningItem(
            id =
                LearningItemId(
                    id
                ),
            contentId =
                contentId,
            mode =
                LearningMode.MEANING_RECOGNITION
        )
}
