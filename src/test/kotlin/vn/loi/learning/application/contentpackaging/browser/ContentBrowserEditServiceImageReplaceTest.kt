package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType

class ContentBrowserEditServiceImageReplaceTest {

    private class InMemoryContentRepo(
        val store: MutableMap<ContentId, Content> = mutableMapOf()
    ) : ContentRepository {
        override fun findById(contentId: ContentId): Content? = store[contentId]
        override fun findAll(): List<Content> = store.values.toList()
        override fun save(content: Content) { store[content.id] = content }
        override fun deleteById(contentId: ContentId) { store.remove(contentId) }
    }

    @Test
    fun `replaceContentImage modifies only the image reference while preserving all other fields`() {
        val repo = InMemoryContentRepo()
        val service = ContentBrowserEditService(contentRepository = repo)

        val contentId = ContentId("content_101")
        val initialContent = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "bank",
                translatedText = "ngân hàng",
                pronunciation = "/bæŋk/",
                exampleText = "I deposited money in the bank.",
                exampleTranslation = "Tôi đã gửi tiền vào ngân hàng."
            ),
            media = ContentMedia(
                image = "media/old_bank.jpg",
                primaryAudio = "media/bank_q.mp3",
                translatedAudio = "media/bank_a.mp3",
                exampleAudio = "media/bank_ex.mp3",
                exampleTranslatedAudio = "media/bank_ex_vi.mp3"
            ),
            customFields = ContentCustomFields()
        )
        repo.save(initialContent)

        // Replace image
        val updated = service.replaceContentImage(contentId, "media/unique123_bank.jpg")

        assertEquals(contentId, updated.id)
        assertEquals("media/unique123_bank.jpg", updated.media.image)
        // All other media references preserved
        assertEquals("media/bank_q.mp3", updated.media.primaryAudio)
        assertEquals("media/bank_a.mp3", updated.media.translatedAudio)
        assertEquals("media/bank_ex.mp3", updated.media.exampleAudio)
        assertEquals("media/bank_ex_vi.mp3", updated.media.exampleTranslatedAudio)
        // All text fields preserved
        assertEquals("bank", updated.text.primaryText)
        assertEquals("ngân hàng", updated.text.translatedText)
        assertEquals("/bæŋk/", updated.text.pronunciation)
        assertEquals("I deposited money in the bank.", updated.text.exampleText)
        assertEquals("Tôi đã gửi tiền vào ngân hàng.", updated.text.exampleTranslation)

        // Persisted state matches
        val persisted = repo.findById(contentId)
        assertEquals("media/unique123_bank.jpg", persisted?.media?.image)
    }

    @Test
    fun `replaceContentImage handles null or sentinel correctly`() {
        val repo = InMemoryContentRepo()
        val service = ContentBrowserEditService(contentRepository = repo)

        val contentId = ContentId("content_102")
        val initialContent = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(primaryText = "river"),
            media = ContentMedia(image = "media/river.jpg")
        )
        repo.save(initialContent)

        // Set to no_image sentinel -> canonicalized to null
        val updated = service.replaceContentImage(contentId, "no_image.png")
        assertNull(updated.media.image)
    }
}
