package vn.loi.learning.application.partofspeech

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content

data class CanonicalPartOfSpeech(
    val value: String,
    val known: Boolean
)

enum class PosColorFamily {
    BLUE,
    GREEN,
    PURPLE,
    ORANGE,
    CYAN,
    TEAL,
    AMBER,
    ROSE,
    INDIGO,
    SKY,
    EMERALD,
    LIME,
    DEEP_GREEN,
    VIOLET,
    FUCHSIA,
    PINK,
    SLATE,
    BROWN,
    TURQUOISE,
    CORAL,
    NEUTRAL,
    DYNAMIC
}

data class PosColorKey(
    val stableId: String,
    val family: PosColorFamily,
    val visualSlot: Int
)

data class PartOfSpeechIdentity(
    val canonical: CanonicalPartOfSpeech,
    val colorKey: PosColorKey
)

object PartOfSpeechNormalizer {
    private val whitespace = Regex("""\s+""")
    private val separators = Regex("""[-_]+""")
    private val trailingAbbreviationPunctuation = Regex("""[.,:;]+$""")

    private val aliases = mapOf(
        "N" to "NOUN",
        "NOUN" to "NOUN",
        "V" to "VERB",
        "VERB" to "VERB",
        "ADJ" to "ADJECTIVE",
        "ADJECTIVE" to "ADJECTIVE",
        "ADV" to "ADVERB",
        "ADVERB" to "ADVERB",
        "PREP" to "PREPOSITION",
        "PREPOSITION" to "PREPOSITION",
        "PRON" to "PRONOUN",
        "PRONOUN" to "PRONOUN",
        "CONJ" to "CONJUNCTION",
        "CONJUNCTION" to "CONJUNCTION",
        "INTJ" to "INTERJECTION",
        "INTERJECTION" to "INTERJECTION",
        "DET" to "DETERMINER",
        "DETERMINER" to "DETERMINER",
        "ART" to "ARTICLE",
        "ARTICLE" to "ARTICLE",
        "AUX" to "AUXILIARY",
        "AUXILIARY" to "AUXILIARY",
        "MODAL" to "MODAL",
        "N PHR" to "NOUN PHRASE",
        "NOUN PHR" to "NOUN PHRASE",
        "NOUN PHRASE" to "NOUN PHRASE",
        "V PHR" to "PHRASAL VERB",
        "VERB PHR" to "PHRASAL VERB",
        "VERB PHRASE" to "PHRASAL VERB",
        "PHRASAL V" to "PHRASAL VERB",
        "PHRASAL VERB" to "PHRASAL VERB",
        "PHR" to "PHRASE",
        "PHRASE" to "PHRASE",
        "IDIOM" to "IDIOM",
        "COLLOCATION" to "COLLOCATION",
        "NUM" to "NUMBER",
        "NUMBER" to "NUMBER",
        "ABBR" to "ABBREVIATION",
        "ABBREVIATION" to "ABBREVIATION",
        "PREFIX" to "PREFIX",
        "SUFFIX" to "SUFFIX",
        "WORD" to "WORD"
    )

    val knownCatalog: Set<String> = setOf(
        "NOUN", "VERB", "ADJECTIVE", "ADVERB", "PREPOSITION", "PRONOUN",
        "CONJUNCTION", "INTERJECTION", "DETERMINER", "ARTICLE", "AUXILIARY",
        "MODAL", "PHRASAL VERB", "PHRASE", "IDIOM", "COLLOCATION", "NUMBER",
        "ABBREVIATION", "PREFIX", "SUFFIX", "WORD"
    )

    fun canonicalize(raw: String?): CanonicalPartOfSpeech? {
        val normalized = raw
            ?.trim()
            ?.replace(separators, " ")
            ?.replace(whitespace, " ")
            ?.trim()
            ?.replace(trailingAbbreviationPunctuation, "")
            ?.uppercase(Locale.ROOT)
            ?.takeIf(String::isNotBlank)
            ?: return null
        val canonical = aliases[normalized] ?: normalized
        return CanonicalPartOfSpeech(canonical, canonical in knownCatalog)
    }
}

data class PartOfSpeechObservation(
    val rawValue: String?,
    val trimmedValue: String?,
    val canonical: CanonicalPartOfSpeech?,
    val source: PartOfSpeechSource
)

enum class PartOfSpeechSource {
    CUSTOM_PART_OF_SPEECH,
    CUSTOM_POS,
    PRONUNCIATION,
    TAG
}

data class PartOfSpeechInventoryEntry(
    val rawValue: String,
    val trimmedValue: String,
    val canonical: CanonicalPartOfSpeech,
    val source: PartOfSpeechSource,
    val occurrenceCount: Int
)

