package vn.loi.learning.application.port

interface Opd2RecoveryMediaPort {
    fun prepare(expectedCanonicalReferences: Set<String>): PreparedOpd2RecoveryMedia
}

interface PreparedOpd2RecoveryMedia : AutoCloseable {
    val recoverableReferenceCount: Int
    val publishedFileCount: Int
    val missingReferences: Set<String>
    fun publish()
    fun verifyPublished()
    fun rollbackPublished()
    fun complete()
}
