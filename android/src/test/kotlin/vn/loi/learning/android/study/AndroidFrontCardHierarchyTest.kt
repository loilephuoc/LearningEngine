package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Path

class AndroidFrontCardHierarchyTest {

    private val studyScreenSrc by lazy {
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt"))
    }

    private val posBadgeSrc by lazy {
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/components/PartOfSpeechBadge.kt"))
    }

    @Test
    fun `front card layout places POS between image and vietnamese meaning`() {
        val unrevealedSection = studyScreenSrc
            .substringAfter("if (!revealed) {")
            .substringBefore("} else {")

        val posIndex = unrevealedSection.indexOf("PartOfSpeechBadge(pos, prominent = true)")
        val meaningIndex = unrevealedSection.indexOf("StudyAudioTextTarget(")
        val hintIndex = unrevealedSection.indexOf("IntroductionInteractionHint(")

        assertTrue(posIndex in 0..<meaningIndex, "POS badge must appear before Vietnamese meaning")
        assertTrue(meaningIndex in 0..<hintIndex, "Vietnamese meaning must appear before reveal hint")
    }

    @Test
    fun `part of speech badge supports prominent variant for front card`() {
        assertTrue(posBadgeSrc.contains("prominent: Boolean = false"))
        assertTrue(posBadgeSrc.contains("prominent -> MaterialTheme.typography.titleSmall"))
        assertTrue(posBadgeSrc.contains("PaddingValues(horizontal = 14.dp, vertical = 5.dp)"))
    }

    @Test
    fun `front card reveal hint uses de-emphasized secondary style`() {
        val unrevealedSection = studyScreenSrc
            .substringAfter("if (!revealed) {")
            .substringBefore("} else {")

        assertTrue(unrevealedSection.contains("emphasized = false"))

        val hintComposable = studyScreenSrc
            .substringAfter("private fun IntroductionInteractionHint(")
            .substringBefore("fun StudyAudioButton")

        assertTrue(hintComposable.contains("surfaceContainerHigh.copy(alpha = 0.45f)"))
        assertTrue(hintComposable.contains("color = if (emphasized) MaterialTheme.colorScheme.primaryContainer"))
    }

    @Test
    fun `front card vietnamese text retains passive interaction semantics`() {
        val unrevealedSection = studyScreenSrc
            .substringAfter("if (!revealed) {")
            .substringBefore("} else {")

        assertTrue(unrevealedSection.contains("interactionEnabled = false"))
        assertTrue(unrevealedSection.contains("interaction = StudyTextInteraction.PASSIVE"))
    }

    @Test
    fun `back card revealed section isolates part of speech badge in answer section`() {
        val answerSectionSrc = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/components/IntroductionAnswerSection.kt")
        )
        assertTrue(answerSectionSrc.contains("partOfSpeech?.let { PartOfSpeechBadge(it) }"))
        assertFalse(answerSectionSrc.contains("prominent = true"))
    }
}
