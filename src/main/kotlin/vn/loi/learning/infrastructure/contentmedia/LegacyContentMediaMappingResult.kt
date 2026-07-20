package vn.loi.learning.infrastructure.contentmedia

import vn.loi.learning.domain.content.model.Content

data class LegacyContentMediaMappingResult(
    val contents: List<Content>,
    val warnings: List<String> = emptyList()
)