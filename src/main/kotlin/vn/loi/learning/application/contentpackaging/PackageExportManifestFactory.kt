package vn.loi.learning.application.contentpackaging

/**
 * Tạo manifest OPD3 từ PackageExportPayload.
 */
class PackageExportManifestFactory {

    fun create(
        payload: PackageExportPayload,
        fileHashes: Map<String, String> =
            emptyMap()
    ): PackageExportManifest =
        PackageExportManifest(
            name =
                payload.descriptor.name,
            version =
                payload.descriptor.version,
            format =
                payload.descriptor.format,
            contentCount =
                payload.contents.size,
            learningItemCount =
                payload.learningItems.size,
            schemaVersion =
                payload.descriptor.schemaVersion,
            minimumEngineVersion =
                payload.descriptor.minimumEngineVersion,
            maximumEngineVersion =
                payload.descriptor.maximumEngineVersion,
            dependencies =
                payload.descriptor.dependencies,
            hashAlgorithm =
                if (fileHashes.isEmpty()) {
                    null
                } else {
                    HASH_ALGORITHM
                },
            fileHashes =
                fileHashes.toSortedMap()
        )

    companion object {

        const val HASH_ALGORITHM =
            "SHA-256"
    }
}