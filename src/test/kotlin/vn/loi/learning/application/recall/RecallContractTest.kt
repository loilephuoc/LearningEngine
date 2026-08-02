package vn.loi.learning.application.recall

import kotlin.test.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class RecallContractTest {
    @Test fun `every recall mode has unique stable non-ordinal wire id`() {
        assertEquals(RecallMode.entries.size, RecallMode.entries.map { it.wireId }.toSet().size)
        RecallMode.entries.forEach { assertFalse(it.wireId == it.ordinal.toString()) }
    }

    @Test fun `directions and portable enums expose stable wire ids`() {
        val values: List<StableWireValue> = RecallDirection.entries + RecallOutcome.entries +
            RecallAssistance.entries + RecallCapability.entries + RecallPlatformKind.entries
        assertTrue(values.all { it.wireId.isNotBlank() })
    }

    @Test fun `valid typing plan passes pure validation`() {
        assertEquals(RecallPlanValidationResult.Valid, RecallContractValidator.validatePlan(plan()))
    }

    @Test fun `listening without audio capability is invalid`() {
        val value = plan().copy(mode = RecallMode.LISTENING, prompt = RecallPrompt.Listening(RecallResourceId("audio")))
        assertInvalid(value, RecallRejectionReason.MISSING_CAPABILITY)
    }

    @Test fun `image recall without image capability is invalid`() {
        val value = plan().copy(mode = RecallMode.IMAGE_RECALL, prompt = RecallPrompt.ImageRecall(RecallResourceId("image")))
        assertInvalid(value, RecallRejectionReason.MISSING_CAPABILITY)
    }

    @Test fun `multiple choice requires exactly one correct choice`() {
        listOf(0, 2).forEach { correctCount ->
            val choices = listOf(
                RecallChoice("a", "A", correctCount >= 1), RecallChoice("b", "B", correctCount == 2)
            )
            assertInvalid(
                plan().copy(mode = RecallMode.MULTIPLE_CHOICE, prompt = RecallPrompt.MultipleChoice("Q", choices)),
                RecallRejectionReason.INVALID_PROMPT
            )
        }
    }

    @Test fun `example completion without example capability is invalid`() {
        assertInvalid(
            plan().copy(
                mode = RecallMode.EXAMPLE_COMPLETION,
                prompt = RecallPrompt.ExampleCompletion("word here", RecallTextSpan(0, 4))
            ), RecallRejectionReason.MISSING_CAPABILITY
        )
    }

    @Test fun `submission mode mismatch is typed rejection`() {
        val rejected = RecallContractValidator.validateSubmission(
            plan(), RecallSubmission.TypedText(context(mode = RecallMode.DICTATION), "word")
        ) as RecallSubmissionValidationResult.Rejected
        assertContains(rejected.reasons, RecallRejectionReason.MODE_MISMATCH)
    }

    @Test fun `submission identity mismatch is typed rejection`() {
        val rejected = RecallContractValidator.validateSubmission(
            plan(), RecallSubmission.TypedText(context(contentId = ContentId("other")), "word")
        ) as RecallSubmissionValidationResult.Rejected
        assertContains(rejected.reasons, RecallRejectionReason.IDENTITY_MISMATCH)
    }

    @Test fun `duplicate attempt id is detected without mutation`() {
        val attempt = RecallAttemptId("attempt")
        val existing = setOf(attempt)
        val rejected = RecallContractValidator.validateSubmission(
            plan(), RecallSubmission.TypedText(context(attemptId = attempt), "word"), existing
        ) as RecallSubmissionValidationResult.Rejected
        assertContains(rejected.reasons, RecallRejectionReason.DUPLICATE_ATTEMPT)
        assertEquals(setOf(attempt), existing)
    }

    @Test fun `assistance state is typed immutable value`() {
        val assistance = setOf(RecallAssistance.EXAMPLE_VIEWED, RecallAssistance.PRONUNCIATION_HINT_USED)
        assertEquals(assistance, context(assistance = assistance).assistanceState)
    }

    @Test fun `reveal is ineligible evidence`() {
        assertEquals(
            RecallEvidenceEligibility.INELIGIBLE,
            RecallEvidenceEligibilityPolicy.resolve(
                RecallOutcome.REVEALED, setOf(RecallAssistance.ANSWER_REVEALED), RecallProvenance.EVALUATIVE
            )
        )
    }

    @Test fun `practice and manual provenance are not evaluative evidence`() {
        listOf(RecallProvenance.PRACTICE, RecallProvenance.MANUAL).forEach {
            assertEquals(
                RecallEvidenceEligibility.INELIGIBLE,
                RecallEvidenceEligibilityPolicy.resolve(RecallOutcome.CORRECT, setOf(RecallAssistance.NONE), it)
            )
        }
    }

    @Test fun `fake clock is deterministic`() {
        val clock = RecallClock { Moment(42) }
        assertEquals(Moment(42), clock.now())
        assertEquals(clock.now(), clock.now())
    }

    @Test fun `choice submission is rejected for typing plan`() {
        val rejected = RecallContractValidator.validateSubmission(
            plan(), RecallSubmission.Choice(context(), "choice")
        ) as RecallSubmissionValidationResult.Rejected
        assertContains(rejected.reasons, RecallRejectionReason.INVALID_SUBMISSION_KIND)
    }

    @Test fun `answer contract preserves explicit normalization semantics`() {
        val answer = plan().answerContract
        assertEquals(RecallNormalizationPolicyId("typing-v1"), answer.normalizationPolicy)
        assertEquals(CaseSensitivity.INSENSITIVE, answer.caseSensitivity)
        assertEquals(PunctuationPolicy.EXACT, answer.punctuationPolicy)
        assertEquals(WhitespacePolicy.NORMALIZE, answer.whitespacePolicy)
        assertTrue(answer.acceptedAlternatives.isEmpty())
    }

    @Test fun `normalized result is a passive typed value`() {
        val result = RecallResult(
            RecallContractVersion.CURRENT, RecallPlanId("plan"), RecallAttemptId("attempt"),
            LearnerId("learner"), ContentId("content"), RecallMode.TYPING,
            RecallDirection.SOURCE_TO_TARGET, RecallOutcome.CORRECT, true, "word", TimeSpan(120),
            setOf(RecallAssistance.NONE), false, 0, RecallProvenance.EVALUATIVE,
            RecallEvidenceEligibility.STANDARD, null, Moment(200)
        )
        assertEquals(RecallOutcome.CORRECT, result.outcome)
        assertEquals(RecallEvidenceEligibility.STANDARD, result.evidenceEligibility)
    }

    private fun assertInvalid(value: RecallPlan, reason: RecallRejectionReason) {
        val invalid = RecallContractValidator.validatePlan(value) as RecallPlanValidationResult.Invalid
        assertContains(invalid.reasons, reason)
    }

    private fun context(
        mode: RecallMode = RecallMode.TYPING,
        contentId: ContentId = ContentId("content"),
        attemptId: RecallAttemptId = RecallAttemptId("attempt"),
        assistance: Set<RecallAssistance> = setOf(RecallAssistance.NONE)
    ) = RecallSubmissionContext(
        RecallPlanId("plan"), attemptId, LearnerId("learner"), contentId, SessionId("session"),
        mode, Moment(2), assistance, RecallPlatformKind.WEB
    )
}

