package vn.loi.learning.application.study

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.session.model.SessionId

/** Reorders only NEW candidates using a stable seed owned by the study session. */
class SessionSeededNewItemOrderer {

    fun order(
        candidates: List<SelectionCandidate>,
        sessionId: SessionId
    ): List<SelectionCandidate> {
        val sessionSeed = sha256(sessionId.value.toByteArray(StandardCharsets.UTF_8))
        val randomizedNewCandidates = candidates
            .filter(SelectionCandidate::isNew)
            .sortedWith(
                compareBy<SelectionCandidate> {
                    sha256(
                        sessionSeed +
                            it.learningItemId.value.toByteArray(StandardCharsets.UTF_8)
                    ).toHex()
                }.thenBy { it.learningItemId.value }
            )
            .iterator()

        return candidates.map { candidate ->
            if (candidate.isNew) randomizedNewCandidates.next() else candidate
        }
    }

    private fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
}
