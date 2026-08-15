package vn.loi.learning.desktop.ui.contentlibrary

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*
import vn.loi.learning.application.integrity.*
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.LearningApplicationContext

class LibraryHealthOverviewViewModelTest {
    @Test
    fun `scan snapshots all packages in canonical order and aggregates statuses`() = fixture(
        listOf("Zulu", "Alpha", "Middle")
    ) { f ->
        val calls = mutableListOf<String>()
        val statuses = listOf(IntegrityStatus.HEALTHY, IntegrityStatus.WARNINGS, IntegrityStatus.ERRORS)
        val vm = f.vm { id -> report(id, f.names.getValue(id), statuses[calls.size]).also { calls += id } }

        vm.checkLibraryHealth()

        assertEquals(vm.uiState.packages.map { it.id }, calls)
        assertEquals(calls, vm.libraryHealthOverviewState.results.map { it.packageId })
        assertEquals(listOf(1, 1, 1, 0), with(vm.libraryHealthOverviewState) {
            listOf(healthyPackages, warningPackages, errorPackages, failedPackages)
        })
        assertEquals(NOW, vm.libraryHealthOverviewState.scannedAt)
    }

    @Test
    fun `empty and repeat scans replace runtime-only state`() = fixture(emptyList()) { f ->
        var calls = 0
        val vm = f.vm { id -> calls++; report(id, id, IntegrityStatus.HEALTHY) }
        vm.checkLibraryHealth()
        val first = vm.libraryHealthOverviewState
        vm.checkLibraryHealth()
        assertTrue(first.hasResult)
        assertTrue(vm.libraryHealthOverviewState.results.isEmpty())
        assertEquals(0, calls)
        val restarted = ContentLibraryViewModel(f.facade, f.lessonFacade, loadImmediately = false)
        assertEquals(LibraryHealthOverviewState(), restarted.libraryHealthOverviewState)
    }

    @Test
    fun `duplicate click is ignored and package failure is isolated`() = fixture(
        listOf("Alpha", "Broken", "Zulu")
    ) { f ->
        val runner = QueuedRunner()
        var calls = 0
        val vm = f.vm(runner) { id ->
            calls++
            if (f.names.getValue(id) == "Broken") error("synthetic checker failure")
            report(id, f.names.getValue(id), IntegrityStatus.HEALTHY)
        }
        runner.next() // Complete the ViewModel's initial library load.
        vm.checkLibraryHealth()
        vm.checkLibraryHealth()
        assertTrue(vm.libraryHealthOverviewState.scanning)
        assertEquals(1, runner.size)
        runner.next()
        assertEquals(3, calls)
        assertEquals(2, vm.libraryHealthOverviewState.healthyPackages)
        assertEquals(1, vm.libraryHealthOverviewState.failedPackages)
        assertNull(vm.libraryHealthOverviewState.results.single { it.packageName == "Broken" }.report)
    }

    @Test
    fun `drill-down reuses report and package check remains available`() = fixture(listOf("Alpha")) { f ->
        var calls = 0
        val vm = f.vm { id ->
            calls++
            report(id, f.names.getValue(id), if (calls == 1) IntegrityStatus.WARNINGS else IntegrityStatus.HEALTHY)
        }
        vm.checkLibraryHealth()
        val frozen = vm.libraryHealthOverviewState.results.single().report!!
        vm.showLibraryHealthReport(frozen.packageId)
        assertSame(frozen, vm.packageIntegrityDialogState.report)
        assertEquals(1, calls)
        vm.dismissPackageIntegrityReport()
        vm.checkLibraryHealth()
        assertEquals(IntegrityStatus.HEALTHY, vm.libraryHealthOverviewState.results.single().report!!.status)
        vm.checkPackageIntegrity(vm.uiState.packages.single().id)
        assertEquals(3, calls)
        assertTrue(vm.packageIntegrityDialogState.visible)
    }

    @Test
    fun `real full-library scan leaves every learning and domain repository unchanged`() = fixture(
        listOf("Alpha", "Beta")
    ) { f ->
        val before = repositorySnapshot(f.context)
        val vm = ContentLibraryViewModel(f.facade, f.lessonFacade)

        vm.checkLibraryHealth()

        assertEquals(2, vm.libraryHealthOverviewState.results.size)
        assertEquals(before, repositorySnapshot(f.context))
    }

