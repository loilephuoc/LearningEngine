package vn.loi.learning.application.integrity

import java.nio.file.Files
import java.time.Instant
import kotlin.test.*
import vn.loi.learning.application.port.*
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.library.repository.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory

class RecoverLegacyOpd2PackageTest {
    @Test
    fun `full OPD2 fixture preserves exact history and is idempotent`() {
        val fixture = Fixture()
        val history = fixture.history()

        val first = fixture.command().execute(backupVerified = true)

        assertEquals(Opd2RecoveryStatus.RECOVERED, first.status)
        assertEquals(RecoverLegacyOpd2Package.EXPECTED_PUBLISHED_MEDIA_FILES, first.publishedMediaCount)
        assertEquals(IntegrityStatus.HEALTHY, first.integrity?.status)
        assertEquals(history, fixture.history())
        assertNotNull(fixture.installed.findById(RecoverLegacyOpd2Package.INSTALLED))
        assertTrue(fixture.library.findById(fixture.defaultLibrary)!!.hasPackage(RecoverLegacyOpd2Package.INSTALLED))

        val recovered = fixture.snapshot()
        assertEquals(Opd2RecoveryStatus.ALREADY_RECOVERED, fixture.command().execute(true).status)
        assertEquals(recovered, fixture.snapshot())
        assertEquals(1, fixture.media.prepareCount)
    }

    @Test
    fun `backup and exact graph history guards reject before media staging`() {
        val fixture = Fixture()
        val before = fixture.snapshot()
        assertEquals(Opd2RecoveryStatus.BACKUP_REQUIRED, fixture.command().execute(false).status)
        assertEquals(before, fixture.snapshot())
        fixture.memory.remove(RecoverLegacyOpd2Package.EXPECTED_MEMORY_ITEMS.first())
        assertEquals(Opd2RecoveryStatus.PRECONDITION_FAILED, fixture.command().execute(true).status)
        assertEquals(0, fixture.media.prepareCount)
    }

    @Test
    fun `every staged publish lifecycle commit and integrity failure returns exact pre state`() {
        val failures = listOf(
            "source-mapping", "staging-first", "staging-middle", "staging-final",
            "publish-first", "publish-middle", "publish-final", "installed-write",
            "library-write", "transaction-commit", "post-integrity"
        )
        failures.forEach { failure ->
            val fixture = Fixture(failure)
            val before = fixture.snapshot()
            assertFailsWith<IllegalStateException>(failure) { fixture.command().execute(true) }
            assertEquals(before, fixture.snapshot(), failure)
            assertFalse(fixture.media.published, failure)
        }
    }

    @Test
    fun `missing primary audio is mode degrading without wrong fallback`() {
        val fixture = Fixture()
        val content = fixture.contents.findById(ContentId("legacy-content-c753b8e03656a6f9e7bfb1bc"))!!
        val projection = vn.loi.learning.application.recall.ContentRecallCapabilityResolver.resolve(content)
        assertFalse(projection.supports(vn.loi.learning.domain.study.recall.RecallMode.LISTENING))
        assertFalse(projection.supports(vn.loi.learning.domain.study.recall.RecallMode.DICTATION))
        assertTrue(projection.supports(vn.loi.learning.domain.study.recall.RecallMode.TYPING))
        assertNull(content.media.primaryAudio)
        assertNotNull(content.media.translatedAudio)
        assertNotNull(content.media.exampleAudio)
    }

    private class Fixture(private val failure: String? = null) {
        private val context = LearningApplicationFactory.createInMemory()
        val contents = context.contentRepository!!
        private val items = context.learningItemRepository!!
        private val contentPackages = context.contentPackageRepository!!
        private val contentLibraries = context.contentLibraryRepository!!
        val installed = context.installedPackageRepository!!
        val library = context.domainLibraryRepository!!
        val defaultLibrary = context.defaultLibraryId!!
        val memory = TestMemoryRepository()
        private val events = TestReviewRepository()
        val media = FakeMedia(failure)
        private val mediaFile = Files.createTempFile("opd2-integrity-", ".bin").also { Files.write(it, byteArrayOf(1)) }

