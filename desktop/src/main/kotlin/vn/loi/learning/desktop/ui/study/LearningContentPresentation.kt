package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import vn.loi.learning.application.learningcontent.LearningAssetKind
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LocalLearningAssetReference
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.domain.content.model.ContentTextFormat

data class LearningContentPresentation(
    val sections: List<PresentedLearningSection>
)

enum class LearningSectionKind { QUESTION, ANSWER, EXAMPLE }

data class PresentedLearningSection(
    val kind: LearningSectionKind,
    val blocks: List<PresentedLearningBlock>
)

sealed interface PresentedLearningBlock {
    data class Text(val document: SafeMarkdownDocument) : PresentedLearningBlock
    data class Image(val path: Path, val description: String) : PresentedLearningBlock
    data class Audio(
        val path: Path,
        val description: String,
        val roleLabel: String
    ) : PresentedLearningBlock
    data class Unavailable(val message: String) : PresentedLearningBlock
}

data class LearningContentRendererStrings(
    val answerUnavailable: String,
    val imageUnavailable: String,
    val audioUnavailable: String,
    val imageDescription: String,
    val audioDescription: String,
    val playAudio: String,
    val stopAudio: String,
    val answerLabel: String,
    val exampleLabel: String,
    val promptAudioLabel: String = "Pronunciation",
    val answerAudioLabel: String = "Answer audio",
    val exampleAudioLabel: String = "Example audio",
    val startingAudio: String = "Starting audio",
    val playingAudio: String = "Playing",
    val replayAudio: String = "Replay audio",
    val audioPlaybackFailed: String = "Audio could not be played",
    val promptSceneInstruction: String = "Bring the answer to mind",
    val listeningSceneInstruction: String = "Listen carefully",
    val imageSceneInstruction: String = "Use the image to recall the answer",
    val meaningSceneLabel: String = "Meaning",
    val exampleSceneLabel: String = "Examples",
    val typingSceneInstruction: String = "Type the answer before revealing it",
    val defaultExperience: String = "Default",
    val typingExperience: String = "Typing Recall",
    val typingInputLabel: String = "Your answer",
    val typingSubmit: String = "Check answer",
    val typingCorrect: String = "Correct",
    val typingIncorrect: String = "Not an exact match",
    val typingEmpty: String = "No answer entered"
    ,
    val flowProgress: String = "Learning flow",
    val flowStageTemplate: (Int, Int, String) -> String =
        { current, total, label -> "Stage $current of $total — $label" },
    val flowImageRecall: String = "Image Recall",
    val flowListeningRecall: String = "Listening Recall",
    val flowPromptRecall: String = "Prompt Recall",
    val flowTypingRecall: String = "Typing Recall",
    val nextFlowStage: String = "Next Stage",
    val flowContinueDescription: String = "Continue to the next learning stage",
    val flowPreparingAnswer: String = "Preparing Answer",
    val flowAnswerReady: String = "Answer Ready",
    val flowRetryReveal: String = "Retry Reveal"
)

