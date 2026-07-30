package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.DesktopShortcutKey
import vn.loi.learning.desktop.shortcut.ShortcutChangeResult
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

class DesktopRuntimeConfigurationLoaderTest {
    @Test
    fun `missing configuration returns typed defaults without creating a file`() {
        val directory = Files.createTempDirectory("desktop-config-missing-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)

            assertEquals(
                DesktopRuntimeConfiguration(),
                DesktopRuntimeConfigurationLoader.load(file)
            )
            assertFalse(Files.exists(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `loads valid typed configuration`() {
        val directory = Files.createTempDirectory("desktop-config-valid-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            Files.writeString(
                file,
                "schema.version=1\nlog.level=debug\nlog.retained.files=25\n"
            )

            assertEquals(
                DesktopRuntimeConfiguration(
                    logLevel = DesktopLogLevel.DEBUG,
                    retainedLogFiles = 25
                ),
                DesktopRuntimeConfigurationLoader.load(file)
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `missing audio delay defaults to point three five while an existing value is preserved`() {
        val directory = Files.createTempDirectory("desktop-config-audio-delay-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            Files.writeString(file, "schema.version=1\nlog.level=info\nlog.retained.files=10\n")
            assertEquals(0.35, DesktopRuntimeConfigurationLoader.load(file).audioLoopDelaySeconds)

            Files.writeString(
                file,
                "schema.version=1\nlog.level=info\nlog.retained.files=10\n" +
                    "audio.loop.delay.seconds=1.5\n"
            )
            assertEquals(1.5, DesktopRuntimeConfigurationLoader.load(file).audioLoopDelaySeconds)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `stores theme preference and restores it after restart`() {
        val directory = Files.createTempDirectory("desktop-config-theme-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            val expected =
                DesktopRuntimeConfiguration(
                    logLevel = DesktopLogLevel.DEBUG,
                    retainedLogFiles = 12,
                    theme = DesktopThemePreference.DARK
                )

            DesktopRuntimeConfigurationStore.save(file, expected)

            assertEquals(expected, DesktopRuntimeConfigurationLoader.load(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `stores locale and restores it after restart`() {
        val directory = Files.createTempDirectory("desktop-config-locale-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            val expected =
                DesktopRuntimeConfiguration(locale = DesktopLocale.VIETNAMESE)

            DesktopRuntimeConfigurationStore.save(file, expected)

            assertEquals(expected, DesktopRuntimeConfigurationLoader.load(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `legacy configuration defaults session limits and round trip preserves custom limits`() {
        val directory = Files.createTempDirectory("desktop-config-session-limits-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            Files.writeString(file, "schema.version=1\nlog.level=info\nlog.retained.files=10\n")
            val legacy = DesktopRuntimeConfigurationLoader.load(file)
            assertEquals(20, legacy.newItemsPerSession)
            assertEquals(100, legacy.reviewItemsPerSession)

            val expected = legacy.copy(newItemsPerSession = 30, reviewItemsPerSession = 200)
            DesktopRuntimeConfigurationStore.save(file, expected)
            assertEquals(expected, DesktopRuntimeConfigurationLoader.load(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `remembered custom review target survives presets and application restart`() {
        val directory = Files.createTempDirectory("desktop-config-custom-review-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            val custom = DesktopRuntimeConfiguration(
                reviewItemsPerSession = 5,
                customReviewItemsPerSession = 5
            )
            DesktopRuntimeConfigurationStore.save(file, custom)
            assertEquals(custom, DesktopRuntimeConfigurationLoader.load(file))

            val preset = custom.copy(reviewItemsPerSession = 20)
            DesktopRuntimeConfigurationStore.save(file, preset)
            val restarted = DesktopRuntimeConfigurationLoader.load(file)
            assertEquals(20, restarted.reviewItemsPerSession)
            assertEquals(5, restarted.customReviewItemsPerSession)
            assertEquals(5, restarted.copy(reviewItemsPerSession = restarted.customReviewItemsPerSession)
                .reviewItemsPerSession)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `custom review target accepts boundaries and rejects values outside zero through five hundred`() {
        assertEquals(0, DesktopRuntimeConfiguration(
            newItemsPerSession = 1,
            customReviewItemsPerSession = 0
        ).customReviewItemsPerSession)
        assertEquals(500, DesktopRuntimeConfiguration(
            customReviewItemsPerSession = 500
        ).customReviewItemsPerSession)
        assertFailsWith<IllegalArgumentException> {
            DesktopRuntimeConfiguration(customReviewItemsPerSession = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopRuntimeConfiguration(customReviewItemsPerSession = 501)
        }
    }

    @Test
    fun `legacy configuration defaults study typography and round trip preserves custom sizes`() {
        val directory = Files.createTempDirectory("desktop-config-study-typography-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            Files.writeString(file, "schema.version=1\nlog.level=info\nlog.retained.files=10\n")

            val legacy = DesktopRuntimeConfigurationLoader.load(file)
            assertEquals(StudyTypographyPreferences(), legacy.studyTypography)

            val expected = legacy.copy(
                studyTypography = StudyTypographyPreferences(
                    exampleEnglishFontSize = 26,
                    exampleVietnameseFontSize = 22
                )
            )
            DesktopRuntimeConfigurationStore.save(file, expected)

            assertEquals(expected, DesktopRuntimeConfigurationLoader.load(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `legacy configuration defaults shortcuts and round trip preserves registry`() {
        val directory = Files.createTempDirectory("desktop-config-study-shortcuts-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            Files.writeString(file, "schema.version=1\nlog.level=info\nlog.retained.files=10\n")
            assertEquals(ShortcutRegistry.defaults(), DesktopRuntimeConfigurationLoader.load(file).studyShortcuts)

            val changed = ShortcutRegistry.defaults().requestChange(
                StudyShortcutCommand.REVEAL_ANSWER,
                DesktopKeyChord(DesktopShortcutKey.ENTER)
            ) as ShortcutChangeResult.Changed
            val expected = DesktopRuntimeConfiguration(studyShortcuts = changed.registry)
            DesktopRuntimeConfigurationStore.save(file, expected)

            assertEquals(expected, DesktopRuntimeConfigurationLoader.load(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `legacy configuration defaults presentation preferences and round trip preserves every mode`() {
        val directory = Files.createTempDirectory("desktop-config-study-presentation-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            Files.writeString(file, "schema.version=1\nlog.level=info\nlog.retained.files=10\n")
            assertEquals(
                StudyPresentationPreferences(),
                DesktopRuntimeConfigurationLoader.load(file).studyPresentation
            )

            StudyPresentationControlMode.entries.forEachIndexed { index, mode ->
                val expected = DesktopRuntimeConfiguration(
                    studyPresentation = StudyPresentationPreferences(
                        controlMode = mode,
                        showEnglish = index % 2 == 0,
                        showVietnamese = index % 2 != 0,
                        autoplayEnglish = index != 1,
                        autoplayVietnamese = index == 2
                    )
                )
                DesktopRuntimeConfigurationStore.save(file, expected)
                assertEquals(expected, DesktopRuntimeConfigurationLoader.load(file))
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects malformed presentation mode and boolean without changing bytes`() {
        val directory = Files.createTempDirectory("desktop-config-invalid-study-presentation-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            fun reject(property: String, value: String) {
                val bytes = (
                    "schema.version=1\nlog.level=info\nlog.retained.files=10\n" +
                        "$property=$value\n"
                    ).toByteArray()
                Files.write(file, bytes)
                assertEquals(
                    property,
                    assertFailsWith<InvalidDesktopConfigurationException> {
                        DesktopRuntimeConfigurationLoader.load(file)
                    }.propertyName
                )
                assertContentEquals(bytes, Files.readAllBytes(file))
            }

            reject("study.presentation.mode", "automatic")
            reject("study.presentation.show.english", "yes")
            reject("study.presentation.autoplay.vietnamese", "1")
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `invalid persisted shortcuts fall back to defaults`() {
        val directory = Files.createTempDirectory("desktop-config-invalid-study-shortcuts-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            Files.writeString(
                file,
                "schema.version=1\nlog.level=info\nlog.retained.files=10\n" +
                    "study.shortcuts=REVEAL_ANSWER=UNKNOWN\n"
            )

            assertEquals(
                ShortcutRegistry.defaults(),
                DesktopRuntimeConfigurationLoader.load(file).studyShortcuts
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects malformed and out of range study typography`() {
        val directory = Files.createTempDirectory("desktop-config-invalid-study-typography-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            fun write(english: String, vietnamese: String) {
                Files.writeString(
                    file,
                    "schema.version=1\nlog.level=info\nlog.retained.files=10\n" +
                        "study.typography.example.english.font.size=$english\n" +
                        "study.typography.example.vietnamese.font.size=$vietnamese\n"
                )
            }

            write("large", "16")
            assertEquals(
                "study.typography.example.english.font.size",
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }.propertyName
            )
            write("31", "16")
            assertEquals(
                "study.typography.example.english.font.size",
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }.propertyName
            )
            write("20", "13")
            assertEquals(
                "study.typography.example.vietnamese.font.size",
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }.propertyName
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects malformed out of range and zero zero session limits`() {
        val directory = Files.createTempDirectory("desktop-config-invalid-session-limits-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            fun write(newLimit: String, reviewLimit: String) {
                Files.writeString(
                    file,
                    "schema.version=1\nlog.level=info\nlog.retained.files=10\n" +
                        "study.new.items.per.session=$newLimit\n" +
                        "study.review.items.per.session=$reviewLimit\n"
                )
            }
            write("invalid", "100")
            assertEquals(
                "study.new.items.per.session",
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }.propertyName
            )
            write("101", "100")
            assertFailsWith<InvalidDesktopConfigurationException> {
                DesktopRuntimeConfigurationLoader.load(file)
            }
            write("0", "0")
            assertFailsWith<InvalidDesktopConfigurationException> {
                DesktopRuntimeConfigurationLoader.load(file)
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects invalid theme without changing configuration bytes`() {
        val directory = Files.createTempDirectory("desktop-config-theme-invalid-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            val bytes =
                "schema.version=1\nlog.level=info\nlog.retained.files=10\ntheme=unknown\n"
                    .toByteArray()
            Files.write(file, bytes)

            val failure =
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }

            assertEquals("theme", failure.propertyName)
            assertContentEquals(bytes, Files.readAllBytes(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `corrupt configuration remains unchanged across loader recreation`() {
        val directory = Files.createTempDirectory("desktop-config-corrupt-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            val bytes =
                "schema.version=1\nlog.level=private-value\nlog.retained.files=10\n"
                    .toByteArray()
            Files.write(file, bytes)

            repeat(2) {
                val failure =
                    assertFailsWith<InvalidDesktopConfigurationException> {
                        DesktopRuntimeConfigurationLoader.load(file)
                    }

                assertEquals(file, failure.filePath)
                assertEquals("log.level", failure.propertyName)
                assertFalse(failure.message.orEmpty().contains("private-value"))
            }

            assertContentEquals(bytes, Files.readAllBytes(file))
            assertEquals(
                listOf(DesktopRuntimeConfiguration.FILE_NAME),
                Files.list(directory).use { files ->
                    files.map { it.fileName.toString() }.toList()
                }
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects unsupported schema and out of range retention`() {
        val directory = Files.createTempDirectory("desktop-config-validation-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)

            Files.writeString(
                file,
                "schema.version=2\nlog.level=info\nlog.retained.files=10\n"
            )
            assertEquals(
                "schema.version",
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }.propertyName
            )

            Files.writeString(
                file,
                "schema.version=1\nlog.level=info\nlog.retained.files=0\n"
            )
            assertEquals(
                "log.retained.files",
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }.propertyName
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
