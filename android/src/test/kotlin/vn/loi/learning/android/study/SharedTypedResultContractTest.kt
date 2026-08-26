package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SharedTypedResultContractTest {
    private val typed = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/TypedAnswerStages.kt")
    )
    private val image = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/ImageRecallStage.kt")
    )
    private val screen = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
    )

    @Test
    fun `all three typed modes route compact success through one canonical component`() {
        val mapping = typed.substringAfter("internal fun typedResultPresentation(")
            .substringBefore("internal fun TypedCompactSuccess(")
        listOf("Typing", "Listening", "ImageRecall").forEach {
            assertTrue(mapping.contains("is AndroidStudyState.$it"), it)
        }
        assertEquals(3, Regex("TypedCompactSuccess\\(").findAll(typed).count())
        assertEquals(1, Regex("TypedCompactSuccess\\(").findAll(image).count())
        assertEquals(1, Regex("internal fun TypedCompactSuccess\\(").findAll(typed).count())
    }

    @Test
    fun `shared compact success owns examples rating and render gate contract`() {
        val compact = typed.substringAfter("internal fun TypedCompactSuccess(")
            .substringBefore("internal fun TypingStudyStage(")
        assertTrue(compact.contains("StudyMedia("))
        assertTrue(compact.contains("StudyAnswerSection("))
        assertTrue(compact.contains("englishExample = null"))
        assertTrue(compact.contains("vietnameseExample = null"))
        assertTrue(compact.contains("resolveTypingRatingTransition("))
        assertTrue(compact.contains("SideEffect(onRendered)"))
    }

    @Test
    fun `typed prompts remain isolated while reveal and scroll routing stay shared`() {
        assertTrue(typed.contains("StudyListeningAudioPrompt("))
        assertTrue(image.contains("imageRecallMediaRole(imeVisible, false)"))
        assertTrue(typed.contains("text = state.prompt"))
        assertEquals(1, Regex("private fun StudyRevealAndFeedbackContent\\(").findAll(screen).count())
        assertTrue(screen.contains("resolveRevealExamplePair("))
        assertTrue(screen.contains("is AndroidStudyState.ImageRecall -> Modifier.fillMaxWidth().weight(1f)"))
        assertFalse(screen.contains("is AndroidStudyState.ImageRecall -> Modifier.fillMaxWidth().verticalScroll"))
    }

    @Test
    fun `new Typing card claims Vietnamese autoplay once through item ownership`() {
        val autoplay = screen.substringAfter(
            "LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen) {\n" +
                "        val typing = state as? AndroidStudyState.Typing"
        ).substringBefore("DisposableEffect(audioController")

        assertTrue(autoplay.contains("!typing.completed && !typing.revealed && !typing.viAutoplayMuted"))
        assertTrue(autoplay.contains("audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.MEANING)"))
        assertEquals(1, Regex("restartAudio\\(AudioRole\\.MEANING").findAll(autoplay).count())
        assertFalse(autoplay.contains("AudioRole.EXPECTED_ANSWER"))
    }

    @Test
    fun `Adaptive Typing wrong reveal routes only through approved shared reveal`() {
        val route = screen.substringAfter("val feedbackContent: @Composable () -> Unit")
            .substringBefore("if (state is AndroidStudyState.Typing)")
        val reveal = screen.substringAfter("private fun StudyRevealAndFeedbackContent(")

        assertTrue(route.contains("StudyRevealAndFeedbackContent("))
        assertTrue(
            Regex("is AndroidStudyState\\.Typing,\\s*is AndroidStudyState\\.Listening,\\s*" +
                "is AndroidStudyState\\.ImageRecall -> false").containsMatchIn(route)
        )
        assertTrue(route.contains("TypingDifferenceComparison("))
        assertTrue(reveal.contains("englishExample = revealExamplePair.english"))
        assertTrue(reveal.contains("vietnameseExample = revealExamplePair.vietnamese"))
        assertTrue(reveal.contains("AudioRole.EXAMPLE_ENGLISH"))
        assertTrue(reveal.contains("AudioRole.EXAMPLE_VIETNAMESE"))
    }

    @Test
    fun `Image Recall legacy combined example is split before shared surfaces`() {
        val pair = resolveRevealExamplePair(
            englishExample = "Air pollution causes health problems.\nÔ nhiễm không khí gây ra các vấn đề sức khỏe.",
            vietnameseExample = ""
        )
        val reveal = screen.substringAfter("private fun StudyRevealAndFeedbackContent(")

        assertEquals("Air pollution causes health problems.", pair.english)
        assertEquals("Ô nhiễm không khí gây ra các vấn đề sức khỏe.", pair.vietnamese)
        assertTrue(reveal.contains("forcedTypingReveal || listeningWrongReveal || imageRecallWrongReveal"))
        assertTrue(reveal.contains("englishExample = revealExamplePair.english"))
        assertTrue(reveal.contains("vietnameseExample = revealExamplePair.vietnamese"))
        assertTrue(reveal.contains("allowStandaloneVietnameseExample = forcedTypedReveal"))
    }

    @Test
    fun `reveal example pair preserves separated values and handles legacy and English only`() {
        val separated = resolveRevealExamplePair(
            englishExample = "I'll make some hot chocolate.",
            vietnameseExample = "Tôi sẽ pha một ít sô cô la nóng."
        )
        assertEquals("I'll make some hot chocolate.", separated.english)
        assertEquals("Tôi sẽ pha một ít sô cô la nóng.", separated.vietnamese)

        val legacyMerged = resolveRevealExamplePair(
            englishExample = "Air pollution causes health problems.\nÔ nhiễm không khí gây ra các vấn đề sức khỏe.",
            vietnameseExample = ""
        )
        assertEquals("Air pollution causes health problems.", legacyMerged.english)
        assertEquals("Ô nhiễm không khí gây ra các vấn đề sức khỏe.", legacyMerged.vietnamese)

        val englishOnly = resolveRevealExamplePair(
            englishExample = "Air pollution causes health problems.",
            vietnameseExample = ""
        )
        assertEquals("Air pollution causes health problems.", englishOnly.english)
        assertEquals(null, englishOnly.vietnamese)
    }
}
