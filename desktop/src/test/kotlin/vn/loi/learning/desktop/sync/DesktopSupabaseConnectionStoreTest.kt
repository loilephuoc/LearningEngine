package vn.loi.learning.desktop.sync

import java.nio.file.Files
import kotlin.test.*

class DesktopSupabaseConnectionStoreTest {
    @Test fun `missing configuration stays disabled without creating a file`() {
        val root = Files.createTempDirectory("desktop-sync-config-")
        try {
            val file = root.resolve("sync.properties")
            assertNull(DesktopSupabaseConnectionStore(file).load())
            assertFalse(Files.exists(file))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `connection config round trips without session secrets`() {
        val root = Files.createTempDirectory("desktop-sync-config-")
        try {
            val file = root.resolve("sync.properties")
            val store = DesktopSupabaseConnectionStore(file)
            store.save(DesktopSupabaseConnection("https://project.supabase.co/", "sb_publishable_client"))
            assertEquals(DesktopSupabaseConnection("https://project.supabase.co", "sb_publishable_client"), store.load())
            val persisted = Files.readString(file)
            assertFalse(persisted.contains("access_token")); assertFalse(persisted.contains("refresh_token")); assertFalse(persisted.contains("password"))
            assertFalse(store.load()!!.maskedKey().contains("publishable"))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `invalid and unsafe URLs are rejected before persistence`() {
        val root = Files.createTempDirectory("desktop-sync-config-")
        try {
            val file = root.resolve("sync.properties")
            val store = DesktopSupabaseConnectionStore(file)
            assertFailsWith<IllegalArgumentException> { store.save(DesktopSupabaseConnection("http://remote.example", "key")) }
            assertFailsWith<IllegalArgumentException> { store.save(DesktopSupabaseConnection("https://project.supabase.co/path?x=1", "key")) }
            assertFalse(Files.exists(file))
        } finally { root.toFile().deleteRecursively() }
    }
}
