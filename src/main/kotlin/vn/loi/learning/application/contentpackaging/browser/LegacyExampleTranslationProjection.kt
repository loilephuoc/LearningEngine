package vn.loi.learning.application.contentpackaging.browser

object LegacyExampleTranslationProjection {
    data class SplitExample(
        val exampleText: String?,
        val exampleTranslation: String?
    )

    fun project(rawExampleText: String?, rawExampleTranslation: String?): SplitExample {
        if (!rawExampleTranslation.isNullOrBlank()) {
            return SplitExample(
                exampleText = rawExampleText?.trim()?.takeIf { it.isNotBlank() },
                exampleTranslation = rawExampleTranslation.trim()
            )
        }

        if (rawExampleText.isNullOrBlank()) {
            return SplitExample(null, null)
        }

        val text = rawExampleText.trim()
        
        // Handle common legacy bilingual combined patterns
        // 1. Newline separator: "English example\nVietnamese translation"
        if (text.contains("\n")) {
            val parts = text.split("\n", limit = 2)
            val eng = parts[0].trim()
            val vi = parts[1].trim().removeSurrounding("(", ")").removeSurrounding("“", "”").removeSurrounding("\"", "\"").trim()
            if (eng.isNotBlank() && vi.isNotBlank()) {
                return SplitExample(eng, vi)
            }
        }

        // 2. Parentheses separator: "English example (Vietnamese translation)"
        if (text.endsWith(")") && text.contains(" (")) {
            val openParenIdx = text.lastIndexOf(" (")
            if (openParenIdx > 0) {
                val eng = text.substring(0, openParenIdx).trim()
                val vi = text.substring(openParenIdx + 2, text.length - 1).trim()
                if (eng.isNotBlank() && vi.isNotBlank()) {
                    return SplitExample(eng, vi)
                }
            }
        }

        // 3. Dash separator: "English example - Vietnamese translation"
        if (text.contains(" - ")) {
            val parts = text.split(" - ", limit = 2)
            val eng = parts[0].trim()
            val vi = parts[1].trim()
            if (eng.isNotBlank() && vi.isNotBlank()) {
                return SplitExample(eng, vi)
            }
        }

        return SplitExample(text, null)
    }
}
