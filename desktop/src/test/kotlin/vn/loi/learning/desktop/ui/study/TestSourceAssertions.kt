package vn.loi.learning.desktop.ui.study

internal fun String.containsCodeIgnoringWhitespace(expected: String): Boolean =
    replace(Regex("\\s+"), "").contains(expected.replace(Regex("\\s+"), ""))
