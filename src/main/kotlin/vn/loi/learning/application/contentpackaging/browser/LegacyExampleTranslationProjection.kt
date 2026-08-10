package vn.loi.learning.application.contentpackaging.browser

object LegacyExampleTranslationProjection {
    data class SplitExample(val exampleText: String?, val exampleTranslation: String?)

    fun project(rawExampleText: String?, rawExampleTranslation: String?): SplitExample {
        if (!rawExampleTranslation.isNullOrBlank()) {
            return SplitExample(
                rawExampleText?.trim()?.takeIf(String::isNotBlank),
                rawExampleTranslation.trim()
            )
        }
        if (rawExampleText.isNullOrBlank()) return SplitExample(null, null)

        val text = rawExampleText.trim()
        // A newline alone is not evidence: valid English examples may be multiline.
        val logicalLines = text.lines().map(String::trim).filter(String::isNotBlank)
        if (logicalLines.size == 2) {
            splitWhenBilingual(logicalLines[0], logicalLines[1])?.let { return it }
        }

        if (text.endsWith(")") && text.contains(" (")) {
            val boundary = text.lastIndexOf(" (")
            if (boundary > 0) {
                splitWhenBilingual(text.substring(0, boundary), text.substring(boundary + 2, text.length - 1))
                    ?.let { return it }
            }
        }

        if (text.contains(" - ")) {
            val parts = text.split(" - ", limit = 2)
            splitWhenBilingual(parts[0], parts[1])?.let { return it }
        }
        return SplitExample(text, null)
    }

    private fun splitWhenBilingual(englishRaw: String, vietnameseRaw: String): SplitExample? {
        val english = englishRaw.trim()
        val vietnamese = vietnameseRaw.trim()
            .removeSurrounding("(", ")")
            .removeSurrounding("“", "”")
            .removeSurrounding("\"", "\"")
            .trim()
        return if (english.isNotBlank() && !hasVietnameseSignal(english) && hasVietnameseSignal(vietnamese)) {
            SplitExample(english, vietnamese)
        } else null
    }

    private fun hasVietnameseSignal(value: String): Boolean =
        value.any { it in VIETNAMESE_SPECIFIC_CHARACTERS }

    private const val VIETNAMESE_SPECIFIC_CHARACTERS =
        "ăâđêôơưĂÂĐÊÔƠƯáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ" +
            "ÁÀẢÃẠẤẦẨẪẬẮẰẲẴẶÉÈẺẼẸẾỀỂỄỆÍÌỈĨỊÓÒỎÕỌỐỒỔỖỘỚỜỞỠỢÚÙỦŨỤỨỪỬỮỰÝỲỶỸỴ"
}
