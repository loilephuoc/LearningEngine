package vn.loi.learning.desktop.sync

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.infrastructure.sync.supabase.SecureSupabaseSessionStore
import vn.loi.learning.infrastructure.sync.supabase.SupabaseSession

internal fun interface DesktopSecretProtector {
    fun protect(plainText: ByteArray): ByteArray
}

internal fun interface DesktopSecretUnprotector {
    fun unprotect(cipherText: ByteArray): ByteArray
}

internal class WindowsDpapiProtector : DesktopSecretProtector, DesktopSecretUnprotector {
    override fun protect(plainText: ByteArray): ByteArray = invokeDpapi(plainText, protect = true)
    override fun unprotect(cipherText: ByteArray): ByteArray = invokeDpapi(cipherText, protect = false)

    private fun invokeDpapi(input: ByteArray, protect: Boolean): ByteArray {
        val operation = if (protect) "Protect" else "Unprotect"
        val script = "Add-Type -AssemblyName System.Security;" +
            "${'$'}b=[Convert]::FromBase64String([Console]::In.ReadToEnd());" +
            "${'$'}o=[Security.Cryptography.ProtectedData]::$operation(${'$'}b,${'$'}null,[Security.Cryptography.DataProtectionScope]::CurrentUser);" +
            "[Console]::Out.Write([Convert]::ToBase64String(${'$'}o))"
        val process = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
            .redirectErrorStream(true)
            .start()
        process.outputStream.bufferedWriter(Charsets.US_ASCII).use {
            it.write(Base64.getEncoder().encodeToString(input))
        }
        check(process.waitFor(15, TimeUnit.SECONDS)) { "DPAPI operation timed out." }
        val output = process.inputStream.bufferedReader(Charsets.US_ASCII).use { it.readText() }.trim()
        check(process.exitValue() == 0) { "DPAPI operation failed." }
        return Base64.getDecoder().decode(output)
    }
}

internal class DesktopSupabaseSessionStore(
    private val file: Path,
    private val protector: DesktopSecretProtector,
    private val unprotector: DesktopSecretUnprotector
) : SecureSupabaseSessionStore {
    @Synchronized override fun load(): SupabaseSession? {
        if (!Files.isRegularFile(file)) return null
        return runCatching { decode(unprotector.unprotect(Files.readAllBytes(file))) }
            .getOrElse { clear(); null }
    }

    @Synchronized override fun replace(session: SupabaseSession) {
        require(!session.refreshToken.isNullOrBlank()) { "A resumable Supabase session requires a refresh token." }
        Files.createDirectories(file.parent)
        val temporary = Files.createTempFile(file.parent, "supabase-session.", ".tmp")
        try {
            Files.write(temporary, protector.protect(encode(session)))
            runCatching {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }.getOrElse { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temporary) }
    }

    @Synchronized override fun clear() { Files.deleteIfExists(file) }

    private fun encode(session: SupabaseSession) = buildJsonObject {
        put("version", 1)
        put("accountId", session.accountId.value)
        put("accessToken", session.accessToken)
        put("refreshToken", requireNotNull(session.refreshToken))
        put("expiresAtEpochSeconds", session.expiresAtEpochSeconds)
        session.userEmail?.let { put("userEmail", it) }
    }.toString().encodeToByteArray()

    private fun decode(bytes: ByteArray): SupabaseSession {
        val value = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
        require(value.getValue("version").jsonPrimitive.long == 1L)
        return SupabaseSession(
            SyncAccountId(value.getValue("accountId").jsonPrimitive.content),
            value.getValue("accessToken").jsonPrimitive.content,
            value.getValue("refreshToken").jsonPrimitive.content,
            value.getValue("expiresAtEpochSeconds").jsonPrimitive.long,
            value["userEmail"]?.jsonPrimitive?.content
        ).also {
            require(it.accessToken.isNotBlank())
            require(!it.refreshToken.isNullOrBlank())
        }
    }

    companion object { const val FILE_NAME = "supabase-session.dpapi" }
}
