package vn.loi.learning.infrastructure.contentpackaging

data class Opd3EntryReadLimits(
    val maximumTextEntryBytes: Long = DEFAULT_MAXIMUM_TEXT_ENTRY_BYTES
) {

    init {
        require(maximumTextEntryBytes > 0) {
            "Maximum OPD3 text entry size must be positive."
        }
    }

    companion object {
        const val DEFAULT_MAXIMUM_TEXT_ENTRY_BYTES: Long =
            32L * 1024L * 1024L
    }
}