        init {
            val special = listOf(
                "legacy-content-004343ba9598b49a4d86c0bb",
                "legacy-content-006c40c30fc8d73f2c0c0284",
                "legacy-content-00751ebedbf5b05cef6556d4",
                "legacy-content-008a82df23ad25a0458183c8",
                "legacy-content-c753b8e03656a6f9e7bfb1bc"
            )
            val ids = linkedSetOf<ContentId>()
            repeat(RecoverLegacyOpd2Package.EXPECTED_CONTENTS) { index ->
                val id = ContentId(special.getOrNull(index) ?: "legacy-content-fixture-$index")
                ids += id
                val missingPrimary = id.value == "legacy-content-c753b8e03656a6f9e7bfb1bc"
                val media = ContentMedia(
                    primaryAudio = if (missingPrimary) null else "OPD_2nd/audio-$index.mp3",
                    translatedAudio = "OPD_2nd/audio-vi-$index.mp3",
                    image = if (index == 6) null else "OPD_2nd/image-$index.jpg",
                    exampleAudio = if (index == 209) null else "OPD_2nd/example-$index.mp3",
                    exampleTranslatedAudio = if (index in MISSING_EXAMPLE_TRANSLATIONS) null else "OPD_2nd/example-vi-$index.mp3"
                )
                contents.save(Content(id, ContentType.WORD, ContentText("word-$index", "meaning-$index", exampleText = "A word-$index example."), media))
                RecoverLegacyOpd2Package.OPD2_MODES.forEach { mode ->
                    val suffix = mode.name.lowercase().replace('_', '-')
                    items.save(LearningItem(LearningItemId("${id.value}-$suffix"), id, mode))
                }
            }
            contentLibraries.save(ContentLibrary(RecoverLegacyOpd2Package.CONTENT_LIBRARY, LibraryDescriptor("OPD_2nd"), ids))
            contentPackages.save(ContentPackage(RecoverLegacyOpd2Package.PACKAGE, PackageDescriptor("OPD_2nd", "1", "OPD3"), setOf(RecoverLegacyOpd2Package.CONTENT_LIBRARY)))
            seedHistory()
        }

        fun command(): RecoverLegacyOpd2Package {
            val storage = object : ContentMediaStorage {
                override fun store(packageName: String, fileName: String, content: ByteArray) = error("read only")
                override fun resolve(relativePath: String) = mediaFile
                override fun exists(relativePath: String) = true
            }
            val checker = PackageIntegrityChecker(
                installed, contentPackages, contentLibraries, contents, items, library, storage,
                memoryStates = memory, reviewEvents = events
            )
            return RecoverLegacyOpd2Package(
                contents, items, contentPackages, contentLibraries, installed, library, defaultLibrary,
                memory, events, context.learningTrajectoryRepository!!, context.studySessionRepository!!,
                context.studyQueueRepository!!, context.transactionRunner!!, media,
                integrityCheck = { id -> if (failure == "post-integrity") error(failure) else checker.check(id, storage) },
                clock = { Instant.EPOCH },
                failureHook = { phase -> if (failure == phase) error(failure) }
            )
        }

        fun history() = memory.findAll().sortedBy { it.learningItemId.value } to events.findAll()
        fun snapshot() = listOf(installed.findAll(), library.findById(defaultLibrary), history(), media.published)

