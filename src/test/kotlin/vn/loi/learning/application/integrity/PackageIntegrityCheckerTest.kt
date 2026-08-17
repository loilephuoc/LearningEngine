package vn.loi.learning.application.integrity

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.persistence.memory.*
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class PackageIntegrityCheckerTest {
    @Test
    fun `healthy multi-library package counts shared content once and includes disabled item`() {
        val fixture = Fixture(twoLibraries = true, disabledItem = true)
        val report = fixture.checker().check(fixture.installedId, fixture.media)
        assertEquals(IntegrityStatus.HEALTHY, report.status)
        assertEquals(IntegritySummary(0, 0, 0), report.summary)
    }

    @Test
    fun `count drift reports typed errors without correcting canonical package`() {
        val fixture = Fixture(storedContentCount = 9, storedItemCount = 8)
        val before = fixture.installed.findById(fixture.installedId)
        val report = fixture.checker().check(fixture.installedId, fixture.media)
        assertEquals(setOf("CONTENT_COUNT_MISMATCH", "LEARNING_ITEM_COUNT_MISMATCH"),
            report.findings.filter { it.severity == IntegritySeverity.ERROR }.map { it.code }.toSet())
        assertEquals(before, fixture.installed.findById(fixture.installedId))
    }

    @Test
    fun `missing selected ContentPackage returns a package error without mutation`() {
        val fixture = Fixture()
        fixture.removeSelectedContentPackage()

        val report = fixture.checker().check(fixture.installedId, fixture.media)

        assertEquals(listOf("MISSING_CONTENT_PACKAGE"), report.findings.map { it.code })
        assertEquals(IntegrityStatus.ERRORS, report.status)
        assertEquals(1, fixture.installed.findAll().size)
    }

    @Test
    fun `missing Content referenced by selected package membership is reported`() {
        val fixture = Fixture()
        fixture.removeOwnedContent()

        val report = fixture.checker().check(fixture.installedId, fixture.media)

        assertTrue(report.findings.any { it.code == "MISSING_CONTENT_REFERENCED_BY_LIBRARY" && it.entityId == "content" })
    }

    @Test
    fun `repeat scan over unchanged repositories produces equivalent deterministic findings`() {
        val fixture = Fixture(storedContentCount = 2, storedItemCount = 2)

        val first = fixture.checker().check(fixture.installedId, fixture.media)
        val second = fixture.checker().check(fixture.installedId, fixture.media)

        assertEquals(first, second)
    }

    @Test
    fun `selected package report excludes unrelated incomplete lifecycle finding`() {
        val fixture = Fixture()
        fixture.addUninstalledPackage("unrelated", "OPD2")

        val report = fixture.checker().check(fixture.installedId, fixture.media)

        assertTrue(report.findings.none { it.entityId == "unrelated" })
        assertTrue(report.findings.none { it.code == "INCOMPLETE_INSTALL_LIFECYCLE" })
    }

    @Test
    fun `package A report never adopts package B lifecycle findings`() {
        val fixture = Fixture()
        fixture.addUninstalledPackage("package-b", "Package B")

        val reportA = fixture.checker().check(fixture.installedId, fixture.media)

        assertTrue(reportA.findings.none { it.entityId == "package-b" })
    }

    @Test
    fun `generic local orphan without package evidence is excluded from package report`() {
        val fixture = Fixture()
        fixture.addOrphan(ContentId("local-orphan"), ContentMetadata(), itemCount = 1)

        val report = fixture.checker().check(fixture.installedId, fixture.media)

        assertTrue(report.findings.none { it.entityId == "local-orphan" })
    }

    @Test
    fun `package-related orphan is attributed and explains matching count drift without causal claim`() {
        val fixture = Fixture(storedContentCount = 2, storedItemCount = 6)
        fixture.addOrphan(
            ContentId("package-orphan"),
            ContentMetadata(
                group = "Pkg",
                source = "Pkg.json",
                tags = setOf("pkg")
            ),
            itemCount = 5
        )

        val report = fixture.checker().check(fixture.installedId, fixture.media)

        val orphan = report.findings.single { it.code == "PACKAGE_RELATED_ORPHAN_CONTENT" }
        assertEquals("package-orphan", orphan.entityId)
        assertEquals("Pkg.json", orphan.details["source"])
        val contentMismatch = report.findings.single { it.code == "CONTENT_COUNT_MISMATCH" }
        assertEquals("2", contentMismatch.details["storedContentCount"])
        assertEquals("1", contentMismatch.details["canonicalOwnedContentCount"])
        assertEquals("1", contentMismatch.details["packageRelatedOrphanContentOutsideMembership"])
        val itemMismatch = report.findings.single { it.code == "LEARNING_ITEM_COUNT_MISMATCH" }
        assertEquals("6", itemMismatch.details["storedLearningItemCount"])
        assertEquals("1", itemMismatch.details["canonicalOwnedLearningItemCount"])
        assertEquals("5", itemMismatch.details["associatedLearningItemsOutsideMembership"])
        assertTrue(contentMismatch.details.getValue("reconciliationEvidence").contains("causality is not assumed"))
    }

    @Test
    fun `selected package graph failures are reported while unrelated orphan item is excluded`() {
        val fixture = Fixture(registerLifecycle = false, includeMissingLibrary = true)
        fixture.items.save(LearningItem(LearningItemId("orphan-item"), ContentId("missing-content"), LearningMode.DICTATION))
        val report = fixture.checker().check(fixture.installedId, fixture.media)
        val codes = report.findings.map { it.code }.toSet()
        assertTrue("INCOMPLETE_INSTALL_LIFECYCLE" in codes)
        assertTrue("MISSING_CONTENT_LIBRARY" in codes)
        assertTrue("ORPHAN_LEARNING_ITEM" !in codes)
        assertEquals(IntegrityStatus.ERRORS, report.status)
    }

    @Test
    fun `all canonical media slots resolve and missing references are reported by slot`() {
        val fixture = Fixture(allMediaSlots = true)
        val healthy = fixture.checker().check(fixture.installedId, fixture.media)
        assertTrue(healthy.findings.none { it.code == "MISSING_MEDIA" })
        Files.delete(fixture.mediaFiles.getValue("Pkg/audio/example-translation.mp3"))
        val report = fixture.checker().check(fixture.installedId, fixture.media)
        val missing = report.findings.single { it.code == "MISSING_MEDIA" }
        assertEquals("exampleTranslatedAudio", missing.details["slot"])
    }

    @Test
    fun `compatible same-basename fallback is not classified as MISSING_MEDIA`() {
        val fixture = Fixture(allMediaSlots = true)
        val content = fixture.contents.findById(ContentId("content"))!!
        val mediaDir = Files.createTempDirectory("media_fallback_test")
        try {
            val storage = JvmContentMediaStorage(mediaDir)
            storage.store("Pkg", "fallback_test.jpg", byteArrayOf(1, 2, 3))
            val pngRef = "Pkg/fallback_test.png"
            fixture.contents.save(content.copy(media = ContentMedia(image = pngRef)))

            val report = fixture.checker().check(fixture.installedId, storage)
            assertTrue(report.findings.none { it.code == "MISSING_MEDIA" })
        } finally {
            mediaDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `no_image sentinel produces no MISSING_MEDIA warning`() {
        val fixture = Fixture(allMediaSlots = true)
        val content = fixture.contents.findById(ContentId("content"))!!
        fixture.contents.save(content.copy(media = ContentMedia(image = "no_image.jpg")))

        val report = fixture.checker().check(fixture.installedId, fixture.media)
        assertTrue(report.findings.none { it.code == "MISSING_MEDIA" })
    }

    @Test
    fun `repeated media reference is resolved once`() {
        val fixture = Fixture(allMediaSlots = true)
        val content = fixture.contents.findById(ContentId("content"))!!
        val repeated = "Pkg/image/a.png"
        fixture.contents.save(content.copy(media = ContentMedia(image = repeated, primaryAudio = repeated, exampleAudio = repeated)))
        var resolves = 0
        val counting = object : ContentMediaStorage {
            override fun store(packageName: String, fileName: String, content: ByteArray) = error("read only")
            override fun resolve(relativePath: String): Path? { resolves++; return fixture.media.resolve(relativePath) }
            override fun exists(relativePath: String) = resolve(relativePath) != null
        }
        fixture.checker().check(fixture.installedId, counting)
        assertEquals(1, resolves)
    }

    @Test
    fun `scan leaves repositories and media bytes exactly unchanged`() {
        val fixture = Fixture(storedContentCount = 7, allMediaSlots = true)
        val packageBefore = fixture.packages.findAll()
        val librariesBefore = fixture.contentLibraries.findAll()
        val contentsBefore = fixture.contents.findAll()
        val itemsBefore = fixture.items.findAll()
        val installedBefore = fixture.installed.findAll()
        val bytesBefore = fixture.mediaFiles.mapValues { Files.readAllBytes(it.value) }

        fixture.checker().check(fixture.installedId, fixture.media)

        assertEquals(packageBefore, fixture.packages.findAll())
        assertEquals(librariesBefore, fixture.contentLibraries.findAll())
        assertEquals(contentsBefore, fixture.contents.findAll())
        assertEquals(itemsBefore, fixture.items.findAll())
        assertEquals(installedBefore, fixture.installed.findAll())
        bytesBefore.forEach { (reference, bytes) -> assertContentEquals(bytes, Files.readAllBytes(fixture.mediaFiles.getValue(reference))) }
    }

    @Test
    fun `dangling package session and queue references are warnings and are not reconciled`() {
        val fixture = Fixture()
        val session = StudySession.start(
            SessionId("session"), LearnerId("learner"), Moment(1), SessionPolicy(),
            includedContentIds = setOf(ContentId("content"), ContentId("missing-content")),
            installedPackageId = fixture.installedId
        )
        val queue = StudyQueueSnapshot.create(
            session.id, Moment(1), listOf(LearningItemId("missing-item")),
            itemContentIds = mapOf(LearningItemId("missing-item") to ContentId("missing-content"))
        )
        val sessions = object : StudySessionRepository {
            override fun findById(sessionId: SessionId) = session.takeIf { it.id == sessionId }
            override fun findActiveByLearner(learnerId: LearnerId) = session
            override fun save(session: StudySession) = error("read only")
            override fun findAll() = listOf(session)
        }
        val queues = object : StudyQueueRepository {
            override fun findAll() = listOf(queue)
            override fun findBySessionId(sessionId: SessionId) = queue.takeIf { it.sessionId == sessionId }
            override fun save(snapshot: StudyQueueSnapshot) = error("read only")
            override fun deleteBySessionId(sessionId: SessionId) = error("read only")
        }
        val report = fixture.checker(sessions, queues).check(fixture.installedId, fixture.media)
        assertTrue(report.findings.any { it.code == "DANGLING_STUDY_SESSION_ITEM" })
        assertTrue(report.findings.any { it.code == "DANGLING_STUDY_QUEUE_ITEM" })
        assertEquals(listOf(session), sessions.findAll())
        assertEquals(listOf(queue), queues.findAll())
    }

    @Test
    fun `persisted integrity scan leaves every canonical json and media byte unchanged`() {
        val directory = Files.createTempDirectory("integrity-persisted-")
        val context = LearningApplicationFactory.createPersisted(directory, reconcilePartOfSpeechRegistryOnCreate = false)
        val packageId = PackageId("persistent-pkg")
        val installedId = InstalledPackageId("persistent-installed")
        val libraryId = requireNotNull(context.defaultLibraryId)
        val contentLibraryId = ContentLibraryId("persistent-content-library")
        val contentId = ContentId("persistent-content")
        val itemId = LearningItemId("persistent-item")
        val storage = JvmContentMediaStorage(directory.resolve("media"))
        val asset = storage.store("Persistent_Pkg", "audio/question.mp3", byteArrayOf(9, 8, 7, 6))
        context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("persisted"),
            ContentMedia(primaryAudio = asset.relativePath)))
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL, false))
        context.contentLibraryRepository!!.save(ContentLibrary(contentLibraryId, LibraryDescriptor("Persistent"), setOf(contentId)))
        context.contentPackageRepository!!.save(ContentPackage(packageId,
            PackageDescriptor("Persistent_Pkg", "1.0", "OPD3"), setOf(contentLibraryId)))
        val installedPackage = InstalledPackage.reconstitute(installedId, libraryId, packageId, TopicId("persistent-topic"),
            PackageName("Persistent_Pkg"), PackageVersion("1.0"), PackageState.ACTIVE, Instant.EPOCH, 99, 1)
        context.installedPackageRepository!!.save(installedPackage)
        val canonical = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository.save(canonical.registerEntry(installedId, packageId, Instant.EPOCH))
        val before = snapshotFiles(directory)

        val report = context.packageIntegrityChecker!!.check(installedId, storage)

        assertTrue(report.findings.any { it.code == "CONTENT_COUNT_MISMATCH" })
        val after = snapshotFiles(directory)
        assertEquals(before.keys, after.keys)
        before.forEach { (path, bytes) -> assertContentEquals(bytes, after.getValue(path), path) }
    }

    private fun snapshotFiles(root: Path): Map<String, ByteArray> =
        Files.walk(root).use { paths -> paths.iterator().asSequence().filter(Files::isRegularFile).sorted().associate { path ->
            root.relativize(path).toString() to Files.readAllBytes(path)
        } }

    private class Fixture(
        storedContentCount: Int = 1,
        storedItemCount: Int = 1,
        twoLibraries: Boolean = false,
        disabledItem: Boolean = false,
        registerLifecycle: Boolean = true,
        includeMissingLibrary: Boolean = false,
        allMediaSlots: Boolean = false
    ) {
        val installedId = InstalledPackageId("installed-pkg")
        private val packageId = PackageId("pkg")
        private val libraryId = LibraryId("default-library")
        private val contentId = ContentId("content")
        val installed = InMemoryInstalledPackageRepository()
        val packages = InMemoryContentPackageRepository()
        val contentLibraries = InMemoryContentLibraryRepository()
        val contents = InMemoryContentRepository()
        val items = InMemoryLearningItemRepository()
        private val libraries = InMemoryLibraryRepository()
        private val mediaRoot = Files.createTempDirectory("integrity-media-")
        val mediaFiles = linkedMapOf<String, Path>()
        val media = object : ContentMediaStorage {
            override fun store(packageName: String, fileName: String, content: ByteArray) = error("read only")
            override fun resolve(relativePath: String): Path? = mediaFiles[relativePath]?.takeIf(Files::exists)
            override fun exists(relativePath: String): Boolean = resolve(relativePath) != null
        }

        init {
            val refs = if (allMediaSlots) listOf(
                "Pkg/image/a.png", "Pkg/audio/primary.mp3", "Pkg/audio/translated.mp3",
                "Pkg/audio/example.mp3", "Pkg/audio/example-translation.mp3"
            ) else emptyList()
            refs.forEachIndexed { index, reference ->
                val path = mediaRoot.resolve(reference.substringAfterLast('/'))
                Files.write(path, byteArrayOf(index.toByte(), 4, 2))
                mediaFiles[reference] = path
            }
            val mediaValue = if (allMediaSlots) ContentMedia(
                image = refs[0], primaryAudio = refs[1], translatedAudio = refs[2],
                exampleAudio = refs[3], exampleTranslatedAudio = refs[4]
            ) else ContentMedia()
            contents.save(Content(contentId, ContentType.WORD, ContentText("word"), mediaValue))
            items.save(LearningItem(LearningItemId("item"), contentId, LearningMode.MEANING_RECALL, !disabledItem))
            val firstLibraryId = ContentLibraryId("content-library-a")
            contentLibraries.save(ContentLibrary(firstLibraryId, LibraryDescriptor("A"), setOf(contentId)))
            val libraryIds = linkedSetOf(firstLibraryId)
            if (twoLibraries) {
                val second = ContentLibraryId("content-library-b")
                contentLibraries.save(ContentLibrary(second, LibraryDescriptor("B"), setOf(contentId)))
                libraryIds += second
            }
            if (includeMissingLibrary) libraryIds += ContentLibraryId("missing-library")
            packages.save(ContentPackage(packageId, PackageDescriptor("Pkg", "1.0", "OPD3"), libraryIds))
            val installedPackage = InstalledPackage.reconstitute(
                installedId, libraryId, packageId, TopicId("topic"), PackageName("Pkg"), PackageVersion("1.0"),
                PackageState.ACTIVE, Instant.EPOCH, storedContentCount, storedItemCount
            )
            installed.save(installedPackage)
            val entries = if (registerLifecycle) listOf(LibraryEntry(installedId, packageId, Instant.EPOCH)) else emptyList()
            libraries.save(Library.reconstitute(libraryId, "Library", entries))
        }

        fun checker(sessions: StudySessionRepository? = null, queues: StudyQueueRepository? = null) =
            PackageIntegrityChecker(installed, packages, contentLibraries, contents, items, libraries, media,
                studySessions = sessions, studyQueues = queues, clock = { Instant.EPOCH })

        fun addUninstalledPackage(id: String, name: String) {
            packages.save(
                ContentPackage(
                    PackageId(id),
                    PackageDescriptor(name, "1.0", "OPD3"),
                    emptySet()
                )
            )
        }

        fun addOrphan(id: ContentId, metadata: ContentMetadata, itemCount: Int) {
            contents.save(Content(id, ContentType.WORD, ContentText("orphan"), metadata = metadata))
            LearningMode.entries.take(itemCount).forEach { mode ->
                items.save(LearningItem(LearningItemId("${id.value}-${mode.name}"), id, mode))
            }
        }

        fun removeSelectedContentPackage() {
            packages.deleteById(packageId)
        }

        fun removeOwnedContent() {
            contents.deleteById(contentId)
        }
    }
}
