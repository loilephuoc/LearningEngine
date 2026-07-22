package vn.loi.learning.desktop.ui.study

import vn.loi.learning.infrastructure.persistence.mapper.InvalidPersistedRecordException

internal object StudyFailureMessage {

    fun forStudyData(failure: Throwable): String {
        val invalidRecord =
            failure.causes()
                .filterIsInstance<InvalidPersistedRecordException>()
                .firstOrNull()

        return if (invalidRecord == null) {
            "Study data could not be loaded safely. Select Retry to try again. " +
                "If the problem continues, restore a verified backup."
        } else {
            buildString {
                append("Study data contains an incompatible ")
                append(invalidRecord.recordType)
                append(" record. Restore a verified backup, then select Retry to continue.")
            }
        }
    }

    private fun Throwable.causes(): Sequence<Throwable> =
        generateSequence(this) { current ->
            current.cause?.takeUnless { it === current }
        }

}
