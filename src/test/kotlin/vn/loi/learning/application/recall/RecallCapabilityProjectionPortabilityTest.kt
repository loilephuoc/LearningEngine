package vn.loi.learning.application.recall

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.recall.*

class RecallCapabilityProjectionPortabilityTest {
    private val content = Content(
        ContentId("shared"), ContentType.WORD,
        ContentText("word", "từ", "/wɜːd/", "A word.", "Một từ."),
        ContentMedia(primaryAudio = "audio", image = "image", exampleAudio = "example-audio")
    )

    @Test fun `sibling learning items share ContentId projection authority`() {
        val first = ContentRecallCapabilityResolver.resolve(content)
        val second = ContentRecallCapabilityResolver.resolve(content)
        assertEquals(ContentId("shared"), first.contentId)
        assertEquals(first, second)
    }

    @Test fun `mode direction reason and fact ordering is stable by wire id`() {
        val result = ContentRecallCapabilityResolver.resolve(content)
        assertEquals(result.orderedEligibility.sortedBy { it.mode.wireId }, result.orderedEligibility)
        result.orderedEligibility.forEach {
            assertEquals(it.orderedDirections.sortedBy(RecallDirection::wireId), it.orderedDirections)
            assertEquals(it.orderedUnavailableReasons.sortedBy(RecallUnavailableReason::wireId), it.orderedUnavailableReasons)
        }
    }

    @Test fun `projection serialization is deterministic and round trips`() {
        val projection = ContentRecallCapabilityResolver.resolve(content)
        val first = RecallCapabilityProjectionWireCodec.encode(projection)
        val second = RecallCapabilityProjectionWireCodec.encode(projection)
        assertEquals(first, second)
        val decoded = RecallCapabilityProjectionWireCodec.decode(first) as RecallCapabilityProjectionDecodeResult.Success
        assertEquals(projection, decoded.projection)
    }

    @Test fun `projection wire does not use enum ordinals`() {
        val wire = RecallCapabilityProjectionWireCodec.encode(ContentRecallCapabilityResolver.resolve(content))
        RecallMode.entries.forEach { assertTrue(wire.contains(it.wireId)); assertFalse(wire.contains("\"mode\":${it.ordinal}")) }
    }

    @Test fun `resolver is pure and does not mutate Content`() {
        val before = content.copy()
        repeat(3) { ContentRecallCapabilityResolver.resolve(content) }
        assertEquals(before, content)
    }

    @Test fun `resolver has no learner scheduler evidence Desktop or filesystem dependency`() {
        val paths = listOf(
            "src/main/kotlin/vn/loi/learning/application/recall/ContentRecallCapabilityResolver.kt",
            "src/main/kotlin/vn/loi/learning/domain/study/recall/RecallCapabilityProjection.kt",
            "src/main/kotlin/vn/loi/learning/application/recall/RecallCapabilityProjectionWireCodec.kt"
        )
        val source = paths.joinToString("\n") { Files.readString(Path.of(it)) }
        listOf(
            "LearnerId", "Scheduler", "Evidence", "LearningDifficulty", "LearningRecommendation",
            "androidx.compose", "desktop.ui", "java.io.File", "java.nio.file", "InputStream",
            "System.currentTimeMillis", "Random("
        ).forEach { assertFalse(source.contains(it), it) }
    }

    @Test fun `LQ-006A Content capability model is reused rather than duplicated`() {
        val projection = ContentRecallCapabilityResolver.resolve(content)
        assertIs<RecallContentCapabilities>(projection.sourceCapabilities)
        assertEquals(content.id, projection.sourceCapabilities.contentId)
    }

    @Test fun `unknown projection version is typed unsupported`() {
        val wire = RecallCapabilityProjectionWireCodec.encode(ContentRecallCapabilityResolver.resolve(content))
            .replaceFirst("\"version\":1", "\"version\":99")
        assertEquals(
            RecallCapabilityProjectionDecodeResult.UnsupportedVersion(99),
            RecallCapabilityProjectionWireCodec.decode(wire)
        )
    }
}
