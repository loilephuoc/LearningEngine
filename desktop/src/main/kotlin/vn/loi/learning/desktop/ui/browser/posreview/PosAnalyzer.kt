package vn.loi.learning.desktop.ui.browser.posreview

import java.util.Locale
import vn.loi.learning.application.partofspeech.PartOfSpeechNormalizer

enum class PosConfidence {
    HIGH,
    MEDIUM,
    UNCERTAIN
}

enum class PosSuggestionReason {
    NUMBER_PERCENT_OR_NUMERIC,
    NUMERIC_EXPRESSION,
    CONTAINER_OR_QUANTIFIER_NOUN_PHRASE,
    DETERMINER_OR_COMPOUND_NOUN_PHRASE,
    ACTION_VERB_PHRASE,
    KNOWN_PHRASAL_VERB,
    FINITE_VERB_CLAUSE_SENTENCE,
    LEXICAL_SINGLE_MATCH,
    LEXICAL_CONTEXT_DISAMBIGUATION,
    LEXICAL_DEFAULT_PRIMARY,
    SINGLE_WORD_SUFFIX_HEURISTIC,
    CURRENT_POS_CONFIRMED,
    AMBIGUOUS_OR_UNKNOWN
}

data class PosAnalysisInput(
    val contentId: String,
    val question: String,
    val answer: String = "",
    val translation: String = "",
    val exampleText: String? = null,
    val pronunciation: String = "",
    val currentPos: String = ""
)

data class PosSuggestion(
    val suggestedPos: String?,
    val confidence: PosConfidence,
    val reasonCode: PosSuggestionReason
)

object PosAnalyzer {

    // Finite verbs and auxiliaries that indicate a finite clause/sentence
    private val finiteVerbsAndAuxiliaries = setOf(
        "am", "is", "are", "was", "were", "be", "been", "being",
        "has", "have", "had", "having",
        "do", "does", "did", "doing", "done",
        "can", "could", "will", "would", "shall", "should", "may", "might", "must",
        "isn't", "aren't", "wasn't", "weren't", "hasn't", "haven't", "hadn't",
        "don't", "doesn't", "didn't", "won't", "can't", "couldn't", "wouldn't", "shouldn't",
        "cannot", "ain't"
    )

    // Partitive / container / quantifier nouns for NOUN PHRASE
    private val containerAndQuantifierNouns = setOf(
        "bag", "bags", "bottle", "bottles", "box", "boxes", "bunch", "bunches",
        "can", "cans", "carton", "cartons", "clove", "cloves", "cup", "cups",
        "loaf", "loaves", "piece", "pieces", "slice", "slices", "pair", "pairs",
        "set", "sets", "bowl", "bowls", "glass", "glasses", "head", "heads",
        "jar", "jars", "packet", "packets", "sheet", "sheets", "bar", "bars",
        "tub", "tubs", "roll", "rolls", "pinch", "pinches", "dash", "dashes",
        "drop", "drops", "spoon", "spoons", "spoonful", "spoonfuls",
        "tablespoon", "tablespoons", "teaspoon", "teaspoons",
        "plate", "plates", "pack", "packs", "package", "packages",
        "bucket", "buckets", "tin", "tins", "basket", "baskets",
        "handful", "handfuls", "stalk", "stalks", "ear", "ears", "sprig", "sprigs"
    )

    // Determiners, articles, and possessives
    private val determinersAndArticles = setOf(
        "a", "an", "the", "this", "that", "these", "those",
        "my", "your", "his", "her", "its", "our", "their"
    )

    // Common phrasal verb base verbs
    private val phrasalBaseVerbs = setOf(
        "look", "give", "take", "turn", "get", "put", "pick", "bring", "call", "come",
        "carry", "break", "check", "fall", "find", "go", "hold", "keep", "make", "pass",
        "run", "set", "stand", "show", "try", "wake", "work", "blow", "cut", "drop",
        "fill", "hang", "knock", "leave", "let", "point", "pull", "send", "shut", "slow",
        "step", "switch", "throw", "watch", "wrap", "write", "catch", "back", "calm",
        "cheer", "clean", "dress", "eat", "figure", "grow", "hand", "hook", "iron",
        "kick", "line", "lock", "log", "mess", "mop", "move", "open", "pay", "print",
        "roll", "rub", "screw", "shop", "sign", "sit", "sleep", "snap", "speed", "spell",
        "start", "stay", "stick", "stop", "sweep", "tear", "tidy", "tie", "tip", "track",
        "trade", "use", "warm", "wash", "wipe", "zip", "zoom", "buckle"
    )

    // Phrasal verb particles
    private val phrasalParticles = setOf(
        "after", "up", "off", "on", "in", "out", "away", "back", "down", "over",
        "through", "along", "across", "around", "by", "forward", "ahead", "into",
        "onto", "about", "apart", "aside"
    )

