package vn.loi.learning.application.contentpackaging

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.port.TransactionRunner
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

    private class ImmediateTransactionRunner : TransactionRunner {
        override fun <T> runInTransaction(block: () -> T): T = block()
    }

    private class FailingTransactionRunner : TransactionRunner {
        override fun <T> runInTransaction(block: () -> T): T {
            block()
            throw RuntimeException("Database write transaction failed midway.")
        }
    }

    private fun makeImporter(
        repo: InMemoryInstalledPackageRepository,
        transactionRunner: TransactionRunner = ImmediateTransactionRunner()
    ) = ConflictAwarePackageImporter(
        inspector = PackageImportInspector(repo),
        installedPackageRepository = repo,
        transactionRunner = transactionRunner
    )

    private fun installedPkg(
        instId: InstalledPackageId,
        pkgId: PackageId,
        topicId: TopicId,
        version: String = "1.0.0",
        name: String = "Package",
        checksum: String? = null
    ) = InstalledPackage.reconstitute(
        id = instId,
        libraryId = libId,
        packageId = pkgId,
        topicId = topicId,
        name = PackageName(name),
        version = PackageVersion(version),
        state = PackageState.ACTIVE,
        installedAt = Instant.parse("2026-01-01T00:00:00Z"),
        contentCount = 10,
        learningItemCount = 30,
        contentChecksum = checksum
    )

    // ---------------------------------------------------------------------------
    // AC-R7.1: Package name same, canonical identity different → not same package
    // ---------------------------------------------------------------------------

    @Test
    fun `same package name but different canonical PackageId and TopicId is recognized as new package`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        // Existing package has pkgId-1 / topic-1
        repo.save(installedPkg(
            instId = InstalledPackageId("inst-1"),
            pkgId = PackageId("pkg-original"),
            topicId = TopicId("topic-original"),
            name = "Same Name Package"
        ))

        // Candidate has different PackageId and TopicId but same display name
        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-totally-new"),
            candidateTopicId = TopicId("topic-totally-new"),
            candidateName = "Same Name Package",
            candidateVersion = "1.0.0",
            libraryId = libId
        )

        // Different canonical identity → NEW_PACKAGE (name is not an authority)
        assertEquals(ImportDecisionType.NEW_PACKAGE, decision.type)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.2: Different package name, same canonical identity → identity still valid
    // ---------------------------------------------------------------------------

    @Test
    fun `different package name but same canonical PackageId is matched by canonical identity`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(
            instId = InstalledPackageId("inst-1"),
            pkgId = PackageId("pkg-canonical"),
            topicId = TopicId("topic-canonical"),
            name = "Original Name"
        ))

        // Candidate with same PackageId + same TopicId but different name → should still resolve as existing
        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-canonical"),
            candidateTopicId = TopicId("topic-canonical"),
            candidateName = "Renamed Package",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        // Not NEW_PACKAGE — canonical identity was matched
        assertFalse(decision.type == ImportDecisionType.NEW_PACKAGE)
        assertEquals(InstalledPackageId("inst-1"), decision.existingInstalledPackageId)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.3: Same PackageId + version + DIFFERENT checksum → not identical
    // ---------------------------------------------------------------------------

    @Test
    fun `same PackageId and version with different canonical checksums is not identical`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(
            instId = InstalledPackageId("inst-1"),
            pkgId = PackageId("pkg-content"),
            topicId = TopicId("topic-content"),
            version = "1.0.0",
            checksum = "sha256-abc123"
        ))

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-content"),
            candidateTopicId = TopicId("topic-content"),
            candidateName = "Content Package",
            candidateVersion = "1.0.0",
            candidateChecksum = "sha256-different",
            libraryId = libId
        )

        // Different content fingerprint → CONFLICT, not IDENTICAL
        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertTrue(decision.conflictReasons.contains(PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE))
        assertFalse(decision.isExecutable)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.4: Identical only when canonical checksum matches
    // ---------------------------------------------------------------------------

    @Test
    fun `identical verdict requires same canonical checksum to be present and matching`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)
        val checksum = "sha256-deterministic-fingerprint"

        repo.save(installedPkg(
            instId = InstalledPackageId("inst-identical"),
            pkgId = PackageId("pkg-identical"),
            topicId = TopicId("topic-identical"),
            version = "1.0.0",
            checksum = checksum
        ))

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-identical"),
            candidateTopicId = TopicId("topic-identical"),
            candidateName = "Identical Package",
            candidateVersion = "1.0.0",
            candidateChecksum = checksum,
            libraryId = libId
        )

        assertEquals(ImportDecisionType.IDENTICAL_PACKAGE, decision.type)
        assertTrue(decision.isExecutable)

        // Execute: must return the real aggregate
        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.AlreadyInstalledIdentical)
        assertEquals("1.0.0", outcome.installedPackage.version.value)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.5: Same version, no checksum stored → conservative CONFLICT
    // ---------------------------------------------------------------------------

    @Test
    fun `same version without any checksum evidence returns conservative conflict`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        // Existing record without contentChecksum (legacy record)
        repo.save(installedPkg(
            instId = InstalledPackageId("inst-legacy"),
            pkgId = PackageId("pkg-legacy"),
            topicId = TopicId("topic-legacy"),
            version = "1.0.0",
            checksum = null
        ))

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-legacy"),
            candidateTopicId = TopicId("topic-legacy"),
            candidateName = "Legacy Package",
            candidateVersion = "1.0.0",
            candidateChecksum = null, // also no checksum from candidate
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertTrue(decision.conflictReasons.contains(PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE))
    }

    // ---------------------------------------------------------------------------
    // AC-R7.6: No fabricated aggregate in success outcome
    // ---------------------------------------------------------------------------

    @Test
    fun `identical outcome always returns real aggregate from repository not a fabricated one`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)
        val checksum = "sha256-real"
        val installTime = Instant.parse("2025-06-01T00:00:00Z")

        val real = InstalledPackage.reconstitute(
            id = InstalledPackageId("inst-real"),
            libraryId = libId,
            packageId = PackageId("pkg-real"),
            topicId = TopicId("topic-real"),
            name = PackageName("Real Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = installTime,
            contentCount = 77,
            learningItemCount = 300,
            contentChecksum = checksum
        )
        repo.save(real)

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-real"),
            candidateTopicId = TopicId("topic-real"),
            candidateName = "Real Package",
            candidateVersion = "1.0.0",
            candidateChecksum = checksum,
            libraryId = libId
        )

        assertEquals(ImportDecisionType.IDENTICAL_PACKAGE, decision.type)

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.AlreadyInstalledIdentical)

        // Must be the REAL aggregate — original installedAt and contentCount preserved
        assertEquals(installTime, outcome.installedPackage.installedAt)
        assertEquals(77, outcome.installedPackage.contentCount)
        assertEquals(InstalledPackageId("inst-real"), outcome.installedPackage.id)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.7: Repository inconsistency → TechnicalFailure, no fabricated aggregate
    // ---------------------------------------------------------------------------

    @Test
    fun `repository inconsistency for identical decision returns technical failure not fabricated aggregate`() {
        val repo = InMemoryInstalledPackageRepository()

        // Build an inspector that lies: reports existingInstId that is NOT in the repo
        val missingId = InstalledPackageId("inst-missing")
        val fakePkgId = PackageId("pkg-fake")
        val fakeTopic = TopicId("topic-fake")

        val decision = PackageImportDecision(
            type = ImportDecisionType.IDENTICAL_PACKAGE,
            candidatePackageId = fakePkgId,
            candidateTopicId = fakeTopic,
            candidateName = "Fake",
            candidateVersion = "1.0.0",
            existingInstalledPackageId = missingId,
            existingPackageId = fakePkgId
        )

        val importer = ConflictAwarePackageImporter(
            inspector = PackageImportInspector(repo),
            installedPackageRepository = repo,
            transactionRunner = ImmediateTransactionRunner()
        )

        val outcome = importer.executeImport(decision, libId)

        // Must be a failure — no InstalledPackage fabricated
        assertTrue(outcome is PackageImportOutcome.TechnicalFailure)
        assertTrue(outcome.sanitizedMessage.contains("cannot be found"))
    }

    // ---------------------------------------------------------------------------
    // AC-R7.8: Typed conflict reason per conflict path
    // ---------------------------------------------------------------------------

    @Test
    fun `downgrade version returns typed OLDER_VERSION conflict reason`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(
            instId = InstalledPackageId("inst-v2"),
            pkgId = PackageId("pkg-version"),
            topicId = TopicId("topic-version"),
            version = "2.0.0"
        ))

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-version"),
            candidateTopicId = TopicId("topic-version"),
            candidateName = "Version Package",
            candidateVersion = "1.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertEquals(listOf(PackageImportConflictReason.OLDER_VERSION), decision.conflictReasons)

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.ConflictDetected)
        assertEquals(listOf(PackageImportConflictReason.OLDER_VERSION), outcome.conflictReasons)
    }

    @Test
    fun `topic ID mismatch returns typed TOPIC_ID_MISMATCH conflict reason`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(
            instId = InstalledPackageId("inst-t"),
            pkgId = PackageId("pkg-t"),
            topicId = TopicId("topic-original"),
            version = "1.0.0"
        ))

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-t"),
            candidateTopicId = TopicId("topic-different"),
            candidateName = "Topic Package",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertEquals(listOf(PackageImportConflictReason.TOPIC_ID_MISMATCH), decision.conflictReasons)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.9: Consumer can switch on typed reason without parsing strings
    // ---------------------------------------------------------------------------

    @Test
    fun `consumer can switch on typed conflict reason without parsing any string`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(
            instId = InstalledPackageId("inst-sw"),
            pkgId = PackageId("pkg-sw"),
            topicId = TopicId("topic-sw"),
            version = "3.0.0"
        ))

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-sw"),
            candidateTopicId = TopicId("topic-sw"),
            candidateName = "Package",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)

        // Consumer switches on typed reason
        val reason = decision.conflictReasons.first()
        val label = when (reason) {
            PackageImportConflictReason.OLDER_VERSION -> "older"
            PackageImportConflictReason.TOPIC_ID_MISMATCH -> "topic-mismatch"
            PackageImportConflictReason.AMBIGUOUS_EXISTING_IDENTITY -> "ambiguous"
            PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE -> "insufficient"
        }
        assertEquals("older", label)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.10: Ambiguous identity (PackageId → record A, TopicId → record B) → CONFLICT, no mutation
    // ---------------------------------------------------------------------------

    @Test
    fun `ambiguous identity when PackageId and TopicId point to different records creates conflict without mutation`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        val pkgIdA = PackageId("pkg-a")
        val topicIdA = TopicId("topic-a")
        val pkgIdB = PackageId("pkg-b")
        val topicIdB = TopicId("topic-b")

        repo.save(installedPkg(InstId("inst-a"), pkgIdA, topicIdA, "1.0.0"))
        repo.save(installedPkg(InstId("inst-b"), pkgIdB, topicIdB, "1.0.0"))

        // Candidate whose PackageId matches inst-a, but TopicId matches inst-b
        val decision = importer.inspectCandidate(
            candidatePackageId = pkgIdA,
            candidateTopicId = topicIdB,
            candidateName = "Ambiguous",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertTrue(decision.conflictReasons.contains(PackageImportConflictReason.AMBIGUOUS_EXISTING_IDENTITY))

        val snapshotBefore = repo.findAll().size
        importer.executeImport(decision, libId)
        val snapshotAfter = repo.findAll().size
        assertEquals(snapshotBefore, snapshotAfter, "Repository must not be mutated on AMBIGUOUS conflict")
    }

    // ---------------------------------------------------------------------------
    // AC-R7.11: Conflict path has zero writes (repository unchanged)
    // ---------------------------------------------------------------------------

    @Test
    fun `conflict outcome does not mutate repository`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(
            instId = InstalledPackageId("inst-nomutate"),
            pkgId = PackageId("pkg-nomutate"),
            topicId = TopicId("topic-nomutate"),
            version = "2.0.0",
            checksum = "sha256-original"
        ))

        val snapshotBefore = repo.findAll().map { it.version.value }

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-nomutate"),
            candidateTopicId = TopicId("topic-nomutate"),
            candidateName = "Package",
            candidateVersion = "1.0.0", // downgrade → conflict
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        importer.executeImport(decision, libId)

        val snapshotAfter = repo.findAll().map { it.version.value }
        assertEquals(snapshotBefore, snapshotAfter)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.1 (original LP-004): new package identified and installed
    // ---------------------------------------------------------------------------

    @Test
    fun `new package identified and installed successfully`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

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

    // ---------------------------------------------------------------------------
    // AC-R7.x: Identical import no-op with checksum — no duplicates, preserves installedAt
    // ---------------------------------------------------------------------------

    @Test
    fun `identical import with matching checksum is no-op preserving all original record fields`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)
        val checksum = "sha256-no-duplicate"
        val installTime = Instant.parse("2026-01-01T00:00:00Z")

        val initial = InstalledPackage.reconstitute(
            id = InstalledPackageId("inst-identical"),
            libraryId = libId,
            packageId = PackageId("pkg-identical"),
            topicId = TopicId("topic-identical"),
            name = PackageName("Identical Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = installTime,
            contentCount = 50,
            learningItemCount = 200,
            contentChecksum = checksum
        )
        repo.save(initial)

        val decision = importer.inspectCandidate(
            candidatePackageId = PackageId("pkg-identical"),
            candidateTopicId = TopicId("topic-identical"),
            candidateName = "Identical Package",
            candidateVersion = "1.0.0",
            candidateChecksum = checksum,
            libraryId = libId
        )

        assertEquals(ImportDecisionType.IDENTICAL_PACKAGE, decision.type)

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.AlreadyInstalledIdentical)

        // No duplicates
        val allInRepo = repo.findAllByLibraryId(libId)
        assertEquals(1, allInRepo.size)
        // installedAt preserved (original record returned)
        assertEquals(installTime, allInRepo.first().installedAt)
    }

    // ---------------------------------------------------------------------------
    // AC-R7.x: Safe replacement preserves canonical TopicId
    // ---------------------------------------------------------------------------

    @Test
    fun `compatible update recognized as safe replacement preserving canonical TopicId`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

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
    }

    // ---------------------------------------------------------------------------
    // AC-R7.x: Rollback on failure
    // ---------------------------------------------------------------------------

    @Test
    fun `transaction failure during replacement returns technical failure`() {
        val repo = InMemoryInstalledPackageRepository()
        val failingImporter = makeImporter(repo, FailingTransactionRunner())

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

    // ---------------------------------------------------------------------------
    // Helper to keep tests concise
    // ---------------------------------------------------------------------------

    private fun InstId(value: String) = InstalledPackageId(value)
}