        private fun seedHistory() {
            RecoverLegacyOpd2Package.EXPECTED_MEMORY_ITEMS.sortedBy { it.value }.forEachIndexed { itemIndex, itemId ->
                var state = MemoryState.new(LearnerId("default-learner"), itemId, Moment(0))
                val itemEventIds = EVENT_IDS_BY_ITEM.getValue(itemId)
                itemEventIds.forEachIndexed { reviewIndex, eventId ->
                    val reviewedAt = Moment((itemIndex * 10 + reviewIndex + 1).toLong())
                    val after = MemoryState(
                        state.learnerId, itemId, LearningStage.REVIEW, 2.1, 2.0 + reviewIndex,
                        Moment(reviewedAt.epochMillis + 100), reviewedAt, state.reviewCount + 1, 0
                    )
                    events.append(ReviewEvent(ReviewEventId(eventId), ReviewRating.GOOD, reviewedAt, null, state, after))
                    state = after
                }
                memory.save(state)
            }
        }

        private companion object {
            val MISSING_EXAMPLE_TRANSLATIONS = setOf(44, 236, 241, 849, 915, 1231, 1237, 1415, 1714, 1944, 2258, 2376, 2390)
            val EVENT_IDS_BY_ITEM = mapOf(
                LearningItemId("legacy-content-004343ba9598b49a4d86c0bb-dictation") to listOf("77b51311-bd1a-4695-ae6a-52d34beff9ce", "a9b4e3db-ee30-4fd1-9235-0511a7914779"),
                LearningItemId("legacy-content-004343ba9598b49a4d86c0bb-listening-recognition") to listOf("1360f1a3-a57d-4d22-9da0-27e42cf13fe6"),
                LearningItemId("legacy-content-006c40c30fc8d73f2c0c0284-dictation") to listOf("825daa73-7ca3-404e-9133-df96fe178af3", "d4755006-026d-4676-8ec7-2aafec613a70"),
                LearningItemId("legacy-content-00751ebedbf5b05cef6556d4-dictation") to listOf("4e4311dc-7ab1-47e8-80b9-d4c5ab96a75a", "83ecc304-2608-4da4-bbaa-b4cb0c02d676"),
                LearningItemId("legacy-content-008a82df23ad25a0458183c8-dictation") to listOf("6990b4b9-e9ab-4971-86e1-268b349bc394")
            )
        }
    }

    private class TestMemoryRepository : MemoryStateRepository {
        private val values = linkedMapOf<LearningItemId, MemoryState>()
        override fun findAll() = values.values.toList()
        override fun find(learnerId: LearnerId, learningItemId: LearningItemId) = values[learningItemId]
        override fun save(memoryState: MemoryState) { values[memoryState.learningItemId] = memoryState }
        fun remove(id: LearningItemId) { values.remove(id) }
    }

    private class TestReviewRepository : ReviewEventRepository {
        private val values = mutableListOf<ReviewEvent>()
        override fun findAll() = values.toList()
        override fun append(event: ReviewEvent) { values += event }
        override fun findAll(learnerId: LearnerId) = values.filter { it.learnerId == learnerId }
        override fun findAll(learnerId: LearnerId, learningItemId: LearningItemId) = values.filter { it.learnerId == learnerId && it.learningItemId == learningItemId }
    }

    private class FakeMedia(private val failure: String?) : Opd2RecoveryMediaPort {
        var prepareCount = 0
        var published = false
        override fun prepare(expectedCanonicalReferences: Set<String>): PreparedOpd2RecoveryMedia {
            prepareCount++
            if (failure == "source-mapping" || failure?.startsWith("staging") == true) error(failure)
            return object : PreparedOpd2RecoveryMedia {
                override val recoverableReferenceCount = RecoverLegacyOpd2Package.EXPECTED_RECOVERABLE_MEDIA_REFERENCES
                override val publishedFileCount = RecoverLegacyOpd2Package.EXPECTED_PUBLISHED_MEDIA_FILES
                override val missingReferences = RecoverLegacyOpd2Package.EXPECTED_MISSING_MEDIA
                override fun publish() { if (failure?.startsWith("publish") == true) error(failure); published = true }
                override fun verifyPublished() = Unit
                override fun rollbackPublished() { published = false }
                override fun complete() = Unit
                override fun close() = Unit
            }
        }
    }

}
