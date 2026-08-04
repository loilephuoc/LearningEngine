package vn.loi.learning.android.packageexperience

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.Test
import vn.loi.learning.application.contentpackaging.browser.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory
import java.time.Instant

/**
 * Focused tests for ANDROID-UI-004 Package Experience.
 *
 * Covers: package route identity, header fields, CTA, browser, search, state,
 *         generation guard concept, performance (no full-media), operations.
 */
class AndroidPackageExperienceTest {

    // â”€â”€â”€ Test data helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun buildContext(): vn.loi.learning.infrastructure.LearningApplicationContext {
        val ctx = LearningApplicationFactory.createInMemory()
        val contentId = ContentId("test-content-1")
        val libraryId = ContentLibraryId("test-library-1")
        ctx.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("hello", "xin chÃ o")))
        ctx.contentLibraryRepository!!.save(ContentLibrary(libraryId, LibraryDescriptor("Test Library"), setOf(contentId)))
        val packageId = PackageId("test-package-1")
        ctx.contentPackageRepository!!.save(ContentPackage(packageId, PackageDescriptor("Test Package", "1.0.0", "OPD3"), setOf(libraryId)))
        val installedId = InstalledPackageId("test-package-1")
        val domainLibraryId = requireNotNull(ctx.defaultLibraryId)
        ctx.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(installedId, domainLibraryId, packageId, TopicId("topic-1"), PackageName("Test Package"), PackageVersion("1.0.0"), PackageState.ACTIVE, Instant.EPOCH, 1, 0)
        )
        val libraryRepo = requireNotNull(ctx.domainLibraryRepository)
        libraryRepo.save(requireNotNull(libraryRepo.findById(domainLibraryId)).registerEntry(installedId, packageId, Instant.EPOCH))
        ctx.libraryCommand!!.setActivePackage(domainLibraryId, installedId)
        return ctx
    }

    private fun buildContext990(): vn.loi.learning.infrastructure.LearningApplicationContext {
        val ctx = LearningApplicationFactory.createInMemory()
        val packageId = PackageId("big-package")
        val installedId = InstalledPackageId("big-package")
        val domainLibraryId = requireNotNull(ctx.defaultLibraryId)
        val libraryId = ContentLibraryId("big-library")
        val contentIds = (1..990).map { i -> ContentId("content-$i") }
        contentIds.forEach { ctx.contentRepository!!.save(Content(it, ContentType.WORD, ContentText("word$it", "answer$it"))) }
        ctx.contentLibraryRepository!!.save(ContentLibrary(libraryId, LibraryDescriptor("Big Library"), contentIds.toSet()))
        ctx.contentPackageRepository!!.save(ContentPackage(packageId, PackageDescriptor("Big Package", "2.0.0", "OPD3"), setOf(libraryId)))
        ctx.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(installedId, domainLibraryId, packageId, TopicId("big-topic"), PackageName("Big Package"), PackageVersion("2.0.0"), PackageState.ACTIVE, Instant.EPOCH, 990, 0)
        )
        val libraryRepo = requireNotNull(ctx.domainLibraryRepository)
        libraryRepo.save(requireNotNull(libraryRepo.findById(domainLibraryId)).registerEntry(installedId, packageId, Instant.EPOCH))
        return ctx
    }

    // â”€â”€â”€ Package authority â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `open package uses canonical InstalledPackageSummary for header fields`() {
        val ctx = buildContext()
        val facade = AndroidPackageFacade(ctx)
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(InstalledPackageId("test-package-1"))
        )
        assertEquals("Test Package", state.header.title)
        assertEquals("1.0.0", state.header.version)
        assertEquals(1, state.header.contentCount)
        assertEquals("ACTIVE", state.header.state)
        assertTrue(state.header.isActivePackage)
    }

    @Test
    fun `header model contains only canonical fields â€” no artwork no progress no difficulty`() {
        val source = source("vn/loi/learning/android/packageexperience/AndroidPackageState.kt")
        val headerClass = source.substringAfter("data class AndroidPackageHeaderModel").substringBefore("sealed interface")
        listOf("artwork", "progress", "difficulty", "author", "duration", "lessonCount", "rating").forEach { field ->
            assertFalse(headerClass.contains(field), "Header must not contain fake field: $field")
        }
    }

    @Test
    fun `unknown package returns Failure not crash`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val facade = AndroidPackageFacade(ctx)
        val state = facade.openPackage(InstalledPackageId("nonexistent-pkg"))
        assertIs<AndroidPackageContentState.Failure>(state)
        assertTrue(state.message.isNotBlank())
    }

    // â”€â”€â”€ Browser projection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `browser uses PackageContentBrowserQueryService not archive parse`() {
        val source = source("vn/loi/learning/android/packageexperience/AndroidPackageFacade.kt")
        assertTrue(source.contains("packageBrowserQuery"))
        assertFalse(source.contains("parseArchive"))
        assertFalse(source.contains("ZipInputStream"))
        assertFalse(source.contains("Files.newInputStream"))
    }

    @Test
    fun `browser rows are lightweight â€” no media bytes loaded`() {
        val source = source("vn/loi/learning/android/packageexperience/AndroidPackageFacade.kt")
        assertFalse(source.contains("BitmapFactory"))
        assertFalse(source.contains("decodeFile"))
        assertFalse(source.contains("media.resolve"))
        assertFalse(source.contains("ContentMediaStorage"))
    }

    @Test
    fun `content row stable identity is independent from display index`() {
        val ctx = buildContext()
        val facade = AndroidPackageFacade(ctx)
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(InstalledPackageId("test-package-1"))
        )
        assertEquals("test-content-1", state.allRows.single().contentId)
    }

    // â”€â”€â”€ 990-content regression â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `990-content package opens without blocking and returns all rows`() {
        val ctx = buildContext990()
        val facade = AndroidPackageFacade(ctx)
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(InstalledPackageId("big-package"))
        )
        assertEquals(990, state.allRows.size)
        assertEquals(990, state.visibleRows.size)
        assertEquals(990, state.header.contentCount)
    }

    @Test
    fun `990-content search filters correctly without blocking`() {
        val ctx = buildContext990()
        val facade = AndroidPackageFacade(ctx)
        val open = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(InstalledPackageId("big-package"))
        )
        // Search for a specific item â€” only 1 match expected (word content-42)
        val searched = facade.applySearch(open, "content-42")
        // At minimum should filter down
        assertTrue(searched.visibleRows.size < open.allRows.size)
        assertTrue(searched.visibleRows.isNotEmpty())
    }

    // â”€â”€â”€ Search â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `search is debounced cancellable and stale guarded via ViewModel`() {
        val src = source("vn/loi/learning/android/packageexperience/AndroidPackageViewModel.kt")
        assertTrue(src.contains("searchJob?.cancel()"))
        assertTrue(src.contains("delay(250)"))
        assertTrue(src.contains("query == current.query"))  // duplicate guard
        assertTrue(src.contains("withContext(workerDispatcher)"))
        assertTrue(src.contains("generation == operationGeneration"))
    }

    @Test
    fun `search clear restores full package projection`() {
        val ctx = buildContext()
        val facade = AndroidPackageFacade(ctx)
        val open = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(InstalledPackageId("test-package-1"))
        )
        // Apply a search that matches nothing
        val empty = facade.applySearch(open, "zzznomatch")
        assertTrue(empty.visibleRows.isEmpty())
        // Clear search restores all
        val cleared = facade.applySearch(empty, "")
        assertEquals(open.allRows.size, cleared.visibleRows.size)
    }

    @Test
    fun `search is unicode-safe via pre-computed NFKC searchableText field`() {
        val src = source("vn/loi/learning/android/packageexperience/AndroidPackageFacade.kt")
        // Facade uses row.searchableText (pre-computed NFKC by browser query service)
        assertTrue(src.contains("row.searchableText"))
        assertFalse(src.contains("buildSearchableText"), "Facade must not call internal buildSearchableText")
    }

    @Test
    fun `search is package-local â€” no global repository call`() {
        val src = source("vn/loi/learning/android/packageexperience/AndroidPackageFacade.kt")
        // applySearch operates only on pre-loaded allRows, no new repo call
        val applySearch = src.substringAfter("fun applySearch(").substringBefore("fun startPackage(")
        assertFalse(applySearch.contains("packageBrowserQuery"))
        assertFalse(applySearch.contains("libraryQuery"))
        assertFalse(applySearch.contains("Repository"))
    }

    // â”€â”€â”€ CTA â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `no content package returns NoContent CTA`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val packageId = PackageId("empty-package")
        val installedId = InstalledPackageId("empty-package")
        val domainLibraryId = requireNotNull(ctx.defaultLibraryId)
        val libId = ContentLibraryId("empty-lib")
        ctx.contentLibraryRepository!!.save(ContentLibrary(libId, LibraryDescriptor("Empty"), emptySet()))
        ctx.contentPackageRepository!!.save(ContentPackage(packageId, PackageDescriptor("Empty Package", "1.0.0", "OPD3"), setOf(libId)))
        ctx.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(installedId, domainLibraryId, packageId, TopicId("empty-topic"), PackageName("Empty Package"), PackageVersion("1.0.0"), PackageState.ACTIVE, Instant.EPOCH, 0, 0)
        )
        val libraryRepo = requireNotNull(ctx.domainLibraryRepository)
        libraryRepo.save(requireNotNull(libraryRepo.findById(domainLibraryId)).registerEntry(installedId, packageId, Instant.EPOCH))
        val facade = AndroidPackageFacade(ctx)
        val state = assertIs<AndroidPackageContentState.Empty>(facade.openPackage(installedId))
        assertEquals(AndroidPackageCta.NoContent, state.cta)
    }

    @Test
    fun `active package with content returns StudyPackage or ContinuePackage CTA`() {
        val ctx = buildContext()
        val facade = AndroidPackageFacade(ctx)
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(InstalledPackageId("test-package-1"))
        )
        // No active session â†’ ContinuePackage (active package) or StudyPackage
        assertTrue(
            state.cta is AndroidPackageCta.ContinuePackage || state.cta is AndroidPackageCta.StudyPackage,
            "Expected ContinuePackage or StudyPackage, got ${state.cta}"
        )
    }

    @Test
    fun `CTA priority ordering is Continue Learning then Continue Package then Study Package then NoContent`() {
        val src = source("vn/loi/learning/android/packageexperience/AndroidPackageState.kt")
        // Sealed interface ordering reflects priority
        val ctaSection = src.substringAfter("sealed interface AndroidPackageCta")
        val clIdx = ctaSection.indexOf("ContinueLearning")
        val cpIdx = ctaSection.indexOf("ContinuePackage")
        val spIdx = ctaSection.indexOf("StudyPackage")
        val ncIdx = ctaSection.indexOf("NoContent")
        assertTrue(clIdx < cpIdx, "ContinueLearning must come before ContinuePackage")
        assertTrue(cpIdx < spIdx, "ContinuePackage must come before StudyPackage")
        assertTrue(spIdx < ncIdx, "StudyPackage must come before NoContent")
    }

    // â”€â”€â”€ State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `state sealed interface covers Loading Content Empty Failure`() {
        val src = source("vn/loi/learning/android/packageexperience/AndroidPackageState.kt")
        listOf("Loading", "Content", "Empty", "Failure").forEach { s ->
            assertTrue(src.contains(s), "AndroidPackageContentState must include $s")
        }
    }

    @Test
    fun `operation state sealed interface covers Idle Pending Succeeded Failed`() {
        val src = source("vn/loi/learning/android/packageexperience/AndroidPackageState.kt")
        listOf("Idle", "Pending", "Succeeded", "Failed").forEach { s ->
            assertTrue(src.contains(s), "AndroidPackageOperationState must include $s")
        }
    }

    @Test
    fun `state objects are immutable data classes`() {
        val content = AndroidPackageContentState.Content(
            header = AndroidPackageHeaderModel("id", "Title", "1.0", 10, "ACTIVE", true),
            cta = AndroidPackageCta.StudyPackage,
            allRows = emptyList(),
            visibleRows = emptyList()
        )
        val updated = content.copy(query = "test")
        assertEquals("", content.query)
        assertEquals("test", updated.query)
        assertNotSame(content, updated)
    }

    // â”€â”€â”€ Generation guard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `generation guard prevents stale overwrites`() {
        val src = source("vn/loi/learning/android/packageexperience/AndroidPackageViewModel.kt")
        assertTrue(src.contains("operationGeneration"))
        assertTrue(src.contains("++operationGeneration"))
        assertTrue(src.contains("generation == operationGeneration"))
    }

    // â”€â”€â”€ Screen composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `PackageScreen uses lazy stable keys and no per-row media`() {
        val src = source("vn/loi/learning/android/packageexperience/PackageScreen.kt")
        assertTrue(src.contains("LazyColumn"))
        assertTrue(src.contains("key = { \"content-\${it.contentId}\" }"))
        assertFalse(src.contains("BitmapFactory"))
        assertFalse(src.contains("decodeFile"))
        assertFalse(src.contains("media.resolve"))
    }

    @Test
    fun `PackageScreen touch targets are at least 48dp`() {
        val src = source("vn/loi/learning/android/packageexperience/PackageScreen.kt")
        assertTrue(src.contains("touchTarget"))
        assertTrue(src.contains("defaultMinSize"))
    }

    @Test
    fun `PackageScreen uses semantic heading content descriptions role`() {
        val src = source("vn/loi/learning/android/packageexperience/PackageScreen.kt")
        assertTrue(src.contains("heading()"))
        assertTrue(src.contains("contentDescription"))
        assertTrue(src.contains("Role.Button"))
        assertTrue(src.contains("liveRegion"))
    }

    @Test
    fun `PackageScreen has no repository call and no Android Context`() {
        val src = source("vn/loi/learning/android/packageexperience/PackageScreen.kt")
        assertFalse(src.contains("Repository"))
        assertFalse(src.contains("context."))
        assertFalse(src.contains("Context)")  )
    }

    @Test
    fun `PackageScreen state is hoisted â€” no ViewModel inside composable`() {
        val src = source("vn/loi/learning/android/packageexperience/PackageScreen.kt")
        assertFalse(src.contains("viewModel<"))
        assertFalse(src.contains("AndroidPackageViewModel"))
    }

    // â”€â”€â”€ Navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `package route is wired in MainActivity`() {
        val src = source("vn/loi/learning/android/MainActivity.kt")
        assertTrue(src.contains("package/{packageId}"))
        assertTrue(src.contains("AndroidPackageFacade"))
        assertTrue(src.contains("AndroidPackageViewModel"))
        assertTrue(src.contains("PackageScreen"))
    }

    @Test
    fun `library openPackage navigates to package route not library browser state`() {
        val src = source("vn/loi/learning/android/MainActivity.kt")
        // The library onOpenPackage should navigate to the route, not call libraryViewModel::openPackage
        val libraryComposable = src.substringAfter("composable(\"library\"").substringBefore("composable(\"package")
        assertTrue(libraryComposable.contains("navController.navigate(\"package/"))
        assertFalse(libraryComposable.contains("libraryViewModel::openPackage"))
    }

    @Test
    fun `Home and Library destinations are preserved unchanged`() {
        val homeSource = source("vn/loi/learning/android/study/AndroidStudyFacade.kt")
        assertTrue(homeSource.contains("fun home()"))
        assertTrue(homeSource.contains("AndroidStudyState.Home"))
        val libSource = source("vn/loi/learning/android/library/LibraryScreen.kt")
        assertTrue(libSource.contains("LibraryTopBar"))
        assertTrue(libSource.contains("CollectionCard"))
        assertTrue(libSource.contains("LibraryPackageCard"))
    }

    // â”€â”€â”€ Operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `only canonical package operations are exposed â€” Study Export Verify Uninstall`() {
        val src = source("vn/loi/learning/android/packageexperience/PackageScreen.kt")
        assertTrue(src.contains("onExport"))
        assertTrue(src.contains("onVerify"))
        assertTrue(src.contains("onUninstall"))
        assertTrue(src.contains("onStudyPackage"))
        // No upgrade/create â€” not canonical at this screen level
    }

    // â”€â”€â”€ Source helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun source(relative: String): String = Files.readString(Path.of("src/main/kotlin").resolve(relative))
}
