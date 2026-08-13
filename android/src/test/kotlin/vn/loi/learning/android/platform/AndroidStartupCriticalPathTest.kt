package vn.loi.learning.android.platform

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue
import org.junit.Test

class AndroidStartupCriticalPathTest {
    @Test fun `Android graph skips blocking POS full scan while other platforms retain default`() {
        val graph = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/platform/AndroidApplicationGraph.kt"))
        assertTrue(graph.contains("reconcilePartOfSpeechRegistryOnCreate = false"))
    }

    @Test fun `Study start projects authoritative session before secondary HUD`() {
        val facade = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt"))
        val viewModel = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyViewModel.kt"))
        assertTrue(facade.contains("knownSession = session, deferHud = true"))
        assertTrue(facade.contains("if (knownSession == null)"))
        assertTrue(viewModel.contains("onEvent(AndroidStudyEvent.RefreshHud)"))
        assertTrue(facade.contains("includedContentIds = actionContentIds"))
    }

    @Test fun `Home uses session summary while Study entry retains full reconciliation`() {
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt"))
        val home = source.substringAfter("fun home()").substringBefore("fun start(")
        val exactLoad = source.substringAfter("fun loadExact(").substringBefore("private fun attachHud(")
        assertTrue(home.contains("context.engine.getActiveSession(learnerId)"))
        assertTrue(!home.contains("reconcileActiveSession()"))
        assertTrue(exactLoad.contains("reconcileActiveSession()"))
    }
}
