package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.*
import vn.loi.learning.domain.content.model.ContentId

class PackageContentBrowserSearchEnterPolicyTest {
    @Test
    fun `one normalized exact Question wins among partial results`() {
        val exact = item("exact", "ＴＯ MAKE")
        val target = PackageContentBrowserSearchEnterPolicy.resolveTarget(
            listOf(item("up", "to make up"), exact, item("sure", "to make sure")), " to make "
        )
        assertEquals(exact.contentId, target?.contentId)
    }

    @Test
    fun `multiple identical exact Questions never choose an arbitrary ContentId`() {
        val matches = listOf(item("a", "to make"), item("b", "to make"), item("c", "to make"))
        assertNull(PackageContentBrowserSearchEnterPolicy.resolveTarget(matches, "to make"))
    }

    @Test
    fun `unique non-exact resolves while ambiguous and zero results do not`() {
        val sole = item("sole", "to lean against")
        assertEquals(sole, PackageContentBrowserSearchEnterPolicy.resolveTarget(listOf(sole), "lean"))
        assertNull(PackageContentBrowserSearchEnterPolicy.resolveTarget(listOf(sole, item("b", "lean over")), "lean"))
        assertNull(PackageContentBrowserSearchEnterPolicy.resolveTarget(emptyList(), "lean"))
    }

    private fun item(id: String, question: String) = PackageContentBrowserItem(
        index = id.hashCode(), contentId = ContentId(id), questionText = question,
        answerText = "Answer $id", pronunciation = "", partOfSpeech = "WORD",
        group = null, section = null, lesson = "Lesson", packageName = "Package",
        hasImage = false, hasAudio = false, imageRef = null, audioRef = null, questionAudioRef = null,
        exampleText = null, exampleTranslation = null, learningItemCount = 0,
        learningItemIds = emptyList(), learningModes = emptyList(), tags = emptySet(),
        searchableText = PackageContentBrowserProjectionPolicy.normalizeSearchText("$question Answer $id")
    )
}
