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
        // Compute each deterministic key once. The previous comparator recalculated
        // SHA-256 and hex formatting O(n log n) times during sort, creating heavy
        // allocation pressure for real packages with hundreds of NEW items.
        val randomizedNewCandidates = candidates
            .asSequence()
            .filter(SelectionCandidate::isNew)
            .map { candidate ->
                candidate to sha256(
                    sessionSeed + candidate.learningItemId.value.toByteArray(StandardCharsets.UTF_8)
                ).toHex()
            }
            .sortedWith(compareBy<Pair<SelectionCandidate, String>> { it.second }
                .thenBy { it.first.learningItemId.value })
            .map { it.first }
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
