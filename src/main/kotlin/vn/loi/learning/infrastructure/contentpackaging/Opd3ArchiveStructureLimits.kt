package vn.loi.learning.infrastructure.contentpackaging

data class Opd3ArchiveStructureLimits(
    val maximumEntryCount: Int = DEFAULT_MAXIMUM_ENTRY_COUNT
) {

    init {
        require(maximumEntryCount > 0) {
            "Maximum OPD3 archive entry count must be positive."
        }
    }

    companion object {
        const val DEFAULT_MAXIMUM_ENTRY_COUNT: Int = 4096
    }
}
