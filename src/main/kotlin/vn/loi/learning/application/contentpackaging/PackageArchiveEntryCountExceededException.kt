package vn.loi.learning.application.contentpackaging

class PackageArchiveEntryCountExceededException(
    maximumEntryCount: Int
) : PackageImportException(
    "OPD3 archive exceeds the maximum allowed entry count of $maximumEntryCount."
)
