package vn.loi.learning.application.contentpackaging

import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service xác thực tính toàn vẹn, checksum, schema version và liên kết media của gói OPD3 archive.
 */
class Opd3PackageVerifier(
    private val inspector: Opd3PackageInspector = Opd3PackageInspector(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {

    fun verify(zipBytes: ByteArray): PackageVerificationReport {
        val inspection = inspector.inspect(zipBytes)
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Phân loại các thông điệp từ inspection
        errors.addAll(inspection.errors)
        warnings.addAll(inspection.warnings)

        // 2. Schema & Version Validation
        if (inspection.schemaVersion != "1.0") {
            errors += "Unsupported schema version: '${inspection.schemaVersion}'. Expected '1.0'."
        }

        // 3. Media Manifest & Actual Media Cross-Reference Validation
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

    fun verify(packagePath: Path): PackageVerificationReport {
        val inspection = inspector.inspect(packagePath)
        return verifyInspection(inspection)
    }

    private fun verifyInspection(inspection: PackageInspectionResult): PackageVerificationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        errors.addAll(inspection.errors)
        warnings.addAll(inspection.warnings)

        if (inspection.schemaVersion != "1.0") {
            errors += "Unsupported schema version: '${inspection.schemaVersion}'. Expected '1.0'."
        }

        val isValid = errors.isEmpty()

        return PackageVerificationReport(
            isValid = isValid,
            errors = errors.distinct(),
            warnings = warnings.distinct()
        )
    }
}
