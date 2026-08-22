package vn.loi.learning.application.port

interface IncrementalMediaBlobStore {
    fun sha256(bytes: ByteArray): String
    fun stage(sha256: String, bytes: ByteArray)
    fun hasStaged(sha256: String, sizeBytes: Long): Boolean
    fun materialize(sha256: String, sizeBytes: Long, mimeType: String): String
    fun digestReference(reference: String): String?
    fun discardStaged(sha256: String)
    fun deleteManaged(reference: String): Boolean
    fun isManaged(reference: String): Boolean
}
