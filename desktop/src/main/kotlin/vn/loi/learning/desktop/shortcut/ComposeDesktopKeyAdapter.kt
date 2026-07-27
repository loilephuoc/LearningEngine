package vn.loi.learning.desktop.shortcut

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

fun KeyEvent.toDesktopKeyChord(): DesktopKeyChord? {
    val desktopKey =
        when (key) {
            Key.Enter, Key.NumPadEnter -> DesktopShortcutKey.ENTER
            Key.Spacebar -> DesktopShortcutKey.SPACE
            Key.Escape -> DesktopShortcutKey.ESCAPE
            Key.Zero, Key.NumPad0 -> DesktopShortcutKey.ZERO
            Key.One, Key.NumPad1 -> DesktopShortcutKey.ONE
            Key.Two, Key.NumPad2 -> DesktopShortcutKey.TWO
            Key.Three, Key.NumPad3 -> DesktopShortcutKey.THREE
            Key.Four, Key.NumPad4 -> DesktopShortcutKey.FOUR
            Key.Five, Key.NumPad5 -> DesktopShortcutKey.FIVE
            Key.Six, Key.NumPad6 -> DesktopShortcutKey.SIX
            Key.Seven, Key.NumPad7 -> DesktopShortcutKey.SEVEN
            Key.Eight, Key.NumPad8 -> DesktopShortcutKey.EIGHT
            Key.Nine, Key.NumPad9 -> DesktopShortcutKey.NINE
            Key.A -> DesktopShortcutKey.A
            Key.B -> DesktopShortcutKey.B
            Key.C -> DesktopShortcutKey.C
            Key.D -> DesktopShortcutKey.D
            Key.E -> DesktopShortcutKey.E
            Key.F -> DesktopShortcutKey.F
            Key.G -> DesktopShortcutKey.G
            Key.H -> DesktopShortcutKey.H
            Key.I -> DesktopShortcutKey.I
            Key.J -> DesktopShortcutKey.J
            Key.K -> DesktopShortcutKey.K
            Key.L -> DesktopShortcutKey.L
            Key.M -> DesktopShortcutKey.M
            Key.N -> DesktopShortcutKey.N
            Key.O -> DesktopShortcutKey.O
            Key.P -> DesktopShortcutKey.P
            Key.Q -> DesktopShortcutKey.Q
            Key.R -> DesktopShortcutKey.R
            Key.S -> DesktopShortcutKey.S
            Key.T -> DesktopShortcutKey.T
            Key.U -> DesktopShortcutKey.U
            Key.V -> DesktopShortcutKey.V
            Key.W -> DesktopShortcutKey.W
            Key.X -> DesktopShortcutKey.X
            Key.Y -> DesktopShortcutKey.Y
            Key.Z -> DesktopShortcutKey.Z
            else -> null
        } ?: return null
    return DesktopKeyChord(
        key = desktopKey,
        controlPressed = isCtrlPressed,
        altPressed = isAltPressed,
        shiftPressed = isShiftPressed
    )
}
