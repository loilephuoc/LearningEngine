package vn.loi.learning.application.integrity

import java.nio.file.Files
import java.time.Instant
import vn.loi.learning.application.port.*
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository

enum class IntegritySeverity { ERROR, WARNING, INFO }
enum class IntegrityStatus { HEALTHY, WARNINGS, ERRORS }

data class IntegrityFinding(
    val severity: IntegritySeverity,
    val code: String,
    val entityType: String,
    val entityId: String,
    val message: String,
    val details: Map<String, String> = emptyMap()
)

data class IntegritySummary(val errors: Int, val warnings: Int, val info: Int) {
    val total: Int get() = errors + warnings + info
}

data class PackageIntegrityReport(
    val installedPackageId: String,
    val packageId: String,
    val packageName: String,
    val checkedAtRuntime: Instant,
    val status: IntegrityStatus,
    val findings: List<IntegrityFinding>,
    val summary: IntegritySummary
)

/** Application-owned, observational package diagnostic. It has no mutation dependency. */
class PackageIntegrityChecker(
    private val installedPackages: InstalledPackageRepository,
    private val contentPackages: ContentPackageRepository,
    private val contentLibraries: ContentLibraryRepository,
    private val contents: ContentRepository,
    private val learningItems: LearningItemRepository,
    private val libraries: LibraryRepository,
    private val mediaStorage: ContentMediaStorage?,
    private val studySessions: StudySessionRepository? = null,
    private val studyQueues: StudyQueueRepository? = null,
    private val memoryStates: MemoryStateRepository? = null,
    private val reviewEvents: ReviewEventRepository? = null,
    private val trajectories: LearningTrajectoryRepository? = null,
    private val clock: () -> Instant = Instant::now
) {
    fun check(installedPackageId: InstalledPackageId, mediaStorageOverride: ContentMediaStorage? = mediaStorage): PackageIntegrityReport {
        val installed = requireNotNull(installedPackages.findById(installedPackageId)) {
            "InstalledPackage $installedPackageId does not exist."
        }
        val findings = mutableListOf<IntegrityFinding>()
        val contentPackage = contentPackages.findById(installed.packageId)
        if (contentPackage == null) {
            findings.error("MISSING_CONTENT_PACKAGE", "InstalledPackage", installed.id.toString(),
                "Installed package references a missing ContentPackage.")
            return report(installed.id.toString(), installed.packageId.toString(), installed.name.toString(), findings)
        }
        if (contentPackage.id != installed.packageId) {
            findings.error("PACKAGE_ID_MISMATCH", "InstalledPackage", installed.id.toString(),
                "InstalledPackage and ContentPackage identities do not match.")
        }

        val canonicalLibrary = libraries.findById(installed.libraryId)
        val matchingEntry = canonicalLibrary?.entries?.firstOrNull { it.installedPackageId == installed.id }
        if (canonicalLibrary == null || matchingEntry == null || matchingEntry.packageId != installed.packageId) {
            findings.error("INCOMPLETE_INSTALL_LIFECYCLE", "InstalledPackage", installed.id.toString(),
                "Canonical Library registration is missing or inconsistent.")
        }
        contentPackages.findAll().filter {
            it.id != contentPackage.id && it.name.equals(contentPackage.name, ignoreCase = true)
        }.forEach { other ->
            findings.info("DUPLICATE_PACKAGE_NAME", "ContentPackage", other.id.toString(),
                "Another package has the same display name but a different PackageId.")
        }

        val packageLibraries = contentPackage.libraryIds.mapNotNull { libraryId ->
            contentLibraries.findById(libraryId) ?: run {
                findings.error("MISSING_CONTENT_LIBRARY", "ContentLibrary", libraryId.toString(),
                    "ContentPackage references a missing ContentLibrary.")
                null
            }
        }
        val ownedIds = packageLibraries.flatMapTo(linkedSetOf()) { it.contentIds }
        val existingContents = contents.findByIds(ownedIds)
        val existingById = existingContents.associateBy(Content::id)
        packageLibraries.forEach { library ->
            library.contentIds.filterNot(existingById::containsKey).forEach { id ->
                findings.error("MISSING_CONTENT_REFERENCED_BY_LIBRARY", "Content", id.toString(),
                    "ContentLibrary ${library.id} references missing Content.")
            }
        }
        val allContents = contents.findAll()
        val globallyReferencedIds = contentLibraries.findAll().flatMapTo(hashSetOf()) { it.contentIds }
        val packageRelatedOrphans = allContents
            .asSequence()
            .filter { it.id !in globallyReferencedIds }
            .mapNotNull { content ->
                packageRelationshipEvidence(content, installed.name.toString())
                    .takeIf { it.isNotEmpty() }
                    ?.let { evidence -> content to evidence }
            }
            .toList()
        val relatedOrphanIds = packageRelatedOrphans.mapTo(hashSetOf()) { it.first.id }
        val relatedOrphanItems = learningItems.findByContentIds(relatedOrphanIds).distinctBy { it.id }

        val graphComplete = contentPackage.libraryIds.size == packageLibraries.size && existingById.size == ownedIds.size
        if (graphComplete) {
            val itemCount = learningItems.findByContentIds(ownedIds).distinctBy { it.id }.size
            if (installed.contentCount != existingById.size) {
                val details = linkedMapOf(
                    "storedContentCount" to installed.contentCount.toString(),
                    "canonicalOwnedContentCount" to existingById.size.toString()
                )
                if (installed.contentCount - existingById.size == packageRelatedOrphans.size && packageRelatedOrphans.isNotEmpty()) {
                    details["packageRelatedOrphanContentOutsideMembership"] = packageRelatedOrphans.size.toString()
                    details["reconciliationEvidence"] = "Stored-to-canonical delta matches package-related orphan count; causality is not assumed."
                }
                findings.error("CONTENT_COUNT_MISMATCH", "InstalledPackage", installed.id.toString(),
                    "Installed content count differs from canonical ownership union.",
                    details)
            }
            if (installed.learningItemCount != itemCount) {
                val details = linkedMapOf(
                    "storedLearningItemCount" to installed.learningItemCount.toString(),
                    "canonicalOwnedLearningItemCount" to itemCount.toString()
                )
                if (installed.learningItemCount - itemCount == relatedOrphanItems.size && relatedOrphanItems.isNotEmpty()) {
                    details["associatedLearningItemsOutsideMembership"] = relatedOrphanItems.size.toString()
                    details["reconciliationEvidence"] = "Stored-to-canonical delta matches associated outside-membership item count; causality is not assumed."
                }
                findings.error("LEARNING_ITEM_COUNT_MISMATCH", "InstalledPackage", installed.id.toString(),
                    "Installed learning-item count differs from all owned items, including disabled items.",
                    details)
            }
        }

        val allContentIds = allContents.mapTo(hashSetOf()) { it.id }
        packageRelatedOrphans.forEach { (content, evidence) ->
            findings.warning("PACKAGE_RELATED_ORPHAN_CONTENT", "Content", content.id.toString(),
                "Content is outside package membership but has deterministic provenance tying it to this package.", evidence)
        }

        val allItems = learningItems.findAll()
        val allItemIds = allItems.mapTo(hashSetOf()) { it.id }
        checkMedia(existingContents, installed.name.toString(), mediaStorageOverride, findings)
        checkOwnershipObservations(contentPackage.id.toString(), contentPackage.libraryIds.map { it.toString() }.toSet(), ownedIds, findings)
        checkSessions(installed.id, ownedIds, allContentIds, allItemIds, findings)

        return report(installed.id.toString(), installed.packageId.toString(), installed.name.toString(), findings)
    }

    private fun packageRelationshipEvidence(content: Content, packageName: String): Map<String, String> {
        val packageFingerprint = packageName.integrityFingerprint()
        if (packageFingerprint.isEmpty()) return emptyMap()
        val evidence = linkedMapOf<String, String>()
        content.metadata.source?.takeIf { it.integrityFingerprint().contains(packageFingerprint) }?.let {
            evidence["source"] = it
        }
        val mediaRoots = listOfNotNull(
            content.media.image,
            content.media.primaryAudio,
            content.media.translatedAudio,
            content.media.exampleAudio,
            content.media.exampleTranslatedAudio
        ).mapNotNull { reference -> reference.replace('\\', '/').substringBefore('/').takeIf { '/' in reference.replace('\\', '/') } }
            .filter { it.integrityFingerprint() == packageFingerprint }
            .distinct()
        if (mediaRoots.isNotEmpty()) evidence["mediaRoot"] = mediaRoots.joinToString(",")
        val metadataFingerprint = listOfNotNull(content.metadata.group, content.metadata.section)
            .joinToString("").integrityFingerprint()
        if (metadataFingerprint == packageFingerprint) {
            evidence["metadata"] = listOfNotNull(content.metadata.group, content.metadata.section).joinToString(" / ")
        }
        val tagFingerprint = content.metadata.tags.joinToString("").integrityFingerprint()
        if (tagFingerprint.contains(packageFingerprint)) evidence["tags"] = content.metadata.tags.sorted().joinToString(",")
        return evidence
    }

    private fun checkMedia(packageContents: List<Content>, packageName: String, resolver: ContentMediaStorage?, findings: MutableList<IntegrityFinding>) {
        val references = packageContents.flatMap { content -> listOf(
            "image" to content.media.image,
            "primaryAudio" to content.media.primaryAudio,
            "translatedAudio" to content.media.translatedAudio,
            "exampleAudio" to content.media.exampleAudio,
            "exampleTranslatedAudio" to content.media.exampleTranslatedAudio
        ).mapNotNull { (slot, ref) -> ref?.let { Triple(content.id, slot, it) } } }
        val resolution = references.map { it.third }.distinct().associateWith { ref -> resolver?.resolve(ref) }
        references.forEach { (contentId, slot, reference) ->
            val path = resolution[reference]
            if (path == null || !Files.isRegularFile(path)) {
                findings.warning("MISSING_MEDIA", "Content", contentId.toString(),
                    "Media reference cannot be resolved to a regular file.", mapOf("slot" to slot, "reference" to reference))
            } else {
                val expectedRoot = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val normalizedReference = reference.replace('\\', '/')
                if ('/' in normalizedReference && normalizedReference.substringBefore('/') != expectedRoot) {
                    findings.warning("CROSS_PACKAGE_MEDIA_REFERENCE", "Content", contentId.toString(),
                        "Media reference points outside the expected package media root.",
                        mapOf("slot" to slot, "reference" to reference))
                }
            }
        }
    }

    private fun checkOwnershipObservations(packageId: String, libraryIds: Set<String>, ownedIds: Set<ContentId>, findings: MutableList<IntegrityFinding>) {
        contentPackages.findAll().filter { it.id.toString() != packageId }.forEach { other ->
            if (other.libraryIds.any { it.toString() in libraryIds }) {
                findings.info("SHARED_LIBRARY_OWNERSHIP", "ContentPackage", other.id.toString(),
                    "Another package references a ContentLibrary used by this package.")
            }
            val otherOwned = other.libraryIds.mapNotNull(contentLibraries::findById).flatMapTo(hashSetOf()) { it.contentIds }
            if (otherOwned.any { it in ownedIds }) {
                findings.info("SHARED_CONTENT_OWNERSHIP", "ContentPackage", other.id.toString(),
                    "Another package ownership union includes Content used by this package.")
            }
        }
    }

    private fun checkSessions(installedId: InstalledPackageId, ownedIds: Set<ContentId>, allContentIds: Set<ContentId>, allItemIds: Set<vn.loi.learning.domain.study.learning.model.LearningItemId>, findings: MutableList<IntegrityFinding>) {
        val relevant = studySessions?.findAll().orEmpty().filter {
            it.installedPackageId == installedId || it.includedContentIds.any(ownedIds::contains)
        }
        relevant.forEach { session ->
            val missingContents = (session.includedContentIds + session.reviewedContentIds + session.introducedContentIds).filterNot(allContentIds::contains)
            val missingItems = (session.reviewedItemIds + listOfNotNull(session.currentLearningItemId)).filterNot(allItemIds::contains)
            (missingContents.map { it.toString() } + missingItems.map { it.toString() }).distinct().forEach { id ->
                findings.warning("DANGLING_STUDY_SESSION_ITEM", "StudySession", session.id.toString(),
                    "StudySession references missing canonical learning data.", mapOf("missingId" to id))
            }
        }
        studyQueues?.findAll().orEmpty().filter { queue -> relevant.any { it.id == queue.sessionId } }.forEach { queue ->
            queue.learningItemIds.filterNot(allItemIds::contains).distinct().forEach { id ->
                findings.warning("DANGLING_STUDY_QUEUE_ITEM", "StudyQueue", queue.sessionId.toString(),
                    "StudyQueue references a missing LearningItem.", mapOf("learningItemId" to id.toString()))
            }
        }
    }

    private fun report(installedId: String, packageId: String, name: String, findings: List<IntegrityFinding>): PackageIntegrityReport {
        val sorted = findings.distinct().sortedWith(compareBy({ it.severity.ordinal }, { it.code }, { it.entityType }, { it.entityId }))
        val summary = IntegritySummary(sorted.count { it.severity == IntegritySeverity.ERROR }, sorted.count { it.severity == IntegritySeverity.WARNING }, sorted.count { it.severity == IntegritySeverity.INFO })
        val status = when { summary.errors > 0 -> IntegrityStatus.ERRORS; summary.warnings > 0 -> IntegrityStatus.WARNINGS; else -> IntegrityStatus.HEALTHY }
        return PackageIntegrityReport(installedId, packageId, name, clock(), status, sorted, summary)
    }
}

private fun MutableList<IntegrityFinding>.error(code: String, type: String, id: String, message: String, details: Map<String, String> = emptyMap()) = add(IntegrityFinding(IntegritySeverity.ERROR, code, type, id, message, details))
private fun MutableList<IntegrityFinding>.warning(code: String, type: String, id: String, message: String, details: Map<String, String> = emptyMap()) = add(IntegrityFinding(IntegritySeverity.WARNING, code, type, id, message, details))
private fun MutableList<IntegrityFinding>.info(code: String, type: String, id: String, message: String, details: Map<String, String> = emptyMap()) = add(IntegrityFinding(IntegritySeverity.INFO, code, type, id, message, details))

private fun String.integrityFingerprint(): String = lowercase().filter(Char::isLetterOrDigit)
