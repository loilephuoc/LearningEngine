package vn.loi.learning.infrastructure

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.contentpackaging.PackageImportService

/**
 * Composition Root của ứng dụng.
 *
 * Gom các service cấp cao mà Desktop / CLI sử dụng.
 * Không chứa business logic.
 */
data class PersistedLearningPlatform(
    val learningEngine: LearningEngine,
    val packageImportService: PackageImportService
)
