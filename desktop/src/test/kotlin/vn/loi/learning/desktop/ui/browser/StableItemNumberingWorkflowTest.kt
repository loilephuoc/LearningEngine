package vn.loi.learning.desktop.ui.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserSearchEnterPolicy
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class StableItemNumberingWorkflowTest {

    private fun createItem(
        canonicalIndex: Int,
        id: String,
        question: String,
        imageRef: String?,
        lesson: String = "Unit 1"
    ): PackageContentBrowserItem = PackageContentBrowserItem(
        index = canonicalIndex,
        contentId = ContentId(id),
        questionText = question,
        answerText = "Meaning of $question",
        pronunciation = "",
        partOfSpeech = "noun",
        group = null,
        section = null,
        lesson = lesson,
        packageName = "Vocabulary_In_Use",
        hasImage = !imageRef.isNullOrBlank(),
        hasAudio = false,
        imageRef = imageRef,
        audioRef = null,
        exampleText = "Example $question",
        exampleTranslation = "Ví dụ $question",
        learningItemCount = 1,
        learningItemIds = emptyList(),
        learningModes = emptyList(),
        tags = emptySet(),
        searchableText = "$question Meaning of $question $lesson"
    )

    @Test
    fun `Section 17 - Filter removal does not renumber remaining items`() {
        val item4 = createItem(4, "c4", "abduction", null)
        val item191 = createItem(191, "c191", "balanced diet", null)
        val item195 = createItem(195, "c195", "bang", null)
        val item198 = createItem(198, "c198", "banking", null)
        val item202 = createItem(202, "c202", "bark up the wrong tree", null)

        val stateBefore = PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("pkg_1"),
            packageName = "Vocabulary_In_Use",
            allItems = listOf(item4, item191, item195, item198, item202),
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE
        )

        assertEquals(listOf(4, 191, 195, 198, 202), stateBefore.filteredItems.map { it.index })
        assertEquals(listOf("abduction", "balanced diet", "bang", "banking", "bark up the wrong tree"), stateBefore.filteredItems.map { it.questionText })

        val item191Saved = item191.copy(imageRef = "balanced_diet.jpg", hasImage = true)
        val stateAfter = stateBefore.copy(
            allItems = listOf(item4, item191Saved, item195, item198, item202)
        )

        assertEquals(listOf(4, 195, 198, 202), stateAfter.filteredItems.map { it.index })
        assertEquals(listOf("abduction", "bang", "banking", "bark up the wrong tree"), stateAfter.filteredItems.map { it.questionText })
    }

    @Test
    fun `Section 18 - Search does not renumber matching items`() {
        val item10 = createItem(10, "c10", "apple", null)
        val item100 = createItem(100, "c100", "bank", null)
        val item250 = createItem(250, "c250", "banking", null)

        val state = PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("pkg_1"),
            packageName = "Vocabulary_In_Use",
            allItems = listOf(item10, item100, item250),
            query = "bank",
            appliedQuery = "bank"
        )

        val filtered = state.filteredItems
        assertEquals(2, filtered.size)
        assertEquals(listOf(100, 250), filtered.map { it.index })
        assertEquals(listOf("bank", "banking"), filtered.map { it.questionText })
    }

    @Test
    fun `Section 19 - Same item preserves stable number across all filters`() {
        val item1 = createItem(1, "c1", "abandon", "abandon.jpg")
        val item50 = createItem(50, "c50", "bank", null)
        val item100 = createItem(100, "c100", "cat", "cat.jpg")
        val item150 = createItem(150, "c150", "dog", "cat.jpg")

        val allItems = listOf(item1, item50, item100, item150)
        val baseState = PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("pkg_1"),
            packageName = "Vocabulary_In_Use",
            allItems = allItems
        )

        val allFiltered = baseState.copy(imageStatusFilter = ImageStatusFilter.ALL).filteredItems
        assertEquals(50, allFiltered.first { it.contentId.value == "c50" }.index)
        assertEquals(100, allFiltered.first { it.contentId.value == "c100" }.index)

        val missingFiltered = baseState.copy(imageStatusFilter = ImageStatusFilter.MISSING_IMAGE).filteredItems
        assertEquals(1, missingFiltered.size)
        assertEquals(50, missingFiltered.first().index)

        val hasImageFiltered = baseState.copy(imageStatusFilter = ImageStatusFilter.HAS_IMAGE).filteredItems
        assertEquals(listOf(1, 100, 150), hasImageFiltered.map { it.index })

        val duplicateFiltered = baseState.copy(imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE).filteredItems
        assertEquals(listOf(100, 150), duplicateFiltered.map { it.index })
    }

    @Test
    fun `Section 20 - Editor selection number matches Content Explorer row number`() {
        val item4 = createItem(4, "c4", "abduction", null)
        val item191 = createItem(191, "c191", "balanced diet", null)
        val item195 = createItem(195, "c195", "bang", null)

        val state = PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("pkg_1"),
            packageName = "Vocabulary_In_Use",
            allItems = listOf(item4, item191, item195),
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE,
            selectedContentId = "c191"
        )

        val explorerItem = state.selectedItemInView
        val editorItem = state.selectedItemAnywhere

        assertEquals(191, explorerItem?.index)
        assertEquals(191, editorItem?.index)
        assertEquals(explorerItem?.index, editorItem?.index)
    }

    @Test
    fun `Section 21 - Direct numeric jump selects canonical item 195 within current filtered items`() {
        val item4 = createItem(4, "c4", "abduction", null)
        val item191 = createItem(191, "c191", "balanced diet", null)
        val item195 = createItem(195, "c195", "bang", null)
        val item198 = createItem(198, "c198", "banking", null)

        val state = PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("pkg_1"),
            packageName = "Vocabulary_In_Use",
            allItems = listOf(item4, item191, item195, item198),
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE
        )

        val target = PackageContentBrowserSearchEnterPolicy.resolveTarget(state.filteredItems, "195")
        assertEquals(item195, target)
        assertEquals(195, target?.index)
        assertEquals("bang", target?.questionText)

        // With leading hash
        val targetHash = PackageContentBrowserSearchEnterPolicy.resolveTarget(state.filteredItems, " #195 ")
        assertEquals(item195, targetHash)
    }

    @Test
    fun `Section 22 - Direct numeric jump preserves active MISSING_IMAGE filter`() {
        val item4 = createItem(4, "c4", "abduction", null)
        val item195 = createItem(195, "c195", "bang", null)

        val state = PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("pkg_1"),
            packageName = "Vocabulary_In_Use",
            allItems = listOf(item4, item195),
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE
        )

        val target = PackageContentBrowserSearchEnterPolicy.resolveTarget(state.filteredItems, "195")
        assertEquals(item195, target)
        // Filter remains MISSING_IMAGE
        assertEquals(ImageStatusFilter.MISSING_IMAGE, state.imageStatusFilter)
    }

    @Test
    fun `Section 23 - Direct numeric jump for item not in current filter returns null without changing filter`() {
        val item4 = createItem(4, "c4", "abduction", null)
        val item191 = createItem(191, "c191", "balanced diet", "balanced_diet.jpg") // has image
        val item195 = createItem(195, "c195", "bang", null)

        val state = PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("pkg_1"),
            packageName = "Vocabulary_In_Use",
            allItems = listOf(item4, item191, item195),
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE // 191 is excluded from missing
        )

        // Item 191 exists in allItems but not in filteredItems
        assertTrue(state.allItems.any { it.index == 191 })
        assertTrue(state.filteredItems.none { it.index == 191 })

        val target = PackageContentBrowserSearchEnterPolicy.resolveTarget(state.filteredItems, "191")
        assertNull(target)
        assertEquals(ImageStatusFilter.MISSING_IMAGE, state.imageStatusFilter)
    }

    @Test
    fun `Section 24 - Direct numeric jump after Save and Refresh locates item in refreshed projection`() {
        val item4 = createItem(4, "c4", "abduction", null)
        val item191 = createItem(191, "c191", "balanced diet", null)
        val item195 = createItem(195, "c195", "bang", null)
        val item198 = createItem(198, "c198", "banking", null)

        val stateBefore = PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("pkg_1"),
            packageName = "Vocabulary_In_Use",
            allItems = listOf(item4, item191, item195, item198),
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE
        )

        // Save image for 191
        val item191Saved = item191.copy(imageRef = "balanced_diet.jpg", hasImage = true)
        val stateAfter = stateBefore.copy(
            allItems = listOf(item4, item191Saved, item195, item198)
        )

        // Jump to 195 in refreshed projection
        val target = PackageContentBrowserSearchEnterPolicy.resolveTarget(stateAfter.filteredItems, "195")
        assertEquals(item195, target)
        assertEquals(listOf(4, 195, 198), stateAfter.filteredItems.map { it.index })
    }

    @Test
    fun `Section 25 - Normal text search queries preserve existing text matching`() {
        val item1 = createItem(1, "c1", "bank", null)
        val item2 = createItem(2, "c2", "banking account", null)
        val item3 = createItem(3, "c3", "dog", null)

        val items = listOf(item1, item2, item3)

        // "bank" is not a direct item number
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("bank"))

        // Text search finds exact Question match "bank"
        val target = PackageContentBrowserSearchEnterPolicy.resolveTarget(items, "bank")
        assertEquals(item1, target)

        // "unit 195" is not a direct item number
        assertNull(PackageContentBrowserSearchEnterPolicy.parseDirectItemNumber("unit 195"))
    }
}
