package vn.loi.learning.desktop.runtime

import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

data class DesktopWindowPlacement(
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT,
    val x: Int? = null,
    val y: Int? = null,
    val maximized: Boolean = false
) {
    init {
        require(width in MIN_WIDTH..MAX_WIDTH) { "Window width is outside safe bounds." }
        require(height in MIN_HEIGHT..MAX_HEIGHT) { "Window height is outside safe bounds." }
        require((x == null) == (y == null)) {
            "Window position must provide both coordinates or neither."
        }
        x?.let { require(it in MIN_POSITION..MAX_POSITION) { "Window x is unsafe." } }
        y?.let { require(it in MIN_POSITION..MAX_POSITION) { "Window y is unsafe." } }
    }

    companion object {
        const val FILE_NAME = "window-state.properties"
        const val SCHEMA_VERSION = 1
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 800
        const val MIN_WIDTH = 800
        const val MIN_HEIGHT = 600
        const val MAX_WIDTH = 7680
        const val MAX_HEIGHT = 4320
        const val MIN_POSITION = -32_768
        const val MAX_POSITION = 32_768
    }
}

class InvalidDesktopWindowPlacementException(
    val filePath: Path,
    cause: Throwable
) : IllegalStateException(
    "Invalid Desktop window state: ${filePath.toAbsolutePath().normalize()}",
    cause
)

class DesktopWindowPlacementSession private constructor(
    val initial: DesktopWindowPlacement,
    val invalidStatePreserved: Boolean,
    private val filePath: Path,
    private val saveEnabled: Boolean
) {
    fun save(placement: DesktopWindowPlacement): Boolean {
        if (!saveEnabled) {
            return false
        }

        DesktopWindowPlacementStore.save(filePath, placement)
        return true
    }

    companion object {
        fun open(filePath: Path): DesktopWindowPlacementSession =
            try {
                val loaded = DesktopWindowPlacementStore.load(filePath)
                DesktopWindowPlacementSession(
                    initial = loaded ?: DesktopWindowPlacement(),
                    invalidStatePreserved = false,
                    filePath = filePath,
                    saveEnabled = true
                )
            } catch (failure: InvalidDesktopWindowPlacementException) {
                DesktopWindowPlacementSession(
                    initial = DesktopWindowPlacement(),
                    invalidStatePreserved = true,
                    filePath = filePath,
                    saveEnabled = false
                )
            }
    }
}

object DesktopWindowPlacementStore {
    fun load(filePath: Path): DesktopWindowPlacement? {
        if (Files.notExists(filePath)) {
            return null
        }

        val content = Files.readString(filePath, StandardCharsets.UTF_8)
        if (content.isBlank()) {
            throw invalid(filePath, IllegalArgumentException("Window state is blank."))
        }

        return try {
            val properties = Properties().apply { load(StringReader(content)) }
            val schema = properties.required("schema.version").toInt()
            require(schema == DesktopWindowPlacement.SCHEMA_VERSION) {
                "Unsupported window-state schema: $schema"
            }
            val positionSet = properties.required("position.set").toBooleanStrict()

            DesktopWindowPlacement(
                width = properties.required("width").toInt(),
                height = properties.required("height").toInt(),
                x = if (positionSet) properties.required("x").toInt() else null,
                y = if (positionSet) properties.required("y").toInt() else null,
                maximized = properties.required("maximized").toBooleanStrict()
            )
        } catch (failure: RuntimeException) {
            throw invalid(filePath, failure)
        }
    }

    fun save(filePath: Path, placement: DesktopWindowPlacement) {
        val target = filePath.toAbsolutePath().normalize()
        val parent = requireNotNull(target.parent) { "Window-state path requires a parent." }
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, "window-state.", ".tmp")

        try {
            Files.writeString(
                temporary,
                serialize(placement),
                StandardCharsets.UTF_8
            )
            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (unsupported: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun serialize(placement: DesktopWindowPlacement): String =
        buildString {
            appendLine("schema.version=${DesktopWindowPlacement.SCHEMA_VERSION}")
            appendLine("width=${placement.width}")
            appendLine("height=${placement.height}")
            appendLine("position.set=${placement.x != null}")
            appendLine("x=${placement.x ?: 0}")
            appendLine("y=${placement.y ?: 0}")
            appendLine("maximized=${placement.maximized}")
        }

    private fun Properties.required(key: String): String =
        requireNotNull(getProperty(key)?.trim()?.takeIf(String::isNotEmpty)) {
            "Missing window-state property: $key"
        }

    private fun invalid(
        filePath: Path,
        cause: Throwable
    ): InvalidDesktopWindowPlacementException =
        InvalidDesktopWindowPlacementException(filePath, cause)
}
