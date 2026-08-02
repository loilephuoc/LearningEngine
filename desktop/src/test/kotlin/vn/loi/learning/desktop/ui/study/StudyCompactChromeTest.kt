package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudyCompactChromeTest {
    private val traits = StudyVisualContentTraits(
        hasImage = true,
        hasPronunciation = true,
        hasPartOfSpeech = true,
        hasExamples = true
    )

    @Test
    fun `top actions and rating chrome compact by resolver height mode`() {
        val comfortable = StudyVisualLayoutResolver.resolve(800, 1080, traits)
        val compact = StudyVisualLayoutResolver.resolve(800, 800, traits)
        val minimum = StudyVisualLayoutResolver.resolve(800, 640, traits)

        assertEquals(listOf(48, 40, 36), listOf(
            comfortable.topActionHeightDp,
            compact.topActionHeightDp,
            minimum.topActionHeightDp
        ))
        assertEquals(listOf(64, 56, 52), listOf(
            comfortable.ratingButtonHeightDp,
            compact.ratingButtonHeightDp,
            minimum.ratingButtonHeightDp
        ))
        assertEquals(listOf(48, 44, 40), listOf(
            comfortable.frontRatingSegmentHeightDp,
            compact.frontRatingSegmentHeightDp,
            minimum.frontRatingSegmentHeightDp
        ))
        assertTrue(compact.ratingDockReservedHeightDp < comfortable.ratingDockReservedHeightDp)
        assertTrue(minimum.ratingDockVerticalPaddingDp < comfortable.ratingDockVerticalPaddingDp)
    }

    @Test
    fun `production chrome keeps shared buttons callbacks semantics and one center scroll`() {
        val screen = source("StudyScreen.kt")
        val button = designSource("components/base/LEButton.kt")

        assertTrue(screen.contains("visualLayout.ratingButtonHeightDp.dp"))
        assertTrue(screen.contains("visualLayout.frontRatingSegmentHeightDp.dp"))
        assertTrue(screen.contains("signaturePresentation.headerHeightDp.dp"))
        assertTrue(screen.contains("studyActionSemantics"))
        assertEquals(1, Regex("""\.verticalScroll\(""").findAll(screen).count())
        assertTrue(button.contains("compact: Boolean = false"))
        assertTrue(button.contains("shape: Shape? = null"))
        assertTrue(button.contains("minHeight = style.minimumTargetSize"))
    }

    @Test
    fun `typing recall reserves only its compact four-status dock`() {
        val legacy = StudyVisualLayoutResolver.resolve(800, 800, traits)
        val typing = StudyVisualLayoutResolver.resolve(
            800,
            800,
            traits.copy(isTypingRecall = true)
        )

        assertTrue(typing.availableAnswerHeightDp > legacy.availableAnswerHeightDp)
        assertEquals(legacy.frontRatingSegmentHeightDp, typing.frontRatingSegmentHeightDp)
        assertTrue(typing.preserveRatingReachability)
    }

    private fun source(name: String) = read(
        "desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/$name",
        "src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"
    )

    private fun designSource(path: String) = read(
        "desktop/src/main/kotlin/vn/loi/learning/desktop/ui/designsystem/$path",
        "src/main/kotlin/vn/loi/learning/desktop/ui/designsystem/$path"
    )

    private fun read(vararg candidates: String): String =
        java.nio.file.Files.readString(
            candidates.map(java.nio.file.Path::of).first(java.nio.file.Files::exists)
        )
}
