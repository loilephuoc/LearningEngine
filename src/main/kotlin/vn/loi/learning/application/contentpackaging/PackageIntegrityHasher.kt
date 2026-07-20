package vn.loi.learning.application.contentpackaging

fun interface PackageIntegrityHasher {

    fun hash(
        content: String
    ): String
}