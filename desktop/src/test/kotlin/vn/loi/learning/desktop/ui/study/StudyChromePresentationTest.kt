package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

class StudyChromePresentationTest {
    @Test
    fun `actual chrome width selects text icon and minimum modes`() {
        val standard = resolveStudyChromePresentation(900)
        val compact = resolveStudyChromePresentation(600)
        val minimum = resolveStudyChromePresentation(360)

        assertEquals(StudyTopActionComposition.STANDARD_TEXT, standard.topActionComposition)
        assertEquals(StudyTopActionComposition.COMPACT_ICON, compact.topActionComposition)
        assertEquals(StudyTopActionComposition.COMPACT_ICON, minimum.topActionComposition)
        assertEquals(
            listOf(
                StudyShortcutStripComposition.STANDARD,
                StudyShortcutStripComposition.COMPACT,
                StudyShortcutStripComposition.MINIMUM
            ),
            listOf(standard, compact, minimum).map(StudyChromePresentation::shortcutStripComposition)
        )
        assertEquals(listOf(36, 32, 28), listOf(standard, compact, minimum).map {
            it.shortcutStripHeightDp
        })
        assertTrue(standard.showShortcutLabels)
        assertFalse(compact.showShortcutLabels)
        assertFalse(compact.showSessionStatusText)
        assertEquals(3, minimum.maximumShortcutItems)
    }

    @Test
    fun `width resize recomputes standard compact standard immediately`() {
        assertEquals(
            listOf(
                StudyTopActionComposition.STANDARD_TEXT,
                StudyTopActionComposition.COMPACT_ICON,
                StudyTopActionComposition.STANDARD_TEXT
            ),
            listOf(900, 600, 900).map {
                resolveStudyChromePresentation(it).topActionComposition
            }
        )
    }

    @Test
    fun `minimum stage keeps highest priority semantic shortcuts`() {
        val preAnswer =
            resolveStudyShortcutStatus(false, ShortcutRegistry.defaults())
                .items.sortedBy(StudyShortcutStatusItem::priority).take(3)
        val rating =
            resolveStudyShortcutStatus(true, ShortcutRegistry.defaults())
                .items.sortedBy(StudyShortcutStatusItem::priority).take(3)

        assertEquals(
            listOf(
                StudyShortcutCommand.REVEAL_ANSWER,
                StudyShortcutCommand.REPLAY_PRIMARY_AUDIO,
                StudyShortcutCommand.UNDO
            ),
            preAnswer.map(StudyShortcutStatusItem::command)
        )
        assertEquals(
            listOf(
                StudyShortcutCommand.RATE_AGAIN,
                StudyShortcutCommand.RATE_HARD,
                StudyShortcutCommand.RATE_GOOD
            ),
            rating.map(StudyShortcutStatusItem::command)
        )
    }

    @Test
    fun `compact fixture returns wrapped chrome height to learning content`() {
        val legacyWrappedChrome =
            measuredStudyChromeHeightDp(
                topRowHeightDp = 64,
                statisticsHeightDp = 92,
                progressHeightDp = 4,
                sectionGapDp = 8,
                statusStripHeightDp = 140
            )
        val compact = resolveStudyChromePresentation(600)
        val compactChrome =
            measuredStudyChromeHeightDp(
                topRowHeightDp = compact.topActionButtonSizeDp,
                statisticsHeightDp = 92,
                progressHeightDp = 4,
                sectionGapDp = 8,
                statusStripHeightDp = compact.shortcutStripHeightDp
            )

        assertEquals(308, legacyWrappedChrome)
        assertEquals(176, compactChrome)
        assertEquals(132, legacyWrappedChrome - compactChrome)
    }

    @Test
    fun `production uses icon actions bounded strip and complete semantics`() {
        val screen = studySource("StudyScreen.kt")
        val shortcut = studySource("StudyShortcutPresentation.kt")

        assertTrue(screen.contains("resolveStudyChromePresentation(maxWidth.value.toInt()"))
        assertTrue(screen.contains("StudyChromeIconAction("))
        assertTrue(screen.contains("icon = LEIcons.Undo"))
        assertTrue(screen.contains("icon = LEIcons.Pause"))
        assertTrue(screen.contains("tooltip = \"${'$'}{undo.visibleLabel} (${'$'}{undo.shortcutHint})\""))
        assertTrue(screen.contains("tooltip = \"${'$'}{pause.visibleLabel} (${'$'}{pause.shortcutHint})\""))
        assertTrue(screen.contains(".height(chrome.shortcutStripHeightDp.dp)"))
        assertTrue(screen.contains("maxLines = 1"))
        assertTrue(screen.contains("softWrap = false"))
        assertTrue(screen.contains("presentation.accessibleDescription"))
        assertTrue(screen.contains("StatusStrip("))
        assertTrue(shortcut.contains("val items: List<StudyShortcutStatusItem>"))
        assertFalse(shortcut.contains("val text: String"))
        assertFalse(screen.contains("presentation.text"))
    }

    @Test
    fun `bottom bar is an icon only quick action toolbar in standard and compact widths`() {
        val screen = studySource("StudyScreen.kt")

        assertTrue(screen.contains("StudyQuickActionToolbar("))
        assertTrue(screen.contains("StudyRatingQuickAction("))
        assertTrue(screen.contains("""number = "1""""))
        assertTrue(screen.contains("""number = "2""""))
        assertTrue(screen.contains("""number = "3""""))
        assertTrue(screen.contains("""number = "4""""))
        assertTrue(screen.contains("color = LEColors.danger"))
        assertTrue(screen.contains("color = LEColors.warning"))
        assertTrue(screen.contains("color = LEColors.success"))
        assertTrue(screen.contains("color = LEColors.info"))
        assertTrue(screen.contains("StudyReplayQuickAction("))
        assertTrue(screen.contains("StudyIconQuickAction("))
        assertTrue(screen.contains("StudySessionStatus(active = uiState.hasActiveSession)"))
        assertTrue(screen.contains("onClick = onAgain"))
        assertTrue(screen.contains("onClick = onReplay"))
        assertTrue(screen.contains("onClick = onUndo"))
        assertTrue(screen.contains("enabled = enabled && canUndo"))
        assertTrue(screen.contains("""tooltip = "Undo latest rating (${'$'}{item.chordText})""""))
        assertFalse(screen.contains("StudyShortcutToken("))
        assertFalse(screen.contains("item.compactLabel"))
        assertFalse(screen.contains("""text = "Ctrl+Z""""))
        assertFalse(screen.contains("""text = "Again""""))
        assertFalse(screen.contains("""text = "Hard""""))
        assertFalse(screen.contains("""text = "Good""""))
        assertFalse(screen.contains("""text = "Easy""""))
    }

    private fun studySource(name: String): String = studySourceDirectory().resolve(name).readText()

    private fun studySourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study")
        return if (fromRoot.isDirectory) fromRoot
        else File("src/main/kotlin/vn/loi/learning/desktop/ui/study")
    }
}
