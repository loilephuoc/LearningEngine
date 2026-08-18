package vn.loi.learning.desktop.ui.browser.posreview

import kotlin.test.*

class PosAnalyzerTest {

    @Test
    fun `analyze classifies OPD real UAT single-word nouns correctly with HIGH confidence`() {
        val singleCategoryCases = listOf(
            "April",
            "apron",
            "archery",
            "armpit",
            "table",
            "apple",
            "window"
        )
        for (q in singleCategoryCases) {
            val input = PosAnalysisInput(
                contentId = "id-1",
                question = q,
                currentPos = "SENTENCE" // Legacy SENTENCE must NOT prevent correct classification
            )
            val result = PosAnalyzer.analyze(input)
            assertEquals("NOUN", result.suggestedPos, "Failed for question: $q")
            assertEquals(PosConfidence.HIGH, result.confidence, "Failed confidence for question: $q")
        }

        // Multi-category nouns with contextual syntactic support also get HIGH
        val contextualCases = listOf(
            "chair" to "Sit on the comfortable chair.",
            "water" to "I drank a glass of water."
        )
        for ((q, ex) in contextualCases) {
            val input = PosAnalysisInput(
                contentId = "id-1",
                question = q,
                exampleText = ex,
                currentPos = "SENTENCE"
            )
            val result = PosAnalyzer.analyze(input)
            assertEquals("NOUN", result.suggestedPos, "Failed for question: $q")
            assertEquals(PosConfidence.HIGH, result.confidence, "Failed confidence for question: $q")
        }
    }

    @Test
    fun `analyze classifies multi-word compound noun phrases correctly`() {
        val cases = listOf(
            "area code",
            "living room",
            "credit card",
            "police officer",
            "swimming pool",
            "high school"
        )
        for (q in cases) {
            val input = PosAnalysisInput(
                contentId = "id-2",
                question = q,
                currentPos = "SENTENCE"
            )
            val result = PosAnalyzer.analyze(input)
            assertEquals("NOUN PHRASE", result.suggestedPos, "Failed for question: $q")
            assertEquals(PosConfidence.HIGH, result.confidence, "Failed confidence for question: $q")
        }
    }

    @Test
    fun `analyze handles slash alternative expressions cleanly`() {
        val input = PosAnalysisInput(
            contentId = "id-3",
            question = "armchair / easy chair",
            currentPos = "SENTENCE"
        )
        val result = PosAnalyzer.analyze(input)
        assertEquals("NOUN PHRASE", result.suggestedPos)
        assertEquals(PosConfidence.HIGH, result.confidence)
    }

    @Test
    fun `analyze disambiguates multi-POS words using Example sentence context`() {
        // "arm" in noun context
        val armNoun = PosAnalyzer.analyze(
            PosAnalysisInput(
                contentId = "id-arm-1",
                question = "arm",
                exampleText = "He raised his right arm.",
                currentPos = "SENTENCE"
            )
        )
        assertEquals("NOUN", armNoun.suggestedPos)
        assertEquals(PosConfidence.HIGH, armNoun.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, armNoun.reasonCode)

        // "record" in verb context
        val recordVerb = PosAnalyzer.analyze(
            PosAnalysisInput(
                contentId = "id-rec-1",
                question = "record",
                exampleText = "Please record the meeting.",
                currentPos = "SENTENCE"
            )
        )
        assertEquals("VERB", recordVerb.suggestedPos)
        assertEquals(PosConfidence.HIGH, recordVerb.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, recordVerb.reasonCode)
    }

    @Test
    fun `analyze classifies action verb phrases as PHRASE`() {
        val input = PosAnalysisInput(
            contentId = "id-act-1",
            question = "arrange the furniture",
            currentPos = "SENTENCE"
        )
        val result = PosAnalyzer.analyze(input)
        assertEquals("PHRASE", result.suggestedPos)
        assertEquals(PosConfidence.HIGH, result.confidence)
        assertEquals(PosSuggestionReason.ACTION_VERB_PHRASE, result.reasonCode)
    }

