package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyIdentityPresentationTest {
    @Test
    fun `wide and compact identity stay inline while only ultra narrow stacks`() {
        val wide = resolveStudyIdentityPresentation(680)
        val compact = resolveStudyIdentityPresentation(400)
        val ultraNarrow = resolveStudyIdentityPresentation(220)

        assertEquals(StudyIdentityComposition.STANDARD_INLINE, wide.composition)
        assertEquals(StudyIdentityComposition.COMPACT_INLINE, compact.composition)
        assertEquals(StudyIdentityComposition.STACKED, ultraNarrow.composition)
        assertTrue(compact.horizontalGapDp < wide.horizontalGapDp)
        assertTrue(compact.speakerButtonSizeDp < wide.speakerButtonSizeDp)
        assertTrue(compact.ipaFontSizeSp < wide.ipaFontSizeSp)
        assertTrue(compact.posHorizontalPaddingDp < wide.posHorizontalPaddingDp)
    }

    @Test
    fun `compact inline identity is shorter than standard and stacked geometries`() {
        val standard =
            measuredStudyIdentityHeightDp(
                resolveStudyIdentityPresentation(680),
                wordLineHeightDp = 58,
                inlineMetadataHeightDp = 40,
                stackedMetadataHeightsDp = listOf(40, 24, 36)
            )
        val compact =
            measuredStudyIdentityHeightDp(
                resolveStudyIdentityPresentation(400),
                wordLineHeightDp = 44,
                inlineMetadataHeightDp = 34,
                stackedMetadataHeightsDp = listOf(34, 20, 28)
            )
        val stacked =
            measuredStudyIdentityHeightDp(
                resolveStudyIdentityPresentation(220),
                wordLineHeightDp = 44,
                inlineMetadataHeightDp = 34,
                stackedMetadataHeightsDp = listOf(34, 20, 28)
            )

        assertEquals(110, standard)
        assertEquals(84, compact)
        assertEquals(142, stacked)
        assertTrue(compact < standard)
        assertTrue(standard < stacked)
    }

    @Test
    fun `wide compact wide resize recomputes without stale composition`() {
        assertEquals(
            listOf(
                StudyIdentityComposition.STANDARD_INLINE,
                StudyIdentityComposition.COMPACT_INLINE,
                StudyIdentityComposition.STANDARD_INLINE
            ),
            listOf(680, 400, 680).map {
                resolveStudyIdentityPresentation(it).composition
            }
        )
    }

    @Test
    fun `recovered compact identity height returns measured budget to image`() {
        val legacyStackedIdentityHeight = 160
        val compactInlineIdentityHeight =
            measuredStudyIdentityHeightDp(
                resolveStudyIdentityPresentation(400),
                wordLineHeightDp = 44,
                inlineMetadataHeightDp = 34,
                stackedMetadataHeightsDp = listOf(34, 20, 28)
            )
        val legacyGeometry = answerGeometry(legacyStackedIdentityHeight)
        val compactGeometry = answerGeometry(compactInlineIdentityHeight)

        assertEquals(76, legacyStackedIdentityHeight - compactInlineIdentityHeight)
        assertEquals(76, compactGeometry.imageHeight - legacyGeometry.imageHeight)
        assertTrue(compactGeometry.requiredExampleBottom <= 700)
    }

    @Test
    fun `production resolves actual card width and leaves pre answer and fit authorities untouched`() {
        val answer = studySource("FocusedAnswerSurface.kt")
        val renderer = studySource("LearningSceneRenderer.kt")
        val fit = studySource("FullAnswerFitLayout.kt")

        assertTrue(answer.containsCodeIgnoringWhitespace("resolveStudyIdentityPresentation(maxWidth.value.toInt()"))
        assertTrue(answer.contains("presentation.composition == StudyIdentityComposition.STACKED"))
        assertTrue(answer.contains("Arrangement.spacedBy(presentation.horizontalGapDp.dp)"))
        assertTrue(answer.contains("buttonSizeDp = presentation.speakerButtonSizeDp"))
        assertTrue(answer.contains("identityPresentation = presentation"))
        assertFalse(renderer.contains("StudyIdentityPresentation"))
        assertFalse(fit.contains("StudyIdentityPresentation"))
    }

    private fun answerGeometry(identityHeight: Int) =
        resolveFullAnswerFitGeometry(
            availableHeight = 700,
            verticalPadding = 6,
            sectionGap = 8,
            minimumImageHeight = 96,
            blocks =
                FullAnswerMeasuredBlocks(
                    identityHeight = identityHeight,
                    meaningHeight = 92,
                    requiredExampleHeight = 164
                )
        )

    private fun studySource(name: String): String = studySourceDirectory().resolve(name).readText()

    private fun studySourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study")
        return if (fromRoot.isDirectory) {
            fromRoot
        } else {
            File("src/main/kotlin/vn/loi/learning/desktop/ui/study")
        }
    }
}
