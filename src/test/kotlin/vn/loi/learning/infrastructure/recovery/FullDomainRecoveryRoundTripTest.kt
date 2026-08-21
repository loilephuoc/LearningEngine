package vn.loi.learning.infrastructure.recovery

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.application.session.StudyQueueService
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.evidence.ChainAnchor
import vn.loi.learning.domain.study.evidence.ChainAnchorReason
import vn.loi.learning.domain.study.evidence.EvidenceChain
import vn.loi.learning.domain.study.evidence.LearningTrajectory
import vn.loi.learning.domain.study.evidence.PromotionStage
import vn.loi.learning.domain.study.evidence.RecallEvidence
import vn.loi.learning.domain.study.evidence.RecallEvidenceOrigin
import vn.loi.learning.domain.study.evidence.RecallResult
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.LearningApplicationFactory

class FullDomainRecoveryRoundTripTest {
    @Test
    fun `first middle and final live repository reload failures roll back exact pre state`() {
        listOf("content-libraries", "review-events", "installed-packages").forEach { selected ->
            fixture().use { fixture ->
                fixture.populate()
                val archive = fixture.root.resolve("reload-$selected.lebak")
                fixture.manager().createBackup(archive)
                Files.writeString(fixture.dataPath.resolve("pre-restore-marker.txt"), "pre")
                val pre = fixture.snapshotManaged()
                val manager = fixture.manager(domainValidator = { roots ->
                    val candidate = roots.getValue("data")
                    LearningApplicationFactory.validatePersisted(candidate) { checkpoint ->
                        if (candidate == fixture.dataPath && checkpoint == selected) error("injected reload $selected")
                    }
                })

                assertFailsWith<LearningDataRecoveryException>(selected) { manager.restore(archive, false) }

                assertSnapshot(pre, fixture.snapshotManaged(), selected)
            }
        }
    }

    @Test
    fun `manifest v1 remains accepted and repeated backups never recurse`() {
        fixture().use { fixture ->
            fixture.populate()
            val first = fixture.root.resolve("first.lebak")
            val second = fixture.root.resolve("second.lebak")
            fixture.manager().createBackup(first)
            fixture.manager().createBackup(second)
            val firstNames = fixture.manager().validate(first)
            val secondNames = fixture.manager().validate(second)
            assertEquals(firstNames, secondNames)
            assertFalse(secondNames.any { it.endsWith(".lebak") || "backups/" in it || ".learning-engine-" in it })
            ZipFile(first.toFile()).use { zip ->
                val manifest = zip.getInputStream(zip.getEntry(JvmLearningDataRecoveryManager.MANIFEST_ENTRY))
                    .bufferedReader().readText()
                assertTrue(manifest.lineSequence().any { it == "format=1" })
            }
        }
    }

    @Test
    fun `representative persisted domain and all media slots restore byte exactly`() {
        fixture().use { fixture ->
            fixture.populate()
            val before = fixture.snapshotManaged()
            val mediaBefore = fixture.mediaHashes()
            val archive = fixture.root.resolve("representative.lebak")

            fixture.manager().createBackup(archive)

            assertSnapshot(before, fixture.snapshotManaged(), "Backup must be read-only")
            assertFalse(fixture.manager().validate(archive).any { "backups" in it || ".tmp" in it })
            fixture.replaceRuntimeWithDivergentState()
            fixture.manager().restore(archive, false)

            val after = fixture.snapshotManaged()
            assertEquals(before.keys, after.keys)
            before.forEach { (name, bytes) -> assertContentEquals(bytes, after.getValue(name), name) }
            assertEquals(mediaBefore, fixture.mediaHashes())
            fixture.assertReloadedLogicalState()
        }
    }

