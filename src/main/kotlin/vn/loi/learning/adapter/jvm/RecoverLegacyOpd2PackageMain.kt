package vn.loi.learning.adapter.jvm

import java.nio.file.Path
import vn.loi.learning.application.integrity.RecoverLegacyOpd2Package
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

/** Explicit, backup-gated operator entry point. It is never called by normal application startup. */
fun main(args: Array<String>) {
    require(args.size == 5 && args[4] == "--backup-verified") {
        "Usage: <runtime-data-directory> <OPD2-json> <image-source> <audio-source> --backup-verified"
    }
    val root = Path.of(args[0]).toAbsolutePath().normalize()
    val context = LearningApplicationFactory.createPersisted(root, reconcilePartOfSpeechRegistryOnCreate = false)
    val mediaStorage = JvmContentMediaStorage(root.resolve("media"))
    val command = RecoverLegacyOpd2Package(
        requireNotNull(context.contentRepository),
        requireNotNull(context.learningItemRepository),
        requireNotNull(context.contentPackageRepository),
        requireNotNull(context.contentLibraryRepository),
        requireNotNull(context.installedPackageRepository),
        requireNotNull(context.domainLibraryRepository),
        requireNotNull(context.defaultLibraryId),
        requireNotNull(context.memoryStateRepository),
        requireNotNull(context.reviewEventRepository),
        requireNotNull(context.learningTrajectoryRepository),
        requireNotNull(context.studySessionRepository),
        requireNotNull(context.studyQueueRepository),
        requireNotNull(context.transactionRunner),
        JvmOpd2RecoveryMediaPort(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]), root.resolve("media")),
        integrityCheck = { requireNotNull(context.packageIntegrityChecker).check(it, mediaStorage) }
    )
    val result = command.execute(backupVerified = true)
    println("RECOVERY_RESULT=${result.status}${result.reason?.let { ":$it" }.orEmpty()}")
    result.integrity?.let {
        println("INTEGRITY=${it.status};errors=${it.summary.errors};warnings=${it.summary.warnings};info=${it.summary.info}")
        it.findings.forEach { finding ->
            println("FINDING=${finding.severity}:${finding.code}:${finding.entityType}:${finding.entityId}")
        }
    }
}
