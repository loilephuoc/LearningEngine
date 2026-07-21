package vn.loi.learning.desktop.ui.study

import vn.loi.learning.infrastructure.persistence.mapper.InvalidPersistedRecordException

internal object StudyFailureMessage {

    fun forStudyData(failure: Throwable): String {
        val invalidRecord =
            failure.causes()
                .filterIsInstance<InvalidPersistedRecordException>()
                .firstOrNull()

        return if (invalidRecord == null) {
            buildString {
                append("Study data could not be loaded")
                failure.firstUsefulMessage()?.let { message ->
                    append(": ")
                    append(message)
                }
                append(". Fix the persisted data and select Retry to continue.")
            }
        } else {
            buildString {
                append("Study data contains an incompatible persisted ")
                append(invalidRecord.recordType)
                append(" record '")
                append(invalidRecord.recordId)
                append("'")
                invalidRecord.cause
                    ?.message
                    ?.takeIf(String::isNotBlank)
                    ?.let { message ->
                        append(": ")
                        append(message)
                    }
                append(". Restore or remove that record, then select Retry to continue.")
            }
        }
    }

    private fun Throwable.causes(): Sequence<Throwable> =
        generateSequence(this) { current ->
            current.cause?.takeUnless { it === current }
        }

    private fun Throwable.firstUsefulMessage(): String? =
        causes()
            .mapNotNull { it.message?.trim() }
            .firstOrNull(String::isNotEmpty)
}
