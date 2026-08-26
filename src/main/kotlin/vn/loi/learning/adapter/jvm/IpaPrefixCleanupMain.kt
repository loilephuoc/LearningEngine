package vn.loi.learning.adapter.jvm

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import vn.loi.learning.infrastructure.persistence.json.JsonContentLibraryStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentStore
import vn.loi.learning.infrastructure.persistence.json.JsonInstalledPackageStore
import vn.loi.learning.infrastructure.persistence.record.ContentRecord
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner

private const val DEFAULT_PACKAGE = "Vocabulary_in_Use_Upper_Intermediate"

fun main(args: Array<String>) {
    require(args.size in 1..3) {
        "Usage: <runtime-data-directory> [package-name-or-id] [--dry-run|--apply]"
    }
    val dataRoot = Path.of(args[0]).toAbsolutePath().normalize()
    val mode = args.lastOrNull()?.takeIf { it.startsWith("--") } ?: "--dry-run"
    require(mode == "--dry-run" || mode == "--apply") { "Unknown mode: $mode" }
    val packageSelector = args.getOrNull(1)?.takeUnless { it.startsWith("--") } ?: DEFAULT_PACKAGE

    val installedStore = JsonInstalledPackageStore(dataRoot.resolve("installed-packages.json"))
    val packageStore = JsonContentPackageStore(dataRoot.resolve("content-packages.json"))
    val libraryStore = JsonContentLibraryStore(dataRoot.resolve("content-libraries.json"))
    val contentPath = dataRoot.resolve("contents.json")
    val contentStore = JsonContentStore(contentPath)

    val installedMatches = installedStore.loadAll().filter {
        it.name.equals(packageSelector, ignoreCase = true) ||
            it.id == packageSelector ||
            it.packageId == packageSelector
    }
    require(installedMatches.size == 1) {
        "Expected exactly one installed package matching '$packageSelector', found ${installedMatches.size}."
    }
    val installed = installedMatches.single()
    val contentPackage = packageStore.loadAll().singleOrNull { it.id == installed.packageId }
        ?: error("Canonical ContentPackage not found: ${installed.packageId}")
    val librariesById = libraryStore.loadAll().associateBy { it.id }
    val contentIds = contentPackage.libraryIds.flatMap { libraryId ->
        requireNotNull(librariesById[libraryId]) { "Canonical ContentLibrary not found: $libraryId" }.contentIds
    }.toSet()
    val allContents = contentStore.loadAll()
    val targetContents = allContents.filter { it.id in contentIds }
    require(targetContents.size == contentIds.size) {
        "Package graph references ${contentIds.size} Content records but ${targetContents.size} were found."
    }

    val report = buildReport(targetContents)
    printReport(dataRoot, installed.id, installed.packageId, installed.name, report)

    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val backup = dataRoot.parent.resolve("manual-backups")
        .resolve("${installed.name}_before_ipa_cleanup_$timestamp")
        .resolve("contents.json")
    println("PROPOSED_BACKUP=$backup")

    if (mode == "--dry-run") {
        println("APPLIED=false")
        return
    }

    Files.createDirectories(backup.parent)
    Files.copy(contentPath, backup, StandardCopyOption.COPY_ATTRIBUTES)
    val replacements = report.changes.associate { it.before.id to it.after }
    val updatedAll = allContents.map { replacements[it.id] ?: it }
    require(updatedAll.zip(allContents).all { (after, before) ->
        after == before || after.copy(pronunciation = before.pronunciation) == before
    }) { "Cleanup attempted to alter a field other than pronunciation." }

    JsonFileTransactionRunner(listOf(contentPath)).runInTransaction {
        contentStore.saveAll(updatedAll)
        val persisted = JsonContentStore(contentPath).loadAll()
        require(persisted.size == allContents.size) { "Content count changed after cleanup." }
        require(persisted.filter { it.id in contentIds }.none {
            it.pronunciation?.let(IpaPrefixCleanup::clean) != null
        }) { "One or more package IPA prefixes remain after cleanup." }
    }
    println("BACKUP=$backup")
    println("UPDATED=${report.changes.size}")
    println("APPLIED=true")
}

private data class IpaChange(val before: ContentRecord, val after: ContentRecord)
private data class IpaReport(
    val total: Int,
    val nonEmpty: Int,
    val empty: Int,
    val unmatched: Int,
    val changes: List<IpaChange>,
    val slashBoth: Int,
    val slashStartOnly: Int,
    val slashEndOnly: Int,
    val noSlash: Int
)

private fun buildReport(contents: List<ContentRecord>): IpaReport {
    val changes = contents.mapNotNull { record ->
        val value = record.pronunciation ?: return@mapNotNull null
        val cleaned = IpaPrefixCleanup.clean(value) ?: return@mapNotNull null
        if (cleaned == value) null else IpaChange(record, record.copy(pronunciation = cleaned))
    }
    val simulated = contents.map { record -> changes.firstOrNull { it.before.id == record.id }?.after ?: record }
    val values = simulated.map { it.pronunciation.orEmpty() }
    val nonEmptyValues = values.filter { it.isNotEmpty() }
    return IpaReport(
        total = contents.size,
        nonEmpty = contents.count { !it.pronunciation.isNullOrEmpty() },
        empty = contents.count { it.pronunciation.isNullOrEmpty() },
        unmatched = contents.count { !it.pronunciation.isNullOrEmpty() && IpaPrefixCleanup.clean(it.pronunciation!!) == null },
        changes = changes,
        slashBoth = nonEmptyValues.count { it.startsWith('/') && it.endsWith('/') },
        slashStartOnly = nonEmptyValues.count { it.startsWith('/') && !it.endsWith('/') },
        slashEndOnly = nonEmptyValues.count { !it.startsWith('/') && it.endsWith('/') },
        noSlash = nonEmptyValues.count { !it.startsWith('/') && !it.endsWith('/') }
    )
}

private fun printReport(
    dataRoot: Path,
    installedId: String,
    packageId: String,
    packageName: String,
    report: IpaReport
) {
    println("DATA_ROOT=$dataRoot")
    println("PACKAGE_NAME=$packageName")
    println("INSTALLED_PACKAGE_ID=$installedId")
    println("CONTENT_PACKAGE_ID=$packageId")
    println("TOTAL=${report.total}")
    println("IPA_NON_EMPTY=${report.nonEmpty}")
    println("MATCHED=${report.changes.size}")
    println("WOULD_CHANGE=${report.changes.size}")
    println("ALREADY_CLEAN_OR_UNMATCHED=${report.unmatched}")
    println("EMPTY=${report.empty}")
    evenlySample(report.changes, 20).forEachIndexed { index, change ->
        println("SAMPLE_${index + 1}_ID=${change.before.id}")
        println("SAMPLE_${index + 1}_TITLE=${change.before.primaryText}")
        println("SAMPLE_${index + 1}_BEFORE=${change.before.pronunciation}")
        println("SAMPLE_${index + 1}_AFTER=${change.after.pronunciation}")
    }
    println("ANOMALY_SLASH_BOTH=${report.slashBoth}")
    println("ANOMALY_SLASH_START_ONLY=${report.slashStartOnly}")
    println("ANOMALY_SLASH_END_ONLY=${report.slashEndOnly}")
    println("ANOMALY_NO_SLASH=${report.noSlash}")
    println("ANOMALY_EMPTY=${report.empty}")
}

private fun <T> evenlySample(values: List<T>, limit: Int): List<T> {
    if (values.size <= limit) return values
    return (0 until limit).map { index -> values[index * (values.lastIndex) / (limit - 1)] }
}
