package vn.loi.learning.desktop.ui.shell

import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.desktop.ui.contentlibrary.PackageLessonSelection
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.study.StudyViewModel

/**
 * Owns the presentation boundary between selecting a lesson and opening Study.
 *
 * Navigation occurs only when the study request produced a real active session.
 * StudyViewModel remains responsible for converting failures into recoverable UI
 * state.
 */
class LessonStudyNavigationCoordinator(
    private val studyViewModel: StudyViewModel,
    private val navigationState: NavigationState
) {

    fun startLessonStudy(contentId: String) {
        studyViewModel.startLessonStudy(contentId) {
            if (studyViewModel.uiState.hasActiveSession) {
                navigationState.navigateTo(
                    NavigationDestination.STUDY
                )
            }
        }
    }

    fun startLessonStudy(request: StartPackageLessonStudyRequest) {
        studyViewModel.startLessonStudy(request) {
            if (studyViewModel.uiState.hasActiveSession) {
                navigationState.navigateTo(
                    NavigationDestination.STUDY
                )
            }
        }
    }

    fun startLessonStudy(selection: PackageLessonSelection) {
        val pkgId = selection.installedPackageId ?: return
        startLessonStudy(
            StartPackageLessonStudyRequest(
                installedPackageId = pkgId,
                contentId = ContentId(selection.lessonId)
            )
        )
    }
}
