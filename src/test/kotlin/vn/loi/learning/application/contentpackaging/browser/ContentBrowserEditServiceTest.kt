package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
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
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

/**
 * CP2 Tests — Persist Content Edit.
 *
 * TC01: updateTextFields persists mutable text fields
 * TC02: ContentId và ContentType không thay đổi sau save
 * TC03: save không xóa LearningItem
 * TC04: Media và Metadata (lesson/tags) không thay đổi sau save
 * TC05: updateTextFields với questionText trống → IllegalArgumentException
 * TC06: persistEdit qua Facade → reload state phản ánh data mới
 */
class ContentBrowserEditServiceTest {

    // ---------------------------------------------------------------------------
    // TC01 — updateTextFields persists mutable text fields
    // ---------------------------------------------------------------------------
    @Test
    fun `TC01 updateTextFields persists question answer pronunciation and example`() {
        val (appContext, _) = createFixture(contentCount = 3)
        val service = ContentBrowserEditService(appContext.contentRepository!!)

        service.updateTextFields(
            contentId = ContentId("cnt-1"),
            questionText = "Updated Question",
            answerText = "Updated Answer",
            pronunciation = "/ʌpˈdeɪtɪd/",
            partOfSpeech = "verb",
            exampleText = "She updated it.",
            exampleTranslation = "Cô ấy đã cập nhật nó."
        )

        val saved = appContext.contentRepository!!.findById(ContentId("cnt-1"))
        assertNotNull(saved)
        assertEquals("Updated Question", saved.text.primaryText)
        assertEquals("Updated Answer", saved.text.translatedText)
        assertEquals("/ʌpˈdeɪtɪd/", saved.text.pronunciation)
        assertEquals("She updated it.", saved.text.exampleText)
        assertEquals("Cô ấy đã cập nhật nó.", saved.text.exampleTranslation)
    }

    // ---------------------------------------------------------------------------
    // TC02 — ContentId và ContentType không thay đổi
    // ---------------------------------------------------------------------------
    @Test
    fun `TC02 ContentId and ContentType are preserved after updateTextFields`() {
        val (appContext, _) = createFixture(contentCount = 2)
        val service = ContentBrowserEditService(appContext.contentRepository!!)
        val originalContent = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!

        service.updateTextFields(
            contentId = ContentId("cnt-1"),
            questionText = "New Question",
            answerText = "New Answer",
            pronunciation = "",
            partOfSpeech = "",
            exampleText = "",
            exampleTranslation = ""
        )

        val saved = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!
        assertEquals(originalContent.id, saved.id)
        assertEquals(originalContent.type, saved.type)
    }

    // ---------------------------------------------------------------------------
    // TC03 — LearningItems không bị xóa sau khi save
    // ---------------------------------------------------------------------------
    @Test
    fun `TC03 save does not delete LearningItems`() {
        val (appContext, _) = createFixture(contentCount = 2)
        val service = ContentBrowserEditService(appContext.contentRepository!!)
        val initialItems = appContext.learningItemRepository!!.findAllEnabled()

        service.updateTextFields(
            contentId = ContentId("cnt-1"),
            questionText = "Updated",
            answerText = "Updated",
            pronunciation = "",
            partOfSpeech = "",
            exampleText = "",
            exampleTranslation = ""
        )

        val finalItems = appContext.learningItemRepository!!.findAllEnabled()
        assertEquals(initialItems.size, finalItems.size)
    }

    // ---------------------------------------------------------------------------
    // TC04 — Media và Metadata không thay đổi
    // ---------------------------------------------------------------------------
    @Test
    fun `TC04 media and metadata are preserved after save`() {
        val (appContext, _) = createFixture(contentCount = 2)
        val service = ContentBrowserEditService(appContext.contentRepository!!)
        val original = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!

        service.updateTextFields(
            contentId = ContentId("cnt-1"),
            questionText = "Updated",
            answerText = "",
            pronunciation = "",
            partOfSpeech = "",
            exampleText = "",
            exampleTranslation = ""
        )

        val saved = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!
        assertEquals(original.media.primaryAudio, saved.media.primaryAudio)
        assertEquals(original.media.image, saved.media.image)
        assertEquals(original.metadata.lesson, saved.metadata.lesson)
        assertEquals(original.metadata.tags, saved.metadata.tags)
    }

    // ---------------------------------------------------------------------------
    // TC05 — blank questionText → IllegalArgumentException
    // ---------------------------------------------------------------------------
    @Test
    fun `TC05 updateTextFields with blank question throws IllegalArgumentException`() {
        val (appContext, _) = createFixture(contentCount = 1)
        val service = ContentBrowserEditService(appContext.contentRepository!!)

        assertFails {
            service.updateTextFields(
                contentId = ContentId("cnt-1"),
                questionText = "   ",
                answerText = "Answer",
                pronunciation = "",
                partOfSpeech = "",
                exampleText = "",
                exampleTranslation = ""
            )
        }

        // Original unchanged
        val unchanged = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!
        assertEquals("Question 1", unchanged.text.primaryText)
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun createFixture(contentCount: Int): Pair<LearningApplicationContext, InstalledPackageId> {
        val appContext = LearningApplicationFactory.createInMemory()
        val instId = InstalledPackageId("inst-edit-svc")
        val pkgId = PackageId("pkg-edit-svc")
        val libId = ContentLibraryId("lib-edit-svc")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = appContext.defaultLibraryId!!,
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Test Package", "OPD3"),
                name = PackageName("Test Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = java.time.Instant.now(),
                contentCount = contentCount,
                learningItemCount = contentCount * 2
            )
        )

        val contentIds = (1..contentCount).map { ContentId("cnt-$it") }.toSet()
        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id = pkgId,
                descriptor = PackageDescriptor(name = "Test Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = libId,
                descriptor = LibraryDescriptor(name = "Test Library"),
                contentIds = contentIds
            )
        )

        for (i in 1..contentCount) {
            val cid = ContentId("cnt-$i")
            appContext.contentRepository!!.save(
                Content(
                    id = cid,
                    type = ContentType.WORD,
                    text = ContentText(
                        primaryText = "Question $i",
                        translatedText = "Answer $i",
                        pronunciation = "pron-$i"
                    ),
                    media = ContentMedia(
                        primaryAudio = if (i % 2 == 0) "audio-$i.mp3" else null,
                        image = if (i % 3 == 0) "img-$i.jpg" else null
                    ),
                    metadata = ContentMetadata(lesson = "Lesson $i", tags = setOf("tag-$i"))
                )
            )
            for (m in 1..2) {
                appContext.learningItemRepository!!.save(
                    LearningItem(
                        id = LearningItemId("item-$i-$m"),
                        contentId = cid,
                        mode = LearningMode.entries[(m - 1) % LearningMode.entries.size]
                    )
                )
            }
        }

        return appContext to instId
    }
}
