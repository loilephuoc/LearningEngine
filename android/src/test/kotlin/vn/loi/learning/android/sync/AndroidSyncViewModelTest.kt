package vn.loi.learning.android.sync

import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.infrastructure.sync.supabase.*

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSyncViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `construction recreation and edits perform no authentication or sync`() = runTest(dispatcher) {
        val fixture = fixture(this)
        AndroidSyncViewModel(fixture.controller)
        AndroidSyncViewModel(fixture.controller)
        assertEquals(0, fixture.sessions.signIns)
        assertEquals(0, fixture.operation.runs)
    }

    @Test fun `valid configuration is saved without retaining key or starting network`() = runTest(dispatcher) {
        val fixture = fixture(this); val model = AndroidSyncViewModel(fixture.controller)
        model.updateProjectUrl(URL); model.updatePublishableKey(KEY)
        assertTrue(model.saveConfiguration()); advanceUntilIdle()
        assertEquals(AndroidSyncPhase.SIGNED_OUT, model.state.value.phase)
        assertEquals("", model.state.value.publishableKey)
        assertEquals(0, fixture.sessions.signIns); assertEquals(0, fixture.operation.runs)
    }

    @Test fun `invalid configuration uses safe diagnostic and never echoes credential`() = runTest(dispatcher) {
        val fixture = fixture(this); val model = AndroidSyncViewModel(fixture.controller)
        model.updateProjectUrl("http://remote.example"); model.updatePublishableKey("private-value")
        assertFalse(model.saveConfiguration())
        assertEquals("SYNC_CONFIGURATION_INVALID", model.state.value.diagnosticCode)
        assertFalse(model.state.value.diagnosticCode!!.contains("private-value"))
    }

    @Test fun `sign in clears password and blocks double submit`() = runTest(dispatcher) {
        val fixture = fixture(this); val model = AndroidSyncViewModel(fixture.controller)
        model.updateProjectUrl(URL); model.updatePublishableKey(KEY); model.saveConfiguration(); advanceUntilIdle()
        model.updateEmail("learner@example.com"); model.updatePassword("secret")
        assertTrue(model.signIn()); assertEquals("", model.state.value.password)
        assertFalse(model.signIn())
        advanceUntilIdle()
        assertEquals(AndroidSyncPhase.READY, model.state.value.phase)
        assertEquals(1, fixture.sessions.signIns)
    }

    @Test fun `explicit sync maps summary and sign out preserves configuration`() = runTest(dispatcher) {
        val fixture = fixture(this); val model = AndroidSyncViewModel(fixture.controller)
        model.updateProjectUrl(URL); model.updatePublishableKey(KEY); model.saveConfiguration()
        model.updateEmail("learner@example.com"); model.updatePassword("secret"); model.signIn(); advanceUntilIdle()
        assertTrue(model.syncNow()); advanceUntilIdle()
        assertEquals(1, model.state.value.summary?.pushed)
        assertNotNull(model.state.value.lastSuccessfulSync)
        model.signOut(); advanceUntilIdle()
        assertEquals(AndroidSyncPhase.SIGNED_OUT, model.state.value.phase)
        assertEquals(URL, model.state.value.projectUrl)
    }

    @Test fun `default and Vietnamese sync resource keys stay equal`() {
        val root = java.nio.file.Path.of(System.getProperty("user.dir"))
        val base = Files.readString(root.resolve("src/main/res/values/strings.xml"))
        val vi = Files.readString(root.resolve("src/main/res/values-vi/strings.xml"))
        val keys = Regex("name=\"(sync_[^\"]+|settings_sync[^\"]*)\"")
        assertEquals(keys.findAll(base).map { it.groupValues[1] }.toSet(), keys.findAll(vi).map { it.groupValues[1] }.toSet())
    }

    private fun fixture(scope: kotlinx.coroutines.CoroutineScope): Fixture {
        val repo = Repo(); val sessions = Sessions(); val operation = Operation()
        val controller = AndroidSyncRuntimeController(scope, repo, { AndroidSyncRuntimeController.Runtime(sessions, operation) }, dispatcher)
        return Fixture(controller, sessions, operation)
    }
    private data class Fixture(val controller: AndroidSyncRuntimeController, val sessions: Sessions, val operation: Operation)
    private class Repo : AndroidSyncConnectionRepository { var value: AndroidSyncConnection? = null; override fun load()=value; override fun save(connection: AndroidSyncConnection){value=connection} }
    private class Sessions : AndroidSyncSessionAccess {
        var current: SupabaseSession?=null; var signIns=0
        override fun currentSession()=current
        override fun signIn(email:String,password:CharArray)=SupabaseSession(ACCOUNT,"access",userEmail=email).also{signIns++;current=it}
        override fun signOut(){current=null}
    }
    private class Operation : AndroidManualSyncOperation { var runs=0; override fun run(accountId:SyncAccountId,deviceId:SyncDeviceId)=AndroidSyncSummary(1,2,3,4,0,0,5).also{runs++} }
    companion object { const val URL="https://project.supabase.co"; const val KEY="publishable"; val ACCOUNT=SyncAccountId("00000000-0000-0000-0000-000000000001") }
}
