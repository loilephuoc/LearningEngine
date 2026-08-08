package vn.loi.learning.application.session

import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.content.model.ContentId

/** Rejects only active sessions whose persisted package, queue, or content ownership is stale. */
class ActiveStudySessionScopeReconciler(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueRepository,
    private val installedPackages: InstalledPackageRepository,
    private val packageContents: InstalledPackageContentQueryService,
    private val learningItems: LearningItemRepository,
    private val leaveActiveSession: (LearnerId, Moment) -> StudySession?
) {
    fun reconcile(learnerId: LearnerId, at: Moment): StudySession? {
        val active = sessions.findActiveByLearner(learnerId) ?: return null
        return if (isCompatible(active)) active else leaveActiveSession(learnerId, at)
    }

    fun isCompatible(session: StudySession): Boolean {
        // Legacy/unscoped sessions have no package identity to contradict; existing recovery owns them.
        val packageId = session.installedPackageId ?: return true
        val installed = installedPackages.findById(packageId)
            ?.takeIf { it.state == PackageState.ACTIVE } ?: return false
        val queue = queues.findBySessionId(session.id) ?: return false
        if (installed.id != packageId || queue.isEmpty) return false
        val ownedContentIds = runCatching { packageContents.getContentsForPackage(packageId) }
            .getOrNull()?.mapTo(hashSetOf()) { ContentId(it.id) } ?: return false
        if (session.includedContentIds.isNotEmpty() && !ownedContentIds.containsAll(session.includedContentIds)) return false
        val queueContentIds = queue.learningItemIds.map { itemId ->
            queue.itemContentIds[itemId] ?: learningItems.findById(itemId)?.contentId ?: return false
        }
        return ownedContentIds.containsAll(queueContentIds)
    }
}
