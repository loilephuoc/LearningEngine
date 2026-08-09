package vn.loi.learning.android.library

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test

class AndroidLibraryExperienceTest {
    private val facade = AndroidLibraryFacade(vn.loi.learning.infrastructure.LearningApplicationFactory.createInMemory())
    private val root = AndroidLibraryState.Root(
        allPackages = listOf(
            AndroidLibraryPackageItem("pkg-vi", "Từ vựng căn bản", "1.2.0", 990, "ACTIVE", true, true),
            AndroidLibraryPackageItem("pkg-en", "English grammar", "2.0.0", 40, "ARCHIVED", false, false)
        ),
        allCollections = listOf(AndroidLibraryCollectionItem("collection-1", "Tiếng Việt", setOf("pkg-vi")))
    )

    @Test fun `unicode package search preserves canonical identity and clear restores projection`() {
        val searched = assertIs<AndroidLibraryState.Root>(facade.searchRoot(root, "từ VỰNG"))
        assertEquals(listOf("pkg-vi"), searched.packages.map { it.packageId })
        val cleared = assertIs<AndroidLibraryState.Root>(facade.searchRoot(searched, ""))
        assertEquals(listOf("pkg-vi", "pkg-en"), cleared.packages.map { it.packageId })
    }

    @Test fun `collection search and open use stable collection membership`() {
        val searched = assertIs<AndroidLibraryState.Root>(facade.searchRoot(root, "tiếng việt"))
        assertEquals("collection-1", searched.collections.single().collectionId)
        val opened = assertIs<AndroidLibraryState.Root>(facade.searchRoot(root, filter=AndroidLibraryFilter.PACKAGES, collectionId="collection-1"))
        assertEquals(listOf("pkg-vi"), opened.packages.map { it.packageId })
        assertTrue(opened.collections.isEmpty())
    }

    @Test fun `filter selection exposes only canonical sections`() {
        assertTrue(assertIs<AndroidLibraryState.Root>(facade.searchRoot(root,filter=AndroidLibraryFilter.COLLECTIONS)).packages.isEmpty())
        assertTrue(assertIs<AndroidLibraryState.Root>(facade.searchRoot(root,filter=AndroidLibraryFilter.PACKAGES)).collections.isEmpty())
    }

    @Test fun `package card model contains real bounded metadata and no optional fiction`() {
        val pkg = root.packages.first()
        assertEquals(990, pkg.contentCount)
        assertEquals("ACTIVE", pkg.status)
        val model = source("vn/loi/learning/android/library/AndroidLibraryFacade.kt")
            .substringAfter("data class AndroidLibraryPackageItem").substringBefore("data class AndroidLibraryCollectionItem")
        listOf("artwork", "rating", "difficulty", "streak", "recommendationScore").forEach { assertFalse(model.contains(it)) }
    }

    @Test fun `root query avoids browser content loads and repositories`() {
        val source = source("vn/loi/learning/android/library/AndroidLibraryFacade.kt")
        val rootLoad = source.substringAfter("fun loadRoot()").substringBefore("fun openPackage")
        assertTrue(rootLoad.contains("getNavigationTree"))
        assertFalse(rootLoad.contains("getBrowserItemsForPackage"))
        assertFalse(rootLoad.contains("Repository"))
    }

    @Test fun `search is debounced cancellable duplicate guarded and off main`() {
        val source = source("vn/loi/learning/android/library/AndroidLibraryViewModel.kt")
        assertTrue(source.contains("searchJob?.cancel()"))
        assertTrue(source.contains("delay(250)"))
        assertTrue(source.contains("value == saved.get<String>(ROOT_QUERY)"))
        assertTrue(source.contains("withContext(workerDispatcher)"))
        assertTrue(source.contains("generation==operationGeneration"))
    }

    @Test fun `root UI uses lazy stable accessible Material foundation`() {
        val source = source("vn/loi/learning/android/library/LibraryScreen.kt")
        listOf("LazyColumn", "key={\"package-\${it.packageId}\"}", "LearningEngineCard",
            "LearningEngineEmptyState", "LearningEngineStatusBadge", "Clear search", "Role.Button"
        ).forEach { assertTrue(source.contains(it), it) }
        assertFalse(source.contains("Color("))
        assertFalse(source.contains("Repository"))
    }

    @Test fun `usable package cards distinguish availability from current learning selection`() {
        val source = source("vn/loi/learning/android/library/LibraryScreen.kt")
        assertTrue(source.contains("Current learning package"))
        assertTrue(source.contains("Use for Study"))
        assertTrue(source.contains("\"Available\""))
        assertFalse(source.contains("LearningEngineStatusBadge(\"Active\""))
    }

    @Test fun `import success refreshes once through existing operation state`() {
        val source = source("vn/loi/learning/android/MainActivity.kt")
        val effect = source.substringAfter("LaunchedEffect(contentState)").substringBefore("val navController")
        assertEquals(1, Regex("libraryViewModel\\.reload\\(\\)").findAll(effect).count())
        assertTrue(source.contains("contentViewModel.begin(AndroidOperationKind.IMPORT)"))
    }

    private fun source(relative: String): String = Files.readString(Path.of("src/main/kotlin").resolve(relative))
}