    @Test
    fun `backup containing orphan package media restores faithfully and subsequent reimport recovers safely`() {
        fixture().use { fixture ->
            fixture.populate()
            // Add orphan package media before backup (Package B media exists on disk but is not installed)
            val orphanMediaFile = fixture.dataPath.resolve("media/OrphanPackage/sample.jpg")
            Files.createDirectories(orphanMediaFile.parent)
            Files.writeString(orphanMediaFile, "OLD_ORPHAN_MEDIA_BYTES")

            val archive = fixture.root.resolve("with-orphan.lebak")
            fixture.manager().createBackup(archive)

            // Wipe and restore
            fixture.replaceRuntimeWithDivergentState()
            fixture.manager().restore(archive, false)

            // Restore faithfully preserves exact physical media including orphan
            assertTrue(Files.exists(orphanMediaFile))
            assertEquals("OLD_ORPHAN_MEDIA_BYTES", Files.readString(orphanMediaFile))

            // Now import the package with new media bytes through the engine without needing manual ADB delete
            val opd3File = fixture.root.resolve("orphan-pkg.opd3")
            java.util.zip.ZipOutputStream(Files.newOutputStream(opd3File)).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                zip.write("""{"name": "OrphanPackage", "version": "1.0.0", "format": "OPD3", "contentCount": 0, "learningItemCount": 0}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(java.util.zip.ZipEntry("contents.json"))
                zip.write("""{"contents":[]}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(java.util.zip.ZipEntry("learning-items.json"))
                zip.write("""{"learningItems":[]}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(java.util.zip.ZipEntry("metadata.json"))
                zip.write("""{"name": "OrphanPackage"}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(java.util.zip.ZipEntry("media/OrphanPackage/sample.jpg"))
                zip.write("NEW_VALID_MEDIA_BYTES".toByteArray())
                zip.closeEntry()
            }

            val app = LearningApplicationFactory.createPersisted(fixture.dataPath, false)
            val importer = vn.loi.learning.infrastructure.contentpackaging.ContentPackageImportFactory.createContentImporter(
                mediaDirectory = fixture.dataPath.resolve("media"),
                installedPackageRepository = app.installedPackageRepository
            )
            val imported = importer.importContent(vn.loi.learning.application.contentpackaging.PackageScanCandidate(opd3File.toString()))
            imported.onCommit?.invoke()

            // Verification: new media replaced old orphan media without collision exception
            assertEquals("NEW_VALID_MEDIA_BYTES", Files.readString(orphanMediaFile))
        }
    }

    private fun fixture() = Fixture(Files.createTempDirectory("full-domain-recovery-"))

    private class Fixture(val root: Path) : AutoCloseable {
        private val data = Files.createDirectories(root.resolve("data"))
        val dataPath: Path get() = data
        private val config = Files.createDirectories(root.resolve("config"))
        private val safety = Files.createDirectories(config.resolve("backups"))
        private val learner = LearnerId("recovery-learner")
        private val firstContent = ContentId("content-a")
        private val secondContent = ContentId("content-b")
        private val orphanContent = ContentId("historical-orphan")
        private val firstItem = LearningItemId("item-enabled")
        private val secondItem = LearningItemId("item-disabled")
        private val mediaPaths = listOf(
            "media/Pkg/image/hero.png",
            "media/Pkg/audio/question.mp3",
            "media/Pkg/audio/answer.mp3",
            "media/Pkg/audio/example.mp3",
            "media/Pkg/audio/translation.mp3"
        )

        fun manager(
            failureHook: (String, String?) -> Unit = { _, _ -> },
            domainValidator: (Map<String, Path>) -> Unit = {
                LearningApplicationFactory.validatePersisted(it.getValue("data"))
            }
        ) =
            JvmLearningDataRecoveryManager(
                roots = mapOf("config" to config, "data" to data),
                safetyDirectory = safety,
                stagedDomainValidator = domainValidator,
                failureHook = failureHook
            )

        fun populate() {
            val context = LearningApplicationFactory.createPersisted(data, false)
            mediaPaths.forEachIndexed { index, relative ->
                val path = data.resolve(relative)
                Files.createDirectories(path.parent)
                Files.write(path, byteArrayOf(index.toByte(), 10, 20, 30))
            }
            val media = ContentMedia(
                image = mediaPaths[0].removePrefix("media/"),
                primaryAudio = mediaPaths[1].removePrefix("media/"),
                translatedAudio = mediaPaths[2].removePrefix("media/"),
                exampleAudio = mediaPaths[3].removePrefix("media/"),
                exampleTranslatedAudio = mediaPaths[4].removePrefix("media/")
            )
            context.contentRepository!!.save(Content(firstContent, ContentType.WORD, ContentText("alpha"), media))
            context.contentRepository.save(Content(secondContent, ContentType.WORD, ContentText("beta")))
            context.contentRepository.save(Content(orphanContent, ContentType.WORD, ContentText("retained history")))
            context.learningItemRepository!!.save(LearningItem(firstItem, firstContent, LearningMode.MEANING_RECALL, true))
            context.learningItemRepository.save(LearningItem(secondItem, secondContent, LearningMode.MEANING_RECALL, false))

            val libraryA = ContentLibraryId("library-a")
            val libraryB = ContentLibraryId("library-b")
            context.contentLibraryRepository!!.save(ContentLibrary(libraryA, LibraryDescriptor("A"), linkedSetOf(firstContent, secondContent)))
            context.contentLibraryRepository.save(ContentLibrary(libraryB, LibraryDescriptor("B"), linkedSetOf(firstContent)))
            val packageA = PackageId("package-a")
            val packageB = PackageId("package-b")
            context.contentPackageRepository!!.save(ContentPackage(packageA, PackageDescriptor("Pkg A", "1", "OPD3"), linkedSetOf(libraryA, libraryB)))
            context.contentPackageRepository.save(ContentPackage(packageB, PackageDescriptor("Pkg B", "1", "OPD3"), linkedSetOf(libraryB)))
            val domainLibraryId = requireNotNull(context.defaultLibraryId)
            listOf(packageA, packageB).forEachIndexed { index, packageId ->
                val installedId = InstalledPackageId("installed-${index + 1}")
                context.installedPackageRepository!!.save(
                    InstalledPackage.reconstitute(
                        installedId, domainLibraryId, packageId, TopicId("topic-${index + 1}"),
                        PackageName("Pkg ${index + 1}"), PackageVersion("1"), PackageState.ACTIVE,
                        Instant.ofEpochMilli(index.toLong()), if (index == 0) 2 else 1, if (index == 0) 2 else 0
                    )
                )
                val library = context.domainLibraryRepository!!.findById(domainLibraryId)!!
                context.domainLibraryRepository.save(library.registerEntry(installedId, packageId, Instant.ofEpochMilli(index.toLong())))
            }

            val before = MemoryState.new(learner, firstItem, Moment(1))
            val after = before.copyForTest()
            context.memoryStateRepository!!.save(after)
            val event = ReviewEvent(ReviewEventId("review-1"), ReviewRating.GOOD, Moment(2), TimeSpan(3), before, after)
            context.reviewEventRepository!!.append(event)
            val evidence = RecallEvidence(
                event.id, firstContent, Moment(2), ReviewRating.GOOD, RecallResult.CORRECT,
                SessionId("active-session"), SessionEvaluationPolicy.EVALUATIVE, RatingSource.STANDARD_REVIEW,
                false, Moment(2), TimeSpan(3), RecallEvidenceOrigin.EVALUATIVE_RECALL
            )
            val anchor = ChainAnchor(ReviewRating.GOOD, Moment(2), evidence, ChainAnchorReason.INITIAL_RATING)
            context.learningTrajectoryRepository!!.save(
                learner,
                LearningTrajectory.start(EvidenceChain.start(firstContent, PromotionStage.GOOD_TO_EASY, anchor))
            )

            val active = StudySession.start(SessionId("active-session"), learner, Moment(10), SessionPolicy(2, 2))
            val finished = StudySession.start(SessionId("finished-session"), learner, Moment(3), SessionPolicy(1, 1)).finish(Moment(9))
            context.studySessionRepository!!.save(active)
            context.studySessionRepository.save(finished)
            context.studyQueue.create(
                sessionId = active.id,
                createdAt = Moment(10),
                learningItemIds = listOf(firstItem, secondItem),
                itemOrigins = mapOf(firstItem to SessionItemOrigin.NEW, secondItem to SessionItemOrigin.REVIEW),
                itemContentIds = mapOf(firstItem to firstContent, secondItem to secondContent),
                configuredNewTarget = 1,
                effectiveNewWorkload = 1,
                configuredReviewTarget = 1,
                effectiveReviewWorkload = 1
            )
            Files.writeString(config.resolve("settings.properties"), "theme=dark\nlocale=vi")
            Files.writeString(safety.resolve("old.lebak"), "must-not-recurse")
            Files.writeString(data.resolve("ignored.tmp"), "temporary")
        }

        private fun MemoryState.copyForTest() = MemoryState(
            learnerId, learningItemId, stage, difficulty, stabilityDays, Moment(100), Moment(2), 1, 0
        )

        fun replaceRuntimeWithDivergentState() {
            listOf(data, config).forEach { managed ->
                Files.walk(managed).use { paths -> paths.sorted(Comparator.reverseOrder())
                    .filter { it != managed && !it.startsWith(safety) }.forEach(Files::deleteIfExists) }
            }
            Files.writeString(data.resolve("different.json"), "different")
            Files.writeString(config.resolve("settings.properties"), "theme=light")
        }

        fun snapshotManaged(): Map<String, ByteArray> = listOf("config" to config, "data" to data).flatMap { (prefix, base) ->
            Files.walk(base).use { paths -> paths.filter(Files::isRegularFile)
                .filter { !it.startsWith(safety) && !it.fileName.toString().endsWith(".tmp") }
                .map { "$prefix/${base.relativize(it).toString().replace('\\', '/')}" to Files.readAllBytes(it) }.toList() }
        }.sortedBy { it.first }.toMap()

        fun mediaHashes() = mediaPaths.associateWith { sha256(Files.readAllBytes(data.resolve(it))) }

        fun assertReloadedLogicalState() {
            val context = LearningApplicationFactory.createPersisted(data, false)
            assertEquals(2, context.installedPackageRepository!!.findAll().size)
            assertEquals(2, context.contentPackageRepository!!.findAll().size)
            assertEquals(2, context.contentLibraryRepository!!.findAll().size)
            assertEquals(listOf(firstContent, secondContent, orphanContent).toSet(), context.contentRepository!!.findAll().map { it.id }.toSet())
            assertEquals(mapOf(firstItem to true, secondItem to false), context.learningItemRepository!!.findAll().associate { it.id to it.isEnabled })
            assertEquals(1, context.memoryStateRepository!!.findAll().size)
            assertEquals(1, context.reviewEventRepository!!.findAll().size)
            assertEquals(1, context.learningTrajectoryRepository!!.findAll().size)
            assertEquals(2, context.studySessionRepository!!.findAll().size)
            assertEquals(1, context.studyQueueRepository!!.findAll().size)
            assertEquals("theme=dark\nlocale=vi", Files.readString(config.resolve("settings.properties")))
        }

        override fun close() { root.toFile().deleteRecursively() }
    }

    companion object {
        private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private fun assertSnapshot(expected: Map<String, ByteArray>, actual: Map<String, ByteArray>, label: String) {
            assertEquals(expected.keys, actual.keys, label)
            expected.forEach { (name, bytes) -> assertContentEquals(bytes, actual.getValue(name), "$label:$name") }
        }
    }
}
