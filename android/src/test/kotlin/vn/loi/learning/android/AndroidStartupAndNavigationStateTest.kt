package vn.loi.learning.android

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.android.ui.AndroidRootDestination
import vn.loi.learning.android.ui.AndroidRootState
import vn.loi.learning.android.study.*
import vn.loi.learning.domain.study.recall.StudyMode

class AndroidStartupAndNavigationStateTest {
    @Test
    fun `application owner creates one graph for repeated consumers`() {
        var creations = 0
        val owner = SingleInstanceOwner {
            creations += 1
            Any()
        }

        val first = owner.value
        val second = owner.value

        assertTrue(first === second)
        assertEquals(1, creations)
    }

    @Test
    fun `application owner retries failed creation and remains single after success`() {
        var attempts = 0
        val owner = SingleInstanceOwner {
            attempts += 1
            if (attempts == 1) error("transient")
            Any()
        }

        assertFailsWith<IllegalStateException> { owner.value }
        val graph = owner.value

        assertTrue(graph === owner.value)
        assertEquals(2, attempts)
    }

    @Test
    fun `root state distinguishes bootstrapping and typed retryable failure`() {
        assertIs<AndroidRootState.Bootstrapping>(AndroidRootState.Bootstrapping)
        val failure = AndroidRootState.Failed("safe message")
        assertTrue(failure.retryable)
        assertEquals("safe message", failure.message)
    }

    @Test
    fun `root destination identity is deterministic and invalid routes fall back safely`() {
        AndroidRootDestination.entries.forEach { destination ->
            assertEquals(destination, AndroidRootDestination.fromRoute(destination.route))
        }
        assertEquals(AndroidRootDestination.HOME, AndroidRootDestination.fromRoute(null))
        assertEquals(AndroidRootDestination.HOME, AndroidRootDestination.fromRoute("unknown"))
    }

    @Test
    fun `root routes render typed non-empty states instead of blank composables`() {
        val source = source("vn/loi/learning/android/MainActivity.kt")
        assertTrue(source.contains("AndroidRootFailure(root)"))
        assertTrue(source.contains("AndroidFeatureLoading(\"Preparing your learning overview\")"))
        assertTrue(source.contains("AndroidFeatureLoading(\"Preparing Review\")"))
        assertFalse(source.contains("AndroidStudyState.Home ?: return@composable"))
    }

    @Test
    fun `restored state never drives root navigation while explicit entry events do`() {
        val source = source("vn/loi/learning/android/MainActivity.kt")
        assertFalse(source.contains("LaunchedEffect(state)"))
        assertTrue(source.contains("startDestination = \"home\""))
        assertFalse(opensStudyFromExplicitEvent(AndroidStudyEvent.RefreshHomeIfIdle))
        assertFalse(opensStudyFromExplicitEvent(AndroidStudyEvent.EnsureHome))
        assertFalse(opensStudyFromExplicitEvent(AndroidStudyEvent.Home))
        assertTrue(opensStudyFromExplicitEvent(AndroidStudyEvent.OpenSession("exact-session")))
        assertTrue(opensStudyFromExplicitEvent(AndroidStudyEvent.Resume))
        assertTrue(opensStudyFromExplicitEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW)))
        assertTrue(opensStudyFromExplicitEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.TYPING)))
    }

    @Test
    fun `feature view models coordinate publication with feature appropriate policy`() {
        val study = source("vn/loi/learning/android/study/AndroidStudyViewModel.kt")
        val library = source("vn/loi/learning/android/library/AndroidLibraryViewModel.kt")
        assertTrue(study.contains("operationMutex.withLock"))
        assertFalse(study.contains("operationGeneration"))
        assertTrue(library.contains("generation==operationGeneration"))
        assertEquals(1, Regex("launchOperation\\(\"study_initial_load\"").findAll(study).count())
        assertFalse(study.contains("GlobalScope"))
        assertFalse(library.contains("GlobalScope"))
    }

    @Test
    fun `startup leaves Library load destination driven`() {
        val activity = source("vn/loi/learning/android/MainActivity.kt")
        val library = source("vn/loi/learning/android/library/AndroidLibraryViewModel.kt")
        assertFalse(library.contains("init {"))
        assertTrue(library.contains("fun ensureLoaded()"))
        val route = activity.substringAfter("composable(\"library\"").substringBefore("composable(\"package/{packageId}\"")
        assertEquals(1, Regex("libraryViewModel\\.ensureLoaded\\(\\)").findAll(route).count())
    }

    @Test
    fun `Study and Review route entry do not recompute Home`() {
        val activity = source("vn/loi/learning/android/MainActivity.kt")
        val routeEffect = activity.substringAfter("LaunchedEffect(currentRoute)")
            .substringBefore("val showRootNavigation")
        assertTrue(routeEffect.contains("currentRoute == \"home\""))
        assertFalse(routeEffect.contains("currentRoute == \"study\""))
        assertFalse(routeEffect.contains("currentRoute == \"review\""))
    }

    private fun source(relative: String): String =
        Files.readString(Path.of("src/main/kotlin").resolve(relative))
}
