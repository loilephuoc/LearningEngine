package vn.loi.learning.android.family

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

internal object FamilyLegacyImportNormalizer {
    private val placeholderTokens = setOf("", "0", "00", "000", "0000", "not", "n/a", "na", "-")
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val credentialPattern = Regex(
        "(?i)(password|facebookpass|vneid\\s*pass|(?:^|\\W)pass\\s*[:=]|(?:^|\\W)pwd\\s*[:=]|(?:^|\\W)pwd(?:\\W|$)|login\\s+password|(?:username|user|login)\\s*[:=].{0,40}(?:password|pass|pwd)\\s*[:=]|(?:username|user)\\s*.{0,40}(?:password|pass\\s*:|pwd)|(?:api[_-]?key|secret|token|bearer)\\s*[:=])"
    )
    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)
    )

    fun isPlaceholder(value: String, allZeroIsPlaceholder: Boolean = true): Boolean {
        val normalized = value.trim().lowercase()
        return normalized in placeholderTokens ||
            (allZeroIsPlaceholder && normalized.isNotEmpty() && normalized.all { it == '0' })
    }

    fun normalizePhoneForComparison(value: String): String = value.filter(Char::isDigit)

    fun splitPhones(value: String): List<String>? {
        val trimmed = value.trim()
        if ('/' !in trimmed) return listOf(trimmed)
        val parts = trimmed.split('/').map(String::trim).filter(String::isNotEmpty)
        return parts.takeIf { it.size > 1 && it.all { part -> normalizePhoneForComparison(part).length in 7..15 } }
    }

    fun isReasonablePhone(value: String): Boolean = normalizePhoneForComparison(value).length in 7..15
    fun isReasonableEmail(value: String): Boolean = emailPattern.matches(value.trim())
    fun normalizeEmail(value: String): String = value.trim().lowercase()

    fun parseDate(value: String): LocalDate? {
        val trimmed = value.trim()
        trimmed.toDoubleOrNull()?.let { serial ->
            if (serial >= 1 && serial <= 2_958_465 && serial % 1.0 == 0.0) {
                return runCatching { LocalDate.of(1899, 12, 30).plusDays(serial.toLong()) }.getOrNull()
            }
        }
        return dateFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(trimmed, formatter) }.getOrNull()
        }
    }

    fun containsSensitiveCredential(value: String): Boolean = credentialPattern.containsMatchIn(value)

    fun normalizeName(value: String): String = Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\s+"), " ")
}
