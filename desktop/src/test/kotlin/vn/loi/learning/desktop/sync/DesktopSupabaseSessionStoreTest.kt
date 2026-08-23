package vn.loi.learning.desktop.sync

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertEquals
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.infrastructure.sync.supabase.SupabaseSession

class DesktopSupabaseSessionStoreTest {
    @Test fun `windows DPAPI round trip is current-user scoped`() {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return
        val protector = WindowsDpapiProtector()
        val plain = "auth-session-fixture".encodeToByteArray()
        val encrypted = protector.protect(plain)
        assertFalse(encrypted.contentEquals(plain))
        assertContentEquals(plain, protector.unprotect(encrypted))
    }

    @Test fun `encrypted session survives store recreation without plaintext secrets`() {
        val root = Files.createTempDirectory("desktop-auth-session-")
        try {
            val file = root.resolve(DesktopSupabaseSessionStore.FILE_NAME)
            val cipher = XorProtector()
            DesktopSupabaseSessionStore(file, cipher, cipher).replace(session())
            val persisted = Files.readAllBytes(file).decodeToString()
            assertFalse(persisted.contains("access-secret"))
            assertFalse(persisted.contains("refresh-secret"))
            assertFalse(persisted.contains("password"))
            assertFalse(persisted.contains("service-role"))
            assertEquals(session(), DesktopSupabaseSessionStore(file, cipher, cipher).load())
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `corrupt persisted session fails closed and deletes unusable file`() {
        val root = Files.createTempDirectory("desktop-auth-session-")
        try {
            val file = root.resolve(DesktopSupabaseSessionStore.FILE_NAME)
            Files.writeString(file, "corrupt")
            val cipher = XorProtector()
            assertNull(DesktopSupabaseSessionStore(file, cipher, cipher).load())
            assertFalse(Files.exists(file))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `logout clear removes persisted material`() {
        val root = Files.createTempDirectory("desktop-auth-session-")
        try {
            val file = root.resolve(DesktopSupabaseSessionStore.FILE_NAME)
            val cipher = XorProtector()
            val store = DesktopSupabaseSessionStore(file, cipher, cipher)
            store.replace(session()); store.clear()
            assertNull(store.load())
            assertFalse(Files.exists(file))
        } finally { root.toFile().deleteRecursively() }
    }

    private fun session() = SupabaseSession(
        SyncAccountId("00000000-0000-0000-0000-000000000001"),
        "access-secret", "refresh-secret", 4_000, "learner@example.com"
    )

    private class XorProtector : DesktopSecretProtector, DesktopSecretUnprotector {
        override fun protect(plainText: ByteArray) = plainText.map { (it.toInt() xor 0x5a).toByte() }.toByteArray()
        override fun unprotect(cipherText: ByteArray) = protect(cipherText)
    }
}