data class PartOfSpeechInventory(
    val entries: List<PartOfSpeechInventoryEntry>,
    val blankOrAbsentContentCount: Int
) {
    val canonicalValues: Set<String>
        get() = entries.mapTo(sortedSetOf()) { it.canonical.value }

    val unresolvedValues: Set<String>
        get() = entries.filterNot { it.canonical.known }
            .mapTo(sortedSetOf()) { it.canonical.value }
}

object PartOfSpeechExtractor {
    private val embeddedPos = Regex("""\(\s*([A-Za-z][A-Za-z ._-]*)\s*\)""")

    fun extract(content: Content): List<PartOfSpeechObservation> {
        val observations = mutableListOf<PartOfSpeechObservation>()
        content.customFields.fields.forEach { field ->
            val source = when {
                field.id.value.equals("partOfSpeech", ignoreCase = true) ->
                    PartOfSpeechSource.CUSTOM_PART_OF_SPEECH
                field.id.value.equals("pos", ignoreCase = true) ->
                    PartOfSpeechSource.CUSTOM_POS
                else -> null
            }
            if (source != null) observations += observation(field.value, source)
        }
        embeddedPos.find(content.text.pronunciation.orEmpty())?.groupValues?.get(1)?.let { raw ->
            observations += observation(raw, PartOfSpeechSource.PRONUNCIATION)
        }
        content.metadata.tags
            .filter { it.startsWith("pos:", ignoreCase = true) }
            .forEach { tag ->
                observations += observation(tag.substringAfter(':'), PartOfSpeechSource.TAG)
            }
        return observations
    }

    fun primary(content: Content): CanonicalPartOfSpeech? =
        extract(content)
            .sortedBy { it.source.ordinal }
            .firstNotNullOfOrNull(PartOfSpeechObservation::canonical)

    private fun observation(raw: String?, source: PartOfSpeechSource): PartOfSpeechObservation {
        val trimmed = raw?.trim()
        return PartOfSpeechObservation(
            rawValue = raw,
            trimmedValue = trimmed,
            canonical = PartOfSpeechNormalizer.canonicalize(trimmed),
            source = source
        )
    }
}

object PartOfSpeechInventoryService {
    fun inventory(contents: Iterable<Content>): PartOfSpeechInventory {
        val contentList = contents.toList()
        val observations = contentList.flatMap(PartOfSpeechExtractor::extract)
        val entries = observations
            .filter { it.canonical != null && !it.trimmedValue.isNullOrBlank() }
            .groupBy {
                listOf(
                    it.rawValue.orEmpty(),
                    it.trimmedValue.orEmpty(),
                    it.canonical!!.value,
                    it.source.name
                )
            }
            .map { (_, grouped) ->
                val first = grouped.first()
                PartOfSpeechInventoryEntry(
                    rawValue = first.rawValue.orEmpty(),
                    trimmedValue = first.trimmedValue.orEmpty(),
                    canonical = first.canonical!!,
                    source = first.source,
                    occurrenceCount = grouped.size
                )
            }
            .sortedWith(
                compareBy<PartOfSpeechInventoryEntry>(
                    { it.canonical.value },
                    { it.rawValue },
                    { it.source.name }
                )
            )
        return PartOfSpeechInventory(
            entries = entries,
            blankOrAbsentContentCount = contentList.count { content ->
                PartOfSpeechExtractor.extract(content).none { it.canonical != null }
            }
        )
    }
}

data class NormalizedPronunciation(
    val ipa: String?,
    val partOfSpeech: String?
)

