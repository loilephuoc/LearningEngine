package vn.loi.learning.infrastructure

import java.nio.file.Path
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.contentlibrary.ContentLibraryQueryService
import vn.loi.learning.application.contentlibrary.LibraryContentQueryService
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.learningdashboard.LearningDashboardQueryService
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService

/**
 * Các Application API dùng chung của ứng dụng.
 *
 * Mọi service trong context phải sử dụng chung các repository instance
 * được tạo bởi LearningApplicationFactory.
 */
data class LearningApplicationContext(
    val engine: LearningEngine,
    val dashboard: LearningDashboardQueryService,
    val statistics: StudyStatisticsQueryService,
    val reviewHistory: ReviewHistoryQueryService,
    val installedPackages: InstalledPackageQueryService,
    val contentLibraries: ContentLibraryQueryService,
    val libraryContents: LibraryContentQueryService,
    val packageImporter: (Path) -> PackageImportService
)