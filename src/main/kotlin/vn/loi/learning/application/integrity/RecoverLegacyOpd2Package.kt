package vn.loi.learning.application.integrity

import java.time.Instant
import vn.loi.learning.application.port.*
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

enum class Opd2RecoveryStatus { RECOVERED, ALREADY_RECOVERED, BACKUP_REQUIRED, PRECONDITION_FAILED }

data class Opd2RecoveryResult(
    val status: Opd2RecoveryStatus,
    val reason: String? = null,
    val publishedMediaCount: Int = 0,
    val integrity: PackageIntegrityReport? = null
)

/** Exact-identity lifecycle completion for the audited legacy OPD2 graph. */
class RecoverLegacyOpd2Package(
    private val contents: ContentRepository,
    private val items: LearningItemRepository,
    private val contentPackages: ContentPackageRepository,
    private val contentLibraries: ContentLibraryRepository,
    private val installedPackages: InstalledPackageRepository,
    private val libraries: LibraryRepository,
    private val libraryId: LibraryId,
    private val memoryStates: MemoryStateRepository,
    private val reviewEvents: ReviewEventRepository,
    private val trajectories: LearningTrajectoryRepository,
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueRepository,
    private val transaction: TransactionRunner,
    private val media: Opd2RecoveryMediaPort,
    private val integrityCheck: (InstalledPackageId) -> PackageIntegrityReport,
    private val clock: () -> Instant = Instant::now,
    private val failureHook: (String) -> Unit = {}
) {
    fun execute(backupVerified: Boolean): Opd2RecoveryResult {
        if (!backupVerified) return Opd2RecoveryResult(Opd2RecoveryStatus.BACKUP_REQUIRED)
        val validation = validate(allowRecovered = true)
        if (validation == ALREADY) return Opd2RecoveryResult(Opd2RecoveryStatus.ALREADY_RECOVERED)
        if (validation != null) return Opd2RecoveryResult(Opd2RecoveryStatus.PRECONDITION_FAILED, validation)

        val expectedReferences = ownedContents().flatMapTo(linkedSetOf()) { content ->
            listOfNotNull(
                content.media.image,
                content.media.primaryAudio,
                content.media.translatedAudio,
                content.media.exampleAudio,
                content.media.exampleTranslatedAudio
            )
        }
        val prepared = media.prepare(expectedReferences)
        try {
            require(prepared.recoverableReferenceCount == EXPECTED_RECOVERABLE_MEDIA_REFERENCES) {
                "Expected $EXPECTED_RECOVERABLE_MEDIA_REFERENCES recoverable media references."
            }
            require(prepared.missingReferences == EXPECTED_MISSING_MEDIA) {
                "The exact optional-missing media set changed."
            }
            val historyBefore = historySnapshot()
            val libraryBefore = requireNotNull(libraries.findById(libraryId))
            val report = try {
                transaction.runInTransaction {
                    check(validate(allowRecovered = false) == null) {
                        "OPD2 recovery preconditions changed before transaction."
                    }
                    prepared.publish()
                    prepared.verifyPublished()
                    val contentPackage = requireNotNull(contentPackages.findById(PACKAGE))
                    val installed = InstalledPackage.reconstitute(
                        id = INSTALLED,
                        libraryId = libraryId,
                        packageId = PACKAGE,
                        topicId = TopicId.deriveForLegacyPackage(contentPackage.name, contentPackage.format),
                        name = PackageName(contentPackage.name),
                        version = PackageVersion(contentPackage.version),
                        state = PackageState.ACTIVE,
                        installedAt = clock(),
                        contentCount = EXPECTED_CONTENTS,
                        learningItemCount = EXPECTED_ITEMS
                    )
                    failureHook("installed-write")
                    installedPackages.save(installed)
                    val library = requireNotNull(libraries.findById(libraryId))
                    failureHook("library-write")
                    libraries.save(library.registerEntry(INSTALLED, PACKAGE, installed.installedAt))
                    check(historySnapshot() == historyBefore) { "OPD2 history changed during recovery." }
                    integrityCheck(INSTALLED).also {
                        check(it.summary.errors == 0) { "OPD2 post-recovery integrity contains structural errors." }
                    }
                    .also { failureHook("transaction-commit") }
                }
            } catch (failure: Throwable) {
                prepared.rollbackPublished()
                // Persisted adapters roll back transaction members. The explicit compensation also
                // preserves the command's all-or-nothing contract for non-transactional adapters.
                if (libraries.findById(libraryId) != libraryBefore) libraries.save(libraryBefore)
                if (installedPackages.findById(INSTALLED) != null) installedPackages.delete(INSTALLED)
                throw failure
            }
            prepared.complete()
            return Opd2RecoveryResult(
                Opd2RecoveryStatus.RECOVERED,
                publishedMediaCount = prepared.publishedFileCount,
                integrity = report
            )
        } finally {
            prepared.close()
        }
    }

    private fun validate(allowRecovered: Boolean): String? {
        val contentPackage = contentPackages.findById(PACKAGE) ?: return "OPD2 ContentPackage is missing."
        if (contentPackage.libraryIds != setOf(CONTENT_LIBRARY)) return "OPD2 package/library graph changed."
        val contentLibrary = contentLibraries.findById(CONTENT_LIBRARY) ?: return "OPD2 ContentLibrary is missing."
        if (contentLibrary.contentIds.size != EXPECTED_CONTENTS) return "OPD2 membership count changed."
        if (contentLibrary.contentIds.toSet().size != EXPECTED_CONTENTS) return "OPD2 membership contains duplicate identities."
        val ownedContents = contents.findByIds(contentLibrary.contentIds)
        if (ownedContents.map { it.id }.toSet() != contentLibrary.contentIds.toSet()) return "OPD2 Content identities changed."
        val ownedItems = items.findByContentIds(contentLibrary.contentIds)
        if (ownedItems.size != EXPECTED_ITEMS || ownedItems.map { it.id }.toSet().size != EXPECTED_ITEMS) {
            return "OPD2 LearningItem identities changed."
        }
        if (ownedItems.any { !it.isEnabled } || ownedItems.groupingBy { it.mode }.eachCount() != EXPECTED_MODE_COUNTS) {
            return "OPD2 enabled mode distribution changed."
        }
        val installed = installedPackages.findById(INSTALLED)
        val library = libraries.findById(libraryId) ?: return "Canonical Library is missing."
        val entry = library.entries.firstOrNull { it.installedPackageId == INSTALLED }
        if (installed != null || entry != null) {
            if (allowRecovered && installed != null && entry?.packageId == PACKAGE &&
                installed.packageId == PACKAGE && installed.contentCount == EXPECTED_CONTENTS &&
                installed.learningItemCount == EXPECTED_ITEMS
            ) return ALREADY
            return "OPD2 lifecycle identity appeared or is only partially registered."
        }
        if (installedPackages.findByPackageId(PACKAGE) != null) return "Another InstalledPackage already owns OPD2."

        val itemIds = ownedItems.mapTo(hashSetOf()) { it.id }
        val memories = memoryStates.findAll().filter { it.learningItemId in itemIds }
        if (memories.map { it.learningItemId }.toSet() != EXPECTED_MEMORY_ITEMS || memories.size != EXPECTED_MEMORY_ITEMS.size) {
            return "The exact five OPD2 MemoryStates changed."
        }
        val events = reviewEvents.findAll().filter { it.learningItemId in itemIds }
        if (events.map { it.id.toString() }.toSet() != EXPECTED_EVENT_IDS || events.size != EXPECTED_EVENT_IDS.size) {
            return "The exact eight OPD2 ReviewEvents changed."
        }
        if (trajectories.findAll().any { it.trajectory.contentId in contentLibrary.contentIds }) return "OPD2 LearningTrajectory appeared."
        if (sessions.findAll().any { session ->
                session.includedContentIds.any(contentLibrary.contentIds::contains) ||
                    session.reviewedItemIds.any(itemIds::contains) || session.currentLearningItemId in itemIds ||
                    session.pendingReview?.learningItemId in itemIds || session.undoableReview?.learningItemId in itemIds
            }) return "OPD2 StudySession reference appeared."
        if (queues.findAll().any { queue -> queue.learningItemIds.any(itemIds::contains) }) return "OPD2 StudyQueue reference appeared."
        return null
    }

    private fun ownedContents(): List<Content> =
        contents.findByIds(requireNotNull(contentLibraries.findById(CONTENT_LIBRARY)).contentIds)

    private fun historySnapshot(): Pair<List<Any>, List<Any>> {
        val itemIds = items.findByContentIds(requireNotNull(contentLibraries.findById(CONTENT_LIBRARY)).contentIds)
            .mapTo(hashSetOf()) { it.id }
        return memoryStates.findAll().filter { it.learningItemId in itemIds }.sortedBy { it.learningItemId.value } to
            reviewEvents.findAll().filter { it.learningItemId in itemIds }.sortedWith(compareBy({ it.learningItemId.value }, { it.reviewedAt.epochMillis }))
    }

    companion object {
        val PACKAGE = PackageId("package-42c94bdebe9f13542e76da69")
        val CONTENT_LIBRARY = ContentLibraryId("legacy-library-92da29d7dad56f8d48386f4c")
        val INSTALLED = InstalledPackageId("inst-package-42c94bdebe9f13542e76da69")
        const val EXPECTED_CONTENTS = 2425
        const val EXPECTED_ITEMS = 12125
        const val EXPECTED_RECOVERABLE_MEDIA_REFERENCES = 12109
        const val EXPECTED_PUBLISHED_MEDIA_FILES = 12071
        val OPD2_MODES = setOf(
            LearningMode.DICTATION,
            LearningMode.LISTENING_RECOGNITION,
            LearningMode.MEANING_RECALL,
            LearningMode.MEANING_RECOGNITION,
            LearningMode.SHADOWING
        )
        val EXPECTED_MODE_COUNTS = OPD2_MODES.associateWith { EXPECTED_CONTENTS }
        val EXPECTED_MEMORY_ITEMS = setOf(
            LearningItemId("legacy-content-004343ba9598b49a4d86c0bb-dictation"),
            LearningItemId("legacy-content-004343ba9598b49a4d86c0bb-listening-recognition"),
            LearningItemId("legacy-content-006c40c30fc8d73f2c0c0284-dictation"),
            LearningItemId("legacy-content-00751ebedbf5b05cef6556d4-dictation"),
            LearningItemId("legacy-content-008a82df23ad25a0458183c8-dictation")
        )
        val EXPECTED_EVENT_IDS = setOf(
            "77b51311-bd1a-4695-ae6a-52d34beff9ce", "a9b4e3db-ee30-4fd1-9235-0511a7914779",
            "1360f1a3-a57d-4d22-9da0-27e42cf13fe6", "825daa73-7ca3-404e-9133-df96fe178af3",
            "d4755006-026d-4676-8ec7-2aafec613a70", "4e4311dc-7ab1-47e8-80b9-d4c5ab96a75a",
            "83ecc304-2608-4da4-bbaa-b4cb0c02d676", "6990b4b9-e9ab-4971-86e1-268b349bc394"
        )
        val EXPECTED_MISSING_MEDIA = setOf(
            "1743864287268_vi.mp3", "1743864287441.mp3", "1743864287469_vi.mp3",
            "1743864287474_vi.mp3", "1743864288119_vi.mp3", "1743864288189_vi.mp3",
            "1743864288525_vi.mp3", "1743864288531_vi.mp3", "1743864288716_vi.mp3",
            "0ef8bce9dfdfe2d9c5395514205ed783.mp3", "1743864289036_vi.mp3",
            "1743864289288_vi.mp3", "1744286961712_vi.mp3", "1743864289040_vi.mp3",
            "1743864288792_vi.mp3"
        )
        private const val ALREADY = "ALREADY"
    }
}
