package vn.loi.learning.desktop.shortcut

enum class DesktopShortcutKey(val displayName: String) {
    ENTER("Enter"),
    SPACE("Space"),
    ESCAPE("Esc"),
    ZERO("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    A("A"),
    B("B"),
    C("C"),
    D("D"),
    E("E"),
    F("F"),
    G("G"),
    H("H"),
    I("I"),
    J("J"),
    K("K"),
    L("L"),
    M("M"),
    N("N"),
    O("O"),
    P("P"),
    Q("Q"),
    R("R"),
    S("S"),
    T("T"),
    U("U"),
    V("V"),
    W("W"),
    X("X"),
    Y("Y"),
    Z("Z")
}

data class DesktopKeyChord(
    val key: DesktopShortcutKey,
    val controlPressed: Boolean = false,
    val altPressed: Boolean = false,
    val shiftPressed: Boolean = false
) {
    val displayName: String
        get() = buildList {
            if (controlPressed) add("Ctrl")
            if (altPressed) add("Alt")
            if (shiftPressed) add("Shift")
            add(key.displayName)
        }.joinToString("+")
}

enum class StudyShortcutCommand(val displayName: String) {
    REVEAL_ANSWER("Reveal Answer"),
    RATE_AGAIN("Again"),
    RATE_HARD("Hard"),
    RATE_GOOD("Good"),
    RATE_EASY("Easy"),
    REPLAY_PRIMARY_AUDIO("Replay Audio"),
    TOGGLE_VOCABULARY_AUDIO_LOOP("Loop Vocabulary Audio"),
    TOGGLE_EXAMPLE_AUDIO_LOOP("Loop Example Audio"),
    PLAY_VIETNAMESE_MEANING_AUDIO("Play Vietnamese Meaning"),
    PLAY_VIETNAMESE_EXAMPLE_AUDIO("Play Vietnamese Example"),
    UNDO("Undo"),
    PAUSE("Pause")
}

data class ShortcutBinding(
    val command: StudyShortcutCommand,
    val chord: DesktopKeyChord
)

enum class ShortcutConflictResolution {
    CANCEL,
    SWAP,
    REPLACE
}

data class ShortcutConflict(
    val requestedCommand: StudyShortcutCommand,
    val occupiedBy: StudyShortcutCommand,
    val chord: DesktopKeyChord
)

sealed interface ShortcutChangeResult {
    data class Changed(val registry: ShortcutRegistry) : ShortcutChangeResult
    data class Conflict(val conflict: ShortcutConflict) : ShortcutChangeResult
}

class ShortcutRegistry private constructor(
    private val chordsByCommand: Map<StudyShortcutCommand, DesktopKeyChord>
) {
    init {
        require(chordsByCommand.keys == StudyShortcutCommand.entries.toSet()) {
            "Every Study shortcut command must have a binding."
        }
        require(chordsByCommand.values.toSet().size == chordsByCommand.size) {
            "Study shortcut bindings must not contain duplicates."
        }
    }

    val bindings: List<ShortcutBinding>
        get() = StudyShortcutCommand.entries.map { ShortcutBinding(it, chordFor(it)) }

    fun chordFor(command: StudyShortcutCommand): DesktopKeyChord =
        requireNotNull(chordsByCommand[command])

    fun commandFor(chord: DesktopKeyChord): StudyShortcutCommand? =
        chordsByCommand.entries.firstOrNull { it.value == chord }?.key

    fun requestChange(
        command: StudyShortcutCommand,
        chord: DesktopKeyChord
    ): ShortcutChangeResult {
        val occupiedBy = commandFor(chord)
        return if (occupiedBy == null || occupiedBy == command) {
            ShortcutChangeResult.Changed(copyWith(command, chord))
        } else {
            ShortcutChangeResult.Conflict(ShortcutConflict(command, occupiedBy, chord))
        }
    }

    fun resolveConflict(
        conflict: ShortcutConflict,
        resolution: ShortcutConflictResolution
    ): ShortcutRegistry =
        when (resolution) {
            ShortcutConflictResolution.CANCEL -> this
            ShortcutConflictResolution.SWAP -> {
                val previous = chordFor(conflict.requestedCommand)
                create(
                    chordsByCommand +
                        (conflict.requestedCommand to conflict.chord) +
                        (conflict.occupiedBy to previous)
                )
            }
            ShortcutConflictResolution.REPLACE -> {
                val previous = chordFor(conflict.requestedCommand)
                create(
                    chordsByCommand +
                        (conflict.requestedCommand to conflict.chord) +
                        (conflict.occupiedBy to previous)
                )
            }
        }

    private fun copyWith(
        command: StudyShortcutCommand,
        chord: DesktopKeyChord
    ): ShortcutRegistry = create(chordsByCommand + (command to chord))

    override fun equals(other: Any?): Boolean =
        other is ShortcutRegistry && chordsByCommand == other.chordsByCommand

    override fun hashCode(): Int = chordsByCommand.hashCode()

    override fun toString(): String = "ShortcutRegistry(${serialize()})"

    fun serialize(): String =
        StudyShortcutCommand.entries.joinToString(",") { command ->
            "${command.name}=${serializeChord(chordFor(command))}"
        }

    companion object {
        fun defaults(): ShortcutRegistry =
            create(
                mapOf(
                    StudyShortcutCommand.REVEAL_ANSWER to DesktopKeyChord(DesktopShortcutKey.SPACE),
                    StudyShortcutCommand.RATE_AGAIN to DesktopKeyChord(DesktopShortcutKey.ONE),
                    StudyShortcutCommand.RATE_HARD to DesktopKeyChord(DesktopShortcutKey.TWO),
                    StudyShortcutCommand.RATE_GOOD to DesktopKeyChord(DesktopShortcutKey.THREE),
                    StudyShortcutCommand.RATE_EASY to DesktopKeyChord(DesktopShortcutKey.FOUR),
                    StudyShortcutCommand.REPLAY_PRIMARY_AUDIO to DesktopKeyChord(DesktopShortcutKey.R),
                    StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP to
                        DesktopKeyChord(DesktopShortcutKey.L),
                    StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP to
                        DesktopKeyChord(DesktopShortcutKey.L, shiftPressed = true),
                    StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO to
                        DesktopKeyChord(DesktopShortcutKey.V),
                    StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO to
                        DesktopKeyChord(DesktopShortcutKey.V, shiftPressed = true),
                    StudyShortcutCommand.UNDO to DesktopKeyChord(DesktopShortcutKey.Z, controlPressed = true),
                    StudyShortcutCommand.PAUSE to DesktopKeyChord(DesktopShortcutKey.ESCAPE)
                )
            )

        fun deserialize(value: String): ShortcutRegistry {
            val entries = value.split(',').map { entry ->
                val parts = entry.split('=', limit = 2)
                require(parts.size == 2) { "Shortcut binding must use command=chord." }
                StudyShortcutCommand.valueOf(parts[0]) to deserializeChord(parts[1])
            }
            require(entries.map { it.first }.distinct().size == entries.size) {
                "Serialized Study shortcuts must not repeat commands."
            }
            val loaded = entries.toMap()
            val upgraded = loaded.toMutableMap()
            val defaultRegistry = defaults()
            StudyShortcutCommand.entries.filterNot(upgraded::containsKey).forEach { command ->
                val preferred = defaultRegistry.chordFor(command)
                upgraded[command] =
                    preferred.takeUnless { it in upgraded.values }
                        ?: deterministicFallback(upgraded.values.toSet())
            }
            return create(upgraded)
        }

        private fun deterministicFallback(occupied: Set<DesktopKeyChord>): DesktopKeyChord =
            DesktopShortcutKey.entries
                .filterNot { it == DesktopShortcutKey.ESCAPE }
                .flatMap { key ->
                    listOf(
                        DesktopKeyChord(key, shiftPressed = true),
                        DesktopKeyChord(key, controlPressed = true),
                        DesktopKeyChord(key, altPressed = true)
                    )
                }
                .first { it !in occupied }

        private fun create(bindings: Map<StudyShortcutCommand, DesktopKeyChord>): ShortcutRegistry =
            ShortcutRegistry(bindings.toMap())

        private fun serializeChord(chord: DesktopKeyChord): String =
            buildList {
                if (chord.controlPressed) add("CTRL")
                if (chord.altPressed) add("ALT")
                if (chord.shiftPressed) add("SHIFT")
                add(chord.key.name)
            }.joinToString("+")

        private fun deserializeChord(value: String): DesktopKeyChord {
            val tokens = value.split('+')
            require(tokens.isNotEmpty() && tokens.none(String::isBlank)) {
                "Shortcut chord must not be empty."
            }
            val key = DesktopShortcutKey.valueOf(tokens.last())
            val modifiers = tokens.dropLast(1)
            require(modifiers.distinct().size == modifiers.size) {
                "Shortcut chord modifiers must not repeat."
            }
            require(modifiers.all { it in setOf("CTRL", "ALT", "SHIFT") }) {
                "Shortcut chord contains an unsupported modifier."
            }
            return DesktopKeyChord(
                key = key,
                controlPressed = "CTRL" in modifiers,
                altPressed = "ALT" in modifiers,
                shiftPressed = "SHIFT" in modifiers
            )
        }
    }
}
