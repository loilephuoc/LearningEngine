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
import vn.loi.learning.application.contentpackaging.ConflictAwarePackageImporter
import vn.loi.learning.application.library.command.LibraryCommandService
import vn.loi.learning.application.library.query.LibraryQueryService
import vn.loi.learning.application.topic.TopicQueryService
import vn.loi.learning.domain.library.model.LibraryId

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
    val libraryCommand: LibraryCommandService? = null,
    val defaultLibraryId: LibraryId? = null,
    val conflictAwareImporter: ConflictAwarePackageImporter? = null,
    val uninstallContentPackage: vn.loi.learning.application.contentpackaging.UninstallContentPackageUseCase? = null,
    val knowledgeGraphQuery: KnowledgeGraphQueryService? = null,
    val saveKnowledgeGraph: SaveKnowledgeGraphUseCase? = null,
    val getKnowledgeGraph: GetKnowledgeGraphUseCase? = null,
    val installedLibraryGraphProjection: InstalledLibraryKnowledgeGraphProjection? = null,
    val domainLibraryRepository: vn.loi.learning.domain.library.repository.LibraryRepository? = null,
    val memoryStateRepository: vn.loi.learning.application.port.MemoryStateRepository? = null,
    val learningItemRepository: vn.loi.learning.application.port.LearningItemRepository? = null,
    val packageContentQuery: vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService? = null,
    val packageProgress: vn.loi.learning.application.packageprogress.PackageLearningProgressQueryService? = null,
    val packageLatestRatings: vn.loi.learning.application.packageprogress.PackageLatestRatingQueryService? = null,
    val studyHeaderStatistics: vn.loi.learning.application.packageprogress.StudyHeaderStatisticsQueryService? = null,
    val packageCatalog: vn.loi.learning.application.port.PackageCatalogRepository? = null,
    val contentPackageRepository: vn.loi.learning.application.port.ContentPackageRepository? = null,
    val contentLibraryRepository: vn.loi.learning.application.port.ContentLibraryRepository? = null,
    val contentRepository: vn.loi.learning.application.port.ContentRepository? = null,
    val installedPackageRepository: vn.loi.learning.domain.library.repository.InstalledPackageRepository? = null,
    val studySessionRepository: vn.loi.learning.application.port.StudySessionRepository? = null,
    val studyQueueRepository: vn.loi.learning.application.port.StudyQueueRepository? = null,
    val reviewEventRepository: vn.loi.learning.application.port.ReviewEventRepository? = null,
    val learningTrajectoryRepository: vn.loi.learning.application.port.LearningTrajectoryRepository? = null,
    val learningInsights: vn.loi.learning.application.learninginsight.GetLearningInsightUseCase? = null,
    val exportContentPackage: vn.loi.learning.application.contentpackaging.export.ExportContentPackageUseCase? = null,
    val packageBrowserQuery: vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserQueryService? = null,
    val contentBrowserEdit: vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService? = null,
    val transactionRunner: vn.loi.learning.application.port.TransactionRunner? = null,
    val recoveryOperationGate: vn.loi.learning.application.port.RecoveryOperationGate? = null,
    val lessonBrowser: vn.loi.learning.application.contentpackaging.browser.LessonBrowserQueryService? = null,
    val scopedStudy: vn.loi.learning.application.session.ScopedStudySessionService? = null,
    val packageVerifier: vn.loi.learning.application.contentpackaging.Opd3PackageVerifier? = null,
    val upgradeContentPackage: vn.loi.learning.application.contentpackaging.UpgradeContentPackageUseCase? = null,
    val partOfSpeechRegistry: vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry =
        vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry(),
    val completePackageImportLifecycle: vn.loi.learning.application.contentpackaging.CompletePackageImportLifecycleUseCase? = null,
    val activeStudySessionScopeReconciler: vn.loi.learning.application.session.ActiveStudySessionScopeReconciler? = null,
    val dailyStudyBudget: vn.loi.learning.application.study.DailyStudyBudgetQueryService? = null,
    val packageIntegrityChecker: vn.loi.learning.application.integrity.PackageIntegrityChecker? = null,
    val intermediatePublicTransportRepair: vn.loi.learning.application.integrity.ReconcileIntermediatePublicTransportOrphan? = null
)
