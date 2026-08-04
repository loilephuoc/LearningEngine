package vn.loi.learning.android.platform

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import kotlin.io.path.extension
import kotlin.test.assertTrue

class AndroidJvmApiCompatibilityTest {
    @Test
    fun `android packaged production source excludes unsupported Files text conveniences`() {
        val roots = listOf(
            Path.of("..", "src", "main", "kotlin"),
            Path.of("src", "main", "kotlin")
        )
        val forbidden = listOf("Files" + ".readString(", "Files" + ".writeString(")
        val violations = roots.flatMap { root ->
            Files.walk(root).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                    .flatMap { source ->
                        val text = Files.newBufferedReader(source).use { it.readText() }
                        forbidden.filter(text::contains).map { api -> "$source: $api" }.stream()
                    }
                    .toList()
            }
        }
        assertTrue(violations.isEmpty(), "Unsupported Android runtime APIs:\n${violations.joinToString("\n")}")
    }
}
