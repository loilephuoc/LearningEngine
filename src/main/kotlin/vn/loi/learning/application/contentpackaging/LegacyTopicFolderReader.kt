package vn.loi.learning.application.contentpackaging

fun interface LegacyTopicFolderReader {

    fun read(folder: String): List<LegacyTopicDiscoveryFile>
}
