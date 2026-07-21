package vn.loi.learning.infrastructure.persistence.mapper

/**
 * Adds record identity to failures raised while rebuilding domain objects from
 * syntactically valid persistence records.
 */
class InvalidPersistedRecordException(
    val recordType: String,
    val recordId: String,
    cause: RuntimeException
) : IllegalStateException(
    buildString {
        append("Invalid persisted ")
        append(recordType)
        append(" record '")
        append(recordId)
        append("'")
        cause.message?.takeIf(String::isNotBlank)?.let { message ->
            append(": ")
            append(message)
        }
    },
    cause
)

internal inline fun <T> mapPersistedRecord(
    recordType: String,
    recordId: String,
    mapping: () -> T
): T =
    try {
        mapping()
    } catch (failure: InvalidPersistedRecordException) {
        throw failure
    } catch (failure: RuntimeException) {
        throw InvalidPersistedRecordException(
            recordType = recordType,
            recordId = recordId,
            cause = failure
        )
    }
