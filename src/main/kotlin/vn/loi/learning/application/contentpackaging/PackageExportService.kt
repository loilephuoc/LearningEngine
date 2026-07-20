package vn.loi.learning.application.contentpackaging

class PackageExportService(
    private val exporter:
    PackageContentExporter,
    private val exportPlanFactory:
    PackageExportPlanFactory =
        PackageExportPlanFactory(),
    private val manifestSerializer:
    PackageExportManifestSerializer =
        PackageExportManifestSerializer()
) {

    fun export(
        payload: PackageExportPayload,
        destination: String
    ): PackageExportBundle {
        val plan =
            exportPlanFactory.create(
                payload
            )

        val bundle =
            PackageExportBundle(
                files =
                    plan.files
            )

        val manifest =
            manifestSerializer.deserialize(
                bundle.manifestFile().content
            )

        exporter.exportContent(
            payload =
                payload,
            manifest =
                manifest,
            destination =
                destination
        )

        require(
            bundle.isNotEmpty()
        ) {
            "Export bundle must not be empty."
        }

        return bundle
    }
}