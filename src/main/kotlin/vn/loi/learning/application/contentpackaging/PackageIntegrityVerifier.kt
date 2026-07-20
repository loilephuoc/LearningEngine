package vn.loi.learning.application.contentpackaging

class PackageIntegrityVerifier(
    private val hasher: PackageIntegrityHasher =
        Sha256PackageIntegrityHasher()
) {

    fun verify(
        bundle: PackageImportBundle,
        manifest: PackageExportManifestJson
    ) {
        manifest.fileHashes
            .toSortedMap()
            .forEach {
                    (
                        relativePath,
                        expectedHash
                    ) ->

                val content =
                    bundle.requireFile(
                        relativePath
                    )

                val actualHash =
                    hasher.hash(
                        content
                    )

                if (
                    !actualHash.equals(
                        expectedHash,
                        ignoreCase = true
                    )
                ) {
                    throw InvalidPackageIntegrityException(
                        relativePath =
                            relativePath,
                        expectedHash =
                            expectedHash,
                        actualHash =
                            actualHash
                    )
                }
            }
    }
}