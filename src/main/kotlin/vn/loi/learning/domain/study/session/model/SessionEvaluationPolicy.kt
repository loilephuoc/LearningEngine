package vn.loi.learning.domain.study.session.model

/**
 * Declares whether a session may mutate durable learning evaluation state.
 *
 * Platform clients only dispatch intents. Shared application boundaries use this policy as the
 * authority before entering review, scheduler, memory, or rating transactions.
 */
enum class SessionEvaluationPolicy {
    EVALUATIVE,
    PRACTICE_ONLY
}
