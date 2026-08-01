package vn.loi.learning.domain.study.session.model

/** Durable authority describing why a session entered FINISHED. */
enum class SessionCompletionProvenance {
    UNKNOWN,
    ORDINARY_SUCCESS,
    RECOVERY_RECONCILIATION,
    REPLACED_OR_LEFT
}
