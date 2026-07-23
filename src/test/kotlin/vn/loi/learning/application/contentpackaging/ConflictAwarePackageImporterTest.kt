package vn.loi.learning.application.contentpackaging

import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
import vn.loi.learning.infrastructure.persistence.json.JsonInstalledPackageStore
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedInstalledPackageRepository

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

    private fun makeImporterBacked(
        repo: StoreBackedInstalledPackageRepository,
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
    // AC-1: Strict safe replacement ordering
    // ---------------------------------------------------------------------------

    @Test
    fun `candidate version strictly greater than existing returns SAFE_REPLACEMENT`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-1"), PkgId("pkg-a"), TopId("topic-a"), "1.0.0"))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-a"),
            candidateTopicId = TopId("topic-a"),
            candidateName = "Package A",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.SAFE_REPLACEMENT, decision.type)
        assertTrue(decision.isExecutable)
        assertTrue(decision.conflictReasons.isEmpty())
        assertEquals("1.0.0", decision.existingVersion)
    }

    @Test
    fun `candidate minor version greater than existing minor returns SAFE_REPLACEMENT`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-1"), PkgId("pkg-b"), TopId("topic-b"), "1.2.3"))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-b"),
            candidateTopicId = TopId("topic-b"),
            candidateName = "Package B",
            candidateVersion = "1.2.4",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.SAFE_REPLACEMENT, decision.type)
    }

    @Test
    fun `candidate version equal to existing but different PackageId is a new package`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-1"), PkgId("pkg-original"), TopId("topic-original"), "1.0.0"))

        // Different canonical PackageId AND TopicId — not related to existing
        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-different"),
            candidateTopicId = TopId("topic-different"),
            candidateName = "Package Different",
            candidateVersion = "1.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.NEW_PACKAGE, decision.type)
    }

    @Test
    fun `candidate version malformed returns INVALID_VERSION conflict with zero mutation`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-1"), PkgId("pkg-c"), TopId("topic-c"), "1.0.0"))
        val countBefore = repo.findAll().size

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-c"),
            candidateTopicId = TopId("topic-c"),
            candidateName = "Package C",
            candidateVersion = "not-a-version",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertTrue(decision.conflictReasons.contains(PackageImportConflictReason.INVALID_VERSION))
        assertFalse(decision.isExecutable)

        // Zero mutation on conflict
        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.ConflictDetected)
        assertEquals(countBefore, repo.findAll().size)
    }

    @Test
    fun `existing version malformed returns INVALID_VERSION conflict with zero mutation`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        // Legacy record with malformed version
        repo.save(installedPkg(InstId("inst-legacy"), PkgId("pkg-d"), TopId("topic-d"), "version-X"))
        val countBefore = repo.findAll().size

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-d"),
            candidateTopicId = TopId("topic-d"),
            candidateName = "Package D",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertTrue(decision.conflictReasons.contains(PackageImportConflictReason.INVALID_VERSION))
        assertFalse(decision.isExecutable)

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.ConflictDetected)
        assertEquals(countBefore, repo.findAll().size)
    }

    @Test
    fun `same version with different checksums returns SAME_VERSION_DIFFERENT_CONTENT not SAFE_REPLACEMENT`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-e"), PkgId("pkg-e"), TopId("topic-e"), "1.0.0", checksum = "sha256-abc"))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-e"),
            candidateTopicId = TopId("topic-e"),
            candidateName = "Package E",
            candidateVersion = "1.0.0",
            candidateChecksum = "sha256-xyz",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertTrue(decision.conflictReasons.contains(PackageImportConflictReason.SAME_VERSION_DIFFERENT_CONTENT))
        assertFalse(decision.isExecutable)
    }

    @Test
    fun `same version with missing checksum returns INSUFFICIENT_IDENTITY_EVIDENCE not SAFE_REPLACEMENT`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        // No checksum stored
        repo.save(installedPkg(InstId("inst-f"), PkgId("pkg-f"), TopId("topic-f"), "1.0.0", checksum = null))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-f"),
            candidateTopicId = TopId("topic-f"),
            candidateName = "Package F",
            candidateVersion = "1.0.0",
            candidateChecksum = null,
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertTrue(decision.conflictReasons.contains(PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE))
    }

    // ---------------------------------------------------------------------------
    // AC-2: Persistence round-trip and restart tests
    // ---------------------------------------------------------------------------

    @Test
    fun `checksum saved and loaded via real StoreBackedInstalledPackageRepository`() {
        val tmpDir = Files.createTempDirectory("lp004r1-persist").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val store = JsonInstalledPackageStore(storeFile.toPath())
            val repo = StoreBackedInstalledPackageRepository(store)

            val checksum = "sha256-round-trip-checksum"
            val pkgId = PkgId("pkg-persist")
            val topicId = TopId("topic-persist")
            val instId = InstId("inst-persist")

            val pkg = InstalledPackage.reconstitute(
                id = instId,
                libraryId = libId,
                packageId = pkgId,
                topicId = topicId,
                name = PackageName("Persist Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.parse("2026-01-01T00:00:00Z"),
                contentCount = 5,
                learningItemCount = 20,
                contentChecksum = checksum
            )
            repo.save(pkg)

            // Simulate restart: create a fresh repo instance from same file
            val reloadedRepo = StoreBackedInstalledPackageRepository(
                JsonInstalledPackageStore(storeFile.toPath())
            )
            val loaded = reloadedRepo.findById(instId)

            assertNotNull(loaded)
            assertEquals(checksum, loaded.contentChecksum)
            assertEquals(pkgId, loaded.packageId)
            assertEquals(topicId, loaded.topicId)
            assertEquals("1.0.0", loaded.version.value)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `legacy record without checksum loads with null contentChecksum`() {
        val tmpDir = Files.createTempDirectory("lp004r1-legacy").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")

            // Write a legacy JSON record without contentChecksum field
            storeFile.writeText(
                """
                {
                  "schemaVersion": 1,
                  "records": [
                    {
                      "id": "inst-legacy-1",
                      "libraryId": "lib-1",
                      "packageId": "pkg-legacy-1",
                      "topicId": "topic-legacy-1",
                      "name": "Legacy Package",
                      "version": "1.0.0",
                      "state": "ACTIVE",
                      "installedAt": "2026-01-01T00:00:00Z",
                      "contentCount": 10,
                      "learningItemCount": 30
                    }
                  ]
                }
                """.trimIndent()
            )

            val repo = StoreBackedInstalledPackageRepository(
                JsonInstalledPackageStore(storeFile.toPath())
            )

            val loaded = repo.findById(InstId("inst-legacy-1"))
            assertNotNull(loaded)
            assertNull(loaded.contentChecksum, "Legacy record should load with null contentChecksum")
            assertEquals("1.0.0", loaded.version.value)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `after restart re-import with same checksum is recognized as IDENTICAL`() {
        val tmpDir = Files.createTempDirectory("lp004r1-restart-identical").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val checksum = "sha256-restart-identical"
            val pkgId = PkgId("pkg-restart")
            val topicId = TopId("topic-restart")
            val instId = InstId("inst-restart")

            // Session 1: install package with checksum
            val repo1 = StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(storeFile.toPath()))
            val importer1 = makeImporterBacked(repo1)

            val installDecision = importer1.inspectCandidate(
                candidatePackageId = pkgId,
                candidateTopicId = topicId,
                candidateName = "Restart Package",
                candidateVersion = "1.0.0",
                candidateChecksum = checksum,
                libraryId = libId
            )
            assertEquals(ImportDecisionType.NEW_PACKAGE, installDecision.type)
            val installOutcome = importer1.executeImport(installDecision, libId, contentChecksum = checksum)
            assertTrue(installOutcome is PackageImportOutcome.NewPackageInstalled)

            // Session 2: simulate restart, re-import same package/checksum
            val repo2 = StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(storeFile.toPath()))
            val importer2 = makeImporterBacked(repo2)

            val reImportDecision = importer2.inspectCandidate(
                candidatePackageId = pkgId,
                candidateTopicId = topicId,
                candidateName = "Restart Package",
                candidateVersion = "1.0.0",
                candidateChecksum = checksum,
                libraryId = libId
            )
            assertEquals(ImportDecisionType.IDENTICAL_PACKAGE, reImportDecision.type,
                "After restart, same checksum should be recognized as IDENTICAL")
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `after restart same version with different checksum is recognized as CONFLICT`() {
        val tmpDir = Files.createTempDirectory("lp004r1-restart-conflict").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val originalChecksum = "sha256-original"
            val differentChecksum = "sha256-different-content"
            val pkgId = PkgId("pkg-restart-conflict")
            val topicId = TopId("topic-restart-conflict")
            val instId = InstId("inst-restart-conflict")

            // Session 1: install package with original checksum
            val repo1 = StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(storeFile.toPath()))
            val importer1 = makeImporterBacked(repo1)

            val installDecision = importer1.inspectCandidate(
                candidatePackageId = pkgId, candidateTopicId = topicId,
                candidateName = "Package", candidateVersion = "1.0.0",
                candidateChecksum = originalChecksum, libraryId = libId
            )
            importer1.executeImport(installDecision, libId, contentChecksum = originalChecksum)

            // Session 2: restart, import same version but different content
            val repo2 = StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(storeFile.toPath()))
            val importer2 = makeImporterBacked(repo2)

            val conflictDecision = importer2.inspectCandidate(
                candidatePackageId = pkgId, candidateTopicId = topicId,
                candidateName = "Package", candidateVersion = "1.0.0",
                candidateChecksum = differentChecksum, libraryId = libId
            )

            assertEquals(ImportDecisionType.CONFLICT, conflictDecision.type)
            assertTrue(conflictDecision.conflictReasons.contains(PackageImportConflictReason.SAME_VERSION_DIFFERENT_CONTENT))
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `checksum preserved exactly across save and load without modification`() {
        val tmpDir = Files.createTempDirectory("lp004r1-exact-checksum").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val exactChecksum = "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"

            val repo = StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(storeFile.toPath()))
            repo.save(
                InstalledPackage.reconstitute(
                    id = InstId("inst-exact"),
                    libraryId = libId,
                    packageId = PkgId("pkg-exact"),
                    topicId = TopId("topic-exact"),
                    name = PackageName("Exact Package"),
                    version = PackageVersion("1.0.0"),
                    state = PackageState.ACTIVE,
                    installedAt = Instant.parse("2026-06-01T00:00:00Z"),
                    contentCount = 1,
                    learningItemCount = 1,
                    contentChecksum = exactChecksum
                )
            )

            val reloaded = StoreBackedInstalledPackageRepository(
                JsonInstalledPackageStore(storeFile.toPath())
            ).findById(InstId("inst-exact"))

            assertNotNull(reloaded)
            assertEquals(exactChecksum, reloaded.contentChecksum)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `repository state consistent after restart - findByPackageId works`() {
        val tmpDir = Files.createTempDirectory("lp004r1-findby").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val pkgId = PkgId("pkg-findby")
            val checksum = "sha256-findby"

            val repo1 = StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(storeFile.toPath()))
            repo1.save(
                installedPkgForPersistence(
                    instId = InstId("inst-findby"),
                    pkgId = pkgId,
                    topicId = TopId("topic-findby"),
                    version = "1.0.0",
                    checksum = checksum
                )
            )

            val repo2 = StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(storeFile.toPath()))
            val found = repo2.findByPackageId(pkgId)
            assertNotNull(found)
            assertEquals(checksum, found.contentChecksum)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    // ---------------------------------------------------------------------------
    // AC-R7.x: Existing LP-004R identity and conflict tests (regression)
    // ---------------------------------------------------------------------------

    @Test
    fun `same package name but different canonical PackageId and TopicId is recognized as new package`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-1"), PkgId("pkg-original"), TopId("topic-original"), name = "Same Name"))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-totally-new"),
            candidateTopicId = TopId("topic-totally-new"),
            candidateName = "Same Name",
            candidateVersion = "1.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.NEW_PACKAGE, decision.type)
    }

    @Test
    fun `identical verdict requires matching checksums from both sides`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)
        val checksum = "sha256-identical-check"

        repo.save(installedPkg(InstId("inst-id"), PkgId("pkg-id"), TopId("topic-id"), "1.0.0", checksum = checksum))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-id"),
            candidateTopicId = TopId("topic-id"),
            candidateName = "ID Package",
            candidateVersion = "1.0.0",
            candidateChecksum = checksum,
            libraryId = libId
        )

        assertEquals(ImportDecisionType.IDENTICAL_PACKAGE, decision.type)

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.AlreadyInstalledIdentical)
    }

    @Test
    fun `repository inconsistency for identical decision returns technical failure`() {
        val repo = InMemoryInstalledPackageRepository()
        val decision = PackageImportDecision(
            type = ImportDecisionType.IDENTICAL_PACKAGE,
            candidatePackageId = PkgId("pkg-fake"),
            candidateTopicId = TopId("topic-fake"),
            candidateName = "Fake",
            candidateVersion = "1.0.0",
            existingInstalledPackageId = InstId("inst-missing"),
            existingPackageId = PkgId("pkg-fake")
        )

        val importer = ConflictAwarePackageImporter(
            inspector = PackageImportInspector(repo),
            installedPackageRepository = repo,
            transactionRunner = ImmediateTransactionRunner()
        )

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.TechnicalFailure)
    }

    @Test
    fun `ambiguous identity when PackageId and TopicId point to different records returns conflict`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-a"), PkgId("pkg-a"), TopId("topic-a")))
        repo.save(installedPkg(InstId("inst-b"), PkgId("pkg-b"), TopId("topic-b")))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-a"),
            candidateTopicId = TopId("topic-b"),
            candidateName = "Ambiguous",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertTrue(decision.conflictReasons.contains(PackageImportConflictReason.AMBIGUOUS_EXISTING_IDENTITY))
    }

    @Test
    fun `topic ID mismatch returns typed TOPIC_ID_MISMATCH conflict reason`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-t"), PkgId("pkg-t"), TopId("topic-original"), "1.0.0"))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-t"),
            candidateTopicId = TopId("topic-different"),
            candidateName = "Topic Package",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.CONFLICT, decision.type)
        assertEquals(listOf(PackageImportConflictReason.TOPIC_ID_MISMATCH), decision.conflictReasons)
    }

    @Test
    fun `downgrade version returns typed OLDER_VERSION conflict reason`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-v"), PkgId("pkg-v"), TopId("topic-v"), "2.0.0"))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-v"),
            candidateTopicId = TopId("topic-v"),
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
    fun `safe replacement preserves canonical TopicId in repository`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        val pkgId = PkgId("pkg-upgrade")
        val topicId = TopId("topic-canonical")
        val instId = InstId("inst-upgrade")

        repo.save(InstalledPackage.reconstitute(
            id = instId, libraryId = libId, packageId = pkgId, topicId = topicId,
            name = PackageName("Package"), version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE, installedAt = Instant.parse("2026-01-01T00:00:00Z"),
            contentCount = 10, learningItemCount = 30
        ))

        val decision = importer.inspectCandidate(
            candidatePackageId = pkgId, candidateTopicId = topicId,
            candidateName = "Package", candidateVersion = "2.0.0", libraryId = libId
        )

        assertEquals(ImportDecisionType.SAFE_REPLACEMENT, decision.type)

        val outcome = importer.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.ReplacementCompleted)
        assertEquals("topic-canonical", outcome.preservedTopicId)

        val updated = repo.findById(instId)
        assertNotNull(updated)
        assertEquals(topicId, updated.topicId)
        assertEquals("2.0.0", updated.version.value)
    }

    @Test
    fun `new package installed successfully and persisted in repository`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-new"),
            candidateTopicId = TopId("topic-new"),
            candidateName = "New Package",
            candidateVersion = "1.0.0",
            candidateChecksum = "sha256-new",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.NEW_PACKAGE, decision.type)

        val outcome = importer.executeImport(decision, libId, contentChecksum = "sha256-new")
        assertTrue(outcome is PackageImportOutcome.NewPackageInstalled)

        val installed = repo.findByPackageId(PkgId("pkg-new"))
        assertNotNull(installed)
        assertEquals("sha256-new", installed.contentChecksum)
    }

    @Test
    fun `transaction failure during replacement returns technical failure`() {
        val repo = InMemoryInstalledPackageRepository()
        val failingImporter = makeImporter(repo, FailingTransactionRunner())

        repo.save(installedPkg(InstId("inst-fail"), PkgId("pkg-fail"), TopId("topic-fail"), "1.0.0"))

        val decision = failingImporter.inspectCandidate(
            candidatePackageId = PkgId("pkg-fail"),
            candidateTopicId = TopId("topic-fail"),
            candidateName = "Package",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        assertEquals(ImportDecisionType.SAFE_REPLACEMENT, decision.type)
        val outcome = failingImporter.executeImport(decision, libId)
        assertTrue(outcome is PackageImportOutcome.TechnicalFailure)
    }

    @Test
    fun `consumer can switch on typed conflict reason without parsing any string`() {
        val repo = InMemoryInstalledPackageRepository()
        val importer = makeImporter(repo)

        repo.save(installedPkg(InstId("inst-sw"), PkgId("pkg-sw"), TopId("topic-sw"), "3.0.0"))

        val decision = importer.inspectCandidate(
            candidatePackageId = PkgId("pkg-sw"),
            candidateTopicId = TopId("topic-sw"),
            candidateName = "Package",
            candidateVersion = "2.0.0",
            libraryId = libId
        )

        val reason = decision.conflictReasons.first()
        val label = when (reason) {
            PackageImportConflictReason.OLDER_VERSION -> "older"
            PackageImportConflictReason.TOPIC_ID_MISMATCH -> "topic-mismatch"
            PackageImportConflictReason.AMBIGUOUS_EXISTING_IDENTITY -> "ambiguous"
            PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE -> "insufficient"
            PackageImportConflictReason.SAME_VERSION_DIFFERENT_CONTENT -> "same-version-diff-content"
            PackageImportConflictReason.INVALID_VERSION -> "invalid-version"
        }
        assertEquals("older", label)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun installedPkgForPersistence(
        instId: InstalledPackageId,
        pkgId: PackageId,
        topicId: TopicId,
        version: String = "1.0.0",
        checksum: String? = null
    ) = InstalledPackage.reconstitute(
        id = instId,
        libraryId = libId,
        packageId = pkgId,
        topicId = topicId,
        name = PackageName("Package"),
        version = PackageVersion(version),
        state = PackageState.ACTIVE,
        installedAt = Instant.parse("2026-01-01T00:00:00Z"),
        contentCount = 10,
        learningItemCount = 30,
        contentChecksum = checksum
    )

    private fun InstId(value: String) = InstalledPackageId(value)
    private fun PkgId(value: String) = PackageId(value)
    private fun TopId(value: String) = TopicId(value)
}