class LearningContentPresenter(
    private val mediaStorage: ContentMediaStorage,
    private val strings: LearningContentRendererStrings
) {
    fun present(
        content: LearningContent?,
        workspaceState: ReviewWorkspaceState
    ): LearningContentPresentation {
        if (content == null) return LearningContentPresentation(emptyList())

        val sections = mutableListOf(
            section(LearningSectionKind.QUESTION, content.question.blocks)
        )
        if (workspaceState is ReviewWorkspaceState.AnswerRevealed) {
            sections += section(LearningSectionKind.ANSWER, content.answer.blocks)
            content.example?.let {
                sections += section(LearningSectionKind.EXAMPLE, it.blocks)
            }
        }
        return LearningContentPresentation(sections)
    }

    private fun section(
        kind: LearningSectionKind,
        blocks: List<LearningContentBlock>
    ): PresentedLearningSection {
        var audioOrdinal = 0
        return PresentedLearningSection(
            kind,
            blocks.map { block ->
                if (block is LearningContentBlock.Audio) audioOrdinal++
                block(block, kind, audioOrdinal)
            }
        )
    }

    private fun block(
        block: LearningContentBlock,
        section: LearningSectionKind,
        audioOrdinal: Int
    ): PresentedLearningBlock =
        when (block) {
            is LearningContentBlock.Text -> PresentedLearningBlock.Text(
                when (block.format) {
                    ContentTextFormat.PLAIN_TEXT -> SafeMarkdownDocument.plain(block.value)
                    ContentTextFormat.MARKDOWN -> SafeMarkdownParser.parse(block.value)
                }
            )
            is LearningContentBlock.Image ->
                resolve(block.reference, LearningAssetKind.IMAGE, section, audioOrdinal)
            is LearningContentBlock.Audio ->
                resolve(block.reference, LearningAssetKind.AUDIO, section, audioOrdinal)
            LearningContentBlock.UnavailableAnswer ->
                PresentedLearningBlock.Unavailable(strings.answerUnavailable)
            is LearningContentBlock.UnavailableAsset -> unavailable(block.kind)
        }

    private fun resolve(
        reference: LocalLearningAssetReference,
        kind: LearningAssetKind,
        section: LearningSectionKind,
        audioOrdinal: Int
    ): PresentedLearningBlock {
        val path = mediaStorage.resolve(reference.value) ?: return unavailable(kind)
        return when (kind) {
            LearningAssetKind.IMAGE -> PresentedLearningBlock.Image(path, strings.imageDescription)
            LearningAssetKind.AUDIO -> PresentedLearningBlock.Audio(
                path,
                strings.audioDescription,
                when (section) {
                    LearningSectionKind.QUESTION -> strings.promptAudioLabel
                    LearningSectionKind.ANSWER -> strings.answerAudioLabel
                    LearningSectionKind.EXAMPLE ->
                        if (audioOrdinal <= 1) strings.exampleAudioLabel
                        else "${strings.exampleAudioLabel} $audioOrdinal"
                }
            )
        }
    }

    private fun unavailable(kind: LearningAssetKind) =
        PresentedLearningBlock.Unavailable(
            when (kind) {
                LearningAssetKind.IMAGE -> strings.imageUnavailable
                LearningAssetKind.AUDIO -> strings.audioUnavailable
            }
        )
}

data class SafeMarkdownDocument(val blocks: List<SafeMarkdownBlock>) {
    companion object {
        fun plain(value: String) = SafeMarkdownDocument(listOf(SafeMarkdownBlock.Paragraph(value)))
    }
}

sealed interface SafeMarkdownBlock {
    val text: String
    data class Paragraph(override val text: String) : SafeMarkdownBlock
    data class Heading(val level: Int, override val text: String) : SafeMarkdownBlock
    data class ListItem(val ordered: Boolean, val number: Int?, override val text: String) : SafeMarkdownBlock
    data class Code(override val text: String) : SafeMarkdownBlock
}

/** Deliberately small Markdown allowlist. HTML, links, images and executable content stay text. */
object SafeMarkdownParser {
    fun parse(source: String): SafeMarkdownDocument {
        val blocks = mutableListOf<SafeMarkdownBlock>()
        val paragraph = mutableListOf<String>()
        val code = mutableListOf<String>()
        var inCode = false

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += SafeMarkdownBlock.Paragraph(paragraph.joinToString("\n"))
                paragraph.clear()
            }
        }
        fun flushCode() {
            blocks += SafeMarkdownBlock.Code(code.joinToString("\n"))
            code.clear()
        }

        source.lines().forEach { line ->
            if (line.trimStart().startsWith("```")) {
                if (inCode) flushCode() else flushParagraph()
                inCode = !inCode
            } else if (inCode) {
                code += line
            } else {
                val heading = Regex("^(#{1,6})\\s+(.+)$").matchEntire(line)
                val ordered = Regex("^(\\d+)\\.\\s+(.+)$").matchEntire(line)
                val unordered = Regex("^[-*+]\\s+(.+)$").matchEntire(line)
                when {
                    line.isBlank() -> flushParagraph()
                    heading != null -> {
                        flushParagraph()
                        blocks += SafeMarkdownBlock.Heading(
                            heading.groupValues[1].length,
                            heading.groupValues[2]
                        )
                    }
                    ordered != null -> {
                        flushParagraph()
                        blocks += SafeMarkdownBlock.ListItem(
                            true,
                            ordered.groupValues[1].toIntOrNull(),
                            ordered.groupValues[2]
                        )
                    }
                    unordered != null -> {
                        flushParagraph()
                        blocks += SafeMarkdownBlock.ListItem(false, null, unordered.groupValues[1])
                    }
                    else -> paragraph += line
                }
            }
        }
        if (inCode) flushCode() else flushParagraph()
        return SafeMarkdownDocument(blocks.ifEmpty { listOf(SafeMarkdownBlock.Paragraph("")) })
    }
}
