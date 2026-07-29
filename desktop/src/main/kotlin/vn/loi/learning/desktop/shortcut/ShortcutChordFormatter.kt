package vn.loi.learning.desktop.shortcut

object ShortcutChordFormatter {
    fun format(chord: DesktopKeyChord, compact: Boolean = false): String =
        buildList {
            if (chord.controlPressed) add("Ctrl")
            if (chord.altPressed) add("Alt")
            if (chord.shiftPressed) add(if (compact) "⇧" else "Shift")
            add(chord.key.displayName)
        }.let { parts ->
            if (compact && "⇧" in parts) {
                val prefix = parts.takeWhile { it != "⇧" }
                (prefix + ("⇧" + chord.key.displayName)).joinToString("+")
            } else {
                parts.joinToString("+")
            }
        }
}
