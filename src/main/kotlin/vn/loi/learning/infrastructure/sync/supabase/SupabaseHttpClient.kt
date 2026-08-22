package vn.loi.learning.infrastructure.sync.supabase

import java.net.HttpURLConnection
import java.net.URI

data class SupabaseHttpRequest(
    val method: String,
    val uri: URI,
    val headers: Map<String, String>,
    val body: ByteArray? = null,
    val timeoutMillis: Int
)

data class SupabaseHttpResponse(val status: Int, val body: ByteArray, val headers: Map<String, List<String>> = emptyMap())

fun interface SupabaseHttpClient { fun execute(request: SupabaseHttpRequest): SupabaseHttpResponse }

class UrlConnectionSupabaseHttpClient : SupabaseHttpClient {
    override fun execute(request: SupabaseHttpRequest): SupabaseHttpResponse {
        check(!Thread.currentThread().isInterrupted) { "Sync request cancelled." }
        val connection = request.uri.toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = request.timeoutMillis
            connection.readTimeout = request.timeoutMillis
            request.headers.forEach(connection::setRequestProperty)
            request.body?.let {
                connection.doOutput = true
                connection.outputStream.use { output -> output.write(it) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            return SupabaseHttpResponse(
                status,
                stream?.use { it.readBytes() } ?: byteArrayOf(),
                connection.headerFields.mapKeys { it.key.orEmpty() }
            )
        } finally {
            connection.disconnect()
        }
    }
}

class SupabaseTransportException(
    val code: String,
    val retryable: Boolean,
    val status: Int? = null,
    cause: Throwable? = null
) : RuntimeException(code, cause)
