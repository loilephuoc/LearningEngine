package vn.loi.learning.android.sync

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.infrastructure.sync.supabase.*

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSyncRuntimeControllerTest {
    @Test fun `construction and configuration never authenticate or synchronize`() = runTest {
        val fixture = fixture()
        assertEquals(AndroidSyncPhase.UNCONFIGURED, fixture.controller.state.value.phase)
        fixture.controller.configure(URL, KEY)
        assertEquals(AndroidSyncPhase.SIGNED_OUT, fixture.controller.state.value.phase)
        assertEquals(0, fixture.sessions.signIns)
        assertEquals(0, fixture.operation.runs)
    }

    @Test fun `authentication success enables explicit sync and second sync is no changes`() = runTest {
        val fixture = fixture(listOf(summary(pushed = 1, applied = 2), summary()))
        fixture.controller.configure(URL, KEY)
        fixture.controller.signIn("learner@example.com", "secret".toCharArray())
        advanceUntilIdle()
        assertEquals(AndroidSyncPhase.READY, fixture.controller.state.value.phase)

        fixture.controller.syncNow(); advanceUntilIdle()
        assertEquals(AndroidSyncPhase.SUCCESS, fixture.controller.state.value.phase)
        assertEquals(2, fixture.controller.state.value.summary?.applied)
        fixture.controller.syncNow(); advanceUntilIdle()
        assertEquals(AndroidSyncPhase.NO_CHANGES, fixture.controller.state.value.phase)
        assertEquals(2, fixture.operation.runs)
    }

    @Test fun `authentication failure is signed out with stable diagnostic`() = runTest {
        val fixture = fixture()
        fixture.sessions.signInFailure = SupabaseAuthException("SYNC_AUTH_INVALID_CREDENTIALS", 400)
        fixture.controller.configure(URL, KEY)
        fixture.controller.signIn("learner@example.com", "secret".toCharArray())
        advanceUntilIdle()
        assertEquals(AndroidSyncPhase.SIGNED_OUT, fixture.controller.state.value.phase)
        assertEquals("SYNC_AUTH_INVALID_CREDENTIALS", fixture.controller.state.value.diagnosticCode)
    }

    @Test fun `controller prevents concurrent manual sync`() = runBlocking {
        val gate = CountDownLatch(1)
        val entered = CountDownLatch(1)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = FakeRepository()
        val sessions = FakeSessions()
        val operation = FakeOperation(block = { entered.countDown(); gate.await(); summary() })
        val controller = AndroidSyncRuntimeController(
            scope, repository,
            { AndroidSyncRuntimeController.Runtime(sessions, operation) },
            Dispatchers.IO
        )
        val fixture = Fixture(controller, sessions, operation)
        fixture.controller.configure(URL, KEY)
        fixture.controller.signIn("learner@example.com", "secret".toCharArray())!!.join()
        val first = fixture.controller.syncNow()
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        assertNotNull(first)
        assertEquals(AndroidSyncPhase.SYNCING, fixture.controller.state.value.phase)
        assertNull(fixture.controller.syncNow())
        gate.countDown(); first.join()
        assertEquals(1, fixture.operation.runs)
        scope.cancel()
    }

    @Test fun `cancelling sync interrupts work and reaches cancelled state`() = runBlocking {
        val entered = CountDownLatch(1)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = FakeRepository()
        val sessions = FakeSessions()
        val operation = FakeOperation(block = {
            entered.countDown()
            Thread.sleep(30_000)
            summary()
        })
        val controller = AndroidSyncRuntimeController(
            scope, repository,
            { AndroidSyncRuntimeController.Runtime(sessions, operation) },
            Dispatchers.IO
        )
        val fixture = Fixture(controller, sessions, operation)
        fixture.controller.configure(URL, KEY)
        fixture.controller.signIn("learner@example.com", "secret".toCharArray())!!.join()
        val job = fixture.controller.syncNow()!!
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        assertTrue(fixture.controller.cancelSync())
        job.join()
        assertEquals(AndroidSyncPhase.CANCELLED, fixture.controller.state.value.phase)
        scope.cancel()
    }

    @Test fun `offline retry conflict and pending media remain visible`() = runTest {
        val operation = FakeOperation()
        val fixture = fixture(operation = operation)
        fixture.controller.configure(URL, KEY)
        fixture.controller.signIn("learner@example.com", "secret".toCharArray()); advanceUntilIdle()

        operation.failure = SupabaseTransportException("SYNC_SUPABASE_NETWORK_ERROR", true)
        fixture.controller.syncNow(); advanceUntilIdle()
        assertEquals(AndroidSyncPhase.OFFLINE, fixture.controller.state.value.phase)
        operation.failure = SupabaseTransportException("SYNC_SUPABASE_BUSY", true)
        fixture.controller.syncNow(); advanceUntilIdle()
        assertEquals(AndroidSyncPhase.RETRYABLE_ERROR, fixture.controller.state.value.phase)
        operation.failure = null; operation.result = summary(conflicts = 1)
        fixture.controller.syncNow(); advanceUntilIdle()
        assertEquals(AndroidSyncPhase.CONFLICTS, fixture.controller.state.value.phase)
        operation.result = summary(pending = 1)
        fixture.controller.syncNow(); advanceUntilIdle()
        assertEquals(AndroidSyncPhase.PENDING_MEDIA, fixture.controller.state.value.phase)
    }

    @Test fun `sign out clears only memory session and recreation does not synchronize`() = runTest {
        val repository = FakeRepository(AndroidSyncConnection(URL, KEY))
        val first = fixture(repository = repository)
        first.controller.signIn("learner@example.com", "secret".toCharArray()); advanceUntilIdle()
        first.controller.signOut()
        assertNull(first.sessions.currentSession())
        assertEquals(AndroidSyncPhase.SIGNED_OUT, first.controller.state.value.phase)

        val recreated = fixture(repository = repository)
        assertEquals(AndroidSyncPhase.SIGNED_OUT, recreated.controller.state.value.phase)
        assertEquals(0, recreated.operation.runs)
    }

    private fun TestScope.fixture(
        results: List<AndroidSyncSummary> = emptyList(),
        repository: FakeRepository = FakeRepository(),
        operation: FakeOperation = FakeOperation(results),
        scope: CoroutineScope = this,
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
    ): Fixture {
        val sessions = FakeSessions()
        val controller = AndroidSyncRuntimeController(scope, repository, { AndroidSyncRuntimeController.Runtime(sessions, operation) }, dispatcher)
        return Fixture(controller, sessions, operation)
    }

    private data class Fixture(val controller: AndroidSyncRuntimeController, val sessions: FakeSessions, val operation: FakeOperation)

    private class FakeRepository(private var value: AndroidSyncConnection? = null) : AndroidSyncConnectionRepository {
        override fun load() = value
        override fun save(connection: AndroidSyncConnection) { value = connection }
    }

    private class FakeSessions : AndroidSyncSessionAccess {
        var session: SupabaseSession? = null
        var signIns = 0
        var signInFailure: RuntimeException? = null
        override fun signIn(email: String, password: CharArray): SupabaseSession {
            signIns++
            signInFailure?.let { throw it }
            return SupabaseSession(ACCOUNT, "access", "refresh", userEmail = email).also { session = it }
        }
        override fun currentSession() = session
        override fun signOut() { session = null }
    }

    private class FakeOperation(
        results: List<AndroidSyncSummary> = emptyList(),
        private val block: (() -> AndroidSyncSummary)? = null
    ) : AndroidManualSyncOperation {
        private val queued = ArrayDeque(results)
        var runs = 0
        var result = summary()
        var failure: RuntimeException? = null
        override fun run(accountId: SyncAccountId, deviceId: vn.loi.learning.domain.sync.protocol.SyncDeviceId): AndroidSyncSummary {
            runs++
            failure?.let { throw it }
            return block?.invoke() ?: if (queued.isEmpty()) result else queued.removeFirst()
        }
    }

    companion object {
        private const val URL = "https://project.supabase.co"
        private const val KEY = "publishable-key"
        private val ACCOUNT = SyncAccountId("00000000-0000-0000-0000-000000000001")
        private fun summary(pushed: Int = 0, applied: Int = 0, conflicts: Int = 0, pending: Int = 0) =
            AndroidSyncSummary(pushed, applied, 0, 0, conflicts, pending, 0)
    }
}
