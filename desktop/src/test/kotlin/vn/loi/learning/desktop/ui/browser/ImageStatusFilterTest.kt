package vn.loi.learning.desktop.ui.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.domain.content.model.ContentId

class ImageStatusFilterTest {

    private fun createItem(
        index: Int,
        id: String,
        question: String,
        imageRef: String?,
        answer: String = "nghĩa"
    ): PackageContentBrowserItem = PackageContentBrowserItem(
        index = index,
        contentId = ContentId(id),
        questionText = question,
        answerText = answer,
        pronunciation = "",
        partOfSpeech = "noun",
        group = null,
        section = null,
        lesson = "Unit 1",
        packageName = "TestPkg",
        hasImage = imageRef != null,
        hasAudio = false,
        imageRef = imageRef,
        audioRef = null,
        exampleText = "Example $question",
        exampleTranslation = "Ví dụ $question",
        learningItemCount = 1,
        learningItemIds = emptyList(),
        learningModes = emptyList(),
        tags = emptySet(),
        searchableText = "$question $answer"
    )

    @Test
    fun `isMissingImage correctly identifies null, blank, and canonical sentinel filenames`() {
        assertTrue(ImageStatusProjectionPolicy.isMissingImage(null))
        assertTrue(ImageStatusProjectionPolicy.isMissingImage(""))
        assertTrue(ImageStatusProjectionPolicy.isMissingImage("   "))
        assertTrue(ImageStatusProjectionPolicy.isMissingImage("no_image.jpg"))
        assertTrue(ImageStatusProjectionPolicy.isMissingImage("no_image.jpeg"))
        assertTrue(ImageStatusProjectionPolicy.isMissingImage("no_image.png"))
        assertTrue(ImageStatusProjectionPolicy.isMissingImage("no_image.webp"))
        assertTrue(ImageStatusProjectionPolicy.isMissingImage("media/no_image.png"))
        assertTrue(ImageStatusProjectionPolicy.isMissingImage("media\\no_image.jpg"))

        assertFalse(ImageStatusProjectionPolicy.isMissingImage("bank.jpg"))
        assertFalse(ImageStatusProjectionPolicy.isMissingImage("media/flower.png"))
        assertFalse(ImageStatusProjectionPolicy.isMissingImage("media/cat.webp"))
    }

