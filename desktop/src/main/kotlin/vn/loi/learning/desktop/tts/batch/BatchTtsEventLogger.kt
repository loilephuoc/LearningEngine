package vn.loi.learning.desktop.tts.batch

import vn.loi.learning.desktop.runtime.DesktopLogLevel
import vn.loi.learning.desktop.runtime.DesktopRuntimeLogger

/**
 * Structured diagnostic logger for Batch TTS operations.
 *
 * Invariant: Never logs text payloads, translations, examples, passwords, or credentials.
 * All messages contain only identifiers, counts, statuses, and sanitized reasons.
 */
interface BatchTtsEventLogger {
    fun logPlanCreated(
        planId: String,
        packageId: String,
        targetCount: Int,
        overwriteMode: Boolean,
        selectedFields: String,
        languageRequirements: String
    )

    fun logLeaseAcquireAttempt(
        planId: String,
        ownerPid: Long,
        ownerToken: String,
        existingOwnerPid: Long?,
        existingOwnerAlive: Boolean?,
        decision: String
    )

    fun logLeaseReclaimed(
        planId: String,
        deadOwnerPid: Long,
        leaseAgeMillis: Long
    )

    fun logResume(
        planId: String,
        successCount: Int,
        failedCount: Int,
        skippedCount: Int,
        pendingCount: Int
    )

    fun logTargetStart(
        planId: String,
        targetIndex: Int,
        jobId: String,
        contentId: String,
        field: String,
        language: String
    )

    fun logAttemptStart(
        targetIndex: Int,
        voiceId: String,
        attemptNumber: Int,
        candidateIndex: Int
    )

    fun logAttemptTimeout(
        targetIndex: Int,
        voiceId: String,
        attemptNumber: Int,
        candidateIndex: Int,
        timeoutMillis: Long
    )

    fun logAttemptFailure(
        targetIndex: Int,
        voiceId: String,
        attemptNumber: Int,
        candidateIndex: Int,
        errorCategory: String,
        reason: String
    )

    fun logAttemptClosed(
        targetIndex: Int,
        voiceId: String,
        attemptNumber: Int,
        candidateIndex: Int,
        details: String
    )

    fun logFallback(
        targetIndex: Int,
        fromVoiceId: String,
        toVoiceId: String,
        candidateIndex: Int
    )

    fun logTargetSuccess(
        planId: String,
        targetIndex: Int,
        jobId: String,
        voiceId: String,
        assetPath: String,
        recoveredViaFallback: Boolean
    )

    fun logTargetFailed(
        planId: String,
        targetIndex: Int,
        jobId: String,
        errorCategory: String,
        attemptsCount: Int
    )

    fun logTargetSkipped(
        planId: String,
        targetIndex: Int,
        jobId: String,
        reason: String
    )

    fun logTargetCancelled(
        planId: String,
        targetIndex: Int,
        jobId: String
    )

    fun logCheckpointWritten(
        planId: String,
        targetIndex: Int,
        recordsCount: Int
    )

    fun logBatchCompleted(
        planId: String,
        totalCount: Int,
        successCount: Int,
        failedCount: Int,
        skippedCount: Int,
        cancelledCount: Int
    )

    fun logBatchCancelled(
        planId: String,
        completedCount: Int,
        cancelledCount: Int
    )

    fun logBatchFatal(
        planId: String,
        reason: String
    )

    object NoOp : BatchTtsEventLogger {
        override fun logPlanCreated(planId: String, packageId: String, targetCount: Int, overwriteMode: Boolean, selectedFields: String, languageRequirements: String) {}
        override fun logLeaseAcquireAttempt(planId: String, ownerPid: Long, ownerToken: String, existingOwnerPid: Long?, existingOwnerAlive: Boolean?, decision: String) {}
        override fun logLeaseReclaimed(planId: String, deadOwnerPid: Long, leaseAgeMillis: Long) {}
        override fun logResume(planId: String, successCount: Int, failedCount: Int, skippedCount: Int, pendingCount: Int) {}
        override fun logTargetStart(planId: String, targetIndex: Int, jobId: String, contentId: String, field: String, language: String) {}
        override fun logAttemptStart(targetIndex: Int, voiceId: String, attemptNumber: Int, candidateIndex: Int) {}
        override fun logAttemptTimeout(targetIndex: Int, voiceId: String, attemptNumber: Int, candidateIndex: Int, timeoutMillis: Long) {}
        override fun logAttemptFailure(targetIndex: Int, voiceId: String, attemptNumber: Int, candidateIndex: Int, errorCategory: String, reason: String) {}
        override fun logAttemptClosed(targetIndex: Int, voiceId: String, attemptNumber: Int, candidateIndex: Int, details: String) {}
        override fun logFallback(targetIndex: Int, fromVoiceId: String, toVoiceId: String, candidateIndex: Int) {}
        override fun logTargetSuccess(planId: String, targetIndex: Int, jobId: String, voiceId: String, assetPath: String, recoveredViaFallback: Boolean) {}
        override fun logTargetFailed(planId: String, targetIndex: Int, jobId: String, errorCategory: String, attemptsCount: Int) {}
        override fun logTargetSkipped(planId: String, targetIndex: Int, jobId: String, reason: String) {}
        override fun logTargetCancelled(planId: String, targetIndex: Int, jobId: String) {}
        override fun logCheckpointWritten(planId: String, targetIndex: Int, recordsCount: Int) {}
        override fun logBatchCompleted(planId: String, totalCount: Int, successCount: Int, failedCount: Int, skippedCount: Int, cancelledCount: Int) {}
        override fun logBatchCancelled(planId: String, completedCount: Int, cancelledCount: Int) {}
        override fun logBatchFatal(planId: String, reason: String) {}
    }
}

