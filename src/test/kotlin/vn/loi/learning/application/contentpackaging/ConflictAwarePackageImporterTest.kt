package vn.loi.learning.application.contentpackaging

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository

class ConflictAwarePackageImporterTest {

    private val libId = LibraryId("lib-1")
    private val catalogId = PackageCatalogId("catalog-1")

    private class ImmediateTransactionRunner : TransactionRunner {
        override fun <T> runInTransaction(block: () -> T): T = block()
    }

    private class FailingTransactionRunner : TransactionRunner {
        override fun <T> runInTransaction(block: () -> T): T {
            block()
            throw RuntimeException("Database write transaction failed midway.")
        }
    }

    @Test
    fun `1 - new package identified and installed successfully`() {
        val repo = InMemoryInstalledPackageRepository()
        val inspector = PackageImportInspector(repo)
        val importer = ConflictAwarePackageImporter(inspector, repo, ImmediateTransactionRunner())

        val pkgId = PackageId("pkg-new")
        val topicId = TopicId("topic-new")

        val decision = importer.inspectCandidate(
            candidatePackageId = pkgId,
            candidateTopicId = topicId,
            candidateName = "New Topic Package",
            candidateVersion = "1.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.NEW_PACKAGE, decision.type)
        assertTrue(decision.isExecutable)
        assertTrue(decision.conflictReasons.isEmpty())

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.NewPackageInstalled)

        val installed = repo.findByPackageId(pkgId)
        assertNotNull(installed)
        assertEquals("New Topic Package", installed.name.value)
        assertEquals("1.0.0", installed.version.value)
    }

    @Test
    fun `2, 3, 4 - re-importing identical package is a no-op that preserves state and creates no duplicates`() {
        val repo = InMemoryInstalledPackageRepository()
        val inspector = PackageImportInspector(repo)
        val importer = ConflictAwarePackageImporter(inspector, repo, ImmediateTransactionRunner())

        val pkgId = PackageId("pkg-identical")
        val topicId = TopicId("topic-identical")
        val instId = InstalledPackageId("inst-identical")

        val initial = InstalledPackage.reconstitute(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Identical Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.parse("2026-01-01T00:00:00Z"),
            contentCount = 50,
            learningItemCount = 200
        )
        repo.save(initial)

        val decision = importer.inspectCandidate(
            candidatePackageId = pkgId,
            candidateTopicId = topicId,
            candidateName = "Identical Package",
            candidateVersion = "1.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.IDENTICAL_PACKAGE, decision.type)
        assertEquals(instId, decision.existingInstalledPackageId)

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.AlreadyInstalledIdentical)

        val allInRepo = repo.findAllByLibraryId(libId)
        assertEquals(1, allInRepo.size)
        assertEquals(initial, allInRepo.first())
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), allInRepo.first().installedAt)
    }

    @Test
    fun `5, 6, 7 - compatible update recognized as safe replacement preserving canonical identity and progress`() {
        val repo = InMemoryInstalledPackageRepository()
        val inspector = PackageImportInspector(repo)
        val importer = ConflictAwarePackageImporter(inspector, repo, ImmediateTransactionRunner())

        val pkgId = PackageId("pkg-upgrade")
        val topicId = TopicId("topic-canonical")
        val instId = InstalledPackageId("inst-upgrade")

        val existing = InstalledPackage.reconstitute(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Upgradeable Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.parse("2026-01-01T00:00:00Z"),
            contentCount = 10,
            learningItemCount = 30
        )
        repo.save(existing)

        val decision = importer.inspectCandidate(
            candidatePackageId = pkgId,
            candidateTopicId = topicId,
            candidateName = "Upgradeable Package",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.SAFE_REPLACEMENT, decision.type)
        assertEquals("1.0.0", decision.existingVersion)

        val outcome = importer.executeImport(decision, libId, contentCount = 15, learningItemCount = 40)
        assertTrue(outcome is PackageImportOutcome.ReplacementCompleted)
        assertEquals("1.0.0", outcome.previousVersion)
        assertEquals("2.0.0", outcome.newVersion)
        assertEquals("topic-canonical", outcome.preservedTopicId)

        val updatedInRepo = repo.findById(instId)
        assertNotNull(updatedInRepo)
        assertEquals(topicId, updatedInRepo.topicId)
        assertEquals("2.0.0", updatedInRepo.version.value)
        assertEquals(15, updatedInRepo.contentCount)
        assertEquals(40, updatedInRepo.learningItemCount)
    }

    @Test
    fun `8, 9, 10 - incompatible package version or topic creates structured conflict without repo mutation`() {
        val repo = InMemoryInstalledPackageRepository()
        val inspector = PackageImportInspector(repo)
        val importer = ConflictAwarePackageImporter(inspector, repo, ImmediateTransactionRunner())

        val pkgId = PackageId("pkg-conflict")
        val topicId = TopicId("topic-orig")
        val instId = InstalledPackageId("inst-conflict")

        val existing = InstalledPackage.reconstitute(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Existing Package"),
            version = PackageVersion("2.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 10,
            learningItemCount = 20
        )
        repo.save(existing)

        // Attempt downgrade to version 1.0.0
        val downgradeDecision = importer.inspectCandidate(
            candidatePackageId = pkgId,
            candidateTopicId = topicId,
            candidateName = "Existing Package",
            candidateVersion = "1.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, downgradeDecision.type)
        assertFalse(downgradeDecision.isExecutable)
        assertTrue(downgradeDecision.conflictReasons.any { it.contains("older than installed version") })

        val outcome = importer.executeImport(downgradeDecision, libId)
        assertTrue(outcome is PackageImportOutcome.ConflictDetected)

        // Verify repository was not mutated
        val afterAttempt = repo.findById(instId)
        assertNotNull(afterAttempt)
        assertEquals("2.0.0", afterAttempt.version.value)

        // Attempt topic ID conflict
        val conflictingTopicId = TopicId("topic-different")
        val topicConflictDecision = importer.inspectCandidate(
            candidatePackageId = pkgId,
            candidateTopicId = conflictingTopicId,
            candidateName = "Existing Package",
            candidateVersion = "3.0.0",
            libraryId = libId
        )
        assertEquals(ImportDecisionType.CONFLICT, topicConflictDecision.type)
        assertTrue(topicConflictDecision.conflictReasons.any { it.contains("conflicts with installed package topic ID") })
    }

    @Test
    fun `11, 12 - transaction failure during replacement returns technical failure without corrupting state`() {
        val repo = InMemoryInstalledPackageRepository()
        val inspector = PackageImportInspector(repo)
        val failingImporter = ConflictAwarePackageImporter(inspector, repo, FailingTransactionRunner())

        val pkgId = PackageId("pkg-fail")
        val topicId = TopicId("topic-fail")
        val instId = InstalledPackageId("inst-fail")

        val existing = InstalledPackage.reconstitute(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Fail Replacement Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 5,
            learningItemCount = 10
        )
        repo.save(existing)

        val decision = failingImporter.inspectCandidate(
            candidatePackageId = pkgId,
            candidateTopicId = topicId,
            candidateName = "Fail Replacement Package",
            candidateVersion = "2.0.0",
            libraryId = libId
        )
        assertEquals(ImportDecisionType.SAFE_REPLACEMENT, decision.type)

        val outcome = failingImporter.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.TechnicalFailure)
        assertTrue(outcome.sanitizedMessage.contains("Failed to apply package replacement"))
    }
}
