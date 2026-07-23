package vn.loi.learning.application.contentpackaging

interface PackageIntegrityHasher {

    fun hash(
        content: String
    ): String

    fun hash(
        bytes: ByteArray
    ): String =
        hash(String(bytes, Charsets.UTF_8))
}