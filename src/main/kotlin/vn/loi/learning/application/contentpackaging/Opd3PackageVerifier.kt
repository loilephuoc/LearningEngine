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

        // 3. Asset & Media Cross-Reference Verification
        inspection.diagnostics.forEach { diag ->
            if (diag.contains("Missing required entry")) {
                errors += diag
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