    @Test
    fun `analyze detects numeric and percent expressions as NUMBER with HIGH confidence`() {
        val cases = listOf(
            "10 percent",
            "25 percent",
            "100 percent",
            "50%",
            "0.5",
            "123",
            "one hundred percent",
            "twenty-five percent"
        )
        for (q in cases) {
            val result = PosAnalyzer.analyze(PosAnalysisInput(contentId = "1", question = q, currentPos = "SENTENCE"))
            assertEquals("NUMBER", result.suggestedPos, "Failed for question: $q")
            assertEquals(PosConfidence.HIGH, result.confidence, "Failed confidence for question: $q")
        }
    }

    @Test
    fun `analyze does not classify 10-year-old girl as NUMBER`() {
        val result = PosAnalyzer.analyze(PosAnalysisInput(contentId = "1", question = "10-year-old girl", currentPos = "SENTENCE"))
        assertNotEquals("NUMBER", result.suggestedPos)
        assertEquals("NOUN PHRASE", result.suggestedPos)
        assertEquals(PosConfidence.HIGH, result.confidence)
    }

    @Test
    fun `analyze detects container and partitive noun phrases as NOUN PHRASE with HIGH confidence`() {
        val cases = listOf(
            "a bag of flour",
            "a bottle of water",
            "a box of cereal",
            "a bunch of bananas",
            "a can of beans",
            "a carton of eggs",
            "a clove of garlic",
            "a cup of flour",
            "a loaf of bread",
            "a piece of cake",
            "a pair of shoes",
            "a slice of pizza"
        )
        for (q in cases) {
            val result = PosAnalyzer.analyze(PosAnalysisInput(contentId = "1", question = q, currentPos = "SENTENCE"))
            assertEquals("NOUN PHRASE", result.suggestedPos, "Failed for question: $q")
            assertEquals(PosConfidence.HIGH, result.confidence, "Failed confidence for question: $q")
            assertEquals(PosSuggestionReason.CONTAINER_OR_QUANTIFIER_NOUN_PHRASE, result.reasonCode)
        }
    }

