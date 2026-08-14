package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.application.integrity.IntegrityStatus
import vn.loi.learning.application.integrity.PackageIntegrityReport

data class PackageIntegrityPresentation(
    val statusText: String,
    val summaryText: String,
    val accessibilityDescription: String,
    val findingCount: Int,
    val offersRepair: Boolean = false
)

fun PackageIntegrityReport.toPresentation(): PackageIntegrityPresentation {
    val readableStatus = when (status) {
        IntegrityStatus.HEALTHY -> "Healthy"
        IntegrityStatus.WARNINGS -> "Warnings"
        IntegrityStatus.ERRORS -> "Errors"
    }
    val summaryText = "${summary.errors} errors · ${summary.warnings} warnings · ${summary.info} info"
    return PackageIntegrityPresentation(
        statusText = readableStatus,
        summaryText = summaryText,
        accessibilityDescription = "$packageName. Status $readableStatus. ${summary.errors} errors, ${summary.warnings} warnings, ${summary.info} info.",
        findingCount = findings.size
    )
}
