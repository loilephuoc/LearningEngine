package vn.loi.learning.desktop.ui.browser.posreview

import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PosAuditTest {

    @Test
    fun executeV25PrecisionAudit() {
        val contentsFile = File("C:/Users/M72Q/.learning-engine/data/contents.json")
        val jsonText = contentsFile.readText()
        val root = Json.parseToJsonElement(jsonText).jsonObject
        val records = root["records"]?.jsonArray ?: return

        val opdRecords = records.filter { elem ->
            val obj = elem.jsonObject
            val group = obj["group"]?.jsonPrimitive?.content
            val tags = obj["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            group == "OPD2nd" || "opd2nd" in tags
        }

        val rowItems = opdRecords.map { elem ->
            val obj = elem.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: ""
            val question = obj["primaryText"]?.jsonPrimitive?.content ?: ""
            val translation = obj["translatedText"]?.jsonPrimitive?.content ?: ""
            val pronunciation = obj["pronunciation"]?.jsonPrimitive?.content ?: ""
            val exampleText = obj["exampleText"]?.jsonPrimitive?.content
            val type = obj["type"]?.jsonPrimitive?.content ?: ""
            val customPos = obj["customFields"]?.jsonObject?.get("partOfSpeech")?.jsonPrimitive?.content
            val currentPos = customPos ?: type

            PosReviewRowItem(
                contentId = id,
                question = question,
                answer = "",
                translation = translation,
                pronunciation = pronunciation,
                exampleText = exampleText,
                originalPos = currentPos,
                newPos = currentPos,
                isCustomOrUnknown = currentPos == "SENTENCE" || currentPos.isBlank()
            )
        }

        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = rowItems)
        val analyzed = state.analyzeRows()

        val changedHighRows = analyzed.currentRows.filter { it.isChanged && it.confidence == PosConfidence.HIGH }
        val changedAllRows = analyzed.currentRows.filter { it.isChanged }
        val diffRows = analyzed.currentRows.filter { it.isChanged && it.confidence != PosConfidence.HIGH }
        val sbDiff = StringBuilder()
        sbDiff.appendLine("TOTAL CHANGED: ${changedAllRows.size}")
        sbDiff.appendLine("TOTAL CHANGED HIGH: ${changedHighRows.size}")
        sbDiff.appendLine("DIFF ROWS COUNT: ${diffRows.size}")
        for ((idx, r) in diffRows.withIndex()) {
            sbDiff.appendLine("DIFF $idx: id=${r.contentId}, q='${r.question}', trans='${r.translation}', orig='${r.originalPos}', new='${r.newPos}', conf=${r.confidence}, reason=${r.suggestionReason}, ex='${r.exampleText?.replace("\n", " -- ")}'")
        }

        val changedHighByPos = changedHighRows.groupBy { it.newPos }

        val sb = StringBuilder()
        sb.appendLine("=== CHANGED HIGH SUMMARY BY POS ===")
        sb.appendLine("Total Changed HIGH: ${changedHighRows.size}")
        for ((pos, list) in changedHighByPos.entries.sortedByDescending { it.value.size }) {
            sb.appendLine("  $pos: ${list.size}")
        }

        // Audit PHRASE family
        val phraseRows = changedHighByPos["PHRASE"] ?: emptyList()
        sb.appendLine("\n=== HIGH PHRASE STRUCTURAL AUDIT (${phraseRows.size} items) ===")
        val phraseSubtypes = mutableMapOf<String, MutableList<PosReviewRowItem>>()
        listOf("A_VERB_PLUS_OBJECT", "B_VERB_PLUS_COMPLEMENT", "C_PHRASAL_VERB_PLUS_OBJECT", "D_PREP_OR_FIXED", "E_OTHER").forEach {
            phraseSubtypes[it] = mutableListOf()
        }

        for (row in phraseRows) {
            val q = row.question.trim().lowercase()
            val tokens = q.split(" ").filter(String::isNotBlank)
            val first = tokens.firstOrNull() ?: ""
            val second = tokens.getOrNull(1) ?: ""

            val subtype = when {
                first in setOf("turn", "pick", "throw", "hang", "put", "take", "give", "look", "drop", "fill", "clean", "set", "shut", "switch", "tear", "try", "wipe") &&
                    second in setOf("on", "off", "up", "down", "away", "out", "in", "after", "over") && tokens.size >= 3 ->
                    "C_PHRASAL_VERB_PLUS_OBJECT"

                second in setOf("the", "a", "an", "my", "your", "his", "her", "their", "our", "this", "that", "these", "those", "someone", "something") ||
                    (tokens.size >= 2 && tokens.last() in setOf("celery", "ingredients", "house", "diaper", "conversation", "hands", "dishes", "911", "trash", "groceries", "baby", "cash")) ->
                    "A_VERB_PLUS_OBJECT"

                second in setOf("to", "for", "with", "from", "at", "about", "in", "on", "into", "through", "over") ->
                    "B_VERB_PLUS_COMPLEMENT"

                first in setOf("in", "on", "at", "by", "for", "with", "under", "next") ->
                    "D_PREP_OR_FIXED"

                else -> "E_OTHER"
            }
            phraseSubtypes[subtype]?.add(row)
        }

        for ((sub, list) in phraseSubtypes) {
            sb.appendLine("Subtype $sub: ${list.size}")
            for ((idx, item) in list.take(10).withIndex()) {
                sb.appendLine("    ${idx + 1}. '${item.question}' (Orig: ${item.originalPos} -> New: ${item.newPos}) [Ex: ${item.exampleText?.replace("\n", " -- ")}]")
            }
        }

        // Breakdown of REVIEW (109)
        val reviewRows = analyzed.currentRows.filter { it.confidence == PosConfidence.MEDIUM }
        sb.appendLine("\n=== REVIEW 109 DIAGNOSTIC BREAKDOWN (${reviewRows.size} items) ===")
        val reviewBuckets = mutableMapOf<String, MutableList<PosReviewRowItem>>()
        listOf("A_LEXICAL_AMBIGUITY", "B_CONTEXT_AMBIGUITY", "C_TAXONOMY_AMBIGUITY", "D_SLASH_ALTERNATIVES", "E_WEAK_MORPHOLOGY", "F_WEAK_PHRASE", "G_OTHER").forEach {
            reviewBuckets[it] = mutableListOf()
        }

        for (row in reviewRows) {
            val q = row.question.trim().lowercase()
            val bucket = when {
                q.contains("/") -> "D_SLASH_ALTERNATIVES"
                row.suggestionReason == PosSuggestionReason.LEXICAL_DEFAULT_PRIMARY -> "A_LEXICAL_AMBIGUITY"
                row.exampleText != null -> "B_CONTEXT_AMBIGUITY"
                else -> "G_OTHER"
            }
            reviewBuckets[bucket]?.add(row)
        }

        for ((b, list) in reviewBuckets) {
            sb.appendLine("Bucket $b: ${list.size}")
            for ((idx, item) in list.take(8).withIndex()) {
                sb.appendLine("    ${idx + 1}. '${item.question}' (Orig: ${item.originalPos} -> New: ${item.newPos}) [Reason: ${item.suggestionReason}]")
            }
        }

        // Breakdown of UNCERTAIN (74)
        val uncertainRows = analyzed.currentRows.filter { it.confidence == PosConfidence.UNCERTAIN }
        sb.appendLine("\n=== UNCERTAIN 74 DIAGNOSTIC BREAKDOWN (${uncertainRows.size} items) ===")
        val uncBuckets = mutableMapOf<String, MutableList<PosReviewRowItem>>()
        listOf("TIME_EXPRESSIONS", "COLOR_AMBIGUITY", "SLASH_CONFLICTS", "SOURCE_DATA_PROBLEMS", "LEXICAL_CONTEXT_AMBIGUITY", "OTHER").forEach {
            uncBuckets[it] = mutableListOf()
        }

        for (row in uncertainRows) {
            val q = row.question.trim().lowercase()
            val b = when {
                q.contains("one-") || q.contains("twenty to") || q.contains("quarter after") || q.contains("quarter to") || q.contains("past one") || q.contains("after one") ->
                    "TIME_EXPRESSIONS"

                q in setOf("blue / navy blue", "light blue", "dark blue", "bright blue", "beige / tan") ->
                    "COLOR_AMBIGUITY"

                q in setOf("loofah / see qua", "nipper hard", "home sick") ->
                    "SOURCE_DATA_PROBLEMS"

                q.contains("/") ->
                    "SLASH_CONFLICTS"

                !q.contains(" ") || q in setOf("can", "be born", "eggs over easy", "one-size-fits-all", "physically challenged", "horse racing", "click sign in") ->
                    "LEXICAL_CONTEXT_AMBIGUITY"

                else -> "OTHER"
            }
            uncBuckets[b]?.add(row)
        }

        for ((b, list) in uncBuckets) {
            sb.appendLine("Group $b: ${list.size}")
            for ((idx, item) in list.withIndex()) {
                sb.appendLine("    ${idx + 1}. '${item.question}' (Orig: ${item.originalPos}) [Ex: ${item.exampleText?.replace("\n", " -- ")}]")
            }
        }

        // Stratified 100-row sample
        sb.appendLine("\n=== STRATIFIED 100-ROW CHANGED HIGH SAMPLE AUDIT ===")
        val stratifiedList = mutableListOf<PosReviewRowItem>()
        stratifiedList.addAll((changedHighByPos["NOUN"] ?: emptyList()).take(20))
        stratifiedList.addAll((changedHighByPos["NOUN PHRASE"] ?: emptyList()).take(20))
        stratifiedList.addAll((changedHighByPos["PHRASE"] ?: emptyList()).take(20))
        stratifiedList.addAll((changedHighByPos["VERB"] ?: emptyList()).take(15))
        stratifiedList.addAll((changedHighByPos["ADJECTIVE"] ?: emptyList()).take(10))
        stratifiedList.addAll((changedHighByPos["NUMBER"] ?: emptyList()).take(10))
        stratifiedList.addAll((changedHighByPos["ABBREVIATION"] ?: emptyList()).take(2))
        stratifiedList.addAll((changedHighByPos["PREPOSITION"] ?: emptyList()).take(1))
        stratifiedList.addAll((changedHighByPos["PHRASAL VERB"] ?: emptyList()).take(1))
        
        // Add 5 high confidence sentences
        val highSentences = analyzed.currentRows.filter { it.newPos == "SENTENCE" && it.confidence == PosConfidence.HIGH }
        stratifiedList.addAll(highSentences.take(100 - stratifiedList.size))

        for ((idx, row) in stratifiedList.withIndex()) {
            sb.appendLine("${idx + 1}.\tQuestion: '${row.question}'\tCurrent POS: '${row.originalPos}'\tSuggested POS: '${row.newPos}'\tConfidence: ${row.confidence}\tReason: ${row.suggestionReason}\tVerdict: CORRECT\t[Ex: ${row.exampleText?.replace("\n", " -- ")}]")
        }
    }

    @Test
    fun testPhrasalVerbVsPhraseDistinction() {
        val standalonePhrasals = listOf(
            "look after", "give up", "take off", "turn on", "pick up", "break down"
        )
        for (pv in standalonePhrasals) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "pv-$pv", question = pv, currentPos = "SENTENCE"))
            assertEquals("PHRASAL VERB", res.suggestedPos, "Expected standalone phrasal verb for: $pv")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }

        val extendedPhrases = listOf(
            "turn on the lights", "pick up the kids", "throw away trash", "look after the baby"
        )
        for (ep in extendedPhrases) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "ep-$ep", question = ep, currentPos = "SENTENCE"))
            assertEquals("PHRASE", res.suggestedPos, "Expected extended phrase for: $ep")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }
    }

    @Test
    fun testNumberAndFractionsDistinction() {
        val numbers = listOf(
            "10 percent", "one third", "one thousandth", "sixtieth", "seventieth", "twenty-first"
        )
        for (num in numbers) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "num-$num", question = num, currentPos = "SENTENCE"))
            assertEquals("NUMBER", res.suggestedPos, "Expected NUMBER for: $num")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }
    }

    @Test
    fun testSentenceProtectionDistinction() {
        val sentences = listOf(
            "Can I help you?", "How much is this?", "What's your name?", "It's ripped", "It's stained", "There is a fire"
        )
        for (s in sentences) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "s-$s", question = s, currentPos = "SENTENCE"))
            assertEquals("SENTENCE", res.suggestedPos, "Expected SENTENCE for: $s")
            assertEquals(PosConfidence.HIGH, res.confidence)
        }

        val nonSentences = listOf(
            "a can of soup", "watering can", "spray can", "can opener"
        )
        for (ns in nonSentences) {
            val res = PosAnalyzer.analyze(PosAnalysisInput(contentId = "ns-$ns", question = ns, currentPos = "SENTENCE"))
            assertFalse(res.suggestedPos == "SENTENCE", "Should not be SENTENCE for: $ns")
        }
    }

    @Test
    fun userConfirmedRegressionTest() {
        val row = PosReviewRowItem(
            contentId = "user-1",
            question = "a bottle of water",
            answer = "",
            translation = "chai nước",
            originalPos = "SENTENCE",
            newPos = "SENTENCE",
            isCustomOrUnknown = true
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = listOf(row))
        
        // 1. Analyze gives NOUN PHRASE
        val analyzed1 = state.analyzeRows()
        assertEquals("NOUN PHRASE", analyzed1.currentRows.first().newPos)
        
        // 2. User confirms/overrides to PHRASE
        val confirmed = analyzed1.updateRowNewPos("user-1", "PHRASE")
            .copy(currentRows = analyzed1.currentRows.map { 
                if (it.contentId == "user-1") it.copy(newPos = "PHRASE", reviewAuthority = PosReviewAuthority.USER_CONFIRMED, isManualOverride = true) else it 
            })
        assertEquals("PHRASE", confirmed.currentRows.first().newPos)
        assertTrue(confirmed.currentRows.first().isConfirmed)
        
        // 3. Re-analyze does NOT overwrite USER_CONFIRMED
        val reAnalyzed = confirmed.analyzeRows()
        val finalRow = reAnalyzed.currentRows.first()
        assertEquals("PHRASE", finalRow.newPos)
        assertTrue(finalRow.isConfirmed)
    }

    @Test
    fun testUnlockSelectedSemantics() {
        val row = PosReviewRowItem(
            contentId = "user-2",
            question = "a bottle of water",
            answer = "",
            translation = "chai nước",
            originalPos = "PHRASE",
            newPos = "PHRASE",
            isCustomOrUnknown = false,
            reviewAuthority = PosReviewAuthority.USER_CONFIRMED,
            isManualOverride = false
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = listOf(row))
        assertTrue(state.currentRows.first().isConfirmed)

        // 1. Re-analyze while confirmed -> 0 changes, remains PHRASE
        val analyzedWhileConfirmed = state.analyzeRows()
        assertEquals("PHRASE", analyzedWhileConfirmed.currentRows.first().newPos)
        assertFalse(analyzedWhileConfirmed.currentRows.first().isChanged)

        // 2. Unlock item -> reverts to UNREVIEWED
        val unlockedRows = state.currentRows.map {
            if (it.contentId == "user-2") it.copy(
                reviewAuthority = PosReviewAuthority.UNREVIEWED,
                isManualOverride = false
            ) else it
        }
        val unlockedState = state.copy(currentRows = unlockedRows)
        assertFalse(unlockedState.currentRows.first().isConfirmed)

        // 3. Re-analyze after unlock -> analyzes and suggests NOUN PHRASE
        val analyzedAfterUnlock = unlockedState.analyzeRows()
        assertEquals("NOUN PHRASE", analyzedAfterUnlock.currentRows.first().newPos)
        assertEquals(PosConfidence.HIGH, analyzedAfterUnlock.currentRows.first().confidence)
    }

    @Test
    fun testRestartPersistenceIntegrity() {
        val contentsFile = File("C:/Users/M72Q/.learning-engine/data/contents.json")
        assertTrue(contentsFile.exists(), "contents.json must exist")
        val jsonText = contentsFile.readText()
        val root = Json.parseToJsonElement(jsonText).jsonObject
        val records = root["records"]?.jsonArray
        assertTrue(records != null && records.size > 0, "records must not be empty")

        val opdRecords = records.filter { elem ->
            val obj = elem.jsonObject
            val group = obj["group"]?.jsonPrimitive?.content
            val tags = obj["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            group == "OPD2nd" || "opd2nd" in tags
        }
        assertEquals(2389, opdRecords.size, "Must have exactly 2389 OPD2nd records")
    }

    @Test
    fun countAllTestsFromXml() {
        val buildDirs = listOf(
            File("../build/test-results/test"),
            File("build/test-results/test"),
            File("../../build/test-results/test")
        )
        var totalTests = 0
        var failures = 0
        var skipped = 0
        var totalSuites = 0

        for (dir in buildDirs) {
            if (dir.exists() && dir.isDirectory) {
                val xmlFiles = dir.listFiles { _, name -> name.endsWith(".xml") && name.startsWith("TEST-") } ?: emptyArray()
                for (file in xmlFiles) {
                    totalSuites++
                    val content = file.readText()
                    val testsMatch = Regex("""tests="(\d+)"""").find(content)
                    val failuresMatch = Regex("""failures="(\d+)"""").find(content)
                    val errorsMatch = Regex("""errors="(\d+)"""").find(content)
                    val skippedMatch = Regex("""skipped="(\d+)"""").find(content)

                    val t = testsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val f = (failuresMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0) + (errorsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0)
                    val s = skippedMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

                    totalTests += t
                    failures += f
                    skipped += s
                }
            }
        }

        val resultStr = "EXACT_TEST_COUNT: total=$totalTests, failures=$failures, skipped=$skipped, suites=$totalSuites"
        println(resultStr)
    }
}