internal fun plan(
    seed: Long = 7,
    generatedAt: Moment = Moment(1),
    contentId: ContentId = ContentId("content"),
    learningItemId: LearningItemId? = LearningItemId("item")
) = RecallPlan(
    planId = RecallPlanId("plan"), learnerId = LearnerId("learner"), contentId = contentId,
    learningItemId = learningItemId, sessionId = SessionId("session"), mode = RecallMode.TYPING,
    direction = RecallDirection.SOURCE_TO_TARGET, prompt = RecallPrompt.Typing("nghĩa"),
    answerContract = RecallAnswerContract(
        "word", emptyList(), RecallNormalizationPolicyId("typing-v1"),
        CaseSensitivity.INSENSITIVE, PunctuationPolicy.EXACT, WhitespacePolicy.NORMALIZE,
        RecallLanguageTag("en"), RecallAnswerKind.TEXT
    ),
    availableAssistance = setOf(RecallAssistance.NONE),
    evidenceClass = RecallEvidenceEligibility.STANDARD, deterministicSeed = RecallDeterministicSeed(seed),
    generatedAt = generatedAt, provenance = RecallProvenance.EVALUATIVE,
    platformRequirements = RecallPlatformRequirements(requiresTextInput = true),
    contentCapabilities = RecallContentCapabilities(contentId, setOf(RecallCapability.SOURCE_TEXT))
)