    private fun report(id: String, name: String, status: IntegrityStatus) = PackageIntegrityReport(
        installedPackageId = id,
        packageId = id,
        packageName = name,
        checkedAtRuntime = NOW,
        status = status,
        findings = when (status) {
            IntegrityStatus.HEALTHY -> emptyList()
            IntegrityStatus.WARNINGS -> listOf(IntegrityFinding(IntegritySeverity.WARNING, "TEST", "Package", id, "warning"))
            IntegrityStatus.ERRORS -> listOf(IntegrityFinding(IntegritySeverity.ERROR, "TEST", "Package", id, "error"))
        },
        summary = IntegritySummary(
            errors = if (status == IntegrityStatus.ERRORS) 1 else 0,
            warnings = if (status == IntegrityStatus.WARNINGS) 1 else 0,
            info = 0
        )
    )

    private fun fixture(names: List<String>, block: (Fixture) -> Unit) {
        val dir = Files.createTempDirectory("library-health")
        try {
            val context = LearningApplicationFactory.createPersisted(dir.resolve("db"))
            val facade = ContentLibraryFacade(context)
            val lessons = LessonBrowserFacade(context)
            val importer = ContentLibraryViewModel(facade, lessons)
            names.forEachIndexed { index, name ->
                val archive = dir.resolve("$index.opd3")
                ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
                    fun entry(path: String, body: String) {
                        zip.putNextEntry(ZipEntry(path)); zip.write(body.toByteArray(StandardCharsets.UTF_8)); zip.closeEntry()
                    }
                    entry("manifest.json", """{"name":"$name","version":"1.0","format":"OPD3","schemaVersion":1,"contentCount":1,"learningItemCount":1}""")
                    entry("metadata.json", """{"name":"$name","version":"1.0","format":"OPD3"}""")
                    entry("contents.json", """{"contents":[{"id":"c$index","type":"SENTENCE","primaryText":"Hello","translatedText":"Xin chao","title":"$name","group":"G","section":"S","lesson":"$name"}]}""")
                    entry("learning-items.json", """{"learningItems":[{"id":"c$index-rec","contentId":"c$index","mode":"MEANING_RECOGNITION","isEnabled":true}]}""")
                }
                importer.importFromFiles(listOf(archive))
                assertNull(importer.uiState.importError)
            }
            block(Fixture(context, facade, lessons, importer.uiState.packages.associate { it.id to it.name }))
        } finally { dir.toFile().deleteRecursively() }
    }

    private data class Fixture(
        val context: LearningApplicationContext,
        val facade: ContentLibraryFacade,
        val lessonFacade: LessonBrowserFacade,
        val names: Map<String, String>
    ) {
        fun vm(runner: DesktopTaskRunner = ImmediateDesktopTaskRunner, check: (String) -> PackageIntegrityReport) =
            ContentLibraryViewModel(
                facade, lessonFacade, taskRunner = runner,
                clock = Clock.fixed(NOW, ZoneOffset.UTC), integrityCheck = { id, _ -> check(id) }
            )
    }

    private fun repositorySnapshot(context: LearningApplicationContext): List<Any?> = listOf(
        context.installedPackageRepository?.findAll(),
        context.contentPackageRepository?.findAll(),
        context.contentLibraryRepository?.findAll(),
        context.contentRepository?.findAll(),
        context.learningItemRepository?.findAll(),
        context.memoryStateRepository?.findAll(),
        context.reviewEventRepository?.findAll(),
        context.learningTrajectoryRepository?.findAll(),
        context.studySessionRepository?.findAll(),
        context.studyQueueRepository?.findAll()
    )

    private class QueuedRunner : DesktopTaskRunner {
        private val pending = ArrayDeque<() -> Unit>()
        val size get() = pending.size
        override fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
            pending += { try { onSuccess(work()) } catch (e: Exception) { onFailure(e) } }
        }
        override fun dispatch(action: () -> Unit) = action()
        fun next() = pending.removeFirst().invoke()
    }

    companion object { private val NOW = Instant.parse("2026-08-15T12:00:00Z") }
}
