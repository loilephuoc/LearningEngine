package vn.loi.learning.infrastructure.contentpackaging

data class Opd3ArchiveStructureLimits(
    val maximumEntryCount: Int = DEFAULT_MAXIMUM_ENTRY_COUNT,
    val maximumDeclaredUncompressedBytes: Long =
        DEFAULT_MAXIMUM_DECLARED_UNCOMPRESSED_BYTES
) {

    init {
        require(maximumEntryCount > 0) {
            "Maximum OPD3 archive entry count must be positive."
        }

        require(maximumDeclaredUncompressedBytes > 0) {
            "Maximum OPD3 declared uncompressed size must be positive."
        }
    }

    companion object {
        const val DEFAULT_MAXIMUM_ENTRY_COUNT: Int = 50_000

        const val DEFAULT_MAXIMUM_DECLARED_UNCOMPRESSED_BYTES: Long =
            2L * 1024L * 1024L * 1024L
    }
}
