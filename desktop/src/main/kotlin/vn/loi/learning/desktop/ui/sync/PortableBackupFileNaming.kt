package vn.loi.learning.desktop.ui.sync

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun portableBackupFileName(packageNames: List<String>, localTime: LocalDateTime): String {
    val scope = when (packageNames.size) {
        0 -> "AllPackages"
        1 -> packageNames.single().replace(Regex("[<>:\"/\\\\|?*]+"), "_").trim().take(80)
        else -> "${packageNames.size}Packages"
    }.ifBlank { "Package" }
    val timestamp = localTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    return "LearningEngine_Backup_${scope}_$timestamp.lebak"
}
