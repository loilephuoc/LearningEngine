package vn.loi.learning.application.contentpackaging.browser

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

data class LessonBrowserSummary(val lesson: String, val group: String?, val section: String?, val contentIds: List<ContentId>, val itemCount: Int)

class LessonBrowserQueryService(private val browser: PackageContentBrowserQueryService) {
    fun query(packageId: InstalledPackageId, search: String = ""): List<LessonBrowserSummary> =
        browser.getBrowserItemsForPackage(packageId)
            .filter { search.isBlank() || it.lesson.contains(search.trim(), ignoreCase = true) }
            .groupBy { Triple(it.lesson, it.group, it.section) }
            .map { (key, items) -> LessonBrowserSummary(key.first, key.second, key.third, items.map { it.contentId }, items.size) }
            .sortedWith(compareBy({ it.group.orEmpty().lowercase() }, { it.section.orEmpty().lowercase() }, { it.lesson.lowercase() }))
}
