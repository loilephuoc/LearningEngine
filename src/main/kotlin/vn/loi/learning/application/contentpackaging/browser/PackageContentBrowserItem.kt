package vn.loi.learning.application.contentpackaging.browser

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

/**
 * Representation DTO cho một hàng trong Learning Browser 1.0.
 *
 * Contract Representation: ONE ROW PER CONTENT (Option A).
 * - Mỗi [PackageContentBrowserItem] đại diện cho 1 [vn.loi.learning.domain.content.model.Content] duy nhất trong package.
 * - Tránh duplicate 5x vô nghĩa cho các Content có nhiều LearningItem (e.g. 990 contents -> 990 rows thay vì 4950 rows).
 * - Danh sách [learningItemIds] và [learningModes] được tổng hợp đầy đủ để hiển thị trong Preview Panel.
 */
data class PackageContentBrowserItem(
    val index: Int, // 1-indexed order within package
    val contentId: ContentId,
    val questionText: String,
    val answerText: String,
    val pronunciation: String,
    val partOfSpeech: String,
    val group: String?,
    val section: String?,
    val lesson: String,
    val packageName: String,
    val hasImage: Boolean,
    val hasAudio: Boolean,
    val imageRef: String?,
    val audioRef: String?,
    val exampleText: String?,
    val exampleTranslation: String?,
    val learningItemCount: Int,
    val learningItemIds: List<LearningItemId>,
    val learningModes: List<LearningMode>,
    val tags: Set<String>,
    val searchableText: String
)
