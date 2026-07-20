package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class BundlePackageContentImporterTest {

    @Test
    fun `imports exported contents and learning items`() {
        val content = Content(
            id = ContentId("content-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "apple",
                translatedText = "quả táo",
                pronunciation = "/ˈæp.əl/"
            ),
            media = ContentMedia(
                primaryAudio = "audio/apple.mp3",
                image = "images/apple.png"
            ),
            metadata = ContentMetadata(
                title = "Apple",
                group = "Vocabulary",
                section = "Food",
                lesson = "Lesson 1",
                tags = setOf("fruit"),
                source = "test"
            )
        )

        val learningItem = LearningItem(
            id = LearningItemId("learning-item-1"),
            contentId = content.id,
            mode = LearningMode.MEANING_RECOGNITION,
            isEnabled = true
        )

        val bundle = PackageImportBundle(
            files = mapOf(
                PackageImportBundle.METADATA_FILE to """{"name":"test","version":"1.0","format":"OPD3"}""",
                PackageImportBundle.CONTENTS_FILE to PackageExportContentsSerializer().serialize(listOf(content)),
                PackageImportBundle.LEARNING_ITEMS_FILE to PackageExportLearningItemsSerializer().serialize(listOf(learningItem)),
                PackageImportBundle.MANIFEST_FILE to """{"name":"test","version":"1.0","format":"OPD3","contentCount":1,"learningItemCount":1}"""
            )
        )

        val imported = BundlePackageContentImporter().importContent(bundle)

        assertEquals(listOf(content), imported.contents)
        assertEquals(listOf(learningItem), imported.learningItems)
        assertEquals(emptyList(), imported.libraries)
        assertEquals(emptyList(), imported.warnings)
    }
}