    @Test
    fun `Missing Image filter includes missing and sentinel items and excludes valid images`() {
        val items = listOf(
            createItem(1, "1", "bank", "media/bank.jpg"),
            createItem(2, "2", "river", null),
            createItem(3, "3", "lake", "no_image.png"),
            createItem(4, "4", "ocean", "   "),
            createItem(5, "5", "sea", "media/sea.jpg")
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateImageKeys(items)
        val missingItems = ImageStatusProjectionPolicy.filter(items, ImageStatusFilter.MISSING_IMAGE, duplicateKeys)

        assertEquals(listOf("2", "3", "4"), missingItems.map { it.contentId.value })
    }

    @Test
    fun `Duplicate Image Filename returns items sharing the same valid image filename`() {
        val items = listOf(
            createItem(1, "A", "itemA", "one.jpg"),
            createItem(2, "B", "itemB", "one.jpg"),
            createItem(3, "C", "itemC", "two.jpg")
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateImageKeys(items)
        assertEquals(setOf("one.jpg"), duplicateKeys)

        val duplicates = ImageStatusProjectionPolicy.filter(items, ImageStatusFilter.DUPLICATE_IMAGE, duplicateKeys)
        assertEquals(listOf("A", "B"), duplicates.map { it.contentId.value })
    }

    @Test
    fun `Duplicate Image Filename handles three or more items in the same duplicate group`() {
        val items = listOf(
            createItem(1, "A", "itemA", "one.jpg"),
            createItem(2, "B", "itemB", "one.jpg"),
            createItem(3, "C", "itemC", "one.jpg"),
            createItem(4, "D", "itemD", "two.jpg")
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateImageKeys(items)
        val duplicates = ImageStatusProjectionPolicy.filter(items, ImageStatusFilter.DUPLICATE_IMAGE, duplicateKeys)

        assertEquals(listOf("A", "B", "C"), duplicates.map { it.contentId.value })
    }

    @Test
    fun `Placeholder sentinel images do NOT create duplicate image groups`() {
        val items = listOf(
            createItem(1, "A", "itemA", "no_image.jpg"),
            createItem(2, "B", "itemB", "no_image.jpg"),
            createItem(3, "C", "itemC", "real.jpg")
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateImageKeys(items)
        assertTrue(duplicateKeys.isEmpty(), "Placeholder sentinels must be excluded from duplicate keys")

        val missing = ImageStatusProjectionPolicy.filter(items, ImageStatusFilter.MISSING_IMAGE, duplicateKeys)
        assertEquals(listOf("A", "B"), missing.map { it.contentId.value })

        val duplicates = ImageStatusProjectionPolicy.filter(items, ImageStatusFilter.DUPLICATE_IMAGE, duplicateKeys)
        assertTrue(duplicates.isEmpty(), "Neither A nor B nor C should appear under Duplicate Image Filename")
    }

    @Test
    fun `Different extensions remain distinct and are not merged into duplicate groups`() {
        val items = listOf(
            createItem(1, "A", "itemA", "cat.jpg"),
            createItem(2, "B", "itemB", "cat.jpg"),
            createItem(3, "C", "itemC", "cat.png")
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateImageKeys(items)
        assertEquals(setOf("cat.jpg"), duplicateKeys)

        val duplicates = ImageStatusProjectionPolicy.filter(items, ImageStatusFilter.DUPLICATE_IMAGE, duplicateKeys)
        assertEquals(listOf("A", "B"), duplicates.map { it.contentId.value })
    }

    @Test
    fun `Optional Has Image filter includes only items with valid non-missing images`() {
        val items = listOf(
            createItem(1, "A", "itemA", "cat.jpg"),
            createItem(2, "B", "itemB", "no_image.png"),
            createItem(3, "C", "itemC", null),
            createItem(4, "D", "itemD", "dog.png")
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateImageKeys(items)
        val hasImage = ImageStatusProjectionPolicy.filter(items, ImageStatusFilter.HAS_IMAGE, duplicateKeys)

        assertEquals(listOf("A", "D"), hasImage.map { it.contentId.value })
    }

    @Test
    fun `Filtering preserves original relative item order`() {
        val items = listOf(
            createItem(10, "10", "zebra", "dup.jpg"),
            createItem(20, "20", "apple", "single.jpg"),
            createItem(30, "30", "mango", "dup.jpg"),
            createItem(40, "40", "banana", "dup.jpg")
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateImageKeys(items)
        val duplicates = ImageStatusProjectionPolicy.filter(items, ImageStatusFilter.DUPLICATE_IMAGE, duplicateKeys)

        assertEquals(listOf("10", "30", "40"), duplicates.map { it.contentId.value })
        assertEquals(listOf(10, 30, 40), duplicates.map { it.index })
    }

    @Test
    fun `UiState filteredItems correctly combines search query AND image status filter`() {
        val items = listOf(
            createItem(1, "1", "bank account", null),
            createItem(2, "2", "bank river", "media/river.jpg"),
            createItem(3, "3", "river bank", "media/river.jpg"),
            createItem(4, "4", "house", null),
            createItem(5, "5", "bank building", "media/bank.jpg")
        )

        val baseState = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = items
        )

        // 1. All images, no search -> all 5 items
        assertEquals(5, baseState.filteredItems.size)

        // 2. Search "bank" + Filter ALL -> 1, 2, 3, 5
        val searchOnly = baseState.copy(appliedQuery = "bank")
        assertEquals(listOf("1", "2", "3", "5"), searchOnly.filteredItems.map { it.contentId.value })

        // 3. Search "bank" + Filter MISSING_IMAGE -> only item 1
        val searchAndMissing = baseState.copy(
            appliedQuery = "bank",
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE
        )
        assertEquals(listOf("1"), searchAndMissing.filteredItems.map { it.contentId.value })

        // 4. Search "bank" + Filter DUPLICATE_IMAGE -> items 2 and 3 (share river.jpg)
        val searchAndDuplicate = baseState.copy(
            appliedQuery = "bank",
            imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE
        )
        assertEquals(listOf("2", "3"), searchAndDuplicate.filteredItems.map { it.contentId.value })

        // 5. Clear filter back to ALL -> restores all search matches 1, 2, 3, 5
        val resetFilter = searchAndDuplicate.copy(imageStatusFilter = ImageStatusFilter.ALL)
        assertEquals(listOf("1", "2", "3", "5"), resetFilter.filteredItems.map { it.contentId.value })
    }

    @Test
    fun `Duplicate filter survives deletion and recomputes duplicate groups from remaining items`() {
        val itemA = createItem(1, "A", "itemA", "one.jpg")
        val itemB = createItem(2, "B", "itemB", "one.jpg")
        val itemC = createItem(3, "C", "itemC", "two.jpg")
        val itemD = createItem(4, "D", "itemD", "two.jpg")

        val stateBefore = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = listOf(itemA, itemB, itemC, itemD),
            imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE
        )

        assertEquals(listOf("A", "B", "C", "D"), stateBefore.filteredItems.map { it.contentId.value })

        // Simulate deleting A: Remaining items B, C, D
        val stateAfter = stateBefore.copy(
            allItems = listOf(itemB, itemC, itemD),
            imageStatusFilter = stateBefore.imageStatusFilter // Preserved
        )

        assertEquals(ImageStatusFilter.DUPLICATE_IMAGE, stateAfter.imageStatusFilter)
        // B now has unique one.jpg, so it disappears. Only C and D remain duplicated.
        assertEquals(listOf("C", "D"), stateAfter.filteredItems.map { it.contentId.value })
    }

    @Test
    fun `Three-item duplicate group progressively updates across multiple deletions with filter preserved`() {
        val itemA = createItem(1, "A", "itemA", "one.jpg")
        val itemB = createItem(2, "B", "itemB", "one.jpg")
        val itemC = createItem(3, "C", "itemC", "one.jpg")

        val stateInitial = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = listOf(itemA, itemB, itemC),
            imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE
        )
        assertEquals(listOf("A", "B", "C"), stateInitial.filteredItems.map { it.contentId.value })

        // Delete A -> B and C remain duplicated
        val stateAfterDeleteA = stateInitial.copy(
            allItems = listOf(itemB, itemC),
            imageStatusFilter = stateInitial.imageStatusFilter
        )
        assertEquals(ImageStatusFilter.DUPLICATE_IMAGE, stateAfterDeleteA.imageStatusFilter)
        assertEquals(listOf("B", "C"), stateAfterDeleteA.filteredItems.map { it.contentId.value })

        // Delete B -> C is now unique, disappears from Duplicate view
        val stateAfterDeleteB = stateAfterDeleteA.copy(
            allItems = listOf(itemC),
            imageStatusFilter = stateAfterDeleteA.imageStatusFilter
        )
        assertEquals(ImageStatusFilter.DUPLICATE_IMAGE, stateAfterDeleteB.imageStatusFilter)
        assertTrue(stateAfterDeleteB.filteredItems.isEmpty())
    }

    @Test
    fun `Missing Image filter survives deletion and remaining missing items stay visible`() {
        val item1 = createItem(1, "1", "one", null)
        val item2 = createItem(2, "2", "two", null)
        val item3 = createItem(3, "3", "three", "valid.jpg")

        val stateBefore = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = listOf(item1, item2, item3),
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE
        )
        assertEquals(listOf("1", "2"), stateBefore.filteredItems.map { it.contentId.value })

        // Delete item 1
        val stateAfter = stateBefore.copy(
            allItems = listOf(item2, item3),
            imageStatusFilter = stateBefore.imageStatusFilter
        )
        assertEquals(ImageStatusFilter.MISSING_IMAGE, stateAfter.imageStatusFilter)
        assertEquals(listOf("2"), stateAfter.filteredItems.map { it.contentId.value })
    }

    @Test
    fun `Search and Image Status Filter survive deletion and recompute jointly`() {
        val item1 = createItem(1, "1", "art gallery", "dup.jpg")
        val item2 = createItem(2, "2", "art museum", "dup.jpg")
        val item3 = createItem(3, "3", "history museum", "dup.jpg")
        val item4 = createItem(4, "4", "art school", "single.jpg")

        val stateBefore = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = listOf(item1, item2, item3, item4),
            query = "art",
            appliedQuery = "art",
            imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE
        )
        // Matches "art" AND Duplicate -> item1, item2
        assertEquals(listOf("1", "2"), stateBefore.filteredItems.map { it.contentId.value })

        // Delete item1
        val stateAfter = stateBefore.copy(
            allItems = listOf(item2, item3, item4),
            query = stateBefore.query,
            appliedQuery = stateBefore.appliedQuery,
            imageStatusFilter = stateBefore.imageStatusFilter
        )
        assertEquals("art", stateAfter.appliedQuery)
        assertEquals(ImageStatusFilter.DUPLICATE_IMAGE, stateAfter.imageStatusFilter)
        // item2 still matches "art" AND dup.jpg is still duplicated with item3
        assertEquals(listOf("2"), stateAfter.filteredItems.map { it.contentId.value })
    }

    @Test
    fun `Edit membership change under Missing Image removes edited item while preserving filter`() {
        val item1 = createItem(1, "1", "one", null)
        val item2 = createItem(2, "2", "two", null)

        val stateBefore = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = listOf(item1, item2),
            imageStatusFilter = ImageStatusFilter.MISSING_IMAGE
        )
        assertEquals(listOf("1", "2"), stateBefore.filteredItems.map { it.contentId.value })

        // Item 1 is edited and given a valid image
        val item1Edited = createItem(1, "1", "one", "assigned.jpg")
        val stateAfter = stateBefore.copy(
            allItems = listOf(item1Edited, item2),
            imageStatusFilter = stateBefore.imageStatusFilter
        )
        assertEquals(ImageStatusFilter.MISSING_IMAGE, stateAfter.imageStatusFilter)
        assertEquals(listOf("2"), stateAfter.filteredItems.map { it.contentId.value })
    }

    @Test
    fun `computeDuplicateImageCounts returns exact count for duplicate filenames`() {
        val items = listOf(
            createItem(1, "A", "itemA", "one.jpg"),
            createItem(2, "B", "itemB", "one.jpg"),
            createItem(3, "C", "itemC", "two.png"),
            createItem(4, "D", "itemD", "two.png"),
            createItem(5, "E", "itemE", "two.png"),
            createItem(6, "F", "itemF", "single.jpg")
        )

        val counts = ImageStatusProjectionPolicy.computeDuplicateImageCounts(items)
        assertEquals(mapOf("one.jpg" to 2, "two.png" to 3), counts)
    }

    @Test
    fun `Duplicate Image filter groups items sharing the same filename together`() {
        val items = listOf(
            createItem(1, "1", "item1", "z_image.jpg"),
            createItem(2, "2", "item2", "a_image.jpg"),
            createItem(3, "3", "item3", "z_image.jpg"),
            createItem(4, "4", "item4", "a_image.jpg"),
            createItem(5, "5", "item5", "single.jpg")
        )

        val state = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = items,
            imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE
        )

        val filtered = state.filteredItems
        // a_image.jpg group first (item 2, 4), then z_image.jpg group (item 1, 3)
        assertEquals(listOf("2", "4", "1", "3"), filtered.map { it.contentId.value })
    }

