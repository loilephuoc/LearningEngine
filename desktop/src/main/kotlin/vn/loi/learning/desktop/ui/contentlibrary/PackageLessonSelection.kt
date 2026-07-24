package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Contract typed truyền ngữ cảnh bài học được chọn sang Learning Flow.
 */
data class PackageLessonSelection(
    val installedPackageId: InstalledPackageId,
    val lessonId: String,
    val packageName: String? = null,
    val lessonTitle: String? = null
)
