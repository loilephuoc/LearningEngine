package vn.loi.learning.adapter.jvm

import java.nio.file.Path
import vn.loi.learning.application.integrity.ReconcileIntermediatePublicTransportOrphan
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

fun main(args: Array<String>) {
    require(args.size == 2 && args[1] in setOf("--check-only", "--backup-verified")) {
        "Usage: <runtime-data-directory> (--check-only|--backup-verified)"
    }
    val root = Path.of(args[0]).toAbsolutePath().normalize()
    val context = LearningApplicationFactory.createPersisted(root, reconcilePartOfSpeechRegistryOnCreate = false)
    val checker = requireNotNull(context.packageIntegrityChecker)
    val media = JvmContentMediaStorage(root.resolve("media"))
    val before = checker.check(InstalledPackageId(ReconcileIntermediatePublicTransportOrphan.INSTALLED.value), media)
    val required = setOf("CONTENT_COUNT_MISMATCH", "LEARNING_ITEM_COUNT_MISMATCH", "PACKAGE_RELATED_ORPHAN_CONTENT")
    val beforeCodes = before.findings.mapTo(hashSetOf()) { it.code }
    require(before.status == vn.loi.learning.application.integrity.IntegrityStatus.HEALTHY || beforeCodes.containsAll(required)) {
        "Frozen checker reports neither the exact targeted pre-state nor the reconciled state."
    }
    println("PRE_INTEGRITY=${before.status};errors=${before.summary.errors};warnings=${before.summary.warnings};info=${before.summary.info}")
    before.findings.forEach { println("PRE_FINDING=${it.severity}:${it.code}:${it.entityType}:${it.entityId}") }
    if (args[1] == "--check-only") return
    val result = requireNotNull(context.intermediatePublicTransportRepair).execute(backupVerified = true)
    println("REPAIR_RESULT=${result.status}${result.reason?.let { ":$it" }.orEmpty()}")
    val after = checker.check(InstalledPackageId(ReconcileIntermediatePublicTransportOrphan.INSTALLED.value), media)
    println("INTEGRITY=${after.status};errors=${after.summary.errors};warnings=${after.summary.warnings};info=${after.summary.info}")
    after.findings.forEach { println("FINDING=${it.severity}:${it.code}:${it.entityType}:${it.entityId}") }
}
