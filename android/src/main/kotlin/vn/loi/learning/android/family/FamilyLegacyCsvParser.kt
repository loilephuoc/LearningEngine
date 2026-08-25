package vn.loi.learning.android.family

internal object FamilyLegacyCsvParser {
    fun parse(text: String): LegacyCsvParseResult {
        val records = parseRecords(text)
        if (records.isEmpty()) return LegacyCsvParseResult(emptyList(), emptyList())
        val header = records.first().mapIndexed { index, value -> normalizeHeader(value).let { it to index } }.toMap()
        val missing = LegacyColumn.entries.filter { normalizeHeader(it.header) !in header }
        if (missing.isNotEmpty()) {
            return LegacyCsvParseResult(
                emptyList(),
                missing.map { LegacyImportIssue(1, it, LegacyImportSeverity.BLOCKED, LegacyImportReason.MISSING_REQUIRED_HEADER) }
            )
        }
        val issues = mutableListOf<LegacyImportIssue>()
        val rows = records.drop(1).mapIndexedNotNull { index, cells ->
            if (cells.all(String::isBlank)) return@mapIndexedNotNull null
            if (cells.size != LegacyColumn.entries.size) {
                issues += LegacyImportIssue(index + 2, null, LegacyImportSeverity.WARNING, LegacyImportReason.INVALID_COLUMN_COUNT)
            }
            LegacyPersonRow(
                rowIndex = index + 2,
                values = LegacyColumn.entries.associateWith { column ->
                    cells.getOrElse(header.getValue(normalizeHeader(column.header))) { "" }
                }
            )
        }
        return LegacyCsvParseResult(rows, issues)
    }

    private fun normalizeHeader(value: String): String = value.removePrefix("\uFEFF").trim().lowercase()

    private fun parseRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> {
                    field.append('"'); index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> { row += field.toString(); field.clear() }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += field.toString(); field.clear(); records += row; row = mutableListOf()
                }
                else -> field.append(char)
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); records += row }
        return records
    }
}