class RuntimeBatchTtsEventLogger(
    private val runtimeLogger: DesktopRuntimeLogger
) : BatchTtsEventLogger {

    private fun log(level: DesktopLogLevel, eventCode: String, vararg params: Pair<String, Any?>) {
        val payload = params.joinToString(" ") { (key, value) ->
            "$key=${sanitize(value?.toString().orEmpty())}"
        }
        runCatching {
            runtimeLogger.log(level, eventCode, payload)
        }
    }

    override fun logPlanCreated(
        planId: String,
        packageId: String,
        targetCount: Int,
        overwriteMode: Boolean,
        selectedFields: String,
        languageRequirements: String
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_PLAN_CREATED",
            "planId" to planId,
            "packageId" to packageId,
            "targetCount" to targetCount,
            "overwriteMode" to overwriteMode,
            "selectedFields" to selectedFields,
            "languageRequirements" to languageRequirements
        )
    }

    override fun logLeaseAcquireAttempt(
        planId: String,
        ownerPid: Long,
        ownerToken: String,
        existingOwnerPid: Long?,
        existingOwnerAlive: Boolean?,
        decision: String
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_LEASE_ACQUIRE_ATTEMPT",
            "planId" to planId,
            "ownerPid" to ownerPid,
            "ownerToken" to ownerToken,
            "existingOwnerPid" to existingOwnerPid,
            "existingOwnerAlive" to existingOwnerAlive,
            "decision" to decision
        )
    }

    override fun logLeaseReclaimed(
        planId: String,
        deadOwnerPid: Long,
        leaseAgeMillis: Long
    ) {
        log(
            DesktopLogLevel.WARN,
            "BATCH_TTS_LEASE_RECLAIMED",
            "planId" to planId,
            "deadOwnerPid" to deadOwnerPid,
            "leaseAgeMillis" to leaseAgeMillis
        )
    }

    override fun logResume(
        planId: String,
        successCount: Int,
        failedCount: Int,
        skippedCount: Int,
        pendingCount: Int
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_RESUME",
            "planId" to planId,
            "successCount" to successCount,
            "failedCount" to failedCount,
            "skippedCount" to skippedCount,
            "pendingCount" to pendingCount
        )
    }

    override fun logTargetStart(
        planId: String,
        targetIndex: Int,
        jobId: String,
        contentId: String,
        field: String,
        language: String
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_TARGET_START",
            "planId" to planId,
            "targetIndex" to targetIndex,
            "jobId" to jobId,
            "contentId" to contentId,
            "field" to field,
            "language" to language
        )
    }

    override fun logAttemptStart(
        targetIndex: Int,
        voiceId: String,
        attemptNumber: Int,
        candidateIndex: Int
    ) {
        log(
            DesktopLogLevel.DEBUG,
            "BATCH_TTS_ATTEMPT_START",
            "targetIndex" to targetIndex,
            "voiceId" to voiceId,
            "attemptNumber" to attemptNumber,
            "candidateIndex" to candidateIndex
        )
    }

    override fun logAttemptTimeout(
        targetIndex: Int,
        voiceId: String,
        attemptNumber: Int,
        candidateIndex: Int,
        timeoutMillis: Long
    ) {
        log(
            DesktopLogLevel.WARN,
            "BATCH_TTS_ATTEMPT_TIMEOUT",
            "targetIndex" to targetIndex,
            "voiceId" to voiceId,
            "attemptNumber" to attemptNumber,
            "candidateIndex" to candidateIndex,
            "timeoutMillis" to timeoutMillis
        )
    }

    override fun logAttemptFailure(
        targetIndex: Int,
        voiceId: String,
        attemptNumber: Int,
        candidateIndex: Int,
        errorCategory: String,
        reason: String
    ) {
        log(
            DesktopLogLevel.WARN,
            "BATCH_TTS_ATTEMPT_FAILURE",
            "targetIndex" to targetIndex,
            "voiceId" to voiceId,
            "attemptNumber" to attemptNumber,
            "candidateIndex" to candidateIndex,
            "errorCategory" to errorCategory,
            "reason" to reason
        )
    }

    override fun logAttemptClosed(
        targetIndex: Int,
        voiceId: String,
        attemptNumber: Int,
        candidateIndex: Int,
        details: String
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_ATTEMPT_CLOSED",
            "targetIndex" to targetIndex,
            "voiceId" to voiceId,
            "attemptNumber" to attemptNumber,
            "candidateIndex" to candidateIndex,
            "details" to details
        )
    }

    override fun logFallback(
        targetIndex: Int,
        fromVoiceId: String,
        toVoiceId: String,
        candidateIndex: Int
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_FALLBACK",
            "targetIndex" to targetIndex,
            "fromVoiceId" to fromVoiceId,
            "toVoiceId" to toVoiceId,
            "candidateIndex" to candidateIndex
        )
    }

    override fun logTargetSuccess(
        planId: String,
        targetIndex: Int,
        jobId: String,
        voiceId: String,
        assetPath: String,
        recoveredViaFallback: Boolean
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_TARGET_SUCCESS",
            "planId" to planId,
            "targetIndex" to targetIndex,
            "jobId" to jobId,
            "voiceId" to voiceId,
            "assetPath" to assetPath,
            "recoveredViaFallback" to recoveredViaFallback
        )
    }

    override fun logTargetFailed(
        planId: String,
        targetIndex: Int,
        jobId: String,
        errorCategory: String,
        attemptsCount: Int
    ) {
        log(
            DesktopLogLevel.ERROR,
            "BATCH_TTS_TARGET_FAILED",
            "planId" to planId,
            "targetIndex" to targetIndex,
            "jobId" to jobId,
            "errorCategory" to errorCategory,
            "attemptsCount" to attemptsCount
        )
    }

    override fun logTargetSkipped(
        planId: String,
        targetIndex: Int,
        jobId: String,
        reason: String
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_TARGET_SKIPPED",
            "planId" to planId,
            "targetIndex" to targetIndex,
            "jobId" to jobId,
            "reason" to reason
        )
    }

    override fun logTargetCancelled(
        planId: String,
        targetIndex: Int,
        jobId: String
    ) {
        log(
            DesktopLogLevel.WARN,
            "BATCH_TTS_TARGET_CANCELLED",
            "planId" to planId,
            "targetIndex" to targetIndex,
            "jobId" to jobId
        )
    }

    override fun logCheckpointWritten(
        planId: String,
        targetIndex: Int,
        recordsCount: Int
    ) {
        log(
            DesktopLogLevel.DEBUG,
            "BATCH_TTS_CHECKPOINT_WRITTEN",
            "planId" to planId,
            "targetIndex" to targetIndex,
            "recordsCount" to recordsCount
        )
    }

    override fun logBatchCompleted(
        planId: String,
        totalCount: Int,
        successCount: Int,
        failedCount: Int,
        skippedCount: Int,
        cancelledCount: Int
    ) {
        log(
            DesktopLogLevel.INFO,
            "BATCH_TTS_BATCH_COMPLETED",
            "planId" to planId,
            "totalCount" to totalCount,
            "successCount" to successCount,
            "failedCount" to failedCount,
            "skippedCount" to skippedCount,
            "cancelledCount" to cancelledCount
        )
    }

    override fun logBatchCancelled(
        planId: String,
        completedCount: Int,
        cancelledCount: Int
    ) {
        log(
            DesktopLogLevel.WARN,
            "BATCH_TTS_BATCH_CANCELLED",
            "planId" to planId,
            "completedCount" to completedCount,
            "cancelledCount" to cancelledCount
        )
    }

    override fun logBatchFatal(
        planId: String,
        reason: String
    ) {
        log(
            DesktopLogLevel.ERROR,
            "BATCH_TTS_BATCH_FATAL",
            "planId" to planId,
            "reason" to reason
        )
    }

    private fun sanitize(value: String): String =
        value.replace("\r", " ")
            .replace("\n", " ")
            .replace("\t", " ")
            .trim()
}
