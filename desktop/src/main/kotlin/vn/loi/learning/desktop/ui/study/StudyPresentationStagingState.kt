package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences

data class StudyPresentationStagingState(
    val itemId: String?,
    val active: StudyPresentationPreferences,
    val pending: StudyPresentationPreferences? = null
) {
    val next: StudyPresentationPreferences
        get() = pending ?: active

    fun reconcile(
        currentItemId: String?,
        persisted: StudyPresentationPreferences
    ): StudyPresentationStagingState =
        if (currentItemId != itemId) {
            StudyPresentationStagingState(currentItemId, persisted)
        } else {
            copy(pending = persisted.takeUnless { it == active })
        }

    fun stage(preferences: StudyPresentationPreferences): StudyPresentationStagingState =
        copy(pending = preferences.takeUnless { it == active })
}

data class StudyPresentationHeaderStatus(
    val mode: String,
    val english: String?,
    val vietnamese: String?,
    val pending: Boolean
) {
    val label: String =
        listOfNotNull(mode, english, vietnamese).joinToString(" ")
}

fun resolveStudyPresentationHeaderStatus(
    state: StudyPresentationStagingState
): StudyPresentationHeaderStatus {
    val preferences = state.next
    return StudyPresentationHeaderStatus(
        mode = when (preferences.controlMode) {
            StudyPresentationControlMode.ADAPTIVE -> "Adaptive"
            StudyPresentationControlMode.PREFERENCE_GUIDED -> "PG"
            StudyPresentationControlMode.MANUAL -> "Manual"
        },
        english = preferences.showEnglish.takeUnless {
            preferences.controlMode == StudyPresentationControlMode.ADAPTIVE
        }?.let { if (it) "EN+" else "EN−" },
        vietnamese = preferences.showVietnamese.takeUnless {
            preferences.controlMode == StudyPresentationControlMode.ADAPTIVE
        }?.let { if (it) "VI+" else "VI−" },
        pending = state.pending != null
    )
}