    @Test
    fun `analyze detects finite verb clause and preserves SENTENCE only when structure matches`() {
        // Case 1: with currentPos = SENTENCE -> preserves SENTENCE with HIGH confidence
        val withSentence = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "1", question = "a button is missing", currentPos = "SENTENCE")
        )
        assertEquals("SENTENCE", withSentence.suggestedPos)
        assertEquals(PosConfidence.HIGH, withSentence.confidence)
        assertEquals(PosSuggestionReason.FINITE_VERB_CLAUSE_SENTENCE, withSentence.reasonCode)

        // Case 2: with currentPos = "" or other POS -> UNCERTAIN, strictly NOT NOUN PHRASE
        val withoutSentence = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "1", question = "a button is missing", currentPos = "")
        )
        assertNotEquals("NOUN PHRASE", withoutSentence.suggestedPos)
        assertEquals(PosConfidence.UNCERTAIN, withoutSentence.confidence)
    }

    @Test
    fun `analyze detects common phrasal verbs`() {
        val phrasals = listOf("look after", "give up", "take off", "turn on", "put on", "pick up", "break down", "wake up")
        for (q in phrasals) {
            val result = PosAnalyzer.analyze(PosAnalysisInput(contentId = "1", question = q, currentPos = "SENTENCE"))
            assertEquals("PHRASAL VERB", result.suggestedPos, "Failed for phrasal verb: $q")
            assertEquals(PosConfidence.HIGH, result.confidence)
        }
    }

    @Test
    fun `analyze single words with suffix heuristics conservatively`() {
        val quickly = PosAnalyzer.analyze(PosAnalysisInput(contentId = "1", question = "quickly"))
        assertEquals("ADVERB", quickly.suggestedPos)

        val happiness = PosAnalyzer.analyze(PosAnalysisInput(contentId = "1", question = "happiness"))
        assertEquals("NOUN", happiness.suggestedPos)

        val beautiful = PosAnalyzer.analyze(PosAnalysisInput(contentId = "1", question = "beautiful"))
        assertEquals("ADJECTIVE", beautiful.suggestedPos)
    }

    @Test
    fun `analyze returns UNCERTAIN for ambiguous unknown text without making wild guesses`() {
        val ambiguous = PosAnalyzer.analyze(PosAnalysisInput(contentId = "1", question = "xyz abc foo", currentPos = "SENTENCE"))
        assertEquals(PosConfidence.UNCERTAIN, ambiguous.confidence)
        assertNull(ambiguous.suggestedPos)
    }

    @Test
    fun `analyze satisfies all Section 6 contextual disambiguation regression cases`() {
        // 1. chop -> VERB
        val chop = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "1", question = "chop", exampleText = "He will chop the vegetables for dinner.", currentPos = "SENTENCE")
        )
        assertEquals("VERB", chop.suggestedPos)
        assertEquals(PosConfidence.HIGH, chop.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, chop.reasonCode)

        // 2. vacuum -> VERB
        val vacuum = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "2", question = "vacuum", exampleText = "I need to vacuum the living room floor.", currentPos = "SENTENCE")
        )
        assertEquals("VERB", vacuum.suggestedPos)
        assertEquals(PosConfidence.HIGH, vacuum.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, vacuum.reasonCode)

        // 3. work -> VERB
        val work = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "3", question = "work", exampleText = "She works at a hospital.", currentPos = "SENTENCE")
        )
        assertEquals("VERB", work.suggestedPos)
        assertEquals(PosConfidence.HIGH, work.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, work.reasonCode)

        // 4. cook -> VERB
        val cook = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "4", question = "cook", exampleText = "I cook dinner every night.", currentPos = "SENTENCE")
        )
        assertEquals("VERB", cook.suggestedPos)
        assertEquals(PosConfidence.HIGH, cook.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, cook.reasonCode)

        // 5. arm -> NOUN
        val arm = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "5", question = "arm", exampleText = "He raised his arm to wave goodbye.", currentPos = "SENTENCE")
        )
        assertEquals("NOUN", arm.suggestedPos)
        assertEquals(PosConfidence.HIGH, arm.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, arm.reasonCode)

        // 6. light -> NOUN
        val light = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "6", question = "light", exampleText = "The light was too bright for my eyes.", currentPos = "SENTENCE")
        )
        assertEquals("NOUN", light.suggestedPos)
        assertEquals(PosConfidence.HIGH, light.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, light.reasonCode)

        // 7. drink -> VERB
        val drink = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "7", question = "drink", exampleText = "I'll drink water after my workout.", currentPos = "SENTENCE")
        )
        assertEquals("VERB", drink.suggestedPos)
        assertEquals(PosConfidence.HIGH, drink.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, drink.reasonCode)

        // 8. change -> NOUN
        val change = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "8", question = "change", exampleText = "I need change for the vending machine.", currentPos = "SENTENCE")
        )
        assertEquals("NOUN", change.suggestedPos)
        assertEquals(PosConfidence.HIGH, change.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, change.reasonCode)

        // 9. clean -> VERB
        val clean = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "9", question = "clean", exampleText = "Clean your room, please.", currentPos = "SENTENCE")
        )
        assertEquals("VERB", clean.suggestedPos)
        assertEquals(PosConfidence.HIGH, clean.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, clean.reasonCode)

        // 10. watch -> VERB
        val watch = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "10", question = "watch", exampleText = "I watched television last night.", currentPos = "SENTENCE")
        )
        assertEquals("VERB", watch.suggestedPos)
        assertEquals(PosConfidence.HIGH, watch.confidence)
        assertEquals(PosSuggestionReason.LEXICAL_CONTEXT_DISAMBIGUATION, watch.reasonCode)
    }

    @Test
    fun `analyze satisfies paired-context disambiguation tests`() {
        // work: NOUN vs VERB
        val workNoun = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "1", question = "work", exampleText = "Work can be stressful.")
        )
        assertEquals("NOUN", workNoun.suggestedPos)
        assertEquals(PosConfidence.HIGH, workNoun.confidence)

        val workVerb = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "2", question = "work", exampleText = "I work every day.")
        )
        assertEquals("VERB", workVerb.suggestedPos)
        assertEquals(PosConfidence.HIGH, workVerb.confidence)

        // cook: NOUN vs VERB
        val cookNoun = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "3", question = "cook", exampleText = "The cook prepared dinner.")
        )
        assertEquals("NOUN", cookNoun.suggestedPos)
        assertEquals(PosConfidence.HIGH, cookNoun.confidence)

        val cookVerb = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "4", question = "cook", exampleText = "I cook dinner.")
        )
        assertEquals("VERB", cookVerb.suggestedPos)
        assertEquals(PosConfidence.HIGH, cookVerb.confidence)

        // light: NOUN vs ADJECTIVE
        val lightNoun = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "5", question = "light", exampleText = "Turn on the light.")
        )
        assertEquals("NOUN", lightNoun.suggestedPos)
        assertEquals(PosConfidence.HIGH, lightNoun.confidence)

        val lightAdj = PosAnalyzer.analyze(
            PosAnalysisInput(contentId = "6", question = "light", exampleText = "This is a light bag.")
        )
        assertEquals("ADJECTIVE", lightAdj.suggestedPos)
        assertEquals(PosConfidence.HIGH, lightAdj.confidence)
    }

    @Test
    fun `analyze satisfies all Section 8 action phrase regression cases`() {
        val actionCases = listOf(
            "dice the celery" to "Dice the celery for the stuffing.",
            "measure the ingredients" to "Measure the ingredients before cooking.",
            "clean the house" to "We should clean the house this weekend.",
            "ask about the features" to "Ask about the features before you buy it.",
            "turn on the lights" to "Turn on the lights, it's too dark in here.",
            "throw away trash" to "Throw away the trash in the bin.",
            "pick up the kids" to "I will pick up the kids from school.",
            "go to bed" to "It is time to go to bed.",
            "have a conversation" to "Let's have a conversation about the plan.",
            "change a diaper" to "He needs to change a diaper for the baby."
        )

        for ((q, ex) in actionCases) {
            val res = PosAnalyzer.analyze(
                PosAnalysisInput(
                    contentId = "act-$q",
                    question = q,
                    exampleText = ex,
                    currentPos = "SENTENCE"
                )
            )
            assertEquals("PHRASE", res.suggestedPos, "Failed for action phrase: $q")
            assertEquals(PosConfidence.HIGH, res.confidence, "Failed confidence for: $q")
            assertEquals(PosSuggestionReason.ACTION_VERB_PHRASE, res.reasonCode, "Failed reason for: $q")
        }
    }

    @Test
    fun `analyze satisfies all Section 8 compound noun regression cases`() {
        val compoundCases = listOf(
            "water glass" to "He drank water from a tall glass.",
            "beer glass" to "She raised her beer glass in a toast.",
            "light switch" to "He flipped the light switch to turn on the lights.",
            "tool belt" to "The carpenter wore a heavy tool belt.",
            "paper cutter" to "Use the paper cutter to trim the photos.",
            "storage locker" to "I put my luggage in the storage locker.",
            "power cord" to "Plug the power cord into the wall outlet.",
            "safety rail" to "Hold onto the safety rail when climbing.",
            "shoulder pads" to "The jacket has thick shoulder pads.",
            "garlic press" to "A garlic press makes mincing garlic easy.",
            "hair spray" to "She used hair spray to keep her hairstyle.",
            "face powder" to "She applied face powder before the photo."
        )

        for ((q, ex) in compoundCases) {
            val res = PosAnalyzer.analyze(
                PosAnalysisInput(
                    contentId = "cmp-$q",
                    question = q,
                    exampleText = ex,
                    currentPos = "SENTENCE"
                )
            )
            assertEquals("NOUN PHRASE", res.suggestedPos, "Failed for compound noun: $q")
            assertEquals(PosConfidence.HIGH, res.confidence, "Failed confidence for: $q")
            assertEquals(PosSuggestionReason.DETERMINER_OR_COMPOUND_NOUN_PHRASE, res.reasonCode, "Failed reason for: $q")
        }
    }

    @Test
    fun `analyze satisfies all Section 17 v2_3 regression cases`() {
        // Single word nouns
        val singleNouns = listOf("aunt", "bathrobe", "booth", "forearm", "sneaker", "thunderstorm", "turmeric")
        for (w in singleNouns) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "sn-$w", question = w, currentPos = "SENTENCE"))
            assertEquals("NOUN", res.suggestedPos, "Failed for single noun: $w")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }

        // Noun phrases
        val nounPhrases = listOf(
            "cordless phone", "disposable diaper", "face powder", "hair spray", "paper cutter",
            "power cord", "safety rail", "shoulder pads", "storage locker", "USB port"
        )
        for (np in nounPhrases) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "np-$np", question = np, currentPos = "SENTENCE"))
            assertEquals("NOUN PHRASE", res.suggestedPos, "Failed for noun phrase: $np")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }

        // Abbreviations
        val abbrevs = listOf("a.m.", "CPU")
        for (ab in abbrevs) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "ab-$ab", question = ab, currentPos = "SENTENCE"))
            assertEquals("ABBREVIATION", res.suggestedPos, "Failed for abbreviation: $ab")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }

        // Ordinals
        val ordinals = listOf("sixtieth", "seventieth", "eightieth")
        for (ord in ordinals) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "ord-$ord", question = ord, currentPos = "SENTENCE"))
            assertEquals("NUMBER", res.suggestedPos, "Failed for ordinal: $ord")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }

        // Action expressions
        val actions = listOf(
            "go to college" to "He decided to go to college after high school.",
            "pick up the kids" to "I will pick up the kids from school."
        )
        for ((act, ex) in actions) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "act-$act", question = act, exampleText = ex, currentPos = "SENTENCE"))
            assertEquals("PHRASE", res.suggestedPos, "Failed for action: $act")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }
    }

    @Test
    fun `analyze satisfies all Section 15 v2_4 regression cases`() {
        // Interrogatives and Question Sentences
        val questions = listOf(
            "Can I help you?",
            "How much is this?",
            "What's the matter?",
            "Where is the bathroom?",
            "What time is it?",
            "Do you take credit cards?"
        )
        for (q in questions) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "q-$q", question = q, currentPos = "SENTENCE"))
            assertEquals("SENTENCE", res.suggestedPos, "Failed for question: $q")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }

        // Lexicon gap cases
        val gaps = listOf("bradawl", "bleachers", "bagger")
        for (w in gaps) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "gap-$w", question = w, currentPos = "SENTENCE"))
            assertEquals("NOUN", res.suggestedPos, "Failed for lexicon gap: $w")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }

        // Slash cases with same POS
        val slashSafe = PosAnalyzer.analyze(PosAnalysisInput(contentId = "sl-perfume", question = "perfume / cologne", currentPos = "SENTENCE"))
        assertEquals("NOUN PHRASE", slashSafe.suggestedPos)
        assertEquals(PosConfidence.HIGH, slashSafe.confidence)

        // Ambiguous slash case
        val slashAmbiguous = PosAnalyzer.analyze(PosAnalysisInput(contentId = "sl-full", question = "full / satisfied", currentPos = "SENTENCE"))
        assertEquals(PosConfidence.UNCERTAIN, slashAmbiguous.confidence)
    }
}
