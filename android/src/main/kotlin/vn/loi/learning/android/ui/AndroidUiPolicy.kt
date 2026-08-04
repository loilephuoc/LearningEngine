package vn.loi.learning.android.ui

enum class AndroidWindowWidth { COMPACT, MEDIUM, EXPANDED }

data class AndroidLayoutPolicy(
    val width: AndroidWindowWidth,
    val horizontalPaddingDp: Int,
    val maxContentWidthDp: Int,
    val maxMediaHeightDp: Int
)

fun androidLayoutPolicy(widthDp: Int, heightDp: Int): AndroidLayoutPolicy {
    val width = when {
        widthDp < 600 -> AndroidWindowWidth.COMPACT
        widthDp < 840 -> AndroidWindowWidth.MEDIUM
        else -> AndroidWindowWidth.EXPANDED
    }
    return AndroidLayoutPolicy(
        width = width,
        horizontalPaddingDp = when (width) { AndroidWindowWidth.COMPACT -> 16; AndroidWindowWidth.MEDIUM -> 32; AndroidWindowWidth.EXPANDED -> 48 },
        maxContentWidthDp = when (width) { AndroidWindowWidth.COMPACT -> 600; AndroidWindowWidth.MEDIUM -> 720; AndroidWindowWidth.EXPANDED -> 840 },
        maxMediaHeightDp = if (heightDp < 480) 180 else if (width == AndroidWindowWidth.COMPACT) 320 else 420
    )
}

data class AndroidAccessibilityStrings(
    val answer: String,
    val replay: String,
    val audioUnavailable: String,
    val imagePrompt: String,
    val blank: String,
    val option: (Int, Int, Boolean) -> String,
    val loading: String
)

fun androidAccessibilityStrings(language: String): AndroidAccessibilityStrings = if (language == "vi") {
    AndroidAccessibilityStrings(
        answer = "Câu trả lời", replay = "Phát lại âm thanh", audioUnavailable = "Không có âm thanh",
        imagePrompt = "Hình ảnh gợi nhớ", blank = "chỗ trống cần điền", loading = "Đang xử lý",
        option = { index, total, selected -> "Lựa chọn $index trên $total${if (selected) ", đã chọn" else ""}" }
    )
} else {
    AndroidAccessibilityStrings(
        answer = "Answer", replay = "Replay audio", audioUnavailable = "Audio unavailable",
        imagePrompt = "Recall prompt image", blank = "blank to complete", loading = "Operation in progress",
        option = { index, total, selected -> "Option $index of $total${if (selected) ", selected" else ""}" }
    )
}
