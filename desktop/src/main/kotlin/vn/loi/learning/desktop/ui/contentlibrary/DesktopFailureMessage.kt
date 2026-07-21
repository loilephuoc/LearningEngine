package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.infrastructure.persistence.mapper.InvalidPersistedRecordException

/**
 * Converts infrastructure failures into concise, actionable Desktop messages.
 */
internal object DesktopFailureMessage {

    fun forPersistedData(
        failure: Throwable
    ): String {
        val invalidRecord =
            failure.causes()
                .filterIsInstance<InvalidPersistedRecordException>()
                .firstOrNull()

        return if (invalidRecord == null) {
            buildString {
                append("Content data could not be loaded")
                failure.firstUsefulMessage()?.let { message ->
                    append(": ")
                    append(message)
                }
                append(". Fix the persisted data and select Refresh to try again.")
            }
        } else {
            buildString {
                append("Content data contains an incompatible persisted ")
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
                append(". Restore or remove that record, then select Refresh to try again.")
            }
        }
    }

    private fun Throwable.causes(): Sequence<Throwable> =
        generateSequence(this) { current ->
            current.cause
                ?.takeUnless { cause ->
                    cause === current
                }
        }

    private fun Throwable.firstUsefulMessage(): String? =
        causes()
            .mapNotNull { cause ->
                cause.message?.trim()
            }
            .firstOrNull(String::isNotEmpty)
}
