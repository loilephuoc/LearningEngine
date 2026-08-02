package vn.loi.learning.application.recall

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*

class RecallPortabilityTest {
    @Test fun `documented typing contract example compiles validates and round trips`() {
        val example = plan()
        assertEquals(RecallPlanValidationResult.Valid, RecallContractValidator.validatePlan(example))
        assertEquals(example, (RecallPlanWireCodec.decode(RecallPlanWireCodec.encode(example)) as RecallPlanDecodeResult.Success).plan)
    }

    @Test fun `same input and seed has identical serialized plan`() {
        assertEquals(RecallPlanWireCodec.encode(plan()), RecallPlanWireCodec.encode(plan()))
    }

    @Test fun `wire round trip preserves plan semantics`() {
        val original = plan(seed = 91, generatedAt = Moment(123))
        val decoded = RecallPlanWireCodec.decode(RecallPlanWireCodec.encode(original)) as RecallPlanDecodeResult.Success
        assertEquals(original, decoded.plan)
    }

    @Test fun `unknown contract version is typed unsupported result`() {
        val wire = RecallPlanWireCodec.encode(plan()).replaceFirst("\"version\":1", "\"version\":99")
        assertEquals(RecallPlanDecodeResult.UnsupportedVersion(99), RecallPlanWireCodec.decode(wire))
    }

    @Test fun `content capabilities derive only from content authority`() {
        val content = Content(
            ContentId("content"), ContentType.WORD,
            ContentText("word", "từ", "/wɜːd/", "a word", "một từ"),
            ContentMedia(primaryAudio = "media:word", image = "media:image", exampleAudio = "media:example")
        )
        val result = RecallContentCapabilityResolver.resolve(content)
        assertContains(result.available, RecallCapability.SOURCE_TEXT)
        assertContains(result.available, RecallCapability.TARGET_TRANSLATION)
        assertContains(result.available, RecallCapability.WORD_AUDIO)
        assertContains(result.available, RecallCapability.IMAGE)
        assertContains(result.available, RecallCapability.EXAMPLE_SOURCE)
        assertContains(result.available, RecallCapability.EXAMPLE_AUDIO)
    }

    @Test fun `sibling learning items share one content capability authority`() {
        val content = Content(ContentId("shared"), ContentType.WORD, ContentText("word"))
        val capabilities = RecallContentCapabilityResolver.resolve(content)
        val first = plan(contentId = content.id, learningItemId = LearningItemId("one")).copy(contentCapabilities = capabilities)
        val second = plan(contentId = content.id, learningItemId = LearningItemId("two")).copy(contentCapabilities = capabilities)
        assertEquals(first.contentId, second.contentId)
        assertSame(first.contentCapabilities, second.contentCapabilities)
    }

    @Test fun `shared recall source has no Desktop Compose or filesystem dependency`() {
        val roots = listOf(
            Path.of("src/main/kotlin/vn/loi/learning/domain/study/recall/RecallTypes.kt"),
            Path.of("src/main/kotlin/vn/loi/learning/application/recall/RecallContractServices.kt"),
            Path.of("src/main/kotlin/vn/loi/learning/application/recall/RecallPlanWireCodec.kt")
        )
        val source = roots.joinToString("\n") { Files.readString(it) }
        listOf("androidx.compose", "java.nio.file", "java.io.File", "java.awt", "desktop.ui").forEach {
            assertFalse(source.contains(it), it)
        }
    }

    @Test fun `content model is not expanded with hints choices or distractors`() {
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/domain/content/model/ContentText.kt"))
        assertFalse(source.contains("hint", ignoreCase = true))
        assertFalse(source.contains("choice", ignoreCase = true))
        assertFalse(source.contains("distractor", ignoreCase = true))
    }

    @Test fun `result contract has no persistence scheduling or ReviewEvent execution`() {
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/domain/study/recall/RecallTypes.kt"))
        assertFalse(source.contains("ReviewEvent"))
        assertFalse(source.contains("Scheduler"))
        assertFalse(source.contains("Repository"))
    }
}
