package vn.loi.learning.desktop.notification

import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.contentpackaging.InstalledPackageItem
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.study.ContentLearningState
import vn.loi.learning.application.study.ContentLearningStateQueryService
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState

fun interface DesktopInstalledPackageReadSource {
    fun findById(installedPackageId: InstalledPackageId): InstalledPackageItem?
}

fun interface DesktopPackageContentReadSource {
    fun findContentIds(installedPackageId: InstalledPackageId): Set<ContentId>?
}

fun interface DesktopContentReadSource {
    fun findByIds(contentIds: Collection<ContentId>): List<Content>
}

fun interface DesktopLearningItemReadSource {
    fun findByContentIds(contentIds: Set<ContentId>): List<LearningItem>
}

fun interface DesktopMemoryStateReadSource {
    fun findAll(learnerId: LearnerId): List<MemoryState>
}

fun interface DesktopContentLearningStateReadSource {
    fun resolveAll(learnerId: LearnerId, contentIds: Set<ContentId>): Map<ContentId, ContentLearningState>
}

/** Adapters keep mutation-capable repository contracts outside the selector itself. */
class DesktopVocabularyReminderReadSources(
    installedPackages: InstalledPackageQueryService,
    packageContents: InstalledPackageContentQueryService,
    contents: ContentRepository,
    learningItems: LearningItemRepository,
    memoryStates: MemoryStateQuery,
    contentLearningStates: ContentLearningStateQueryService
) {
    val installedPackage = DesktopInstalledPackageReadSource { installedPackages.findById(it.value) }
    val packageContent = DesktopPackageContentReadSource { id ->
        runCatching { packageContents.getContentIdsForPackage(id) }.getOrNull()
    }
    val content = DesktopContentReadSource(contents::findByIds)
    val learningItem = DesktopLearningItemReadSource(learningItems::findByContentIds)
    val memoryState = DesktopMemoryStateReadSource(memoryStates::findAll)
    val contentLearningState = DesktopContentLearningStateReadSource(contentLearningStates::resolveAll)
}
