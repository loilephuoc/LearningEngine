package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

@Serializable
data class PackageCatalogRecord(
    val id: String,
    val packageIds: List<String>
 )
