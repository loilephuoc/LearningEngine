package vn.loi.learning.desktop.ui.browser.imagereuse

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class ImageReuseDiscoveryEngineTest {

    private fun createBrowserItem(
        contentId: String,
        question: String,
        answer: String = "nghĩa",
        imageRef: String? = null,
        lesson: String = "Unit 1",
        packageName: String = "TestPkg"
    ): PackageContentBrowserItem = PackageContentBrowserItem(
        index = 1,
        contentId = ContentId(contentId),
        questionText = question,
        answerText = answer,
        pronunciation = "",
        partOfSpeech = "noun",
        group = null,
        section = null,
        lesson = lesson,
        packageName = packageName,
        hasImage = imageRef != null,
        hasAudio = false,
        imageRef = imageRef,
        audioRef = null,
        exampleText = "Example for $question",
        exampleTranslation = "Ví dụ cho $question",
        learningItemCount = 1,
        learningItemIds = emptyList(),
        learningModes = emptyList(),
        tags = emptySet(),
        searchableText = "$question $answer"
    )

    @Test
    fun `normalizeQuestion normalizes case, leading trailing spaces, and repeated whitespace`() {
        assertEquals("after all", ImageReuseDiscoveryEngine.normalizeQuestion("After all"))
        assertEquals("after all", ImageReuseDiscoveryEngine.normalizeQuestion("  after   all  "))
        assertEquals("after all", ImageReuseDiscoveryEngine.normalizeQuestion("AFTER ALL"))
        assertEquals("bank", ImageReuseDiscoveryEngine.normalizeQuestion(" Bank "))
    }

    @Test
    fun `discoverCandidates matches normalized questions and excludes invalid images`() {
        val tempDir = Files.createTempDirectory("media_test")
        val storage = JvmContentMediaStorage(tempDir)

        // Store a valid image for source
        storage.store("SourcePkg", "river.jpg", byteArrayOf(1, 2, 3))
        storage.store("SourcePkg", "financial.jpg", byteArrayOf(4, 5, 6))

        val targetItemMissingImage = createBrowserItem("t1", "bank", "ngân hàng", imageRef = null)
        val targetItemWithImage = createBrowserItem("t2", "river", "con sông", imageRef = "media/river_old.jpg")
        val targetItemUnrelated = createBrowserItem("t3", "house", "ngôi nhà", imageRef = null)

        val sourceOption = ImageReusePackageOption("src1", "SourcePkg", "1.0.0")
        val sourceItems = listOf(
            createBrowserItem("s1", "Bank", "bờ sông", imageRef = "media/river.jpg"),
            createBrowserItem("s2", "BANK  ", "ngân hàng", imageRef = "media/financial.jpg"),
            createBrowserItem("s3", "House", "ngôi nhà", imageRef = "no_image.png") // sentinel image -> excluded
        )

        val discovered = ImageReuseDiscoveryEngine.discoverCandidates(
            targetItems = listOf(targetItemMissingImage, targetItemWithImage, targetItemUnrelated),
            sourcePackagesWithItems = mapOf(sourceOption to sourceItems),
            scope = ImageReuseScope.MISSING_IMAGES_ONLY,
            mediaStorage = storage
        )

        // Only targetItemMissingImage ("bank") should match
        assertEquals(1, discovered.size)
        val targetBank = discovered.first()
        assertEquals("t1", targetBank.targetContentId)
        assertEquals("bank", targetBank.question)

        // Preserves duplicate candidates: both s1 (bờ sông) and s2 (ngân hàng) are present
        assertEquals(2, targetBank.candidates.size)
        assertEquals("s1", targetBank.candidates[0].sourceContentId)
        assertEquals("bờ sông", targetBank.candidates[0].answer)
        assertEquals("s2", targetBank.candidates[1].sourceContentId)
        assertEquals("ngân hàng", targetBank.candidates[1].answer)
    }

    @Test
    fun `discoverCandidates with ALL_MATCHING_ITEMS includes target items that already have images`() {
        val tempDir = Files.createTempDirectory("media_test_all")
        val storage = JvmContentMediaStorage(tempDir)
        storage.store("SourcePkg", "flower.jpg", byteArrayOf(10, 20))

        val targetItemWithImage = createBrowserItem("t1", "flower", "bông hoa", imageRef = "media/old_flower.jpg")
        val sourceOption = ImageReusePackageOption("src1", "SourcePkg", "1.0.0")
        val sourceItems = listOf(
            createBrowserItem("s1", "Flower", "hoa", imageRef = "media/flower.jpg")
        )

        val discovered = ImageReuseDiscoveryEngine.discoverCandidates(
            targetItems = listOf(targetItemWithImage),
            sourcePackagesWithItems = mapOf(sourceOption to sourceItems),
            scope = ImageReuseScope.ALL_MATCHING_ITEMS,
            mediaStorage = storage
        )

        assertEquals(1, discovered.size)
        assertEquals("t1", discovered.first().targetContentId)
        assertEquals("media/old_flower.jpg", discovered.first().currentImageRef)
        assertEquals(1, discovered.first().candidates.size)
    }

    @Test
    fun `applyImageReuse copies media to target package and updates target content image only`() {
        val tempDir = Files.createTempDirectory("media_apply_test")
        val storage = JvmContentMediaStorage(tempDir)

        // Store source image
        val sourceBytes = byteArrayOf(100, 101, 102)
        val sourceAsset = storage.store("SourcePkg", "sample_tree.jpg", sourceBytes)

        val repo = object : ContentRepository {
            val store = mutableMapOf<ContentId, Content>()
            override fun findById(contentId: ContentId): Content? = store[contentId]
            override fun findAll(): List<Content> = store.values.toList()
            override fun save(content: Content) { store[content.id] = content }
            override fun deleteById(contentId: ContentId) { store.remove(contentId) }
        }
        val editService = ContentBrowserEditService(contentRepository = repo)

        val targetContentId = ContentId("target_tree_1")
        val initialContent = Content(
            id = targetContentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "tree",
                translatedText = "cái cây",
                exampleText = "Big tree"
            ),
            media = ContentMedia(
                image = null,
                primaryAudio = "media/tree.mp3"
            )
        )
        repo.save(initialContent)

        val candidate = ImageReuseSourceCandidate(
            sourcePackageId = "src1",
            sourcePackageName = "SourcePkg",
            sourceContentId = "s_tree_1",
            question = "tree",
            answer = "cây",
            translation = "cây",
            exampleText = "Tall tree",
            partOfSpeech = "noun",
            imageRef = sourceAsset.relativePath
        )

        val newRef = ImageReuseDiscoveryEngine.applyImageReuse(
            targetPackageName = "TargetPkg",
            targetContentId = targetContentId.value,
            sourceCandidate = candidate,
            mediaStorage = storage,
            editService = editService
        )

        assertNotNull(newRef)
        assertTrue(newRef.contains("sample_tree.jpg"))

        // Target content updated
        val updatedContent = repo.findById(targetContentId)
        assertNotNull(updatedContent)
        assertEquals(newRef, updatedContent.media.image)
        // Non-image fields preserved
        assertEquals("tree", updatedContent.text.primaryText)
        assertEquals("cái cây", updatedContent.text.translatedText)
        assertEquals("Big tree", updatedContent.text.exampleText)
        assertEquals("media/tree.mp3", updatedContent.media.primaryAudio)

        // Target owns the copied media file in TargetPkg storage
        val resolvedTargetMedia = storage.resolve(newRef)
        assertNotNull(resolvedTargetMedia)
        assertTrue(Files.exists(resolvedTargetMedia))
        assertEquals(sourceBytes.toList(), Files.readAllBytes(resolvedTargetMedia).toList())

        // Source media remains 100% intact and unaffected
        val resolvedSourceMedia = storage.resolve(sourceAsset.relativePath)
        assertNotNull(resolvedSourceMedia)
        assertTrue(Files.exists(resolvedSourceMedia))
    }
}
