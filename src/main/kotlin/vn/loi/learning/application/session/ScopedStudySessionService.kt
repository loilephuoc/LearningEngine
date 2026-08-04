package vn.loi.learning.application.session

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession

sealed interface StudyContentScope {
    data class Package(val packageId: InstalledPackageId) : StudyContentScope
    data class Lesson(val packageId: InstalledPackageId, val lesson: String) : StudyContentScope
    data class Selection(val packageId: InstalledPackageId, val contentIds: Set<ContentId>) : StudyContentScope
    data class Collection(val collectionId: CollectionId) : StudyContentScope
}

data class StartScopedStudyRequest(val sessionId: SessionId, val learnerId: LearnerId, val startedAt: Moment, val scope: StudyContentScope)

/** Resolves canonical content membership only; existing StartStudySession remains queue authority. */
class ScopedStudySessionService(
    private val packageItems: (InstalledPackageId) -> List<PackageContentBrowserItem>,
    private val collectionPackages: (CollectionId) -> List<InstalledPackageId>?,
    private val startSession: (StartStudySessionCommand) -> StudySession
) {
    fun execute(request: StartScopedStudyRequest): StudySession {
        val (contentIds, packageId) = when (val scope = request.scope) {
            is StudyContentScope.Package -> packageItems(scope.packageId).map { it.contentId }.toSet() to scope.packageId
            is StudyContentScope.Lesson -> packageItems(scope.packageId).filter { it.lesson == scope.lesson }.map { it.contentId }.toSet() to scope.packageId
            is StudyContentScope.Selection -> {
                val owned = packageItems(scope.packageId).map { it.contentId }.toSet()
                require(scope.contentIds.isNotEmpty() && scope.contentIds.all { it in owned }) { "Selected content must belong to the package." }
                scope.contentIds to scope.packageId
            }
            is StudyContentScope.Collection -> {
                val packages = requireNotNull(collectionPackages(scope.collectionId)) { "Collection is unavailable." }
                packages.flatMap(packageItems).map { it.contentId }.toSet() to null
            }
        }
        require(contentIds.isNotEmpty()) { "Study scope contains no content." }
        return startSession(StartStudySessionCommand(request.sessionId, request.learnerId, request.startedAt, includedContentIds = contentIds, installedPackageId = packageId))
    }
}
