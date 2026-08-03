package vn.loi.learning.application.recall

import kotlin.test.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class RecallExecutionEngineTest {
    private val engine = RecallExecutionEngine()

    @Test fun `typing exact normalized incorrect and accepted alternative are authoritative`() {
        val exact = completed(execute(text("word")))
        assertEquals(RecallOutcome.CORRECT, exact.outcome); assertEquals(RecallCorrectness.EXACT, exact.correctness)
        val normalized = completed(execute(text("  WORD  ")))
        assertEquals(RecallOutcome.CORRECT, normalized.outcome); assertEquals(RecallCorrectness.NORMALIZED, normalized.correctness)
        assertEquals(RecallOutcome.INCORRECT, completed(execute(text("wrong"))).outcome)
        val alternativePlan = basePlan().copy(answerContract = basePlan().answerContract.copy(acceptedAlternatives = listOf("term")))
        assertEquals(RecallOutcome.CORRECT, completed(execute(text("TERM"), alternativePlan)).outcome)
    }

    @Test fun `blank typing is a typed rejection and live prefix semantics are not duplicated`() {
        assertEquals(RecallEvaluationFailure.BLANK_RESPONSE, assertIs<RecallExecutionResult.Rejected>(execute(text("   "))).reason)
        assertEquals(RecallOutcome.INCORRECT, completed(execute(text("wo"))).outcome)
    }

    @Test fun `unicode normalization is deterministic`() {
        val p = basePlan().copy(answerContract = basePlan().answerContract.copy(canonicalAnswer = "café"))
        val result = completed(execute(text("cafe\u0301"), p))
        assertEquals(RecallOutcome.CORRECT, result.outcome)
        assertEquals(RecallCorrectness.NORMALIZED, result.correctness)
    }

    @Test fun `multiple choice evaluates option identity only`() {
        val p = choicePlan()
        assertEquals(RecallOutcome.CORRECT, completed(execute(choice("correct"), p)).outcome)
        assertEquals(RecallOutcome.INCORRECT, completed(execute(choice("wrong"), p)).outcome)
        assertEquals(RecallEvaluationFailure.UNKNOWN_CHOICE, assertIs<RecallExecutionResult.Rejected>(execute(choice("unknown"), p)).reason)
    }

    @Test fun `listening image reverse example and dictation evaluate text contract`() {
        listOf(
            mediaPlan(RecallMode.LISTENING, RecallDirection.AUDIO_TO_TEXT),
            mediaPlan(RecallMode.IMAGE_RECALL, RecallDirection.IMAGE_TO_TEXT),
            textModePlan(RecallMode.REVERSE_TRANSLATION, RecallDirection.TARGET_TO_SOURCE),
            examplePlan(), mediaPlan(RecallMode.DICTATION, RecallDirection.AUDIO_TO_TEXT)
        ).forEach { p -> assertEquals(RecallOutcome.CORRECT, completed(execute(text("word", p.mode), p)).outcome) }
    }

    @Test fun `dictation punctuation and whitespace follow answer contract`() {
        val p = mediaPlan(RecallMode.DICTATION, RecallDirection.AUDIO_TO_TEXT).copy(answerContract =
            basePlan().answerContract.copy(canonicalAnswer = "Hello, world!", punctuationPolicy = PunctuationPolicy.IGNORE))
        assertEquals(RecallOutcome.CORRECT, completed(execute(text(" hello world ", RecallMode.DICTATION), p)).outcome)
    }

    @Test fun `playback acknowledgement never completes listening as correct`() {
        val p = mediaPlan(RecallMode.LISTENING, RecallDirection.AUDIO_TO_TEXT)
        val result = engine.execute(request(p, RecallSubmission.PlaybackAcknowledgement(context(mode = RecallMode.LISTENING))))
        assertEquals(RecallEvaluationFailure.PLAYBACK_INCOMPLETE, assertIs<RecallExecutionResult.Rejected>(result).reason)
    }

    @Test fun `reveal skip timeout are ineligible terminal outcomes`() {
        val p = basePlan().copy(availableAssistance = setOf(RecallAssistance.ANSWER_REVEALED))
        val values = listOf(
            RecallSubmission.Reveal(context()), RecallSubmission.Skip(context()), RecallSubmission.Timeout(context())
        )
        val expected = listOf(RecallOutcome.REVEALED, RecallOutcome.SKIPPED, RecallOutcome.TIMED_OUT)
        values.zip(expected).forEach { (submission, outcome) ->
            val result = completed(engine.execute(request(p, submission)))
            assertEquals(outcome, result.outcome); assertEquals(RecallEvidenceEligibility.INELIGIBLE, result.evidenceEligibility)
        }
    }

    @Test fun `practice correct remains evidence ineligible while evaluative stays typed`() {
        val practicePlan = basePlan().copy(provenance = RecallProvenance.PRACTICE, evidenceClass = RecallEvidenceEligibility.INELIGIBLE)
        val practice = completed(engine.execute(request(practicePlan, text("word"), RecallStrategyContext.PRACTICE_ONLY)))
        assertTrue(practice.correct); assertEquals(RecallEvidenceEligibility.INELIGIBLE, practice.evidenceEligibility)
        assertEquals(RecallEvidenceEligibility.STANDARD, completed(execute(text("word"))).evidenceEligibility)
    }

    @Test fun `multiple choice and light assistance downgrade evaluative eligibility`() {
        assertEquals(RecallEvidenceEligibility.WEAK, completed(execute(choice("correct"), choicePlan())).evidenceEligibility)
        val p = basePlan().copy(availableAssistance = setOf(RecallAssistance.EXAMPLE_VIEWED, RecallAssistance.PRONUNCIATION_HINT_USED))
        listOf(RecallAssistance.EXAMPLE_VIEWED, RecallAssistance.PRONUNCIATION_HINT_USED).forEach { assistance ->
            val submission = text("word", assistance = setOf(assistance))
            assertEquals(RecallEvidenceEligibility.WEAK, completed(execute(submission, p)).evidenceEligibility)
        }
    }

    @Test fun `unsupported assistance and contradictory reveal state are rejected`() {
        assertIs<RecallExecutionResult.InvalidSubmission>(execute(text("word", assistance = setOf(RecallAssistance.EXAMPLE_VIEWED))))
        assertIs<RecallExecutionResult.InvalidSubmission>(execute(text("word", assistance = setOf(RecallAssistance.ANSWER_REVEALED)),
            basePlan().copy(availableAssistance = setOf(RecallAssistance.ANSWER_REVEALED))))
        assertIs<RecallExecutionResult.InvalidSubmission>(execute(text("word", assistance = setOf(RecallAssistance.NONE, RecallAssistance.EXAMPLE_VIEWED)),
            basePlan().copy(availableAssistance = setOf(RecallAssistance.EXAMPLE_VIEWED))))
    }

    @Test fun `identity mode and submission kind mismatches are typed`() {
        assertIs<RecallExecutionResult.IdentityMismatch>(execute(text("word", contentId = ContentId("other"))))
        assertIs<RecallExecutionResult.InvalidSubmission>(execute(text("word", mode = RecallMode.DICTATION)))
        assertIs<RecallExecutionResult.InvalidSubmission>(execute(choice("x")))
    }

    @Test fun `context mismatch and unsupported versions are typed`() {
        assertIs<RecallExecutionResult.InvalidSubmission>(engine.execute(request(basePlan(), text("word"), RecallStrategyContext.PRACTICE_ONLY)))
        assertIs<RecallExecutionResult.UnsupportedContractVersion>(engine.execute(request(basePlan(), text("word")).copy(contractVersion = RecallContractVersion(99))))
    }

    @Test fun `duplicate attempt snapshot is immutable and prevents evaluation`() {
        val snapshot = RecallAttemptSnapshot(setOf(RecallAttemptId("attempt")))
        val result = engine.execute(request(basePlan(), text("word")).copy(attemptSnapshot = snapshot))
        assertEquals(RecallAttemptId("attempt"), assertIs<RecallExecutionResult.DuplicateAttempt>(result).attemptId)
        assertEquals(setOf(RecallAttemptId("attempt")), snapshot.knownAttemptIds)
    }

    @Test fun `negative latency is rejected and valid latency is deterministic`() {
        val early = text("word", submittedAt = Moment(0))
        assertIs<RecallExecutionResult.InvalidSubmission>(execute(early))
        val first = execute(text("word")); val second = execute(text("word"))
        assertEquals(first, second)
        assertEquals(TimeSpan(1_000), completed(first).latency)
    }

    @Test fun `registry detects duplicate and missing evaluator`() {
        val evaluator = object : RecallModeEvaluator {
            override val mode = RecallMode.TYPING
            override fun evaluate(plan: RecallPlan, submission: RecallSubmission) = error("not called")
        }
        assertEquals(listOf(RecallMode.TYPING), assertIs<RecallModeEvaluatorRegistryResult.DuplicateEvaluators>(
            RecallModeEvaluatorRegistry.create(listOf(evaluator, evaluator))).modes)
        val empty = assertIs<RecallModeEvaluatorRegistryResult.Created>(RecallModeEvaluatorRegistry.create(emptyList())).registry
        assertIs<RecallExecutionResult.UnsupportedMode>(RecallExecutionEngine(empty).execute(request(basePlan(), text("word"))))
    }

    @Test fun `result wire round trip preserves evaluation provenance`() {
        val result = completed(execute(text(" WORD ")))
        val encoded = RecallResultWireCodec.encode(result)
        assertEquals(result, assertIs<RecallResultDecodeResult.Success>(RecallResultWireCodec.decode(encoded)).result)
        assertIs<RecallResultDecodeResult.Malformed>(RecallResultWireCodec.decode("{}"))
    }

    @Test fun `platform kind is telemetry and does not affect correctness`() {
        val outcomes = RecallPlatformKind.entries.map { platform -> completed(execute(text("word", platform = platform))).outcome }.toSet()
        assertEquals(setOf(RecallOutcome.CORRECT), outcomes)
    }

    @Test fun `stable wire ids are ordinal independent`() {
        assertEquals("normalized", RecallCorrectness.NORMALIZED.wireId)
        assertEquals("duplicate-attempt", RecallRejectionReason.DUPLICATE_ATTEMPT.wireId)
        assertEquals("platform-submission", RecallExecutionProvenance.PLATFORM_SUBMISSION.wireId)
    }

    private fun execute(submission: RecallSubmission, p: RecallPlan = basePlan()) = engine.execute(request(p, submission))
    private fun completed(result: RecallExecutionResult) = assertIs<RecallExecutionResult.Completed>(result).result
    private fun request(p: RecallPlan, s: RecallSubmission, context: RecallStrategyContext = RecallStrategyContext.EVALUATIVE) =
        RecallExecutionRequest(p, s, evaluationContext = context)

    private fun basePlan() = plan(generatedAt = Moment(1_000)).copy(availableAssistance = setOf(RecallAssistance.NONE))
    private fun choicePlan() = basePlan().copy(
        mode = RecallMode.MULTIPLE_CHOICE, prompt = RecallPrompt.MultipleChoice("meaning", listOf(
            RecallChoice("correct", "word", true), RecallChoice("wrong", "other", false))),
        answerContract = basePlan().answerContract.copy(kind = RecallAnswerKind.CHOICE),
        platformRequirements = RecallPlatformRequirements(requiresChoiceSelection = true)
    )
    private fun mediaPlan(mode: RecallMode, direction: RecallDirection): RecallPlan {
        val resource = RecallResourceId("asset")
        val prompt = when (mode) {
            RecallMode.LISTENING -> RecallPrompt.Listening(resource)
            RecallMode.IMAGE_RECALL -> RecallPrompt.ImageRecall(resource)
            RecallMode.DICTATION -> RecallPrompt.Dictation(resource)
            else -> error("unsupported")
        }
        val capability = if (mode == RecallMode.IMAGE_RECALL) RecallCapability.IMAGE else RecallCapability.WORD_AUDIO
        return basePlan().copy(mode = mode, direction = direction, prompt = prompt,
            platformRequirements = RecallPlatformRequirements(requiresTextInput = true,
                requiresAudioPlayback = mode != RecallMode.IMAGE_RECALL, requiresImageRendering = mode == RecallMode.IMAGE_RECALL),
            contentCapabilities = RecallContentCapabilities(ContentId("content"), setOf(RecallCapability.SOURCE_TEXT, capability),
                image = resource.takeIf { mode == RecallMode.IMAGE_RECALL }, wordAudio = resource.takeIf { mode != RecallMode.IMAGE_RECALL }))
    }
    private fun textModePlan(mode: RecallMode, direction: RecallDirection) = basePlan().copy(mode = mode, direction = direction,
        prompt = RecallPrompt.ReverseTranslation("translation"),
        contentCapabilities = RecallContentCapabilities(ContentId("content"), setOf(RecallCapability.SOURCE_TEXT, RecallCapability.TARGET_TRANSLATION)))
    private fun examplePlan() = basePlan().copy(mode = RecallMode.EXAMPLE_COMPLETION, direction = RecallDirection.CONTEXT_TO_TEXT,
        prompt = RecallPrompt.ExampleCompletion("A ____.", RecallTextSpan(2, 6)),
        platformRequirements = RecallPlatformRequirements(requiresTextInput = true, requiresExampleRendering = true),
        contentCapabilities = RecallContentCapabilities(ContentId("content"), setOf(RecallCapability.SOURCE_TEXT, RecallCapability.EXAMPLE_SOURCE)))

    private fun context(
        mode: RecallMode = RecallMode.TYPING, submittedAt: Moment = Moment(2_000),
        contentId: ContentId = ContentId("content"), assistance: Set<RecallAssistance> = setOf(RecallAssistance.NONE),
        platform: RecallPlatformKind = RecallPlatformKind.WEB
    ) = RecallSubmissionContext(RecallPlanId("plan"), RecallAttemptId("attempt"), LearnerId("learner"), contentId,
        SessionId("session"), mode, submittedAt, assistance, platform)
    private fun text(
        value: String, mode: RecallMode = RecallMode.TYPING, submittedAt: Moment = Moment(2_000),
        contentId: ContentId = ContentId("content"), assistance: Set<RecallAssistance> = setOf(RecallAssistance.NONE),
        platform: RecallPlatformKind = RecallPlatformKind.WEB
    ) = RecallSubmission.TypedText(context(mode, submittedAt, contentId, assistance, platform), value)
    private fun choice(id: String) = RecallSubmission.Choice(context(mode = RecallMode.MULTIPLE_CHOICE), id)
}
