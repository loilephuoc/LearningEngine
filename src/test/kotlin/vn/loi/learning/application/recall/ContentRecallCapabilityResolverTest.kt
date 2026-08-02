package vn.loi.learning.application.recall

import kotlin.test.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.recall.*

class ContentRecallCapabilityResolverTest {
    @Test fun `source and target support typing in both meaningful directions`() {
        val result = resolve(text = ContentText("word", "từ"))
        assertTrue(result.supports(RecallMode.TYPING))
        assertEquals(
            listOf(RecallDirection.SOURCE_TO_TARGET, RecallDirection.TARGET_TO_SOURCE).sortedBy { it.wireId },
            result.supportedDirectionsFor(RecallMode.TYPING)
        )
    }

    @Test fun `Content invariant rejects missing canonical answer`() {
        assertFailsWith<IllegalArgumentException> { ContentText("  ", "target") }
    }

    @Test fun `source and target support reverse translation`() {
        assertTrue(resolve(text = ContentText("word", "từ")).supports(RecallMode.REVERSE_TRANSLATION))
    }

    @Test fun `missing target disables reverse translation with typed reason`() {
        val result = resolve(text = ContentText("word"))
        assertFalse(result.supports(RecallMode.REVERSE_TRANSLATION))
        assertContains(result.unavailableReasonsFor(RecallMode.REVERSE_TRANSLATION), RecallUnavailableReason.MISSING_TARGET_TEXT)
    }

    @Test fun `image and canonical answer support image recall`() {
        assertTrue(resolve(media = ContentMedia(image = "asset:image")).supports(RecallMode.IMAGE_RECALL))
    }

    @Test fun `missing image disables image recall`() {
        assertContains(
            resolve().unavailableReasonsFor(RecallMode.IMAGE_RECALL), RecallUnavailableReason.MISSING_IMAGE
        )
    }

    @Test fun `word audio and answer support listening`() {
        assertTrue(resolve(media = ContentMedia(primaryAudio = "asset:audio")).supports(RecallMode.LISTENING))
    }

    @Test fun `missing audio disables listening`() {
        assertContains(
            resolve().unavailableReasonsFor(RecallMode.LISTENING), RecallUnavailableReason.MISSING_WORD_AUDIO
        )
    }

    @Test fun `dictation follows word audio and canonical text contract`() {
        assertTrue(resolve(media = ContentMedia(primaryAudio = "asset:audio")).supports(RecallMode.DICTATION))
        assertFalse(resolve().supports(RecallMode.DICTATION))
    }

    @Test fun `multiple choice needs no stored distractors`() {
        val result = resolve(text = ContentText("word", "từ"))
        assertTrue(result.supports(RecallMode.MULTIPLE_CHOICE))
        assertContains(result.sourceCapabilities.available, RecallCapability.SOURCE_TEXT)
    }

    @Test fun `multiple choice without prompt side is unavailable`() {
        val result = resolve(text = ContentText("word"))
        assertFalse(result.supports(RecallMode.MULTIPLE_CHOICE))
        assertContains(result.unavailableReasonsFor(RecallMode.MULTIPLE_CHOICE), RecallUnavailableReason.MISSING_TARGET_TEXT)
    }

    @Test fun `unique token boundary creates safe example completion target`() {
        val result = resolve(text = ContentText("word", exampleText = "A word appears."))
        assertTrue(result.supports(RecallMode.EXAMPLE_COMPLETION))
        assertEquals("word", "A word appears.".substring(
            requireNotNull(result.completionTarget).startInclusive, result.completionTarget.endExclusive
        ))
    }

    @Test fun `ambiguous example target is unavailable without replacement`() {
        val result = resolve(text = ContentText("word", exampleText = "word follows word"))
        assertFalse(result.supports(RecallMode.EXAMPLE_COMPLETION))
        assertContains(
            result.unavailableReasonsFor(RecallMode.EXAMPLE_COMPLETION),
            RecallUnavailableReason.UNSAFE_EXAMPLE_TARGET_SPAN
        )
    }

    @Test fun `blank optional facts are missing`() {
        val result = resolve(text = ContentText("word", "  ", "  ", "  ", "  "))
        assertFalse(result.supports(RecallMode.TYPING))
        assertFalse(result.supportsAssistance(RecallAssistance.PRONUNCIATION_HINT_USED))
        assertFalse(RecallContextualCapability.HAS_EXAMPLE_SOURCE in result.contextualCapabilities)
    }

    @Test fun `malformed media reference gives typed reason`() {
        val result = resolve(media = ContentMedia(primaryAudio = "audio\nref"))
        assertContains(
            result.unavailableReasonsFor(RecallMode.LISTENING), RecallUnavailableReason.INVALID_MEDIA_REFERENCE
        )
    }

    @Test fun `pronunciation alone does not create a recall mode`() {
        val result = resolve(text = ContentText("word", pronunciation = "/wɜːd/"))
        assertTrue(RecallLexicalCapability.HAS_PRONUNCIATION in result.lexicalCapabilities)
        assertTrue(result.capabilitySet.isEmpty)
    }

    @Test fun `assistance follows example pronunciation and answer facts`() {
        val result = resolve(
            text = ContentText("word", pronunciation = "/wɜːd/", exampleText = "A word."),
            media = ContentMedia(exampleAudio = "asset:example")
        )
        listOf(
            RecallAssistance.EXAMPLE_VIEWED, RecallAssistance.EXAMPLE_AUDIO_PLAYED,
            RecallAssistance.PRONUNCIATION_HINT_USED, RecallAssistance.ANSWER_REVEALED,
            RecallAssistance.LETTER_HINT_USED
        ).forEach { assertTrue(result.supportsAssistance(it)) }
    }

    @Test fun `same Content produces same projection`() {
        val content = content(ContentText("word", "từ"), ContentMedia(primaryAudio = "audio"))
        assertEquals(ContentRecallCapabilityResolver.resolve(content), ContentRecallCapabilityResolver.resolve(content))
    }

    @Test fun `minimal Content produces valid empty projection with reasons for every mode`() {
        val result = resolve()
        assertTrue(result.capabilitySet.isEmpty)
        RecallMode.entries.forEach { assertTrue(result.unavailableReasonsFor(it).isNotEmpty()) }
    }

    private fun resolve(
        text: ContentText = ContentText("word"),
        media: ContentMedia = ContentMedia()
    ) = ContentRecallCapabilityResolver.resolve(content(text, media))

    private fun content(text: ContentText, media: ContentMedia) =
        Content(ContentId("content"), ContentType.WORD, text, media)
}
