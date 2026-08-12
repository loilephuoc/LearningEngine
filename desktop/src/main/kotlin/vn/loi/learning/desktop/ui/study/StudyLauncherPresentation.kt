package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopContentWidthClass
import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopResponsivePolicyResolver

data class StudyLauncherLayout(
    val widthClass: DesktopContentWidthClass,
    val primaryColumns: Int,
    val quickReviewColumns: Int
)

fun resolveStudyLauncherLayout(availableWidthDp: Int): StudyLauncherLayout {
    val widthClass = DesktopResponsivePolicyResolver.resolve(availableWidthDp).widthClass
    return when (widthClass) {
        DesktopContentWidthClass.NARROW -> StudyLauncherLayout(widthClass, 1, 2)
        DesktopContentWidthClass.MEDIUM -> StudyLauncherLayout(widthClass, 2, 2)
        DesktopContentWidthClass.WIDE -> StudyLauncherLayout(widthClass, 2, 3)
    }
}

data class StudyLauncherSections(
    val primary: List<StudyLearningActionPresentation>,
    val quickReview: List<StudyLearningActionPresentation>,
    val navigation: List<StudyLearningActionPresentation>
)

fun resolveStudyLauncherSections(presentation: StudyIdlePresentation): StudyLauncherSections =
    StudyLauncherSections(
        primary = presentation.actions.filter {
            it.action == StudyLearningAction.CONTINUE ||
                it.action == StudyLearningAction.START_NEW_CONFIGURED
        },
        quickReview = presentation.actions.filter {
            it.action == StudyLearningAction.REVIEW_AGAIN_HARD ||
                it.action == StudyLearningAction.REVIEW_ALL_LEARNED ||
                it.action == StudyLearningAction.REVIEW_LATEST_NEW
        },
        navigation = presentation.actions.filter { it.priority == LearningEntryActionPriority.NAVIGATION }
    )

fun studyLauncherActionTitle(action: StudyLearningAction, hasActiveSession: Boolean): String = when (action) {
    StudyLearningAction.CONTINUE -> if (hasActiveSession) "Tiếp tục phiên" else "Phiên mới"
    StudyLearningAction.START_NEW_CONFIGURED -> "Phiên mới"
    StudyLearningAction.REVIEW_AGAIN_HARD -> "Từ khó"
    StudyLearningAction.REVIEW_ALL_LEARNED -> "Ôn tất cả"
    StudyLearningAction.REVIEW_LATEST_NEW -> "Từ mới"
    StudyLearningAction.BACK_TO_LIBRARY -> "Thư viện"
}

fun studyLauncherActionSubtitle(action: StudyLearningAction): String = when (action) {
    StudyLearningAction.CONTINUE -> "Tiếp tục đúng hàng đợi hiện tại"
    StudyLearningAction.START_NEW_CONFIGURED -> "Theo cấu hình hiện tại"
    StudyLearningAction.REVIEW_AGAIN_HARD -> "Again / Hard"
    StudyLearningAction.REVIEW_ALL_LEARNED -> "Đã học"
    StudyLearningAction.REVIEW_LATEST_NEW -> "Phiên gần nhất"
    StudyLearningAction.BACK_TO_LIBRARY -> "Đổi phạm vi học"
}

fun initialSessionDetailsExpanded(): Boolean = false
