package vn.loi.learning.infrastructure

import java.nio.file.Path
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.contentlibrary.AttachPackageToLibraryCollectionUseCase
import vn.loi.learning.application.contentlibrary.ContentLibraryQueryService
import vn.loi.learning.application.contentlibrary.CreateLibraryCollectionUseCase
import vn.loi.learning.application.contentlibrary.DeleteLibraryCollectionUseCase
import vn.loi.learning.application.contentlibrary.DetachPackageFromLibraryCollectionUseCase
import vn.loi.learning.application.contentlibrary.LibraryCollectionQueryService
import vn.loi.learning.application.contentlibrary.LibraryContentQueryService
import vn.loi.learning.application.contentlibrary.RenameLibraryCollectionUseCase
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.contentpackaging.PackageImportProgressListener
import vn.loi.learning.application.knowledge.GetKnowledgeGraphUseCase
import vn.loi.learning.application.knowledge.InstalledLibraryKnowledgeGraphProjection
import vn.loi.learning.application.knowledge.KnowledgeGraphQueryService
import vn.loi.learning.application.knowledge.SaveKnowledgeGraphUseCase
import vn.loi.learning.application.learningdashboard.LearningDashboardQueryService
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService
import vn.loi.learning.application.session.StudyQueueService
import vn.loi.learning.application.library.query.LibraryQueryService
import vn.loi.learning.application.topic.TopicQueryService

/**
 * Các Application API dùng chung của ứng dụng.
 *
 * Mọi service trong context phải sử dụng chung các repository instance
 * được tạo bởi LearningApplicationFactory.
 */
data class LearningApplicationContext(
    val engine: LearningEngine,
    val studyQueue: StudyQueueService,
    val dashboard: LearningDashboardQueryService,
    val statistics: StudyStatisticsQueryService,
    val reviewHistory: ReviewHistoryQueryService,
    val installedPackages: InstalledPackageQueryService,
    val contentLibraries: ContentLibraryQueryService,
    val libraryContents: LibraryContentQueryService,
    val libraryCollections: LibraryCollectionQueryService,
    val createLibraryCollection: CreateLibraryCollectionUseCase,
    val renameLibraryCollection: RenameLibraryCollectionUseCase,
    val attachPackageToLibraryCollection:
    AttachPackageToLibraryCollectionUseCase,
    val detachPackageFromLibraryCollection:
    DetachPackageFromLibraryCollectionUseCase,
    val deleteLibraryCollection: DeleteLibraryCollectionUseCase,
    val packageImporter: (Path) -> PackageImportService,
    val packageImporterWithProgress:
    (Path, PackageImportProgressListener) -> PackageImportService =
        { path, _ -> packageImporter(path) },
    val topics: TopicQueryService? = null,
    val libraryQuery: LibraryQueryService? = null,
    val defaultLibraryId: vn.loi.learning.domain.library.model.LibraryId? = null,
    val conflictAwareImporter: vn.loi.learning.application.contentpackaging.ConflictAwarePackageImporter? = null,
    val knowledgeGraphQuery: KnowledgeGraphQueryService? = null,
    val saveKnowledgeGraph: SaveKnowledgeGraphUseCase? = null,
    val getKnowledgeGraph: GetKnowledgeGraphUseCase? = null,
    val installedLibraryGraphProjection: InstalledLibraryKnowledgeGraphProjection? = null
)
