package vn.loi.learning.domain.sync.protocol

@JvmInline
value class SyncAccountId(val value: String) {
    init { require(value.isNotBlank()) { "Sync account ID must not be blank." } }
}

@JvmInline
value class SyncDeviceId(val value: String) {
    init { require(value.isNotBlank()) { "Sync device ID must not be blank." } }
}

@JvmInline
value class SyncEventId(val value: String) {
    init { require(value.isNotBlank()) { "Sync event ID must not be blank." } }
}

@JvmInline
value class SyncEntityId(val value: String) {
    init { require(value.isNotBlank()) { "Sync entity ID must not be blank." } }
}

@JvmInline
value class IdempotencyKey(val value: String) {
    init { require(value.isNotBlank()) { "Idempotency key must not be blank." } }
}

@JvmInline
value class SyncRevision(val value: Long) : Comparable<SyncRevision> {
    init { require(value > 0L) { "Sync revision must be positive." } }
    override fun compareTo(other: SyncRevision): Int = value.compareTo(other.value)
}

@JvmInline
value class SyncCursor(val value: Long) : Comparable<SyncCursor> {
    init { require(value >= 0L) { "Sync cursor must not be negative." } }
    override fun compareTo(other: SyncCursor): Int = value.compareTo(other.value)

    companion object { val START = SyncCursor(0L) }
}
