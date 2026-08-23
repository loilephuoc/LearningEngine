package vn.loi.learning.desktop.sync

import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.infrastructure.sync.supabase.*

class DesktopSyncControllerTest {
    @Test fun `restart restores an existing valid session without login`() {
        val root = Files.createTempDirectory("desktop-sync-controller-")
        try {
            val connectionStore = DesktopSupabaseConnectionStore(root.resolve("sync.properties"))
            connectionStore.save(DesktopSupabaseConnection("https://project.supabase.co", "publishable"))
            val controller = DesktopSyncController(connectionStore, { DesktopSyncController.Runtime(FakeSessions(session(USER)), DesktopManualSyncOperation { _, _ -> DesktopManualSyncSummary(0, 0, 0, 0, 0, 0, 0) }) })
            assertEquals(DesktopSyncPhase.READY, controller.state.value.phase)
            assertEquals("learner@example.com", controller.state.value.signedInEmail)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `restart without persisted session remains signed out`() {
        val root = Files.createTempDirectory("desktop-sync-controller-")
        try {
            val connectionStore = DesktopSupabaseConnectionStore(root.resolve("sync.properties"))
            connectionStore.save(DesktopSupabaseConnection("https://project.supabase.co", "publishable"))
            val controller = DesktopSyncController(connectionStore, { runtime() })
            assertEquals(DesktopSyncPhase.SIGNED_OUT, controller.state.value.phase)
            assertNull(controller.state.value.signedInEmail)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `startup and recomposition-equivalent state reads perform no network or sync`() {
        val root = Files.createTempDirectory("desktop-sync-controller-")
        try {
            val factories = AtomicInteger(); val syncs = AtomicInteger()
            val controller = DesktopSyncController(DesktopSupabaseConnectionStore(root.resolve("sync.properties")), { factories.incrementAndGet(); runtime(syncs = syncs) })
            repeat(10) { controller.state.value }
            assertEquals(DesktopSyncPhase.UNCONFIGURED, controller.state.value.phase)
            assertEquals(0, factories.get()); assertEquals(0, syncs.get())
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `save login no-op sync and sign out have explicit states`() {
        val root = Files.createTempDirectory("desktop-sync-controller-")
        try {
            val controller = DesktopSyncController(DesktopSupabaseConnectionStore(root.resolve("sync.properties")), { runtime() })
            controller.saveConnection("https://project.supabase.co", "publishable")
            assertEquals(DesktopSyncPhase.SIGNED_OUT, controller.state.value.phase)
            controller.signIn("learner@example.com", "password".toCharArray())
            assertEquals(DesktopSyncPhase.READY, controller.state.value.phase)
            controller.syncNow()
            assertEquals(DesktopSyncPhase.NO_CHANGES, controller.state.value.phase)
            controller.signOut()
            assertEquals(DesktopSyncPhase.SIGNED_OUT, controller.state.value.phase)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `double login and concurrent sync are suppressed`() {
        val root = Files.createTempDirectory("desktop-sync-controller-")
        try {
            val entered = CountDownLatch(1); val release = CountDownLatch(1); val loginCalls = AtomicInteger(); val syncCalls = AtomicInteger()
            val sessions = object : DesktopSyncController.SessionAccess {
                @Volatile var session: SupabaseSession? = null
                override fun currentSession() = session
                override fun signIn(email: String, password: CharArray): SupabaseSession { loginCalls.incrementAndGet(); entered.countDown(); release.await(); return session(USER).also { session = it } }
                override fun signOut() { session = null }
            }
            val controller = DesktopSyncController(DesktopSupabaseConnectionStore(root.resolve("sync.properties")), { DesktopSyncController.Runtime(sessions, DesktopManualSyncOperation { _, _ -> syncCalls.incrementAndGet(); DesktopManualSyncSummary(1, 1, 0, 0, 0, 0, 1) }) })
            controller.saveConnection("https://project.supabase.co", "key")
            val pool = Executors.newFixedThreadPool(2)
            try {
                val first = pool.submit { controller.signIn("a@b.com", "first".toCharArray()) }
                entered.await(); controller.signIn("a@b.com", "second".toCharArray()); release.countDown(); first.get()
                val syncEntered = CountDownLatch(1); val syncRelease = CountDownLatch(1)
                val blocking = DesktopSyncController(DesktopSupabaseConnectionStore(root.resolve("other.properties")), { DesktopSyncController.Runtime(sessions, DesktopManualSyncOperation { _, _ -> syncCalls.incrementAndGet(); syncEntered.countDown(); syncRelease.await(); DesktopManualSyncSummary(1, 1, 0, 0, 0, 0, 1) }) })
                blocking.saveConnection("https://project.supabase.co", "key"); blocking.signIn("a@b.com", "x".toCharArray())
                val job = pool.submit { blocking.syncNow() }; syncEntered.await(); blocking.syncNow(); syncRelease.countDown(); job.get()
                assertEquals(2, loginCalls.get()); assertEquals(1, syncCalls.get())
            } finally { pool.shutdownNow() }
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `offline and pending blob remain visible without false success`() {
        val root = Files.createTempDirectory("desktop-sync-controller-")
        try {
            fun controller(failure: RuntimeException): DesktopSyncController {
                val result = DesktopSyncController(DesktopSupabaseConnectionStore(root.resolve(java.util.UUID.randomUUID().toString())), { DesktopSyncController.Runtime(FakeSessions(session(USER)), DesktopManualSyncOperation { _, _ -> throw failure }) })
                result.saveConnection("https://project.supabase.co", "key"); result.signIn("a@b.com", "x".toCharArray()); return result
            }
            assertEquals(DesktopSyncPhase.OFFLINE, controller(SupabaseTransportException("NETWORK", true)).also { it.syncNow() }.state.value.phase)
            assertEquals(DesktopSyncPhase.PENDING_BLOB, controller(PendingRemoteMediaException()).also { it.syncNow() }.state.value.phase)
        } finally { root.toFile().deleteRecursively() }
    }

    private fun runtime(syncs: AtomicInteger = AtomicInteger()) = DesktopSyncController.Runtime(FakeSessions(null), DesktopManualSyncOperation { _, _ -> syncs.incrementAndGet(); DesktopManualSyncSummary(0, 0, 0, 0, 0, 0, 0) })
    private class FakeSessions(initial: SupabaseSession?) : DesktopSyncController.SessionAccess {
        private var current = initial
        override fun currentSession() = current
        override fun signIn(email: String, password: CharArray) =
            SupabaseSession(SyncAccountId(USER), "access", "refresh", Long.MAX_VALUE, "learner@example.com")
                .also { password.fill('\u0000'); current = it }
        override fun signOut() { current = null }
    }
    private fun session(id: String) = SupabaseSession(SyncAccountId(id), "access", "refresh", Long.MAX_VALUE, "learner@example.com")
    private companion object { const val USER = "00000000-0000-0000-0000-000000000001" }
}