    @Test
    fun `Explicit user selection of ALL filter changes filter normally`() {
        val state = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = emptyList(),
            imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE
        )
        val reset = state.copy(imageStatusFilter = ImageStatusFilter.ALL)
        assertEquals(ImageStatusFilter.ALL, reset.imageStatusFilter)
    }

    @Test
    fun `computeDuplicateGroups satisfies strict membership and excludes single occurrences`() {
        val itemA = createItem(1, "A", "itemA", "image1.jpg")
        val itemB = createItem(2, "B", "itemB", "image2.jpg")
        val itemC = createItem(3, "C", "itemC", "image1.jpg")
        val itemD = createItem(4, "D", "itemD", "image1.jpg")
        val itemE = createItem(5, "E", "itemE", "image3.jpg")
        val itemF = createItem(6, "F", "itemF", "image2.jpg")

        val items = listOf(itemA, itemB, itemC, itemD, itemE, itemF)
        val groups = ImageStatusProjectionPolicy.computeDuplicateGroups(items)

        assertEquals(2, groups.size)

        val group1 = groups[0]
        assertEquals("image1.jpg", group1.imageKey)
        assertEquals(listOf("A", "C", "D"), group1.items.map { it.contentId.value })

        val group2 = groups[1]
        assertEquals("image2.jpg", group2.imageKey)
        assertEquals(listOf("B", "F"), group2.items.map { it.contentId.value })
    }

    @Test
    fun `computeDuplicateGroups preserves deterministic first occurrence order and internal item order`() {
        val item1 = createItem(1, "1", "one", "z_image.jpg")
        val item2 = createItem(2, "2", "two", "a_image.jpg")
        val item3 = createItem(3, "3", "three", "z_image.jpg")
        val item4 = createItem(4, "4", "four", "a_image.jpg")
        val item5 = createItem(5, "5", "five", "a_image.jpg")

        val items = listOf(item1, item2, item3, item4, item5)
        val groups = ImageStatusProjectionPolicy.computeDuplicateGroups(items)

        assertEquals(2, groups.size)
        // Group z_image first because item1 appeared at index 1 before item2 (a_image) at index 2
        assertEquals("z_image.jpg", groups[0].imageKey)
        assertEquals(listOf("1", "3"), groups[0].items.map { it.contentId.value })

        assertEquals("a_image.jpg", groups[1].imageKey)
        assertEquals(listOf("2", "4", "5"), groups[1].items.map { it.contentId.value })
    }

    @Test
    fun `duplicateImageGroups is populated ONLY when filter is DUPLICATE_IMAGE and empty otherwise`() {
        val itemA = createItem(1, "A", "itemA", "image1.jpg")
        val itemB = createItem(2, "B", "itemB", "image1.jpg")
        val items = listOf(itemA, itemB)

        val allState = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = items,
            imageStatusFilter = ImageStatusFilter.ALL
        )
        assertTrue(allState.duplicateImageGroups.isEmpty())

        val missingState = allState.copy(imageStatusFilter = ImageStatusFilter.MISSING_IMAGE)
        assertTrue(missingState.duplicateImageGroups.isEmpty())

        val dupState = allState.copy(imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE)
        assertEquals(1, dupState.duplicateImageGroups.size)
        assertEquals("image1.jpg", dupState.duplicateImageGroups.first().imageKey)
        assertEquals(listOf("A", "B"), dupState.duplicateImageGroups.first().items.map { it.contentId.value })
    }

    @Test
    fun `Duplicate row selection identifies real content item in UI state`() {
        val itemA = createItem(1, "A", "itemA", "image1.jpg")
        val itemB = createItem(2, "B", "itemB", "image1.jpg")
        val items = listOf(itemA, itemB)

        val state = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = items,
            imageStatusFilter = ImageStatusFilter.DUPLICATE_IMAGE,
            selectedContentId = "B"
        )

        assertEquals("B", state.selectedItemInView?.contentId?.value)
        assertEquals("B", state.selectedItemAnywhere?.contentId?.value)
        assertEquals("itemB", state.selectedItemInView?.questionText)
    }

    @Test
    fun `Duplicate Question correctly identifies tablet, tablet, tablet as duplicates`() {
        val items = listOf(
            createItem(1, "1", "tablet", null),
            createItem(2, "2", "phone", null),
            createItem(3, "3", "tablet", null),
            createItem(4, "4", "tablet", null),
            createItem(5, "5", "laptop", null)
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateQuestionKeys(items)
        assertEquals(setOf("tablet"), duplicateKeys)

        val filtered = ImageStatusProjectionPolicy.filter(
            items = items,
            filter = ContentItemFilter.DUPLICATE_QUESTION,
            duplicateImageKeys = emptySet(),
            duplicateQuestionKeys = duplicateKeys
        )
        assertEquals(listOf("1", "3", "4"), filtered.map { it.contentId.value })
    }

    @Test
    fun `Duplicate Question normalizes trimmed and case-insensitive Tablet, tablet, and space-padded tablet`() {
        val items = listOf(
            createItem(1, "1", "Tablet", null),
            createItem(2, "2", " tablet ", null),
            createItem(3, "3", "other", null),
            createItem(4, "4", "TABLET", null)
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateQuestionKeys(items)
        assertEquals(setOf("tablet"), duplicateKeys)

        val filtered = ImageStatusProjectionPolicy.filter(
            items = items,
            filter = ContentItemFilter.DUPLICATE_QUESTION,
            duplicateImageKeys = emptySet(),
            duplicateQuestionKeys = duplicateKeys
        )
        assertEquals(listOf("1", "2", "4"), filtered.map { it.contentId.value })
    }

    @Test
    fun `Duplicate Question excludes blank questions and does not treat multiple blanks as duplicates`() {
        val items = listOf(
            createItem(1, "1", "", null),
            createItem(2, "2", "   ", null),
            createItem(3, "3", "\t\n", null),
            createItem(4, "4", "valid", null)
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateQuestionKeys(items)
        assertTrue(duplicateKeys.isEmpty())

        val filtered = ImageStatusProjectionPolicy.filter(
            items = items,
            filter = ContentItemFilter.DUPLICATE_QUESTION,
            duplicateImageKeys = emptySet(),
            duplicateQuestionKeys = duplicateKeys
        )
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `Duplicate Question does not fuzzy match tablet vs tablets vs tablet computer`() {
        val items = listOf(
            createItem(1, "1", "tablet", null),
            createItem(2, "2", "tablets", null),
            createItem(3, "3", "tablet computer", null)
        )

        val duplicateKeys = ImageStatusProjectionPolicy.computeDuplicateQuestionKeys(items)
        assertTrue(duplicateKeys.isEmpty())

        val filtered = ImageStatusProjectionPolicy.filter(
            items = items,
            filter = ContentItemFilter.DUPLICATE_QUESTION,
            duplicateImageKeys = emptySet(),
            duplicateQuestionKeys = duplicateKeys
        )
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `PackageContentBrowserUiState groups duplicate question items together and preserves deterministic index order`() {
        val item1 = createItem(1, "1", "zebra", null)
        val item2 = createItem(2, "2", "apple", null)
        val item3 = createItem(3, "3", "zebra", null)
        val item4 = createItem(4, "4", "apple", null)
        val item5 = createItem(5, "5", "apple", null)
        val item6 = createItem(6, "6", "unique", null)

        val state = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = listOf(item1, item2, item3, item4, item5, item6),
            imageStatusFilter = ContentItemFilter.DUPLICATE_QUESTION
        )

        val filtered = state.filteredItems
        assertEquals(5, filtered.size)
        // Group "apple" first (sorted alphabetically by question key: "apple" < "zebra")
        // Within "apple" group: index 2, 4, 5
        // Within "zebra" group: index 1, 3
        assertEquals(listOf("2", "4", "5", "1", "3"), filtered.map { it.contentId.value })
        assertEquals(listOf("apple", "apple", "apple", "zebra", "zebra"), filtered.map { it.questionText })
    }

    @Test
    fun `Duplicate Question projection does not mutate original items or canonical data`() {
        val itemA = createItem(1, "1", " Tablet ", "img.jpg")
        val itemB = createItem(2, "2", "tablet", "img.jpg")
        val originalList = listOf(itemA, itemB)

        val state = PackageContentBrowserUiState(
            installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId("pkg_1"),
            packageName = "Package 1",
            allItems = originalList,
            imageStatusFilter = ContentItemFilter.DUPLICATE_QUESTION
        )

        val filtered = state.filteredItems
        assertEquals(2, filtered.size)
        // Verify original item references and attributes are untouched
        assertEquals(" Tablet ", state.allItems[0].questionText)
        assertEquals("tablet", state.allItems[1].questionText)
        assertEquals("img.jpg", state.allItems[0].imageRef)
        assertEquals("img.jpg", state.allItems[1].imageRef)
    }
}
