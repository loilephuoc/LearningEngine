package vn.loi.learning.android.study.debug

import vn.loi.learning.domain.study.recall.RecallMode

enum class LabStudyMode(val displayName: String, val recallMode: RecallMode) {
    TYPING("Gõ chính tả (Typing)", RecallMode.TYPING),
    LISTENING("Nghe chép chính tả (Listening)", RecallMode.LISTENING),
    MULTIPLE_CHOICE("Trắc nghiệm (Multiple Choice)", RecallMode.MULTIPLE_CHOICE),
    IMAGE_RECALL("Nhận diện hình ảnh (Image Recall)", RecallMode.IMAGE_RECALL),
    EXAMPLE_COMPLETION("Điền vào câu (Example Completion)", RecallMode.EXAMPLE_COMPLETION)
}
