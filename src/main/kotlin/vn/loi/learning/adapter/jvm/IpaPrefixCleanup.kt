package vn.loi.learning.adapter.jvm

internal object IpaPrefixCleanup {
    private val hyphenPrefix = Regex("""^\s*\([^)]*\)\s*-\s*(.*)$""")
    private val whitespacePrefix = Regex("""^\s*\([^)]*\)\s+(.+)$""")

    fun clean(value: String): String? =
        hyphenPrefix.matchEntire(value)?.groupValues?.get(1)?.trim()
            ?: whitespacePrefix.matchEntire(value)?.groupValues?.get(1)?.trim()
}