    // Number regexes
    private val percentRegex = Regex("""^\d+(\.\d+)?\s*(%|percent)$""", RegexOption.IGNORE_CASE)
    private val pureNumberRegex = Regex("""^\d+(\.\d+)?$""")
    private val writtenNumberWords = setOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
        "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
        "hundred", "thousand", "million", "billion", "trillion", "half", "quarter"
    )
    private val ordinalNumberWords = setOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth",
        "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth", "eighteenth", "nineteenth",
        "twentieth", "thirtieth", "fortieth", "fiftieth", "sixtieth", "seventieth", "eightieth", "ninetieth",
        "hundredth", "thousandth", "millionth", "billionth"
    )
    private val standaloneAbbreviations = setOf(
        "a.m.", "p.m.", "a.m", "p.m", "cpu", "usb", "dvd", "cd-rom", "cd", "atm", "vip", "faq", "lcd", "led", "ram", "rom"
    )

    // Suffix heuristics: words ending in -ly that are adjectives, not adverbs
    private val adjectiveLyWords = setOf(
        "early", "family", "ugly", "friendly", "lovely", "lonely", "silly", "lively",
        "elderly", "deadly", "costly", "orderly", "timely", "daily", "weekly", "monthly",
        "yearly", "hourly", "nightly", "holy", "jolly", "curly", "oily", "chilly"
    )

    // Subject pronouns for context disambiguation
    private val subjectPronouns = setOf("i", "you", "he", "she", "we", "they", "who")
    private val subjectContractions = setOf(
        "i'll", "you'll", "he'll", "she'll", "we'll", "they'll",
        "i'd", "you'd", "he'd", "she'd", "we'd", "they'd",
        "i'm", "you're", "he's", "she's", "it's", "we're", "they're"
    )
    private val modalAuxiliaries = setOf(
        "can", "could", "will", "would", "shall", "should", "may", "might", "must",
        "do", "does", "did", "don't", "doesn't", "didn't", "won't", "can't", "couldn't", "shouldn't", "wouldn't",
        "cannot"
    )
    private val copularAndLinkingVerbs = setOf(
        "is", "are", "was", "were", "am", "be", "been", "being",
        "feel", "feels", "felt", "look", "looks", "looked", "taste", "tastes", "tasted",
        "smell", "smells", "smelled", "sound", "sounds", "sounded", "seem", "seems", "seemed",
        "become", "becomes", "became"
    )
    private val degreeAdverbs = setOf("very", "too", "so", "extremely", "quite", "really", "fairly", "sparkling", "freshly")
    private val prepositions = setOf(
        "in", "on", "at", "with", "by", "under", "over", "into", "onto", "from", "for", "about",
        "through", "after", "before", "during", "to", "between", "behind", "near", "against", "of"
    )
    private val commonFrequencyAdverbs = setOf(
        "always", "often", "never", "usually", "sometimes", "regularly", "frequently", "rarely", "seldom", "also", "still", "just", "already"
    )

    fun analyze(input: PosAnalysisInput): PosSuggestion {
        val rawQuestion = input.question.trim()
        if (rawQuestion.isBlank()) {
            return PosSuggestion(null, PosConfidence.UNCERTAIN, PosSuggestionReason.AMBIGUOUS_OR_UNKNOWN)
        }

        // Rule 1: Number (Percent, pure digits, written numbers, ordinals)
        val numberSuggestion = evaluateNumber(rawQuestion)
        if (numberSuggestion != null) {
            return numberSuggestion
        }

        // Rule 2: Standalone Abbreviation
        val strippedLower = rawQuestion.trim().lowercase(Locale.ROOT)
        if (strippedLower in standaloneAbbreviations || strippedLower.replace(".", "") in standaloneAbbreviations) {
            return PosSuggestion(
                suggestedPos = "ABBREVIATION",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.AMBIGUOUS_OR_UNKNOWN
            )
        }

        // Rule 3: Handle slash '/' alternative expressions
        if (rawQuestion.contains("/")) {
            val parts = rawQuestion.split("/").map(String::trim).filter(String::isNotBlank)
            if (parts.size >= 2) {
                val partSuggestions = parts.map { part ->
                    analyze(input.copy(question = part))
                }
                val allHigh = partSuggestions.all { it.suggestedPos != null && it.confidence == PosConfidence.HIGH }
                if (allHigh) {
                    val posSet = partSuggestions.mapNotNull { it.suggestedPos }.toSet()
                    if (posSet.size == 1) {
                        val singlePos = posSet.single()
                        val isMultiWord = parts.any { it.contains(" ") } || rawQuestion.contains("/")
                        val finalPos = if (singlePos == "NOUN" && isMultiWord) "NOUN PHRASE" else singlePos
                        return PosSuggestion(
                            suggestedPos = finalPos,
                            confidence = PosConfidence.HIGH,
                            reasonCode = if (singlePos == "SENTENCE") PosSuggestionReason.FINITE_VERB_CLAUSE_SENTENCE else PosSuggestionReason.DETERMINER_OR_COMPOUND_NOUN_PHRASE
                        )
                    } else if (posSet.all { it in setOf("NOUN", "NOUN PHRASE") }) {
                        return PosSuggestion(
                            suggestedPos = "NOUN PHRASE",
                            confidence = PosConfidence.HIGH,
                            reasonCode = PosSuggestionReason.DETERMINER_OR_COMPOUND_NOUN_PHRASE
                        )
                    } else if (posSet.all { it in setOf("VERB", "PHRASE", "PHRASAL VERB") }) {
                        return PosSuggestion(
                            suggestedPos = "PHRASE",
                            confidence = PosConfidence.HIGH,
                            reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
                        )
                    }
                }
                return PosSuggestion(
                    suggestedPos = null,
                    confidence = PosConfidence.UNCERTAIN,
                    reasonCode = PosSuggestionReason.AMBIGUOUS_OR_UNKNOWN
                )
            }
        }

        val cleaned = cleanText(rawQuestion)
        val tokens = cleaned.split(Regex("""\s+""")).filter(String::isNotBlank)
        if (tokens.isEmpty()) {
            return PosSuggestion(null, PosConfidence.UNCERTAIN, PosSuggestionReason.AMBIGUOUS_OR_UNKNOWN)
        }

        // Rule 3: Finite verb / clause / interrogative sentence detection
        val isInterrogativeQuestion = (rawQuestion.endsWith("?") || rawQuestion.contains("?")) && tokens.size >= 2
        val startsWithWhWord = tokens.size >= 2 && tokens.first() in setOf("how", "what", "where", "when", "why", "who", "which")
        val startsWithAuxQuestion = isInterrogativeQuestion && tokens.first() in setOf("can", "could", "would", "will", "do", "does", "did", "is", "are", "was", "were", "may", "should", "shall", "must")
        val startsWithQuestionWord = startsWithWhWord || startsWithAuxQuestion
        val hasFiniteVerb = hasFiniteVerbOrClause(tokens)
        val isDeclarativeSentenceStart = tokens.size >= 2 && (tokens.first() in setOf("it's", "there's", "they're", "we're", "you're", "he's", "she's", "i'm") || (tokens.first() == "there" && tokens.getOrNull(1) in setOf("is", "are", "was", "were")))
        if (isInterrogativeQuestion || (startsWithQuestionWord && hasFiniteVerb) || (hasFiniteVerb && (isDeclarativeSentenceStart || tokens.first() in determinersAndArticles || tokens.first() in subjectPronouns))) {
            val currentPosTrimmed = input.currentPos.trim()
            if (currentPosTrimmed.equals("SENTENCE", ignoreCase = true) || isInterrogativeQuestion || startsWithQuestionWord || isDeclarativeSentenceStart) {
                return PosSuggestion(
                    suggestedPos = "SENTENCE",
                    confidence = PosConfidence.HIGH,
                    reasonCode = PosSuggestionReason.FINITE_VERB_CLAUSE_SENTENCE
                )
            }
            return PosSuggestion(
                suggestedPos = null,
                confidence = PosConfidence.UNCERTAIN,
                reasonCode = PosSuggestionReason.FINITE_VERB_CLAUSE_SENTENCE
            )
        }

        // Rule 4: Known Phrasal Verb
        val phrasalSuggestion = evaluatePhrasalVerb(tokens)
        if (phrasalSuggestion != null) {
            return phrasalSuggestion
        }

        // Rule 5: Partitive / Container Noun Phrases
        val containerSuggestion = evaluateContainerNounPhrase(tokens)
        if (containerSuggestion != null) {
            return containerSuggestion
        }

        // Rule 5b: Known multi-word compound noun in dictionary
        if (tokens.size >= 2 && EnglishLexicon.LexicalCategory.NOUN in EnglishLexicon.lookup(cleaned)) {
            return PosSuggestion(
                suggestedPos = "NOUN PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.DETERMINER_OR_COMPOUND_NOUN_PHRASE
            )
        }

        // Rule 6: Action / Verb Phrases
        val actionPhraseSuggestion = evaluateActionVerbPhrase(tokens)
        if (actionPhraseSuggestion != null) {
            return actionPhraseSuggestion
        }

        // Rule 7: Prepositional Phrases
        val prepPhraseSuggestion = evaluatePrepositionalPhrase(tokens)
        if (prepPhraseSuggestion != null) {
            return prepPhraseSuggestion
        }

        // Rule 8: Multi-word Determiner / Compound Noun Phrases
        if (tokens.size >= 2) {
            val nounPhraseSuggestion = evaluateNounPhrase(tokens, cleaned, rawQuestion)
            if (nounPhraseSuggestion != null) {
                return nounPhraseSuggestion
            }
        }

        // Rule 9: Single-word Lexical Lookup with Context Disambiguation
        if (tokens.size == 1) {
            val singleWordSuggestion = evaluateSingleWordLexical(
                word = tokens.first(),
                input = input
            )
            if (singleWordSuggestion != null) {
                return singleWordSuggestion
            }
        }

        // Rule 10: Multi-word Expression fallback if tokens are recognized
        if (tokens.size in 2..4) {
            val lastTokenCategories = EnglishLexicon.lookup(tokens.last())
            if (EnglishLexicon.LexicalCategory.NOUN in lastTokenCategories) {
                return PosSuggestion(
                    suggestedPos = "NOUN PHRASE",
                    confidence = PosConfidence.MEDIUM,
                    reasonCode = PosSuggestionReason.DETERMINER_OR_COMPOUND_NOUN_PHRASE
                )
            }
        }

        // Rule 11: Existing Canonical POS confirmation
        val currentTrimmed = input.currentPos.trim()
        if (currentTrimmed.isNotBlank() && !currentTrimmed.equals("SENTENCE", ignoreCase = true)) {
            val canonical = PartOfSpeechNormalizer.canonicalize(currentTrimmed)
            if (canonical != null && canonical.known) {
                return PosSuggestion(
                    suggestedPos = canonical.value,
                    confidence = PosConfidence.MEDIUM,
                    reasonCode = PosSuggestionReason.CURRENT_POS_CONFIRMED
                )
            }
        }

        return PosSuggestion(
            suggestedPos = null,
            confidence = PosConfidence.UNCERTAIN,
            reasonCode = PosSuggestionReason.AMBIGUOUS_OR_UNKNOWN
        )
    }

    private fun cleanText(text: String): String {
        val withoutParens = text.replace(Regex("""\s*\([A-Za-z0-9\s]+\)\s*"""), " ")
        return withoutParens.trim().lowercase(Locale.ROOT)
            .replace('’', '\'')
            .replace(Regex("""[.,/#!$%\^&\*;:{}=\-_`~()?""]"""), " ")
            .trim()
    }

    private fun evaluateNumber(rawQuestion: String): PosSuggestion? {
        val trimmed = rawQuestion.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        if (percentRegex.matches(trimmed)) {
            return PosSuggestion(
                suggestedPos = "NUMBER",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.NUMBER_PERCENT_OR_NUMERIC
            )
        }
        if (pureNumberRegex.matches(trimmed) || lower in writtenNumberWords || lower in ordinalNumberWords) {
            return PosSuggestion(
                suggestedPos = "NUMBER",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.NUMERIC_EXPRESSION
            )
        }
        if (lower.endsWith("percent") || lower.endsWith("%")) {
            val withoutSuffix = lower.removeSuffix("percent").removeSuffix("%").trim()
            val parts = withoutSuffix.replace("-", " ").split(Regex("""\s+""")).filter(String::isNotBlank)
            if (parts.isNotEmpty() && parts.all { it in writtenNumberWords || it in ordinalNumberWords || pureNumberRegex.matches(it) }) {
                return PosSuggestion("NUMBER", PosConfidence.HIGH, PosSuggestionReason.NUMBER_PERCENT_OR_NUMERIC)
            }
        }
        val splitWords = lower.replace("-", " ").split(Regex("""\s+""")).filter(String::isNotBlank)
        if (splitWords.isNotEmpty() && splitWords.all { it in writtenNumberWords || it in ordinalNumberWords || pureNumberRegex.matches(it) }) {
            return PosSuggestion("NUMBER", PosConfidence.HIGH, PosSuggestionReason.NUMERIC_EXPRESSION)
        }
        return null
    }

    private fun hasFiniteVerbOrClause(tokens: List<String>): Boolean {
        if (tokens.size < 2) return false
        val first = tokens[0]
        if (first in setOf("it's", "there's", "they're", "we're", "you're", "he's", "she's", "i'm")) {
            return true
        }
        for (i in tokens.indices) {
            val token = tokens[i]
            val stripped = token.replace(Regex("""[^a-z']"""), "")
            if (stripped in finiteVerbsAndAuxiliaries || stripped in setOf("it's", "there's", "they're", "we're", "you're", "he's", "she's", "i'm")) {
                if (stripped == "can") {
                    val prev = if (i > 0) tokens[i - 1] else null
                    val next = if (i < tokens.size - 1) tokens[i + 1] else null
                    if (prev in determinersAndArticles || prev in setOf("trash", "spray", "watering", "garbage", "tin", "soda", "beer", "oil") || next == "of" || i == tokens.size - 1 || next in setOf("opener", "crusher", "holder", "label")) {
                        continue
                    }
                }
                if (stripped == "have" && i == 0) {
                    val next = if (tokens.size > 1) tokens[1] else null
                    if (next in determinersAndArticles) {
                        continue
                    }
                }
                return true
            }
        }
        return false
    }

    private fun evaluatePhrasalVerb(tokens: List<String>): PosSuggestion? {
        if (tokens.size in 2..3) {
            val base = tokens[0]
            val particle1 = tokens[1]
            if (base in phrasalBaseVerbs && particle1 in phrasalParticles) {
                if (tokens.size == 3) {
                    val particle2 = tokens[2]
                    if (particle2 in phrasalParticles || particle2 in prepositions) {
                        return PosSuggestion(
                            suggestedPos = "PHRASAL VERB",
                            confidence = PosConfidence.HIGH,
                            reasonCode = PosSuggestionReason.KNOWN_PHRASAL_VERB
                        )
                    }
                } else {
                    return PosSuggestion(
                        suggestedPos = "PHRASAL VERB",
                        confidence = PosConfidence.HIGH,
                        reasonCode = PosSuggestionReason.KNOWN_PHRASAL_VERB
                    )
                }
            }
        }
        return null
    }

    private fun evaluateContainerNounPhrase(tokens: List<String>): PosSuggestion? {
        if (tokens.size >= 3 && tokens[0] in determinersAndArticles) {
            val second = tokens[1]
            val third = tokens[2]
            if (second in containerAndQuantifierNouns && third == "of") {
                return PosSuggestion(
                    suggestedPos = "NOUN PHRASE",
                    confidence = PosConfidence.HIGH,
                    reasonCode = PosSuggestionReason.CONTAINER_OR_QUANTIFIER_NOUN_PHRASE
                )
            }
        }
        return null
    }

    private fun evaluateActionVerbPhrase(tokens: List<String>): PosSuggestion? {
        if (tokens.size < 2) return null
        val first = tokens[0]
        val second = tokens[1]

        // 0. Infinitive verb citation form: "to breathe", "to cry", "to sneeze", "to urinate", "to vomit", "to yawn"
        if (first == "to" && tokens.size >= 2) {
            val secondCategories = EnglishLexicon.lookup(second)
            if (tokens.size == 2 && EnglishLexicon.LexicalCategory.VERB in secondCategories) {
                return PosSuggestion(
                    suggestedPos = "VERB",
                    confidence = PosConfidence.HIGH,
                    reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
                )
            }
            return PosSuggestion(
                suggestedPos = "PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
            )
        }

        if (first == "get" && second in setOf("dressed", "engaged", "married", "ready", "lost", "started", "tired")) {
            return PosSuggestion(
                suggestedPos = "PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
            )
        }

        val firstCategories = EnglishLexicon.lookup(first)
        val isFirstVerb = (EnglishLexicon.LexicalCategory.VERB in firstCategories || first in phrasalBaseVerbs || first in setOf("dial", "press", "click", "pay", "order", "meet", "listen", "ask", "play", "read", "compliment", "explain", "invite", "offer", "thank", "buy")) && first !in determinersAndArticles

        if (!isFirstVerb) return null

        // 1. Verb + Determiner/Pronoun Object (e.g., "dice the celery", "compliment someone", "explain something")
        if (second in determinersAndArticles || second in setOf("the", "a", "an", "my", "your", "his", "her", "their", "our", "this", "that", "these", "those", "someone", "something", "somebody", "anybody", "anything", "everyone", "everything", "nobody", "nothing", "send", "in", "cash")) {
            return PosSuggestion(
                suggestedPos = "PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
            )
        }

        // 2. Phrasal verb + object / Prepositional verb phrase (e.g., "turn on the lights", "play with", "read to", "meet with", "ask for", "listen to", "order from")
        if (second in phrasalParticles || (second in prepositions && second != "of")) {
            return PosSuggestion(
                suggestedPos = "PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
            )
        }

        // 3. Verb + number (e.g. "dial 911")
        if (pureNumberRegex.matches(second) || second in writtenNumberWords) {
            return PosSuggestion(
                suggestedPos = "PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
            )
        }

        // 4. Unambiguous action verb + object noun
        val isFirstNoun = EnglishLexicon.LexicalCategory.NOUN in firstCategories
        val isFirstAdjOrPrep = EnglishLexicon.LexicalCategory.ADJECTIVE in firstCategories || first in prepositions
        val isParticipleOrGerund = first.endsWith("ed") || first.endsWith("ing")
        if (!isFirstNoun && !isFirstAdjOrPrep && !isParticipleOrGerund && tokens.size >= 2) {
            val last = tokens.last()
            val lastCategories = EnglishLexicon.lookup(last)
            if (EnglishLexicon.LexicalCategory.NOUN in lastCategories) {
                return PosSuggestion(
                    suggestedPos = "PHRASE",
                    confidence = PosConfidence.HIGH,
                    reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
                )
            }
        }

        // 5. Verb + directional adverb/particle
        if (tokens.size == 2 && second in setOf("up", "down", "away", "back", "out", "in", "off", "on", "slowly", "quickly", "carefully", "together", "apart", "regularly", "frequently", "early", "late", "well")) {
            return PosSuggestion(
                suggestedPos = "PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.ACTION_VERB_PHRASE
            )
        }

        return null
    }

    private fun evaluatePrepositionalPhrase(tokens: List<String>): PosSuggestion? {
        if (tokens.size >= 2) {
            val first = tokens[0]
            val second = tokens[1]
            if (first == "next" && second == "to") {
                return PosSuggestion(
                    suggestedPos = "PREPOSITION",
                    confidence = PosConfidence.HIGH,
                    reasonCode = PosSuggestionReason.AMBIGUOUS_OR_UNKNOWN
                )
            }
            if (first in prepositions && first in setOf("in", "on", "at", "by", "for", "with", "under")) {
                if (second in setOf("front", "back", "top", "bottom", "middle", "left", "right") || (second in determinersAndArticles && tokens.size >= 3 && tokens.getOrNull(2) in setOf("front", "back", "top", "bottom", "middle", "left", "right", "way", "time"))) {
                    return PosSuggestion(
                        suggestedPos = "PHRASE",
                        confidence = PosConfidence.HIGH,
                        reasonCode = PosSuggestionReason.DETERMINER_OR_COMPOUND_NOUN_PHRASE
                    )
                }
            }
        }
        return null
    }

    private fun evaluateNounPhrase(tokens: List<String>, cleaned: String, rawQuestion: String): PosSuggestion? {
        // If there is an internal determiner at index > 0 and the first token is NOT a determiner (e.g. "dice the celery"), it CANNOT be a compound noun
        if (tokens.isNotEmpty() && tokens[0] !in determinersAndArticles) {
            for (i in 1 until tokens.size) {
                if (tokens[i] in determinersAndArticles || tokens[i] in setOf("the", "a", "an", "my", "your", "his", "her", "our", "their", "this", "that", "these", "those")) {
                    return null
                }
            }
        }

        val fullLookup = EnglishLexicon.lookup(cleaned)
        if (EnglishLexicon.LexicalCategory.NOUN in fullLookup && tokens.size >= 2) {
            return PosSuggestion(
                suggestedPos = "NOUN PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.DETERMINER_OR_COMPOUND_NOUN_PHRASE
            )
        }

        val lastToken = tokens.last()
        val lastLookup = EnglishLexicon.lookup(lastToken)
        if (EnglishLexicon.LexicalCategory.NOUN in lastLookup) {
            return PosSuggestion(
                suggestedPos = "NOUN PHRASE",
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.DETERMINER_OR_COMPOUND_NOUN_PHRASE
            )
        }

        return null
    }

    private fun getTargetInflections(baseWord: String): Set<String> {
        val lower = baseWord.lowercase(Locale.ROOT)
        val inflections = mutableSetOf(lower)

        if (lower.endsWith("y") && lower.length > 2 && lower[lower.length - 2] !in "aeiou") {
            inflections.add(lower.dropLast(1) + "ies")
        } else if (lower.endsWith("s") || lower.endsWith("sh") || lower.endsWith("ch") || lower.endsWith("x") || lower.endsWith("z")) {
            inflections.add(lower + "es")
        } else if (lower.endsWith("e")) {
            inflections.add(lower + "s")
        } else {
            inflections.add(lower + "s")
        }

        if (lower.endsWith("e")) {
            inflections.add(lower + "d")
        } else if (lower.endsWith("y") && lower.length > 2 && lower[lower.length - 2] !in "aeiou") {
            inflections.add(lower.dropLast(1) + "ied")
        } else {
            inflections.add(lower + "ed")
            if (lower.length in 3..5 && lower.last() in "pbtdgkmn" && lower[lower.length - 2] in "aeiou" && lower[lower.length - 3] !in "aeiou") {
                inflections.add(lower + lower.last() + "ed")
            }
        }

        if (lower.endsWith("ee")) {
            inflections.add(lower + "ing")
        } else if (lower.endsWith("ie")) {
            inflections.add(lower.dropLast(2) + "ying")
        } else if (lower.endsWith("e")) {
            inflections.add(lower.dropLast(1) + "ing")
        } else {
            inflections.add(lower + "ing")
            if (lower.length in 3..5 && lower.last() in "pbtdgkmn" && lower[lower.length - 2] in "aeiou" && lower[lower.length - 3] !in "aeiou") {
                inflections.add(lower + lower.last() + "ing")
            }
        }

        return inflections
    }

    private fun evaluateSingleWordLexical(word: String, input: PosAnalysisInput): PosSuggestion? {
        val categories = EnglishLexicon.lookup(word)

        if (categories.size == 1) {
            val singleCat = categories.first()
            val posString = when (singleCat) {
                EnglishLexicon.LexicalCategory.NOUN -> "NOUN"
                EnglishLexicon.LexicalCategory.VERB -> "VERB"
                EnglishLexicon.LexicalCategory.ADJECTIVE -> "ADJECTIVE"
                EnglishLexicon.LexicalCategory.ADVERB -> "ADVERB"
                EnglishLexicon.LexicalCategory.NUMBER -> "NUMBER"
                EnglishLexicon.LexicalCategory.PREPOSITION -> "PREPOSITION"
                EnglishLexicon.LexicalCategory.PRONOUN -> "PRONOUN"
                EnglishLexicon.LexicalCategory.CONJUNCTION -> "CONJUNCTION"
                EnglishLexicon.LexicalCategory.INTERJECTION -> "INTERJECTION"
                EnglishLexicon.LexicalCategory.DETERMINER -> "DETERMINER"
                EnglishLexicon.LexicalCategory.ARTICLE -> "ARTICLE"
                EnglishLexicon.LexicalCategory.AUXILIARY -> "AUXILIARY"
                EnglishLexicon.LexicalCategory.MODAL -> "MODAL"
            }
            return PosSuggestion(
                suggestedPos = posString,
                confidence = PosConfidence.HIGH,
                reasonCode = PosSuggestionReason.LEXICAL_SINGLE_MATCH
            )
        }

        if (categories.size > 1) {
            val disambiguated = disambiguateWithExample(word, categories, input.exampleText)
            if (disambiguated != null) {
                return PosSuggestion(
                    suggestedPos = disambiguated,
                    confidence = PosConfidence.HIGH,
                    reasonCode = PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION
                )
            }

            val defaultPos = when {
                EnglishLexicon.LexicalCategory.NOUN in categories -> "NOUN"
                EnglishLexicon.LexicalCategory.VERB in categories -> "VERB"
                EnglishLexicon.LexicalCategory.ADJECTIVE in categories -> "ADJECTIVE"
                EnglishLexicon.LexicalCategory.ADVERB in categories -> "ADVERB"
                else -> "NOUN"
            }
            return PosSuggestion(
                suggestedPos = defaultPos,
                confidence = PosConfidence.MEDIUM,
                reasonCode = PosSuggestionReason.LEXICAL_DEFAULT_PRIMARY
            )
        }

        return evaluateSingleWordSuffixHeuristics(word)
    }

    private fun disambiguateWithExample(
        word: String,
        categories: Set<EnglishLexicon.LexicalCategory>,
        exampleText: String?
    ): String? {
        if (exampleText.isNullOrBlank()) return null
        val lowerExample = exampleText.lowercase(Locale.ROOT)
        val exampleTokens = lowerExample.split(Regex("""[^a-z0-9']+"""))
            .map { it.trim('\'', '"') }
            .filter(String::isNotBlank)

        val inflections = getTargetInflections(word)
        val wordIndex = exampleTokens.indexOfFirst {
            it in inflections || it == word.lowercase(Locale.ROOT) ||
                (it.startsWith(word.lowercase(Locale.ROOT)) && it.length <= word.length + 4)
        }
        if (wordIndex < 0) return null

        val matchedToken = exampleTokens[wordIndex]
        val prevToken = if (wordIndex > 0) exampleTokens[wordIndex - 1] else null
        val prevPrevToken = if (wordIndex > 1) exampleTokens[wordIndex - 2] else null
        val nextToken = if (wordIndex < exampleTokens.size - 1) exampleTokens[wordIndex + 1] else null

        val canBeVerb = EnglishLexicon.LexicalCategory.VERB in categories
        val canBeNoun = EnglishLexicon.LexicalCategory.NOUN in categories
        val canBeAdj = EnglishLexicon.LexicalCategory.ADJECTIVE in categories

        if (canBeVerb) {
            if (prevToken in modalAuxiliaries || prevToken in subjectContractions || prevToken == "ll" || prevToken == "d") {
                return "VERB"
            }
            if (prevToken == "to" || (prevPrevToken == "to" && prevToken in setOf("not", "always", "regularly", "never"))) {
                return "VERB"
            }
            if (prevToken in subjectPronouns || (prevPrevToken in subjectPronouns && prevToken in commonFrequencyAdverbs)) {
                return "VERB"
            }
            if (matchedToken != word.lowercase(Locale.ROOT) && (matchedToken.endsWith("s") || matchedToken.endsWith("ed") || matchedToken.endsWith("ing"))) {
                if (prevToken !in determinersAndArticles && prevToken !in prepositions) {
                    return "VERB"
                }
            }
            if (wordIndex == 0 || (wordIndex == 1 && prevToken == "please")) {
                if (nextToken !in modalAuxiliaries && nextToken !in copularAndLinkingVerbs) {
                    if (nextToken in determinersAndArticles || nextToken in subjectPronouns || nextToken in prepositions || nextToken in setOf("out", "up", "down", "off", "away", "carefully", "please") || (nextToken != null && EnglishLexicon.LexicalCategory.NOUN in EnglishLexicon.lookup(nextToken)) || prevToken == "please") {
                        return "VERB"
                    }
                }
            }
            if (prevPrevToken in setOf("help", "helps", "helped", "let", "lets", "make", "makes") && prevToken in setOf("me", "him", "her", "us", "them", "you")) {
                return "VERB"
            }
        }

        if (canBeAdj) {
            if (nextToken != null && EnglishLexicon.LexicalCategory.NOUN in EnglishLexicon.lookup(nextToken)) {
                if (prevToken in determinersAndArticles || prevToken in prepositions || prevToken in degreeAdverbs || wordIndex == 0) {
                    return "ADJECTIVE"
                }
            }
            if (prevToken in copularAndLinkingVerbs || prevToken in degreeAdverbs || (prevPrevToken in copularAndLinkingVerbs && prevToken in degreeAdverbs)) {
                return "ADJECTIVE"
            }
        }

        if (canBeNoun) {
            if (prevToken in determinersAndArticles || prevToken in setOf("my", "your", "his", "her", "its", "our", "their", "this", "that", "these", "those", "some", "any", "no", "every", "each")) {
                return "NOUN"
            }
            if (prevToken in prepositions) {
                return "NOUN"
            }
            if (prevToken in setOf("need", "needs", "needed", "want", "wants", "wanted", "have", "had", "has", "make", "makes", "made", "get", "gets", "got", "take", "takes", "took", "give", "gives", "gave", "buy", "buys", "bought", "use", "uses", "used", "find", "finds", "found", "see", "sees", "saw")) {
                return "NOUN"
            }
            if (prevToken in setOf("one", "two", "three", "many", "several", "few", "much", "more", "less", "another")) {
                return "NOUN"
            }
            if (prevToken != null && EnglishLexicon.LexicalCategory.ADJECTIVE in EnglishLexicon.lookup(prevToken)) {
                return "NOUN"
            }
            if (wordIndex == 0 && (nextToken in copularAndLinkingVerbs || nextToken in modalAuxiliaries || nextToken in setOf("travels", "requires", "takes", "needs", "starts", "ends", "costs"))) {
                return "NOUN"
            }
        }

        return null
    }

    private fun evaluateSingleWordSuffixHeuristics(word: String): PosSuggestion? {
        val lower = word.lowercase(Locale.ROOT)

        if (lower.endsWith("ly") && lower.length > 4 && lower !in adjectiveLyWords) {
            return PosSuggestion("ADVERB", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if (lower.endsWith("ness") && lower.length > 5) {
            return PosSuggestion("NOUN", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if ((lower.endsWith("tion") || lower.endsWith("sion")) && lower.length > 5) {
            return PosSuggestion("NOUN", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if (lower.endsWith("ment") && lower.length > 5) {
            return PosSuggestion("NOUN", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if (lower.endsWith("ity") && lower.length > 4) {
            return PosSuggestion("NOUN", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if (lower.endsWith("ship") && lower.length > 5) {
            return PosSuggestion("NOUN", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if (lower.endsWith("hood") && lower.length > 5) {
            return PosSuggestion("NOUN", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }

        if ((lower.endsWith("able") || lower.endsWith("ible")) && lower.length > 5) {
            return PosSuggestion("ADJECTIVE", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if (lower.endsWith("ful") && lower.length > 4) {
            return PosSuggestion("ADJECTIVE", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if (lower.endsWith("less") && lower.length > 5) {
            return PosSuggestion("ADJECTIVE", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }
        if (lower.endsWith("ous") && lower.length > 4) {
            return PosSuggestion("ADJECTIVE", PosConfidence.MEDIUM, PosSuggestionReason.SINGLE_WORD_SUFFIX_HEURISTIC)
        }

        return null
    }
}
