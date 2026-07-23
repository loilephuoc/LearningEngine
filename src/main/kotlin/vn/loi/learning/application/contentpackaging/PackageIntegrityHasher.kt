package vn.loi.learning.application.contentpackaging

fun interface PackageIntegrityHasher {

    fun hash(
        content: String
    ): String

    fun hash(
        bytes: ByteArray
    ): String =
        hash(String(bytes, Charsets.UTF_8))
}