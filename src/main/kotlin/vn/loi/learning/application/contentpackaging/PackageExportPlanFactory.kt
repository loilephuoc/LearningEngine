package vn.loi.learning.application.contentpackaging

/**
 * Xây dựng kế hoạch export hoàn chỉnh.
 */
class PackageExportPlanFactory(
    private val manifestFactory:
    PackageExportManifestFactory =
        PackageExportManifestFactory(),
    private val manifestFileFactory:
    PackageExportManifestFileFactory =
        PackageExportManifestFileFactory(),
    private val metadataFileFactory:
    PackageExportMetadataFileFactory =
        PackageExportMetadataFileFactory(),
    private val contentsFileFactory:
    PackageExportContentsFileFactory =
        PackageExportContentsFileFactory(),
    private val learningItemsFileFactory:
    PackageExportLearningItemsFileFactory =
        PackageExportLearningItemsFileFactory(),
    private val integrityHasher:
    PackageIntegrityHasher =
        Sha256PackageIntegrityHasher()
) {

    fun create(
        payload: PackageExportPayload
    ): PackageExportPlan {
        val contentFiles =
            listOf(
                metadataFileFactory.create(
                    payload.descriptor
                ),
                contentsFileFactory.create(
                    payload.contents
                ),
                learningItemsFileFactory.create(
                    payload.learningItems
                )
            )

        val fileHashes =
            contentFiles.associate { file ->
                file.relativePath to
                        integrityHasher.hash(
                            file.content
                        )
            }

        val manifest =
            manifestFactory.create(
                payload =
                    payload,
                fileHashes =
                    fileHashes
            )

        return PackageExportPlan(
            files =
                contentFiles +
                        manifestFileFactory.create(
                            manifest
                        )
        )
    }
}