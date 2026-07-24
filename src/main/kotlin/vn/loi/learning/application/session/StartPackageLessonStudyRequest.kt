package vn.loi.learning.application.session

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Request typed khởi tạo việc học một bài học gắn liền với InstalledPackage xác định.
 */
data class StartPackageLessonStudyRequest(
    val installedPackageId: InstalledPackageId,
    val contentId: ContentId
)
