package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.nio.file.Path

enum class DesktopOnboardingState { REQUIRED, COMPLETED, NOT_REQUIRED }

class DesktopOnboardingSession private constructor(
    val initial: DesktopOnboardingState,
    private val marker: Path
) {
    fun complete() {
        Files.createDirectories(marker.parent)
        if (Files.notExists(marker)) Files.writeString(marker, "onboarding.version=1\n")
    }

    companion object {
        const val FILE_NAME = "onboarding.properties"

        fun open(dataDirectory: Path, configDirectory: Path): DesktopOnboardingSession {
            val marker = configDirectory.resolve(FILE_NAME)
            val hasData = Files.exists(dataDirectory) && Files.walk(dataDirectory).use { paths ->
                paths.anyMatch(Files::isRegularFile)
            }
            val state = when {
                Files.exists(marker) -> DesktopOnboardingState.COMPLETED
                hasData -> DesktopOnboardingState.NOT_REQUIRED
                else -> DesktopOnboardingState.REQUIRED
            }
            return DesktopOnboardingSession(state, marker)
        }
    }
}