fun normalizePronunciation(raw: String?): NormalizedPronunciation {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return NormalizedPronunciation(null, null)
    val partOfSpeech = Regex("""\(\s*([A-Za-z][A-Za-z ._-]*)\s*\)""")
        .find(value)
        ?.groupValues
        ?.get(1)
        ?.let(PartOfSpeechNormalizer::canonicalize)
    val withoutPartOfSpeech = value
        .replace(Regex("""/?\(\s*[A-Za-z][A-Za-z ._-]*\s*\)/?"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    val phonemes = withoutPartOfSpeech.trim('/').trim()
    return NormalizedPronunciation(
        ipa = phonemes.takeIf(String::isNotBlank)?.let { "/$it/" },
        partOfSpeech = partOfSpeech?.value
    )
}

class PartOfSpeechSemanticRegistry(
    private val visualSlotCount: Int = DEFAULT_VISUAL_SLOT_COUNT,
    private val stableDigest: (String) -> ByteArray = ::sha256
) {
    private val active = ConcurrentHashMap<String, PartOfSpeechIdentity>()

    init {
        require(visualSlotCount > 0)
    }

    fun reconcile(contents: Iterable<Content>): Set<PartOfSpeechIdentity> {
        val canonicalValues = PartOfSpeechInventoryService.inventory(contents).canonicalValues
        canonicalValues.forEach(::registerCanonical)
        return identities()
    }

    fun register(contents: Iterable<Content>): Set<PartOfSpeechIdentity> =
        reconcile(contents)

    fun resolve(raw: String?): PartOfSpeechIdentity? =
        PartOfSpeechNormalizer.canonicalize(raw)?.let { canonical ->
            active[canonical.value] ?: identityFor(canonical)
        }

    fun identities(): Set<PartOfSpeechIdentity> =
        active.values.sortedBy { it.canonical.value }.toCollection(linkedSetOf())

    private fun registerCanonical(value: String) {
        active.computeIfAbsent(value) { canonicalValue ->
            val canonical = PartOfSpeechNormalizer.canonicalize(canonicalValue)!!
            val fixed = knownColorKeys[canonical.value]
            if (fixed != null) {
                PartOfSpeechIdentity(canonical, fixed)
            } else {
                val occupiedSlots = active.values.mapTo(hashSetOf()) { it.colorKey.visualSlot }
                var probe = 0
                var slot = dynamicSlot(canonical.value, probe)
                while (slot in occupiedSlots) {
                    probe++
                    slot = dynamicSlot(canonical.value, probe)
                }
                identityFor(canonical, slot, probe)
            }
        }
    }

    private fun identityFor(canonical: CanonicalPartOfSpeech): PartOfSpeechIdentity {
        val fixed = knownColorKeys[canonical.value]
        return if (fixed != null) PartOfSpeechIdentity(canonical, fixed)
        else identityFor(canonical, dynamicSlot(canonical.value, 0), 0)
    }

    private fun identityFor(
        canonical: CanonicalPartOfSpeech,
        slot: Int,
        probe: Int
    ): PartOfSpeechIdentity {
        val digest = stableDigest(canonical.value)
        val digestId = digest.take(12).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return PartOfSpeechIdentity(
            canonical = canonical,
            colorKey = PosColorKey(
                stableId = "dynamic-$digestId-${canonical.value}",
                family = PosColorFamily.DYNAMIC,
                visualSlot = slot + probe * visualSlotCount
            )
        )
    }

    private fun dynamicSlot(value: String, probe: Int): Int {
        val digest = stableDigest("$value#$probe")
        val number = ((digest[0].toInt() and 0xff) shl 16) or
            ((digest[1].toInt() and 0xff) shl 8) or
            (digest[2].toInt() and 0xff)
        return number % visualSlotCount
    }

    companion object {
        private const val DEFAULT_VISUAL_SLOT_COUNT = 1_048_576

        private val knownColorKeys = listOf(
            "NOUN" to PosColorFamily.BLUE,
            "VERB" to PosColorFamily.GREEN,
            "ADJECTIVE" to PosColorFamily.PURPLE,
            "ADVERB" to PosColorFamily.ORANGE,
            "PREPOSITION" to PosColorFamily.CYAN,
            "PRONOUN" to PosColorFamily.TEAL,
            "CONJUNCTION" to PosColorFamily.AMBER,
            "INTERJECTION" to PosColorFamily.ROSE,
            "DETERMINER" to PosColorFamily.INDIGO,
            "ARTICLE" to PosColorFamily.SKY,
            "AUXILIARY" to PosColorFamily.EMERALD,
            "MODAL" to PosColorFamily.LIME,
            "PHRASAL VERB" to PosColorFamily.DEEP_GREEN,
            "PHRASE" to PosColorFamily.VIOLET,
            "IDIOM" to PosColorFamily.FUCHSIA,
            "COLLOCATION" to PosColorFamily.PINK,
            "NUMBER" to PosColorFamily.SLATE,
            "ABBREVIATION" to PosColorFamily.BROWN,
            "PREFIX" to PosColorFamily.TURQUOISE,
            "SUFFIX" to PosColorFamily.CORAL,
            "WORD" to PosColorFamily.NEUTRAL
        ).mapIndexed { index, (canonical, family) ->
            canonical to PosColorKey("known-${canonical.lowercase(Locale.ROOT).replace(' ', '-')}", family, index)
        }.toMap()
    }
}

class PartOfSpeechRegistryReconciler(
    private val contentRepository: ContentRepository,
    private val registry: PartOfSpeechSemanticRegistry
) {
    fun reconcile(): Set<PartOfSpeechIdentity> =
        registry.reconcile(contentRepository.findAll())
}

private fun sha256(value: String): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
