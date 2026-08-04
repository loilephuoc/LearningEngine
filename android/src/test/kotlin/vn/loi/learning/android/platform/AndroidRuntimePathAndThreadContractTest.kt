package vn.loi.learning.android.platform

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.Test

class AndroidRuntimePathAndThreadContractTest {
    @Test fun `graph initialization occurs after setContent on IO dispatcher`() {
        val source=source("vn/loi/learning/android/MainActivity.kt")
        assertTrue(source.indexOf("setContent {") < source.indexOf("produceState<AndroidRootState>"))
        val graphLoad=source.substringAfter("produceState<AndroidRootState>").substringBefore("val graph=")
        assertTrue(graphLoad.contains("withContext(Dispatchers.IO)"))
        assertTrue(graphLoad.contains("app.graph"))
        assertFalse(source.substringBefore("setContent {").contains(".graph"))
    }

    @Test fun `Study engine work uses injected serialized worker`() {
        val source=source("vn/loi/learning/android/study/AndroidStudyViewModel.kt")
        assertTrue(source.contains("Dispatchers.IO.limitedParallelism(1)"))
        assertTrue(source.contains("withContext(workerDispatcher)"))
        assertFalse(source.contains("GlobalScope"))
    }

    @Test fun `scoped handoff consumes StudyStarted and never uses generic Resume`() {
        val source=source("vn/loi/learning/android/MainActivity.kt")
        val handoff=source.substringAfter("if (libraryState is AndroidLibraryState.StudyStarted)").substringBefore("val currentRoute")
        assertTrue(handoff.contains("OpenSession(libraryState.sessionId)"))
        assertTrue(handoff.contains("consumeStudyStarted(libraryState.sessionId)"))
        assertFalse(handoff.contains("AndroidStudyEvent.Resume"))
    }

    @Test fun `study plan resolves the entire session scope through one canonical bulk query`() {
        val source=source("vn/loi/learning/android/study/AndroidStudyFacade.kt")
        val createPlan=source.substringAfter("private fun createPlan").substringBefore("private fun presentTyping")
        assertTrue(createPlan.contains("contentRepository?.findByIds(next.session.includedContentIds)"))
        assertFalse(createPlan.contains("includedContentIds.mapNotNull"))
    }

    @Test fun `device diagnostic uses a non-reserved process identifier and preserves app data`() {
        val script=Files.readString(Path.of("../scripts/verify-android-device-runtime.ps1"))
        assertTrue(script.contains("\$appProcessId"))
        assertFalse(Regex("(?i)\\\$pid\\b").containsMatchIn(script))
        assertFalse(script.contains("pm clear"))
        assertFalse(script.contains("uninstall"))
    }

    private fun source(relative:String):String {
        val candidates=listOf(Path.of("src/main/kotlin").resolve(relative),Path.of("android/src/main/kotlin").resolve(relative))
        return Files.readString(candidates.first(Files::isRegularFile))
    }
}
