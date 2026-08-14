package vn.loi.learning.application.integrity

import vn.loi.learning.application.port.*
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

enum class IntermediateOrphanRepairStatus { REPAIRED, ALREADY_RECONCILED, PRECONDITION_FAILED, BACKUP_REQUIRED }

data class IntermediateOrphanRepairResult(
    val status: IntermediateOrphanRepairStatus,
    val reason: String? = null
)

/** One-off, exact-identity reconciliation for the audited Intermediate public-transport orphan. */
class ReconcileIntermediatePublicTransportOrphan(
    private val contents: ContentRepository,
    private val items: LearningItemRepository,
    private val installedPackages: InstalledPackageRepository,
    private val contentPackages: ContentPackageRepository,
    private val contentLibraries: ContentLibraryRepository,
    private val memoryStates: MemoryStateRepository,
    private val reviewEvents: ReviewEventRepository,
    private val trajectories: LearningTrajectoryRepository,
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueRepository,
    private val transaction: TransactionRunner
) {
    fun execute(backupVerified: Boolean): IntermediateOrphanRepairResult {
        if (!backupVerified) return IntermediateOrphanRepairResult(IntermediateOrphanRepairStatus.BACKUP_REQUIRED)
        val validation = validate()
        if (validation == ALREADY) return IntermediateOrphanRepairResult(IntermediateOrphanRepairStatus.ALREADY_RECONCILED)
        if (validation != null) return IntermediateOrphanRepairResult(IntermediateOrphanRepairStatus.PRECONDITION_FAILED, validation)

        transaction.runInTransaction {
            check(validate() == null) { "Repair preconditions changed before transaction." }
            EXPECTED_ITEMS.forEach(items::deleteById)
            contents.deleteById(ORPHAN_B)
            val current = requireNotNull(installedPackages.findById(INSTALLED))
            installedPackages.save(current.withCounts(POST_CONTENT_COUNT, POST_ITEM_COUNT))
            check(validate() == ALREADY) { "Targeted repair postconditions failed." }
        }
        return IntermediateOrphanRepairResult(IntermediateOrphanRepairStatus.REPAIRED)
    }

    private fun validate(): String? {
        val allContents = contents.findAll()
        val aCount = allContents.count { it.id == OWNED_A }
        val bCount = allContents.count { it.id == ORPHAN_B }
        val packageRecord = installedPackages.findById(INSTALLED) ?: return "Intermediate InstalledPackage is missing."
        val contentPackage = contentPackages.findById(PACKAGE) ?: return "Intermediate ContentPackage is missing."
        if (packageRecord.packageId != PACKAGE) return "InstalledPackage points to another ContentPackage."
        if (contentPackage.libraryIds != setOf(LIBRARY)) return "Intermediate package library graph changed."
        val library = contentLibraries.findById(LIBRARY) ?: return "Intermediate ContentLibrary is missing."
        val ownersA = contentLibraries.findAll().count { OWNED_A in it.contentIds }
        val ownersB = contentLibraries.findAll().count { ORPHAN_B in it.contentIds }
        val canonicalIds = contentPackage.libraryIds.flatMapTo(linkedSetOf()) {
            contentLibraries.findById(it)?.contentIds.orEmpty()
        }
        val canonicalContentCount = contents.findByIds(canonicalIds).distinctBy { it.id }.size
        val canonicalItemCount = items.findByContentIds(canonicalIds).distinctBy { it.id }.size
        val bItems = items.findByContentId(ORPHAN_B)

        if (bCount == 0 && bItems.isEmpty() && aCount == 1 && ownersA == 1 && ownersB == 0 &&
            packageRecord.contentCount == POST_CONTENT_COUNT && packageRecord.learningItemCount == POST_ITEM_COUNT &&
            canonicalContentCount == POST_CONTENT_COUNT && canonicalItemCount == POST_ITEM_COUNT
        ) return ALREADY

        if (aCount != 1) return "Owned Content A must exist exactly once."
        if (bCount != 1) return "Orphan Content B must exist exactly once."
        if (ownersA != 1 || OWNED_A !in library.contentIds) return "Owned Content A membership changed."
        if (ownersB != 0) return "Orphan Content B gained library membership."
        if (packageRecord.contentCount != PRE_CONTENT_COUNT || packageRecord.learningItemCount != PRE_ITEM_COUNT) return "Stored package counts changed."
        if (canonicalContentCount != POST_CONTENT_COUNT || canonicalItemCount != POST_ITEM_COUNT) return "Canonical package counts changed."
        if (bItems.map { it.id }.toSet() != EXPECTED_ITEMS) return "Exact orphan LearningItem set changed."
        if (bItems.any { !it.isEnabled || it.contentId != ORPHAN_B || EXPECTED_MODES[it.id] != it.mode }) return "Orphan LearningItem fields changed."
        if (memoryStates.findAll().any { it.learningItemId in EXPECTED_ITEMS }) return "MemoryState exists for orphan items."
        if (reviewEvents.findAll().any { it.learningItemId in EXPECTED_ITEMS }) return "ReviewEvent exists for orphan items."
        if (trajectories.findAll().any { it.trajectory.contentId == ORPHAN_B }) return "LearningTrajectory exists for orphan Content."
        if (sessions.findAll().any { session ->
                ORPHAN_B in session.includedContentIds || ORPHAN_B in session.reviewedContentIds || ORPHAN_B in session.introducedContentIds ||
                    session.currentLearningItemId in EXPECTED_ITEMS || session.pendingReview?.learningItemId in EXPECTED_ITEMS ||
                    session.undoableReview?.learningItemId in EXPECTED_ITEMS || session.reviewedItemIds.any(EXPECTED_ITEMS::contains)
            }) return "StudySession references orphan data."
        if (queues.findAll().any { queue ->
                queue.learningItemIds.any(EXPECTED_ITEMS::contains) || queue.fixedPracticeMembership.any(EXPECTED_ITEMS::contains) ||
                    queue.itemContentIds.values.any { it == ORPHAN_B }
            }) return "StudyQueue references orphan data."
        return null
    }

    private fun InstalledPackage.withCounts(contentCount: Int, learningItemCount: Int) = InstalledPackage.reconstitute(
        id, libraryId, packageId, topicId, name, version, state, installedAt, contentCount, learningItemCount, contentChecksum
    )

    companion object {
        val OWNED_A = ContentId("legacy-content-0c3c967dbabc7a9383d16728")
        val ORPHAN_B = ContentId("legacy-content-7543f68867e46a9907ca80e5")
        val INSTALLED = InstalledPackageId("inst-package-22f82134f24d0aeb6c5b9e95")
        val PACKAGE = PackageId("package-22f82134f24d0aeb6c5b9e95")
        val LIBRARY = ContentLibraryId("legacy-library-11f33b5e0f071ad4f5378ed0")
        const val PRE_CONTENT_COUNT = 2256
        const val PRE_ITEM_COUNT = 11280
        const val POST_CONTENT_COUNT = 2255
        const val POST_ITEM_COUNT = 11275
        private const val ALREADY = "ALREADY"
        val EXPECTED_MODES = linkedMapOf(
            LearningItemId("${ORPHAN_B.value}-dictation") to LearningMode.DICTATION,
            LearningItemId("${ORPHAN_B.value}-listening-recognition") to LearningMode.LISTENING_RECOGNITION,
            LearningItemId("${ORPHAN_B.value}-meaning-recall") to LearningMode.MEANING_RECALL,
            LearningItemId("${ORPHAN_B.value}-meaning-recognition") to LearningMode.MEANING_RECOGNITION,
            LearningItemId("${ORPHAN_B.value}-shadowing") to LearningMode.SHADOWING
        )
        val EXPECTED_ITEMS: Set<LearningItemId> = EXPECTED_MODES.keys
    }
}
