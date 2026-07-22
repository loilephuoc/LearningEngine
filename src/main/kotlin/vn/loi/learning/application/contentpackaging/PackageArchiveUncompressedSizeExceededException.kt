package vn.loi.learning.application.contentpackaging

class PackageArchiveUncompressedSizeExceededException(
    maximumBytes: Long
) : PackageImportException(
    "OPD3 archive exceeds the maximum declared uncompressed size of $maximumBytes bytes."
)
