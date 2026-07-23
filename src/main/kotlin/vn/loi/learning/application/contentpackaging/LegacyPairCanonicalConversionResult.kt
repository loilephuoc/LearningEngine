package vn.loi.learning.application.contentpackaging

/**
 * Kết quả chuyển đổi cặp file legacy sang canonical package model.
 */
data class LegacyPairCanonicalConversionResult(
    val canonicalPackage: CanonicalTopicPackage?,
    val diagnostics: List<CanonicalConversionDiagnostic>,
    val isReadyForExport: Boolean
) {

    val hasFatalErrors: Boolean
        get() = diagnostics.any { it.severity == CanonicalConversionDiagnosticSeverity.FATAL }

    val warnings: List<CanonicalConversionDiagnostic>
        get() = diagnostics.filter { it.severity == CanonicalConversionDiagnosticSeverity.WARNING }

    val errors: List<CanonicalConversionDiagnostic>
        get() = diagnostics.filter { it.severity == CanonicalConversionDiagnosticSeverity.FATAL }
}
