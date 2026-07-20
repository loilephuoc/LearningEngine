package vn.loi.learning.infrastructure

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.learningdashboard.LearningDashboardQueryService

/**
 * Tập hợp các entry point cấp ứng dụng được tạo từ cùng một
 * composition root.
 *
 * LearningApplicationContext bảo đảm:
 * - LearningEngine và LearningDashboardQueryService có thể dùng chung
 *   các repository instance;
 * - command side và query side vẫn là hai API độc lập;
 * - UI hoặc adapter bên ngoài không cần tự lắp ráp dependency;
 * - không chứa business logic;
 * - không trực tiếp truy cập persistence.
 */
data class LearningApplicationContext(
    val engine: LearningEngine,
    val dashboard: LearningDashboardQueryService
)