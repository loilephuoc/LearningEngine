package vn.loi.learning.android.recording

import android.content.Context
import android.media.MediaRecorder
import android.view.KeyEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.loi.learning.android.controller.ControllerAction
import vn.loi.learning.android.controller.ControllerActionDispatcher
import vn.loi.learning.android.controller.ControllerActionResult
import vn.loi.learning.android.controller.ControllerConfig
import vn.loi.learning.android.controller.ControllerContext
import vn.loi.learning.android.controller.ControllerDiagnosticsHolder
import vn.loi.learning.android.controller.ControllerGesture
import vn.loi.learning.android.controller.ControllerMapping
import vn.loi.learning.android.controller.ControllerMappingResolver
import vn.loi.learning.android.controller.ControllerPhysicalInput
import vn.loi.learning.android.controller.ControllerPressType
import vn.loi.learning.android.controller.ControllerProfile
import vn.loi.learning.android.controller.StudyAudioReason
import vn.loi.learning.android.controller.StudyControllerBridge
import vn.loi.learning.android.media.AndroidAudioPlaybackEvent
import vn.loi.learning.android.media.AndroidAudioState
import vn.loi.learning.android.media.LearningEngineAudioPolicy
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class QuickVoiceRecorderTest {

    private lateinit var tempDir: File
    private lateinit var repository: QuickVoiceRecordingRepository

    private class FakeMediaRecorder : MediaRecorder() {
        var isPrepared = false
        var isStarted = false
        var isStopped = false
        var isReleased = false
        var outputPath: String? = null
        var throwOnStop = false

        override fun setAudioSource(audio_source: Int) {}
        override fun setOutputFormat(output_format: Int) {}
        override fun setAudioEncoder(audio_encoder: Int) {}
        override fun setAudioEncodingBitRate(bitRate: Int) {}
        override fun setAudioSamplingRate(samplingRate: Int) {}
        override fun setOutputFile(path: String?) {
            outputPath = path
        }

        override fun prepare() {
            isPrepared = true
        }

        override fun start() {
            if (!isPrepared) throw IllegalStateException("start called before prepare")
            isStarted = true
            outputPath?.let { path ->
                val f = File(path)
                f.parentFile?.mkdirs()
                f.writeBytes(ByteArray(1024) { 0x42 })
            }
        }

        override fun stop() {
            if (throwOnStop) throw RuntimeException("stop failed: audio file too short")
            isStopped = true
        }

        override fun release() {
            isReleased = true
        }
    }

    private class FakeAudioController : vn.loi.learning.android.media.AndroidAudioController() {
        var replayPath: String? = null
        var isPlaying = false

        override fun replay(
            path: String?,
            isLooping: Boolean,
            onPlaybackEvent: (AndroidAudioPlaybackEvent) -> Unit,
            onState: (AndroidAudioState) -> Unit
        ): AndroidAudioState {
            replayPath = path
            isPlaying = true
            onState(AndroidAudioState.Playing)
            return AndroidAudioState.Playing
        }

        override fun stop() {
            isPlaying = false
        }
    }

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("quick_voice_test").toFile()
        repository = QuickVoiceRecordingRepository.forTesting(tempDir)
        QuickVoiceRecorderController.resetForTesting(repository)
        ControllerDiagnosticsHolder.clear()
        ControllerDiagnosticsHolder.setForeground(true)
        LearningEngineAudioPolicy.resetForTesting(false)
    }

    private fun createFakeContext(): Context {
        return object : android.content.ContextWrapper(null) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
                return android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            override fun getFilesDir(): File = tempDir
        }
    }

    @Test
    fun `foreground first broken stage test - resolves D-Pad Left mapped Everywhere in foreground`() = runTest {
        val dpadLeftInput = ControllerPhysicalInput(vendorId = 0x2dc8, productId = 0x9018, keyCode = KeyEvent.KEYCODE_DPAD_LEFT)
        val gesture = ControllerGesture(dpadLeftInput, ControllerPressType.PRESS)

        val mapping = ControllerMapping(
            context = ControllerContext.GLOBAL,
            gesture = gesture,
            action = ControllerAction.TOGGLE_VOICE_RECORDING
        )
        val profile = ControllerProfile(
            id = "test_profile",
            name = "Test Profile",
            mappings = listOf(mapping)
        )
        val config = ControllerConfig(
            isControllerEnabled = true,
            activeProfileId = "test_profile",
            profiles = listOf(profile)
        )

        // Test in STUDY_QUESTION context
        val resolvedAction = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, gesture)
        assertEquals(ControllerAction.TOGGLE_VOICE_RECORDING, resolvedAction)

        // Also test in pure GLOBAL context
        val resolvedInGlobal = ControllerMappingResolver.resolve(config, ControllerContext.GLOBAL, gesture)
        assertEquals(ControllerAction.TOGGLE_VOICE_RECORDING, resolvedInGlobal)
    }

    @Test
    fun `permission tests - foreground permission missing triggers permission request`() = runTest {
        var permissionRequested = false
        QuickVoicePermissionBridge.register {
            permissionRequested = true
        }

        val fakeContext = object : android.content.ContextWrapper(null) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
                return android.content.pm.PackageManager.PERMISSION_DENIED
            }
            override fun getFilesDir(): File = tempDir
        }

        val result = QuickVoiceRecorderController.startRecording(fakeContext)
        assertTrue(result is QuickVoiceOperationResult.PermissionRequired)
        assertTrue(permissionRequested)

        QuickVoicePermissionBridge.unregister()
    }

    @Test
    fun `permission tests - permission granted requires explicit second record press to start`() = runTest {
        var permissionRequested = false
        QuickVoicePermissionBridge.register {
            permissionRequested = true
        }

        val fakeContext = object : android.content.ContextWrapper(null) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
                return android.content.pm.PackageManager.PERMISSION_DENIED
            }
            override fun getFilesDir(): File = tempDir
        }

        // 1. Initial record press when missing permission -> requests dialog, does not start service
        val firstResult = QuickVoiceRecorderController.startRecording(fakeContext)
        assertTrue(firstResult is QuickVoiceOperationResult.PermissionRequired)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Idle)

        // 2. Permission grant callback
        QuickVoiceRecorderController.onPermissionResult(true)

        // State remains Idle - DOES NOT auto-record unexpectedly
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Idle)

        QuickVoicePermissionBridge.unregister()
    }

    @Test
    fun `permission tests - background permission missing does not trigger UI launcher`() = runTest {
        ControllerDiagnosticsHolder.setForeground(false)
        var permissionRequested = false
        QuickVoicePermissionBridge.register {
            permissionRequested = true
        }

        val fakeContext = object : android.content.ContextWrapper(null) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
                return android.content.pm.PackageManager.PERMISSION_DENIED
            }
            override fun getFilesDir(): File = tempDir
        }

        val result = QuickVoiceRecorderController.startRecording(fakeContext)
        assertTrue(result is QuickVoiceOperationResult.PermissionRequired)
        assertFalse(permissionRequested)

        QuickVoicePermissionBridge.unregister()
    }

    @Test
    fun `asynchronous start lifecycle - state remains Starting before service confirms`() = runTest {
        var requestedSessionId: RecordingSessionId? = null
        var requestedFile: File? = null
        var requestedTime: Long = 0L

        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, startTime ->
            requestedSessionId = sessionId
            requestedFile = file
            requestedTime = startTime
        }

        val fakeContext = createFakeContext()

        // 1. User starts recording -> state is Starting
        val startResult = QuickVoiceRecorderController.startRecording(fakeContext)
        assertTrue(startResult is QuickVoiceOperationResult.Starting)
        val session = (startResult as QuickVoiceOperationResult.Starting).sessionId
        assertEquals(session, requestedSessionId)

        // State must be Starting, NOT Recording yet!
        val currentState = QuickVoiceRecorderController.state.value
        assertTrue(currentState is QuickVoiceRecorderState.Starting)
        assertEquals(session, (currentState as QuickVoiceRecorderState.Starting).sessionId)

        // 2. Service confirms recording started
        QuickVoiceRecorderController.onServiceRecordingStarted(session, requestedFile!!, requestedTime)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Recording)
    }

    @Test
    fun `asynchronous start lifecycle - failure transitions to Error`() = runTest {
        var requestedSessionId: RecordingSessionId? = null
        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, _, _ ->
            requestedSessionId = sessionId
        }

        val fakeContext = createFakeContext()
        val startResult = QuickVoiceRecorderController.startRecording(fakeContext)
        assertTrue(startResult is QuickVoiceOperationResult.Starting)

        // Service reports failure
        QuickVoiceRecorderController.onServiceRecordingFailed(requestedSessionId!!, "Audio hardware busy")

        // Wait for state transition deterministically
        waitUntil { QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Error }
        assertEquals("Audio hardware busy", (QuickVoiceRecorderController.state.value as QuickVoiceRecorderState.Error).message)
    }

    @Test
    fun `asynchronous stop lifecycle - no premature save before service completes`() = runTest {
        var serviceStopRequested = false
        var stopSessionId: RecordingSessionId? = null

        var requestedSessionId: RecordingSessionId? = null
        var requestedFile: File? = null
        val startTime = 1000L

        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, _ ->
            requestedSessionId = sessionId
            requestedFile = file
        }

        QuickVoiceRecorderController.stopServiceDirectly = { _, sessionId ->
            serviceStopRequested = true
            stopSessionId = sessionId
        }

        val fakeContext = createFakeContext()

        // Start
        QuickVoiceRecorderController.startRecording(fakeContext)
        val session = requestedSessionId!!
        QuickVoiceRecorderController.onServiceRecordingStarted(session, requestedFile!!, startTime)

        // Stop requested
        val stopResult = QuickVoiceRecorderController.stopRecording(fakeContext)
        assertTrue(stopResult is QuickVoiceOperationResult.Stopping)
        assertTrue(serviceStopRequested)
        assertEquals(session, stopSessionId)

        // State is Stopping
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Stopping)

        // BEFORE service callback: NO repository items saved yet
        assertEquals(0, repository.getAll().size)

        // Service confirms stop and validates file
        requestedFile!!.writeBytes(ByteArray(1024) { 0x42 })
        QuickVoiceRecorderController.onServiceRecordingStopped(
            sessionId = session,
            outputFile = requestedFile!!,
            durationMs = 2000L,
            isSuccess = true,
            errorMessage = null
        )

        // Wait for background persistence deterministically
        waitUntil { QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Recorded }

        // NOW item is saved exactly once
        val all = repository.getAll()
        assertEquals(1, all.size)
        assertEquals("rec_${requestedFile!!.nameWithoutExtension}", all[0].id)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Recorded)
    }

    private suspend fun waitUntil(timeoutMs: Long = 2000L, condition: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition() && System.currentTimeMillis() - start < timeoutMs) {
            kotlinx.coroutines.delay(10)
        }
        assertTrue("Condition timed out after ${timeoutMs}ms", condition())
    }

    @Test
    fun `exactly-once and idempotency - duplicate stop press is rejected and does not double request`() = runTest {
        var stopRequestCount = 0
        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, startTime ->
            QuickVoiceRecorderController.onServiceRecordingStarted(sessionId, file, startTime)
        }
        QuickVoiceRecorderController.stopServiceDirectly = { _, _ ->
            stopRequestCount++
        }

        val fakeContext = createFakeContext()
        QuickVoiceRecorderController.startRecording(fakeContext)

        // First stop
        val firstStop = QuickVoiceRecorderController.stopRecording(fakeContext)
        assertTrue(firstStop is QuickVoiceOperationResult.Stopping)
        assertEquals(1, stopRequestCount)

        // Second stop while still Stopping -> rejected
        val secondStop = QuickVoiceRecorderController.stopRecording(fakeContext)
        assertTrue(secondStop is QuickVoiceOperationResult.Unavailable)
        assertEquals(1, stopRequestCount) // Not requested again
    }

    @Test
    fun `exactly-once and idempotency - duplicate service stopped callback is idempotent`() = runTest {
        var requestedSessionId: RecordingSessionId? = null
        var requestedFile: File? = null
        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, startTime ->
            requestedSessionId = sessionId
            requestedFile = file
            QuickVoiceRecorderController.onServiceRecordingStarted(sessionId, file, startTime)
        }

        val fakeContext = createFakeContext()
        QuickVoiceRecorderController.startRecording(fakeContext)
        val session = requestedSessionId!!
        val file = requestedFile!!
        file.writeBytes(ByteArray(1024) { 0x42 })

        QuickVoiceRecorderController.stopRecording(fakeContext)

        // First callback
        QuickVoiceRecorderController.onServiceRecordingStopped(session, file, 1500L, true, null)
        waitUntil { repository.getAll().isNotEmpty() }
        assertEquals(1, repository.getAll().size)

        // Duplicate callback for same session
        QuickVoiceRecorderController.onServiceRecordingStopped(session, file, 1500L, true, null)
        // Count must still be exactly 1!
        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun `exactly-once and idempotency - stale session callback is ignored`() = runTest {
        val staleSession = RecordingSessionId("stale_session_123")
        val staleFile = File(tempDir, "stale.m4a").apply { writeBytes(ByteArray(1024)) }

        // Send callback for nonexistent / stale session
        QuickVoiceRecorderController.onServiceRecordingStopped(staleSession, staleFile, 1500L, true, null)

        // Nothing saved
        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun `failed finalization - too short or empty recording is discarded without repository save`() = runTest {
        var requestedSessionId: RecordingSessionId? = null
        var requestedFile: File? = null
        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, startTime ->
            requestedSessionId = sessionId
            requestedFile = file
            QuickVoiceRecorderController.onServiceRecordingStarted(sessionId, file, startTime)
        }

        val fakeContext = createFakeContext()
        QuickVoiceRecorderController.startRecording(fakeContext)

        // Service reports stop failed (e.g. MediaRecorder throw on stop or empty file)
        QuickVoiceRecorderController.onServiceRecordingStopped(
            sessionId = requestedSessionId!!,
            outputFile = requestedFile!!,
            durationMs = 200L,
            isSuccess = false,
            errorMessage = "Recording too short or discarded"
        )

        waitUntil { QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Idle }
        assertEquals(0, repository.getAll().size)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Idle)
    }

    @Test
    fun `audio ownership - starting recording stops Study audio and ongoing replay`() = runTest {
        val fakeAudio = FakeAudioController()
        QuickVoiceRecorderController.audioControllerProvider = { fakeAudio }

        val testFile = File(tempDir, "voice_test.m4a").apply { writeBytes(ByteArray(512)) }
        val item = VoiceRecordingItem(
            id = "rec_test",
            filename = "voice_test.m4a",
            filePath = testFile.absolutePath,
            createdAt = 1000L,
            durationMs = 1500L,
            sizeBytes = 512L
        )
        repository.save(item)

        // 1. Start Replay
        QuickVoiceRecorderController.playRecording(item)
        assertTrue(fakeAudio.isPlaying)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Replaying)

        // 2. Start Recording while replay was playing
        val fakeContext = createFakeContext()
        QuickVoiceRecorderController.startRecording(fakeContext)

        // Replay must be stopped!
        assertFalse(fakeAudio.isPlaying)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Starting)
    }

    @Test
    fun `audio ownership - replay last recording is rejected while recording is active`() = runTest {
        val fakeContext = createFakeContext()
        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, startTime ->
            QuickVoiceRecorderController.onServiceRecordingStarted(sessionId, file, startTime)
        }

        QuickVoiceRecorderController.startRecording(fakeContext)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Recording)

        // Attempt to replay while recording
        val replayResult = QuickVoiceRecorderController.replayLastRecording()
        assertTrue(replayResult is QuickVoiceOperationResult.Unavailable)
    }

    @Test
    fun `exact physical device blocker reproduction - Recording then Replay rejected then Toggle stops same session`() = runTest {
        val fakeAudio = FakeAudioController()
        QuickVoiceRecorderController.audioControllerProvider = { fakeAudio }

        // Setup an existing old recording
        val oldFile = File(tempDir, "voice_old.m4a").apply { writeBytes(ByteArray(512)) }
        val oldItem = VoiceRecordingItem(
            id = "rec_old",
            filename = "voice_old.m4a",
            filePath = oldFile.absolutePath,
            createdAt = 500L,
            durationMs = 1200L,
            sizeBytes = 512L
        )
        repository.save(oldItem)

        var serviceStartCount = 0
        var serviceStopCount = 0
        var activeSessionId: RecordingSessionId? = null
        var activeOutputFile: File? = null

        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, _ ->
            serviceStartCount++
            activeSessionId = sessionId
            activeOutputFile = file
        }

        QuickVoiceRecorderController.stopServiceDirectly = { _, sessionId ->
            serviceStopCount++
            assertEquals(activeSessionId, sessionId)
        }

        val fakeContext = createFakeContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)

        // 1. User presses button mapped to TOGGLE_VOICE_RECORDING -> Start recording
        val toggleStartResult = dispatcher.dispatch(ControllerAction.TOGGLE_VOICE_RECORDING, ControllerContext.GLOBAL)
        assertTrue(toggleStartResult is ControllerActionResult.Executed)
        val sessionA = activeSessionId!!
        val fileA = activeOutputFile!!
        assertEquals(1, serviceStartCount)

        // Service confirms start
        QuickVoiceRecorderController.onServiceRecordingStarted(sessionA, fileA, 1000L)
        val stateRecording = QuickVoiceRecorderController.state.value
        assertTrue(stateRecording is QuickVoiceRecorderState.Recording)
        assertEquals(sessionA, (stateRecording as QuickVoiceRecorderState.Recording).sessionId)

        // 2. User presses button mapped to REPLAY_LAST_RECORDING while recording is running
        val replayResult = dispatcher.dispatch(ControllerAction.REPLAY_LAST_RECORDING, ControllerContext.GLOBAL)
        assertTrue(replayResult is ControllerActionResult.UnavailableInContext)

        // ASSERT: No playback, active recording is NOT stopped, session A is NOT corrupted
        assertFalse(fakeAudio.isPlaying)
        assertNull(fakeAudio.replayPath)
        assertEquals(1, serviceStartCount)
        assertEquals(0, serviceStopCount)

        val stateAfterReplay = QuickVoiceRecorderController.state.value
        assertTrue(stateAfterReplay is QuickVoiceRecorderState.Recording)
        val recState = stateAfterReplay as QuickVoiceRecorderState.Recording
        assertEquals(sessionA, recState.sessionId)
        assertEquals(fileA.absolutePath, recState.file.absolutePath)

        // 3. User presses TOGGLE_VOICE_RECORDING with intent to STOP active recording
        val toggleStopResult = dispatcher.dispatch(ControllerAction.TOGGLE_VOICE_RECORDING, ControllerContext.GLOBAL)
        assertTrue(toggleStopResult is ControllerActionResult.Executed)
        assertEquals("Voice: Stopping recording", (toggleStopResult as ControllerActionResult.Executed).target)

        // ASSERT: Service stop requested for SAME session A, NO new session B started
        assertEquals(1, serviceStartCount) // Did not start a new session!
        assertEquals(1, serviceStopCount)
        val stateStopping = QuickVoiceRecorderController.state.value
        assertTrue(stateStopping is QuickVoiceRecorderState.Stopping)
        assertEquals(sessionA, (stateStopping as QuickVoiceRecorderState.Stopping).sessionId)

        // 4. Service finalizes session A
        fileA.writeBytes(ByteArray(1024) { 0x42 })
        QuickVoiceRecorderController.onServiceRecordingStopped(
            sessionId = sessionA,
            outputFile = fileA,
            durationMs = 2500L,
            isSuccess = true,
            errorMessage = null
        )

        waitUntil { QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Recorded }
        val allRecordings = repository.getAll()
        assertEquals(2, allRecordings.size) // Old item + newly completed session A item
        assertEquals("rec_${fileA.nameWithoutExtension}", allRecordings[0].id)
        assertEquals("rec_old", allRecordings[1].id)
    }

    @Test
    fun `Starting state + Replay rejected - state remains Starting with same session`() = runTest {
        val fakeAudio = FakeAudioController()
        QuickVoiceRecorderController.audioControllerProvider = { fakeAudio }

        val oldFile = File(tempDir, "voice_old.m4a").apply { writeBytes(ByteArray(512)) }
        val oldItem = VoiceRecordingItem(
            id = "rec_old",
            filename = "voice_old.m4a",
            filePath = oldFile.absolutePath,
            createdAt = 500L,
            durationMs = 1200L,
            sizeBytes = 512L
        )
        repository.save(oldItem)

        var activeSessionId: RecordingSessionId? = null
        var activeOutputFile: File? = null

        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, _ ->
            activeSessionId = sessionId
            activeOutputFile = file
        }

        val fakeContext = createFakeContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)

        // Start requested -> state is Starting
        dispatcher.dispatch(ControllerAction.TOGGLE_VOICE_RECORDING, ControllerContext.GLOBAL)
        val sessionA = activeSessionId!!
        val stateStarting = QuickVoiceRecorderController.state.value
        assertTrue(stateStarting is QuickVoiceRecorderState.Starting)

        // Replay dispatched while Starting
        val replayResult = dispatcher.dispatch(ControllerAction.REPLAY_LAST_RECORDING, ControllerContext.GLOBAL)
        assertTrue(replayResult is ControllerActionResult.UnavailableInContext)
        assertFalse(fakeAudio.isPlaying)

        // State remains Starting with session A
        val stateAfterReplay = QuickVoiceRecorderController.state.value
        assertTrue(stateAfterReplay is QuickVoiceRecorderState.Starting)
        assertEquals(sessionA, (stateAfterReplay as QuickVoiceRecorderState.Starting).sessionId)

        // Service confirms start
        QuickVoiceRecorderController.onServiceRecordingStarted(sessionA, activeOutputFile!!, 1000L)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Recording)
    }

    @Test
    fun `Stopping state + Replay rejected - state remains Stopping with same session`() = runTest {
        val fakeAudio = FakeAudioController()
        QuickVoiceRecorderController.audioControllerProvider = { fakeAudio }

        var activeSessionId: RecordingSessionId? = null
        var activeOutputFile: File? = null

        QuickVoiceRecorderController.startServiceDirectly = { _, sessionId, file, _ ->
            activeSessionId = sessionId
            activeOutputFile = file
        }

        val fakeContext = createFakeContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)

        dispatcher.dispatch(ControllerAction.TOGGLE_VOICE_RECORDING, ControllerContext.GLOBAL)
        val sessionA = activeSessionId!!
        val fileA = activeOutputFile!!
        QuickVoiceRecorderController.onServiceRecordingStarted(sessionA, fileA, 1000L)

        // Stop requested -> state is Stopping
        dispatcher.dispatch(ControllerAction.TOGGLE_VOICE_RECORDING, ControllerContext.GLOBAL)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Stopping)

        // Replay dispatched while Stopping
        val replayResult = dispatcher.dispatch(ControllerAction.REPLAY_LAST_RECORDING, ControllerContext.GLOBAL)
        assertTrue(replayResult is ControllerActionResult.UnavailableInContext)
        assertFalse(fakeAudio.isPlaying)

        // State remains Stopping with session A
        val stateAfterReplay = QuickVoiceRecorderController.state.value
        assertTrue(stateAfterReplay is QuickVoiceRecorderState.Stopping)
        assertEquals(sessionA, (stateAfterReplay as QuickVoiceRecorderState.Stopping).sessionId)

        // Service confirms stop
        fileA.writeBytes(ByteArray(1024) { 0x42 })
        QuickVoiceRecorderController.onServiceRecordingStopped(sessionA, fileA, 2000L, true, null)
        waitUntil { QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Recorded }
    }

    @Test
    fun `Idle state + Replay Last plays valid recording normally`() = runTest {
        val fakeAudio = FakeAudioController()
        QuickVoiceRecorderController.audioControllerProvider = { fakeAudio }

        val oldFile = File(tempDir, "voice_old.m4a").apply { writeBytes(ByteArray(512)) }
        val oldItem = VoiceRecordingItem(
            id = "rec_old",
            filename = "voice_old.m4a",
            filePath = oldFile.absolutePath,
            createdAt = 500L,
            durationMs = 1200L,
            sizeBytes = 512L
        )
        repository.save(oldItem)

        val fakeContext = createFakeContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)

        val replayResult = dispatcher.dispatch(ControllerAction.REPLAY_LAST_RECORDING, ControllerContext.GLOBAL)
        assertTrue(replayResult is ControllerActionResult.Executed)
        assertTrue(fakeAudio.isPlaying)
        assertEquals(oldFile.absolutePath, fakeAudio.replayPath)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Replaying)
    }

    @Test
    fun `Replay active then Toggle Recording stops replay and starts new recording`() = runTest {
        val fakeAudio = FakeAudioController()
        QuickVoiceRecorderController.audioControllerProvider = { fakeAudio }

        val oldFile = File(tempDir, "voice_old.m4a").apply { writeBytes(ByteArray(512)) }
        val oldItem = VoiceRecordingItem(
            id = "rec_old",
            filename = "voice_old.m4a",
            filePath = oldFile.absolutePath,
            createdAt = 500L,
            durationMs = 1200L,
            sizeBytes = 512L
        )
        repository.save(oldItem)

        val fakeContext = createFakeContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)

        // Start Replay
        dispatcher.dispatch(ControllerAction.REPLAY_LAST_RECORDING, ControllerContext.GLOBAL)
        assertTrue(fakeAudio.isPlaying)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Replaying)

        // Start Recording -> Replay must be stopped and new recording started
        val toggleResult = dispatcher.dispatch(ControllerAction.TOGGLE_VOICE_RECORDING, ControllerContext.GLOBAL)
        assertTrue(toggleResult is ControllerActionResult.Executed)
        assertFalse(fakeAudio.isPlaying)
        assertTrue(QuickVoiceRecorderController.state.value is QuickVoiceRecorderState.Starting)
    }
}


