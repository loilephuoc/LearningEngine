package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.*
import vn.loi.learning.domain.content.model.ContentId

class PackageContentBrowserSearchEnterPolicyTest {

    @Test
    fun `Section 20 - parseDirectItemNumber recognizes digit-only and hash-prefixed queries`() {
        assertEquals(195, PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("195"))
        assertEquals(195, PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("#195"))
        assertEquals(195, PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber(" 195 "))
        assertEquals(195, PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber(" #195 "))
        assertEquals(0, PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("0"))
        assertEquals(999999, PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("999999"))

        // Invalid direct item number queries
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("195 bank"))
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("unit195"))
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("unit 195"))
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("#"))
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber(""))
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("   "))
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("bank"))
    }

    @Test
    fun `direct item number jump resolves matching canonical index in filteredItems`() {
        val item1 = item("c1", "apple", index = 1)
        val item195 = item("c195", "bang", index = 195)
        val item200 = item("c200", "cat", index = 200)

        val target = PackageContentBrowserSearchEnterPolicy.resolveTarget(
            listOf(item1, item195, item200),
            "195"
        )
        assertEquals(item195, target)

        val targetWithHash = PackageContentBrowserSearchEnterPolicy.resolveTarget(
            listOf(item1, item195, item200),
            " #195 "
        )
        assertEquals(item195, targetWithHash)
    }

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

    private fun item(id: String, question: String, index: Int = id.hashCode()) = PackageContentBrowserItem(
        index = index, contentId = ContentId(id), questionText = question,
        answerText = "Answer $id", pronunciation = "", partOfSpeech = "WORD",
        group = null, section = null, lesson = "Lesson", packageName = "Package",
        hasImage = false, hasAudio = false, imageRef = null, audioRef = null, questionAudioRef = null,
        exampleText = null, exampleTranslation = null, learningItemCount = 0,
        learningItemIds = emptyList(), learningModes = emptyList(), tags = emptySet(),
        searchableText = PackageContentBrowserProjectionPolicy.normalizeSearchText("$question Answer $id")
    )
}
