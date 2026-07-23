package vn.loi.learning.application.contentpackaging

import java.nio.file.Path

/**
 * Service xác thực tính toàn vẹn, checksum, schema version và liên kết media của gói OPD3 archive.
 */
class Opd3PackageVerifier(
    private val inspector: Opd3PackageInspector = Opd3PackageInspector()
) {

    fun verify(zipBytes: ByteArray): PackageVerificationReport {
        val inspection = inspector.inspect(zipBytes)
        return verify(inspection)
    }

    fun verify(packagePath: Path): PackageVerificationReport {
        val inspection = inspector.inspect(packagePath)
        return verify(inspection)
    }

    /**
     * Pipeline xác thực duy nhất cho toàn bộ quá trình verification của OPD3 package.
     * Cả hai phương thức nạp chồng (ByteArray và Path) đều gọi duy nhất pipeline này.
     */
    fun verify(inspection: PackageInspectionResult): PackageVerificationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Phân loại diagnostics từ inspection
        errors.addAll(inspection.errors)
        warnings.addAll(inspection.warnings)

        // 2. Schema Version Validation
        if (inspection.schemaVersion != "1.0") {
            errors += "Unsupported schema version: '${inspection.schemaVersion}'. Expected '1.0'."
        }

        // 3. Task 5: Format Validation (format phải là OPD3)
        if (inspection.format != "OPD3") {
            errors += "Unsupported package format: '${inspection.format}'. Expected 'OPD3'."
        }

        // 4. Task 6: TopicId Mandatory Validation
        if (inspection.topicId == null) {
            errors += "Missing or invalid mandatory TopicId in package metadata."
        }

        // 5. Media Manifest & Actual Media Cross-Reference Validation
        val actualMediaFiles = inspection.checksums.keys.filter { it.startsWith("media/") }.toSet()
        val manifestMediaPaths = inspection.assetSizes.keys.map { if (it.startsWith("media/")) it else "media/$it" }.toSet()

        // Check orphan media (actual file exists under media/ but missing from media-manifest)
        actualMediaFiles.forEach { actualMediaPath ->
            if (actualMediaPath !in manifestMediaPaths && manifestMediaPaths.isNotEmpty()) {
                warnings += "WARNING: Orphan media entry detected in archive: '$actualMediaPath'."
            }
        }

        // Check missing media (declared in media-manifest but missing from archive)
        manifestMediaPaths.forEach { declaredMediaPath ->
            if (declaredMediaPath !in actualMediaFiles) {
                errors += "ERROR: Media manifest references absent media asset: '$declaredMediaPath'."
            }
        }

        val isValid = errors.isEmpty()

        return PackageVerificationReport(
            isValid = isValid,
            errors = errors.distinct(),
            warnings = warnings.distinct()
        )
    }
}
