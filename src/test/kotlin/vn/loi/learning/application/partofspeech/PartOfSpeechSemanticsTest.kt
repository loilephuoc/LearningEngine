package vn.loi.learning.application.partofspeech

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository

class PartOfSpeechSemanticsTest {
    @Test
    fun `normalizer canonicalizes aliases without collapsing distinct linguistic categories`() {
        mapOf(
            "N" to "NOUN",
            "n." to "NOUN",
            " noun " to "NOUN",
            "V" to "VERB",
            "v." to "VERB",
            "Adj." to "ADJECTIVE",
            "Adv" to "ADVERB",
            "Prep." to "PREPOSITION",
            "Pron" to "PRONOUN",
            "Conj." to "CONJUNCTION",
            "phrasal-verb" to "PHRASAL VERB",
            "v phr" to "PHRASAL VERB",
            "n phr" to "NOUN PHRASE"
        ).forEach { (raw, expected) ->
            assertEquals(expected, PartOfSpeechNormalizer.canonicalize(raw)?.value, raw)
        }
        assertEquals("AUXILIARY VERB", PartOfSpeechNormalizer.canonicalize("auxiliary verb")?.value)
        assertEquals("MODAL VERB", PartOfSpeechNormalizer.canonicalize("modal verb")?.value)
        assertEquals("PROPER NOUN", PartOfSpeechNormalizer.canonicalize("proper noun")?.value)
        assertNotEquals(
            PartOfSpeechNormalizer.canonicalize("verb"),
            PartOfSpeechNormalizer.canonicalize("phrasal verb")
        )
        assertNull(PartOfSpeechNormalizer.canonicalize(" \t "))
        assertNull(PartOfSpeechNormalizer.canonicalize(null))
    }

    @Test
    fun `extractor inventories both custom field names case insensitively`() {
        val content = content(
            fields = listOf("PartOfSpeech" to " noun ", "POS" to "V."),
            pronunciation = "/hɪl/"
        )
        val observations = PartOfSpeechExtractor.extract(content)
        assertEquals(
            listOf(PartOfSpeechSource.CUSTOM_PART_OF_SPEECH, PartOfSpeechSource.CUSTOM_POS),
            observations.map { it.source }
        )
        assertEquals(listOf("NOUN", "VERB"), observations.map { it.canonical?.value })
        assertEquals(listOf("noun", "V."), observations.map { it.trimmedValue })
    }

    @Test
    fun `extractor inventories embedded pronunciation and POS tags`() {
        val content = content(
            pronunciation = "/(proper noun) //hɪl///",
            tags = setOf("POS:idiom")
        )
        val observations = PartOfSpeechExtractor.extract(content)
        assertEquals(
            setOf(PartOfSpeechSource.PRONUNCIATION, PartOfSpeechSource.TAG),
            observations.mapTo(hashSetOf()) { it.source }
        )
        assertEquals(setOf("PROPER NOUN", "IDIOM"), observations.mapNotNullTo(hashSetOf()) { it.canonical?.value })
        assertEquals("/hɪl/", normalizePronunciation("/(proper noun) //hɪl///").ipa)
        assertEquals("PROPER NOUN", normalizePronunciation("/(proper noun) //hɪl///").partOfSpeech)
    }

    @Test
    fun `known catalog identities are unique and aliases intentionally share identity`() {
        val registry = PartOfSpeechSemanticRegistry()
        val known = PartOfSpeechNormalizer.knownCatalog.map { registry.resolve(it)!! }
        assertEquals(known.size, known.map { it.colorKey }.toSet().size)
        assertEquals(registry.resolve("n.")?.colorKey, registry.resolve("noun")?.colorKey)
        assertEquals(PosColorFamily.BLUE, registry.resolve("NOUN")?.colorKey?.family)
        assertEquals(PosColorFamily.GREEN, registry.resolve("VERB")?.colorKey?.family)
        assertEquals(PosColorFamily.PURPLE, registry.resolve("ADJECTIVE")?.colorKey?.family)
    }

    @Test
    fun `unknown identities are deterministic across recreated registries and import order`() {
        val values = listOf("GERUND", "PARTICLE", "CLAUSE", "TECHNICAL TERM", "PROPER NOUN")
        val first = PartOfSpeechSemanticRegistry()
        first.reconcile(values.mapIndexed { index, value -> content(index.toString(), fields = listOf("pos" to value)) })
        val reversed = PartOfSpeechSemanticRegistry()
        reversed.reconcile(values.reversed().mapIndexed { index, value -> content(index.toString(), fields = listOf("pos" to value)) })
        values.forEach { value ->
            assertEquals(first.resolve(value), reversed.resolve(value), value)
            assertEquals(first.resolve(value), PartOfSpeechSemanticRegistry().resolve(value), value)
        }
    }

