package vn.loi.learning.application.sync

import java.nio.file.Files
import kotlin.test.*
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.port.IncrementalMediaBlobStore
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.contentmedia.JvmIncrementalMediaBlobStore

class MediaDeltaSyncServiceTest {
    private val account = SyncAccountId("account")
    private val contentId = ContentId("content-1")
    private val mp3 = "ID3canonical-audio".encodeToByteArray()
    private val mp3Other = "ID3different-audio".encodeToByteArray()
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3)

    @Test
    fun `checksum identity is byte-derived and validation rejects mismatches`() {
        val root = Files.createTempDirectory("media-sync-identity-")
        try {
            val blobs = JvmIncrementalMediaBlobStore(root)
            assertEquals(blobs.sha256(mp3), blobs.sha256(mp3.copyOf()))
            assertNotEquals(blobs.sha256(mp3), blobs.sha256(mp3Other))
            val delta = setDelta(MediaSlot.QUESTION_AUDIO, mp3)
            val context = seeded(root.resolve("app"))
            assertFailsWith<IllegalArgumentException> { context.mediaDeltaSyncService!!.provideBlob(delta, mp3Other) }
            assertFailsWith<IllegalArgumentException> {
                context.mediaDeltaSyncService!!.provideBlob(delta.copy(sizeBytes = mp3.size + 1L), mp3)
            }
            context.mediaDeltaSyncService!!.provideBlob(delta, mp3)
            context.mediaDeltaSyncService!!.provideBlob(delta, mp3)
            assertEquals(
                1L,
                Files.list(root.resolve("app/media/.sync-staging")).use { it.count() }
            )
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `all five slots add replace and remove independently`() {
        val root = Files.createTempDirectory("media-sync-slots-")
        try {
            val context = seeded(root)
            val service = context.mediaDeltaSyncService!!
            val cases = listOf(
                MediaSlot.QUESTION_AUDIO to mp3,
                MediaSlot.ANSWER_AUDIO to mp3Other,
                MediaSlot.EXAMPLE_AUDIO to mp3,
                MediaSlot.TRANSLATION_AUDIO to mp3Other,
                MediaSlot.IMAGE to png
            )
            cases.forEachIndexed { index, (slot, bytes) ->
                val before = context.contentRepository!!.findById(contentId)!!
                val delta = setDelta(slot, bytes)
                service.provideBlob(delta, bytes)
                assertIs<MediaDeltaApplyResult.Applied>(service.applyRemote(remote("add-$index", index + 1L, delta)))
                val after = context.contentRepository!!.findById(contentId)!!
                assertEquals(before.text, after.text)
                assertEquals(before.customFields, after.customFields)
                otherSlots(slot).forEach { assertEquals(slotRef(before, it), slotRef(after, it)) }
                assertEquals(delta.mediaReference, slotRef(after, slot))
            }

            cases.forEachIndexed { index, (slot, bytes) ->
                val before = context.contentRepository!!.findById(contentId)!!
                val previous = digest(root, requireNotNull(slotRef(before, slot)))
                val replacementBytes = bytes + (index + 20).toByte()
                val replacement = setDelta(slot, replacementBytes).copy(previousSha256 = previous)
                service.provideBlob(replacement, replacementBytes)
                assertIs<MediaDeltaApplyResult.Applied>(
                    service.applyRemote(remote("replace-$index", 6L + index, replacement))
                )
                val after = context.contentRepository!!.findById(contentId)!!
                otherSlots(slot).forEach { assertEquals(slotRef(before, it), slotRef(after, it)) }
                assertEquals(replacement.mediaReference, slotRef(after, slot))
            }

            cases.forEachIndexed { index, (slot, bytes) ->
                val current = context.contentRepository!!.findById(contentId)!!
                val previous = digest(root, slotRef(current, slot)!!)
                val remove = MediaDelta(slot, DeltaOperation.REMOVE, null, null, null, previousSha256 = previous)
                assertIs<MediaDeltaApplyResult.Applied>(
                    service.applyRemote(remote("remove-$index", 20L + index, remove))
                )
                assertNull(slotRef(context.contentRepository!!.findById(contentId)!!, slot))
            }
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `metadata before bytes stays pending across restart and cursor advances only after apply`() {
        val root = Files.createTempDirectory("media-sync-pending-")
        try {
            val first = seeded(root)
            val delta = setDelta(MediaSlot.QUESTION_AUDIO, mp3)
            val remote = remote("pending", 7, delta)
            assertIs<MediaDeltaApplyResult.PendingBlob>(first.mediaDeltaSyncService!!.applyRemote(remote))
            assertEquals(SyncCursor(0), first.localSyncStateRepository!!.cursor(account))
            assertEquals(1, first.localSyncStateRepository!!.pendingMedia(account).size)

            val reopened = LearningApplicationFactory.createPersisted(root, false)
            reopened.mediaDeltaSyncService!!.provideBlob(delta, mp3)
            assertIs<MediaDeltaApplyResult.Applied>(reopened.mediaDeltaSyncService!!.applyRemote(remote))
            assertEquals(SyncCursor(7), reopened.localSyncStateRepository!!.cursor(account))
            assertTrue(reopened.localSyncStateRepository!!.pendingMedia(account).isEmpty())
            assertIs<MediaDeltaApplyResult.Duplicate>(reopened.mediaDeltaSyncService!!.applyRemote(remote))

            val sameDesired = delta
            reopened.mediaDeltaSyncService!!.provideBlob(sameDesired, mp3)
            val before = reopened.contentRepository!!.findById(contentId)
            assertIs<MediaDeltaApplyResult.Applied>(
                reopened.mediaDeltaSyncService!!.applyRemote(remote("same-desired", 8, sameDesired))
            )
            assertEquals(before, reopened.contentRepository!!.findById(contentId))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `different slots converge while same-slot local pending is quarantined`() {
        val root = Files.createTempDirectory("media-sync-conflict-")
        val peerRoot = Files.createTempDirectory("media-sync-peer-")
        try {
            val context = seeded(root)
            val peer = seeded(peerRoot)
            val local = setDelta(MediaSlot.QUESTION_AUDIO, mp3)
            context.mediaDeltaSyncService!!.applyLocal(change("local", local, "desktop"), mp3)
            val desktopOutbound = context.localSyncStateRepository!!.pendingOutbox(account).single()
            peer.mediaDeltaSyncService!!.provideBlob(local, mp3)
            assertIs<MediaDeltaApplyResult.Applied>(
                peer.mediaDeltaSyncService!!.applyRemote(RemoteSyncChange(SyncRevision(1), desktopOutbound))
            )

            val image = setDelta(MediaSlot.IMAGE, png)
            peer.mediaDeltaSyncService!!.applyLocal(change("peer-image", image, "android"), png)
            val androidOutbound = peer.localSyncStateRepository!!.pendingOutbox(account)
                .single { it.eventId == SyncEventId("peer-image") }
            context.mediaDeltaSyncService!!.provideBlob(image, png)
            assertIs<MediaDeltaApplyResult.Applied>(
                context.mediaDeltaSyncService!!.applyRemote(RemoteSyncChange(SyncRevision(1), androidOutbound))
            )
            val remoteAudio = setDelta(MediaSlot.QUESTION_AUDIO, mp3Other)
            context.mediaDeltaSyncService!!.provideBlob(remoteAudio, mp3Other)
            val conflict = assertIs<MediaDeltaApplyResult.Quarantined>(
                context.mediaDeltaSyncService!!.applyRemote(remote("remote-audio", 2, remoteAudio))
            )
            assertEquals("SYNC_MEDIA_LOCAL_PENDING", conflict.diagnostic.code)

            val answer = setDelta(MediaSlot.ANSWER_AUDIO, mp3Other).copy(previousSha256 = "0".repeat(64))
            context.mediaDeltaSyncService!!.provideBlob(answer, mp3Other)
            val baseConflict = assertIs<MediaDeltaApplyResult.Quarantined>(
                context.mediaDeltaSyncService!!.applyRemote(remote("base-mismatch", 3, answer))
            )
            assertEquals("SYNC_MEDIA_BASE_MISMATCH", baseConflict.diagnostic.code)
            val actual = context.contentRepository!!.findById(contentId)!!
            assertEquals(local.mediaReference, actual.media.primaryAudio)
            assertEquals(image.mediaReference, actual.media.image)
            assertEquals(local.mediaReference, peer.contentRepository!!.findById(contentId)!!.media.primaryAudio)
        } finally { root.toFile().deleteRecursively(); peerRoot.toFile().deleteRecursively() }
    }

    @Test
    fun `missing content unsafe reference unsupported type and future version quarantine safely`() {
        val root = Files.createTempDirectory("media-sync-invalid-")
        try {
            val context = seeded(root)
            val valid = setDelta(MediaSlot.QUESTION_AUDIO, mp3)
            val missing = remote("missing", 1, valid, entity = "missing")
            assertEquals("SYNC_MEDIA_CONTENT_NOT_FOUND", quarantine(context, missing))
            val unsafe = valid.copy(mediaReference = "../escape.mp3")
            assertEquals("SYNC_MEDIA_REFERENCE_UNSAFE", quarantine(context, remote("unsafe", 2, unsafe)))
            val wrongType = valid.copy(slot = MediaSlot.IMAGE)
            assertEquals("SYNC_MEDIA_TYPE_UNSUPPORTED", quarantine(context, remote("type", 3, wrongType)))
            assertEquals("SYNC_MEDIA_UNSUPPORTED_PAYLOAD", quarantine(context, remote("future", 4, valid, version = 2)))
            assertEquals(content(), context.contentRepository!!.findById(contentId))
            assertEquals(4, context.localSyncStateRepository!!.quarantines(account).size)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `shared managed asset survives first removal and is collected after last reference`() {
        val root = Files.createTempDirectory("media-sync-gc-")
        try {
            val context = seeded(root)
            val secondId = ContentId("content-2")
            context.contentRepository!!.save(content().copy(id = secondId))
            val delta = setDelta(MediaSlot.QUESTION_AUDIO, mp3)
            context.mediaDeltaSyncService!!.applyLocal(change("local-1", delta, entity = contentId.value), mp3)
            context.mediaDeltaSyncService!!.applyLocal(change("local-2", delta, entity = secondId.value), mp3)
            val storage = JvmContentMediaStorage(root.resolve("media"))
            assertTrue(storage.exists(requireNotNull(delta.mediaReference)))

            val remove1 = MediaDelta(
                MediaSlot.QUESTION_AUDIO, DeltaOperation.REMOVE, null, null, null,
                previousSha256 = delta.sha256
            )
            context.mediaDeltaSyncService!!.applyLocal(change("remove-1", remove1, entity = contentId.value))
            assertTrue(storage.exists(requireNotNull(delta.mediaReference)))
            assertEquals(1, context.localSyncStateRepository!!.mediaGcCandidates().size)
            context.mediaDeltaSyncService!!.applyLocal(change("remove-2", remove1, entity = secondId.value))
            assertFalse(storage.exists(requireNotNull(delta.mediaReference)))
            assertTrue(context.localSyncStateRepository!!.mediaGcCandidates().isEmpty())
            Files.writeString(root.resolve("media").resolve("unknown.txt"), "keep")
            context.mediaDeltaSyncService!!.collectGarbage()
            assertTrue(Files.exists(root.resolve("media").resolve("unknown.txt")))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `GC delete failure keeps durable candidate for restart retry`() {
        val root = Files.createTempDirectory("media-sync-gc-retry-")
        try {
            val context = seeded(root)
            val delegate = JvmIncrementalMediaBlobStore(root.resolve("media"))
            var failDelete = true
            val failing = object : IncrementalMediaBlobStore by delegate {
                override fun deleteManaged(reference: String): Boolean {
                    if (failDelete) {
                        failDelete = false
                        throw IllegalStateException("injected delete failure")
                    }
                    return delegate.deleteManaged(reference)
                }
            }
            val service = MediaDeltaSyncService(
                context.contentRepository!!, failing, context.localSyncStateRepository!!,
                LocalSyncCoordinator(context.localSyncStateRepository, context.transactionRunner!!)
            )
            val delta = setDelta(MediaSlot.QUESTION_AUDIO, mp3)
            service.applyLocal(change("set-gc-retry", delta), mp3)
            service.applyLocal(
                change(
                    "remove-gc-retry",
                    MediaDelta(
                        MediaSlot.QUESTION_AUDIO, DeltaOperation.REMOVE, null, null, null,
                        previousSha256 = delta.sha256
                    )
                )
            )
            assertEquals(1, context.localSyncStateRepository.mediaGcCandidates().size)
            assertTrue(JvmContentMediaStorage(root.resolve("media")).exists(requireNotNull(delta.mediaReference)))

            val reopened = LearningApplicationFactory.createPersisted(root, false)
            assertEquals(MediaGcResult(deleted = 1, retained = 0), reopened.mediaDeltaSyncService!!.collectGarbage())
            assertTrue(reopened.localSyncStateRepository!!.mediaGcCandidates().isEmpty())
            assertFalse(JvmContentMediaStorage(root.resolve("media")).exists(requireNotNull(delta.mediaReference)))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `media apply preserves learning state and review apply preserves media`() {
        val root = Files.createTempDirectory("media-sync-learning-")
        val originRoot = Files.createTempDirectory("media-sync-review-origin-")
        try {
            val target = seeded(root, learning = true)
            val origin = seeded(originRoot, learning = true)
            val media = setDelta(MediaSlot.ANSWER_AUDIO, mp3)
            target.mediaDeltaSyncService!!.provideBlob(media, mp3)
            val beforeMemory = target.memoryStateRepository!!.findAll()
            val beforeReviews = target.reviewEventRepository!!.findAll()
            target.mediaDeltaSyncService!!.applyRemote(remote("media", 1, media))
            assertEquals(beforeMemory, target.memoryStateRepository!!.findAll())
            assertEquals(beforeReviews, target.reviewEventRepository!!.findAll())

            val review = origin.engine.review(
                ReviewCommand(ReviewEventId("review-1"), LearnerId("learner"), LearningItemId("item-1"),
                    ReviewRating.GOOD, Moment(1_000))
            )
            val reviewDelta = ReviewEventDelta(
                review.reviewEvent.id.value, "item-1", "learner", contentId = contentId.value,
                rating = review.reviewEvent.rating.name, reviewedAtEpochMillis = 1_000,
                ratingSource = review.reviewEvent.source.name,
                stateBefore = review.reviewEvent.stateBefore.toProof(),
                expectedStateAfter = review.reviewEvent.stateAfter.toProof()
            )
            val mediaBefore = target.contentRepository!!.findById(contentId)!!.media
            target.reviewDeltaSyncService!!.applyRemote(
                RemoteSyncChange(SyncRevision(2), change("review-sync", reviewDelta))
            )
            assertEquals(mediaBefore, target.contentRepository!!.findById(contentId)!!.media)
        } finally { root.toFile().deleteRecursively(); originRoot.toFile().deleteRecursively() }
    }

    private fun quarantine(context: LearningApplicationContext, remote: RemoteSyncChange): String =
        assertIs<MediaDeltaApplyResult.Quarantined>(context.mediaDeltaSyncService!!.applyRemote(remote)).diagnostic.code

    private fun setDelta(slot: MediaSlot, bytes: ByteArray): MediaDelta {
        val root = Files.createTempDirectory("media-sync-hash-")
        return try {
            val sha = JvmIncrementalMediaBlobStore(root).sha256(bytes)
            val mime = if (slot == MediaSlot.IMAGE) "image/png" else "audio/mpeg"
            val ext = if (slot == MediaSlot.IMAGE) "png" else "mp3"
            MediaDelta(slot, DeltaOperation.SET, "sync/$sha.$ext", sha, bytes.size.toLong(), mime)
        } finally { root.toFile().deleteRecursively() }
    }

    private fun remote(
        event: String, revision: Long, delta: MediaDelta, entity: String = contentId.value, version: Int = 1
    ) = RemoteSyncChange(SyncRevision(revision), change(event, delta, entity = entity, version = version))

    private fun change(
        event: String, delta: SyncDelta, device: String = "remote",
        entity: String = contentId.value, version: Int = 1
    ) = OutboundSyncChange(
        account, SyncEventId(event), IdempotencyKey("key-$event"), SyncDeviceId(device),
        SyncEntityId(entity), version, delta
    )

    private fun seeded(root: java.nio.file.Path, learning: Boolean = false): LearningApplicationContext =
        LearningApplicationFactory.createPersisted(root, false).also { context ->
            context.contentRepository!!.save(content())
            if (learning) context.learningItemRepository!!.save(
                LearningItem(LearningItemId("item-1"), contentId, LearningMode.MEANING_RECOGNITION)
            )
        }

    private fun content() = Content(
        contentId, ContentType.WORD,
        ContentText("Question", "Answer", exampleText = "Example", exampleTranslation = "Translation"),
        customFields = ContentCustomFields(setOf(ContentCustomField(ContentFieldId("unknown"), "preserve")))
    )

    private fun digest(root: java.nio.file.Path, reference: String): String =
        requireNotNull(JvmIncrementalMediaBlobStore(root.resolve("media")).digestReference(reference))

    private fun otherSlots(slot: MediaSlot) = MediaSlot.entries.filterNot { it == slot }
    private fun slotRef(content: Content, slot: MediaSlot): String? = when (slot) {
        MediaSlot.QUESTION_AUDIO -> content.media.primaryAudio
        MediaSlot.ANSWER_AUDIO -> content.media.translatedAudio
        MediaSlot.EXAMPLE_AUDIO -> content.media.exampleAudio
        MediaSlot.TRANSLATION_AUDIO -> content.media.exampleTranslatedAudio
        MediaSlot.IMAGE -> content.media.image
    }
}
