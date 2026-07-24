package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Facade cho Lesson Browser.
 *
 * Chỉ chuyển Application DTO sang Presentation model.
 * Không chứa business logic.
 */
class LessonBrowserFacade(
    private val applicationContext: LearningApplicationContext
) {

    fun load(
        libraryId: String,
        libraryName: String
    ): LessonBrowserUiState {
        val lessons =
            applicationContext
                .libraryContents
                .query(
                    ContentLibraryId(
                        libraryId
                    )
                )
                .map { content ->
                    LessonBrowserItem(
                        id = content.id,
                        title = content.title,
                        type = content.type,
                        group = content.group,
                        section = content.section,
                        lesson = content.lesson,
                        primaryText =
                            content.primaryText,
                        translatedText =
                            content.translatedText,
                        learningItemCount =
                            content.learningItemCount,
                        imagePath = content.imagePath
                    )
                }

        return LessonBrowserUiState(
            libraryId = libraryId,
            libraryName = libraryName,
            lessons = lessons
        )
    }

    fun loadForPackage(
        installedPackageId: vn.loi.learning.domain.library.model.InstalledPackageId,
        packageName: String
    ): LessonBrowserUiState {
        val defaultLibId = applicationContext.defaultLibraryId
        val libRepo = applicationContext.domainLibraryRepository
        val libQuery = applicationContext.libraryQuery

        val summary = if (libQuery != null && defaultLibId != null) {
            libQuery.getInstalledPackages(defaultLibId).firstOrNull { it.id == installedPackageId }
        } else null

        val allInstalled = applicationContext.installedPackages.query()
        val pkgItem = if (summary != null) {
            allInstalled.firstOrNull { it.id == summary.packageId.value }
                ?: applicationContext.installedPackages.findById(summary.packageId.value)
                ?: allInstalled.firstOrNull { it.id == installedPackageId.value }
        } else {
            allInstalled.firstOrNull { it.id == installedPackageId.value }
        }

        val contentLibraryIds = mutableSetOf<ContentLibraryId>()
        if (pkgItem != null && pkgItem.libraryIds.isNotEmpty()) {
            contentLibraryIds.addAll(pkgItem.libraryIds.map { ContentLibraryId(it) })
        }
        contentLibraryIds.add(ContentLibraryId(installedPackageId.value))
        if (pkgItem != null) {
            contentLibraryIds.add(ContentLibraryId(pkgItem.id))
        }
        if (summary != null) {
            contentLibraryIds.add(ContentLibraryId(summary.packageId.value))
        }

        val rawItems = applicationContext.libraryContents.queryForLibraries(contentLibraryIds)
        if (rawItems.isEmpty() && pkgItem == null && summary == null) {
            throw IllegalArgumentException("Package with id '${installedPackageId.value}' not found.")
        }

        val lessons = rawItems.map { content ->
            LessonBrowserItem(
                id = content.id,
                title = content.title,
                type = content.type,
                group = content.group,
                section = content.section,
                lesson = content.lesson,
                primaryText = content.primaryText,
                translatedText = content.translatedText,
                learningItemCount = content.learningItemCount,
                imagePath = content.imagePath
            )
        }

        return LessonBrowserUiState(
            libraryId = installedPackageId.value,
            libraryName = packageName,
            lessons = lessons
        )
    }
}