    @Test
    fun `dynamic collision probes without replacing an existing assignment`() {
        val digest: (String) -> ByteArray = { value ->
            val probe = value.substringAfterLast('#', "0").toIntOrNull() ?: 0
            byteArrayOf(0, 0, probe.toByte()) + ByteArray(29) { 1 }
        }
        val registry = PartOfSpeechSemanticRegistry(visualSlotCount = 8, stableDigest = digest)
        registry.register(listOf(content("a", fields = listOf("pos" to "GERUND"))))
        val before = registry.resolve("GERUND")
        registry.register(listOf(content("b", fields = listOf("pos" to "PARTICLE"))))
        val after = registry.resolve("GERUND")
        val particle = registry.resolve("PARTICLE")
        assertEquals(before, after)
        assertNotEquals(after?.colorKey, particle?.colorKey)
        assertNotEquals(after?.colorKey?.visualSlot, particle?.colorKey?.visualSlot)
    }

    @Test
    fun `reconciliation is idempotent and aliases share one active identity`() {
        val contents = listOf(
            content("1", fields = listOf("partOfSpeech" to "noun")),
            content("2", fields = listOf("POS" to "N.")),
            content("3", pronunciation = "/(verb) /rʌn//")
        )
        val registry = PartOfSpeechSemanticRegistry()
        val first = registry.reconcile(contents)
        val second = registry.reconcile(contents)
        assertEquals(first, second)
        assertEquals(setOf("NOUN", "VERB"), second.mapTo(hashSetOf()) { it.canonical.value })
    }

    @Test
    fun `inventory counts aliases blanks sources and unresolved values without discarding them`() {
        val inventory = PartOfSpeechInventoryService.inventory(
            listOf(
                content("1", fields = listOf("partOfSpeech" to "noun")),
                content("2", fields = listOf("POS" to "N.")),
                content("3", pronunciation = "/(noun) /naʊn//"),
                content("4", fields = listOf("pos" to "Technical Term")),
                content("5")
            )
        )
        assertEquals(setOf("NOUN", "TECHNICAL TERM"), inventory.canonicalValues)
        assertEquals(3, inventory.entries.filter { it.canonical.value == "NOUN" }.sumOf { it.occurrenceCount })
        assertEquals(setOf("TECHNICAL TERM"), inventory.unresolvedValues)
        assertEquals(1, inventory.blankOrAbsentContentCount)
        assertEquals(
            setOf(
                PartOfSpeechSource.CUSTOM_PART_OF_SPEECH,
                PartOfSpeechSource.CUSTOM_POS,
                PartOfSpeechSource.PRONUNCIATION
            ),
            inventory.entries.filter { it.canonical.value == "NOUN" }.mapTo(hashSetOf()) { it.source }
        )
    }

    @Test
    fun `repository reconciler scans installed content through the application port`() {
        val repository = InMemoryContentRepository()
        repository.saveAll(
            listOf(
                content("1", fields = listOf("partOfSpeech" to "adjective")),
                content("2", pronunciation = "/(adv) /kwɪkli//")
            )
        )
        val registry = PartOfSpeechSemanticRegistry()
        val result = PartOfSpeechRegistryReconciler(repository, registry).reconcile()
        assertEquals(setOf("ADJECTIVE", "ADVERB"), result.mapTo(hashSetOf()) { it.canonical.value })
        assertFalse(repository.findAll().any { it.customFields.fields.any { field -> field.value != "adjective" } })
    }

    @Test
    fun `current inventory canonical values have distinct semantic identities`() {
        val current = setOf(
            "ADJECTIVE", "ADVERB", "ANIMAL", "CONJUNCTION", "DETERMINER", "IDIOM",
            "INTERJECTION", "NOUN", "NOUN PHRASE", "PHRASE", "PHRASAL VERB",
            "PREPOSITION", "PRONOUN", "PROPER NOUN", "VERB", "WORD"
        )
        val registry = PartOfSpeechSemanticRegistry()
        val identities = current.map { registry.resolve(it)!! }
        assertEquals(current.size, identities.map { it.colorKey }.toSet().size)
        assertEquals(current, identities.mapTo(hashSetOf()) { it.canonical.value })
    }

    @Test
    fun `POS authority has no unstable hash random scheduler or persistence coupling`() {
        val source = locateSource().readText()
        assertFalse(source.contains(".hashCode("))
        assertFalse(source.contains("kotlin.random"))
        assertFalse(source.contains("java.util.Random"))
        assertFalse(source.contains("scheduler"))
        assertFalse(source.contains("review"))
        assertFalse(source.contains("persistence"))
        assertFalse(source.contains("desktop"))
        assertFalse(source.contains("compose"))
    }

    private fun content(
        id: String = "content",
        fields: List<Pair<String, String>> = emptyList(),
        pronunciation: String? = null,
        tags: Set<String> = emptySet()
    ) = Content(
        id = ContentId("pos-$id"),
        type = ContentType.WORD,
        text = ContentText(primaryText = "word-$id", pronunciation = pronunciation),
        metadata = ContentMetadata(tags = tags),
        customFields = ContentCustomFields(
            fields.mapTo(linkedSetOf()) { (name, value) ->
                ContentCustomField(ContentFieldId(name), value)
            }
        )
    )

    private fun locateSource(): File {
        val fromRoot = File(
            "src/main/kotlin/vn/loi/learning/application/partofspeech/PartOfSpeechSemantics.kt"
        )
        return if (fromRoot.isFile) fromRoot else File(
            "../src/main/kotlin/vn/loi/learning/application/partofspeech/PartOfSpeechSemantics.kt"
        )
    }
}
