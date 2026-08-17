package vn.loi.learning.android.controller

import android.view.KeyEvent

/**
 * Unified authority for physical controller button directory and human-friendly naming.
 * Verified for 8BitDo Micro (VID 0x2dc8, PID 0x9021) in Keyboard (K-Mode).
 */
object ControllerButtonDirectory {
    data class ButtonInfo(
        val keyCode: Int,
        val primaryName: String,
        val secondaryDescription: String
    )

    val ALL_BUTTONS: List<ButtonInfo> = listOf(
        // Shoulder & Triggers
        ButtonInfo(KeyEvent.KEYCODE_K, "L1", "Left Bumper (Top Left)"),
        ButtonInfo(KeyEvent.KEYCODE_M, "R1", "Right Bumper (Top Right)"),
        ButtonInfo(KeyEvent.KEYCODE_L, "L2", "Left Trigger (Bottom Left)"),
        ButtonInfo(KeyEvent.KEYCODE_R, "R2", "Right Trigger (Bottom Right)"),

        // Face Buttons (Canonical 8BitDo Micro K-Mode)
        ButtonInfo(KeyEvent.KEYCODE_G, "A", "East Button (A)"),
        ButtonInfo(KeyEvent.KEYCODE_J, "B", "South Button (B)"),
        ButtonInfo(KeyEvent.KEYCODE_H, "X", "North Button (X)"),
        ButtonInfo(KeyEvent.KEYCODE_I, "Y", "West Button (Y)"),

        // Directional Pad (D-Pad)
        ButtonInfo(KeyEvent.KEYCODE_C, "D-Pad Up", "Directional Pad Up"),
        ButtonInfo(KeyEvent.KEYCODE_D, "D-Pad Down", "Directional Pad Down"),
        ButtonInfo(KeyEvent.KEYCODE_E, "D-Pad Left", "Directional Pad Left"),
        ButtonInfo(KeyEvent.KEYCODE_F, "D-Pad Right", "Directional Pad Right"),

        // System / Extra Buttons
        ButtonInfo(KeyEvent.KEYCODE_N, "Select / -", "Minus / Select Button"),
        ButtonInfo(KeyEvent.KEYCODE_O, "Start / +", "Plus / Start Button"),
        ButtonInfo(KeyEvent.KEYCODE_S, "Star / Mode", "Star / Mode Button"),
        ButtonInfo(KeyEvent.KEYCODE_P, "Home / Star", "Home / Star Button"),
        ButtonInfo(KeyEvent.KEYCODE_Q, "Home / Mode", "Home / Mode Button")
    )

    fun getButtonLabel(keyCode: Int): String {
        return ALL_BUTTONS.firstOrNull { it.keyCode == keyCode }?.primaryName
            ?: runCatching { KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_") }.getOrDefault("KEY_$keyCode")
    }

    fun getButtonFullName(keyCode: Int): String {
        val info = ALL_BUTTONS.firstOrNull { it.keyCode == keyCode } ?: return getButtonLabel(keyCode)
        return "${info.primaryName} (${info.secondaryDescription})"
    }
}

/**
 * Stable physical controller input identity.
 * Identified by device VID/PID and Android key code.
 */
data class ControllerPhysicalInput(
    val vendorId: Int = 0x2dc8,
    val productId: Int = 0x9021,
    val keyCode: Int
) {
    val keyCodeName: String
        get() = ControllerButtonDirectory.getButtonLabel(keyCode)

    fun matches(devVid: Int, devPid: Int, eventKeyCode: Int): Boolean {
        if (keyCode != eventKeyCode) return false
        // Allow matching if controller VID/PID matches or if generic wildcard VID == 0
        if (vendorId == 0 && productId == 0) return true
        return (vendorId == devVid && (productId == 0 || productId == devPid)) ||
            (devVid == 0 && devPid == 0)
    }
}

enum class ControllerPressType(val label: String) {
    PRESS("Press"),
    DOUBLE_PRESS("Double press"),
    LONG_PRESS("Long press")
}

/**
 * Logical controller gesture composed of a primary input, press type, and optional modifier.
 */
data class ControllerGesture(
    val input: ControllerPhysicalInput,
    val pressType: ControllerPressType = ControllerPressType.PRESS,
    val modifier: ControllerPhysicalInput? = null
) {
    val displayLabel: String
        get() = buildString {
            if (modifier != null) {
                append("${ControllerButtonDirectory.getButtonLabel(modifier.keyCode)} + ")
            }
            append(ControllerButtonDirectory.getButtonLabel(input.keyCode))
            when (pressType) {
                ControllerPressType.PRESS -> Unit
                ControllerPressType.DOUBLE_PRESS -> append(" · Double")
                ControllerPressType.LONG_PRESS -> append(" · Long press")
            }
        }
}

/**
 * Execution context for controller mappings.
 */
enum class ControllerContext(val label: String, val description: String) {
    GLOBAL("Everywhere", "Applies everywhere this action is available"),
    STUDY_QUESTION("Question", "When a question or prompt is currently presented"),
    STUDY_REVEALED("Answer revealed", "After the answer has been shown"),
    STUDY_RATING("Rating card", "When rating buttons (Again, Hard, Good, Easy) are active"),
    AUTO_PLAY("Auto Play", "During Auto Play playback"),
    SHADOWING("Shadowing", "During Shadowing practice (Future)")
}

enum class ControllerActionCategory(val label: String) {
    PLAYBACK("Playback"),
    STUDY("Study & Review"),
    RATING("Rating"),
    NAVIGATION("Navigation"),
    SHADOWING("Shadowing (Future)")
}

/**
 * Centralized applicability policy for all ControllerActions.
 * Single source of truth determining which runtime contexts each action can execute in.
 */
object ControllerActionPolicy {
    fun isApplicable(action: ControllerAction, context: ControllerContext): Boolean {
        if (context == ControllerContext.GLOBAL) return true
        return when (action) {
            ControllerAction.PLAY_PAUSE -> context == ControllerContext.AUTO_PLAY || context == ControllerContext.SHADOWING
            ControllerAction.REPLAY_PRIMARY_AUDIO,
            ControllerAction.LOOP_PRIMARY_AUDIO,
            ControllerAction.PLAY_PRIMARY_EN,
            ControllerAction.PLAY_PRIMARY_VI -> context in setOf(
                ControllerContext.STUDY_QUESTION,
                ControllerContext.STUDY_REVEALED,
                ControllerContext.STUDY_RATING,
                ControllerContext.AUTO_PLAY,
                ControllerContext.SHADOWING
            )
            ControllerAction.PLAY_EXAMPLE_EN,
            ControllerAction.LOOP_EXAMPLE_EN,
            ControllerAction.PLAY_EXAMPLE_VI -> context in setOf(
                ControllerContext.STUDY_QUESTION,
                ControllerContext.STUDY_REVEALED,
                ControllerContext.STUDY_RATING,
                ControllerContext.AUTO_PLAY
            )
            ControllerAction.REVEAL_ANSWER -> context == ControllerContext.STUDY_QUESTION
            ControllerAction.CONTINUE_CURRENT_MODE -> context in setOf(
                ControllerContext.STUDY_QUESTION,
                ControllerContext.STUDY_REVEALED,
                ControllerContext.STUDY_RATING
            )
            ControllerAction.RATE_AGAIN,
            ControllerAction.RATE_HARD,
            ControllerAction.RATE_GOOD,
            ControllerAction.RATE_EASY -> context == ControllerContext.STUDY_RATING || context == ControllerContext.STUDY_REVEALED
            ControllerAction.START_AUTO_PLAY -> context in setOf(
                ControllerContext.STUDY_QUESTION,
                ControllerContext.STUDY_REVEALED,
                ControllerContext.STUDY_RATING
            )
            ControllerAction.STOP_AUTO_PLAY -> context == ControllerContext.AUTO_PLAY
            ControllerAction.PREVIOUS_ITEM, ControllerAction.NEXT_ITEM -> context in setOf(
                ControllerContext.STUDY_QUESTION,
                ControllerContext.STUDY_REVEALED,
                ControllerContext.STUDY_RATING,
                ControllerContext.AUTO_PLAY,
                ControllerContext.SHADOWING
            )
            ControllerAction.MUTE_TOGGLE,
            ControllerAction.SYSTEM_VOLUME_UP,
            ControllerAction.SYSTEM_VOLUME_DOWN,
            ControllerAction.LOCK_SCREEN -> true
            ControllerAction.REPLAY_SENTENCE,
            ControllerAction.TOGGLE_LOOP,
            ControllerAction.SPEED_UP,
            ControllerAction.SPEED_DOWN -> context == ControllerContext.SHADOWING
        }
    }
}

/**
 * Semantic controller actions independent of hardware key codes.
 */
enum class ControllerAction(
    val label: String,
    val description: String,
    val category: ControllerActionCategory
) {
    // Playback / Audio
    REPLAY_PRIMARY_AUDIO("Replay primary audio", "Replay prompt or primary vocabulary audio", ControllerActionCategory.PLAYBACK),
    LOOP_PRIMARY_AUDIO("Loop primary audio", "Loop the current primary vocabulary / answer audio", ControllerActionCategory.PLAYBACK),
    PLAY_PRIMARY_EN("Play primary English audio", "Play the primary English/prompt audio", ControllerActionCategory.PLAYBACK),
    PLAY_PRIMARY_VI("Play primary Vietnamese audio", "Play the primary Vietnamese meaning audio", ControllerActionCategory.PLAYBACK),
    PLAY_EXAMPLE_EN("Play English example", "Play English example sentence audio", ControllerActionCategory.PLAYBACK),
    LOOP_EXAMPLE_EN("Loop English example", "Loop current English example sentence audio", ControllerActionCategory.PLAYBACK),
    PLAY_EXAMPLE_VI("Play Vietnamese example", "Play Vietnamese translation audio", ControllerActionCategory.PLAYBACK),
    PLAY_PAUSE("Play / Pause", "Toggle playback or pause", ControllerActionCategory.PLAYBACK),
    MUTE_TOGGLE("Mute / unmute app audio", "Mute or unmute all LearningEngine audio", ControllerActionCategory.PLAYBACK),
    START_AUTO_PLAY("Auto Play current session", "Launch Auto Play for current session/review items", ControllerActionCategory.PLAYBACK),
    STOP_AUTO_PLAY("Stop Auto Play", "Stop Auto Play session", ControllerActionCategory.PLAYBACK),

    // Study & Review
    REVEAL_ANSWER("Show answer", "Reveal the answer for the current study item", ControllerActionCategory.STUDY),
    CONTINUE_CURRENT_MODE("Continue", "Perform the current screen's normal swipe-up / continue action", ControllerActionCategory.STUDY),
    RATE_AGAIN("Rate Again", "Mark item as Again (FSRS 1)", ControllerActionCategory.RATING),
    RATE_HARD("Rate Hard", "Mark item as Hard (FSRS 2)", ControllerActionCategory.RATING),
    RATE_GOOD("Rate Good", "Mark item as Good (FSRS 3)", ControllerActionCategory.RATING),
    RATE_EASY("Rate Easy", "Mark item as Easy (FSRS 4)", ControllerActionCategory.RATING),

    // Navigation
    PREVIOUS_ITEM("Previous item", "Navigate to previous item", ControllerActionCategory.NAVIGATION),
    NEXT_ITEM("Next item", "Navigate to next item", ControllerActionCategory.NAVIGATION),

    // System Control
    SYSTEM_VOLUME_UP("System volume up", "Increase system media playback volume", ControllerActionCategory.PLAYBACK),
    SYSTEM_VOLUME_DOWN("System volume down", "Decrease system media playback volume", ControllerActionCategory.PLAYBACK),
    LOCK_SCREEN("Lock screen", "Lock the device and turn the display off", ControllerActionCategory.PLAYBACK),

    // Reserved for future Shadowing (Internal/Deferred - hidden from new action picker)
    REPLAY_SENTENCE("Replay Sentence", "Replay current sentence (Deferred)", ControllerActionCategory.SHADOWING),
    TOGGLE_LOOP("Toggle Loop", "Toggle sentence repeat loop (Deferred)", ControllerActionCategory.SHADOWING),
    SPEED_UP("Speed Up", "Increase playback speed (Deferred)", ControllerActionCategory.SHADOWING),
    SPEED_DOWN("Speed Down", "Decrease playback speed (Deferred)", ControllerActionCategory.SHADOWING);

    val isUserSelectable: Boolean
        get() = this !in setOf(REPLAY_SENTENCE, TOGGLE_LOOP, SPEED_UP, SPEED_DOWN)

    fun isApplicableIn(context: ControllerContext): Boolean = ControllerActionPolicy.isApplicable(this, context)

    fun isSupportedInContext(context: ControllerContext): Boolean = isApplicableIn(context)
}

/**
 * Binding of a context + gesture to a semantic action.
 */
data class ControllerMapping(
    val context: ControllerContext,
    val gesture: ControllerGesture,
    val action: ControllerAction
)

/**
 * User-configurable controller profile.
 */
data class ControllerProfile(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val deviceVendorId: Int = 0x2dc8,
    val deviceProductId: Int = 0x9021,
    val modifierInput: ControllerPhysicalInput? = null,
    val mappings: List<ControllerMapping> = emptyList()
)

/**
 * Root configuration container for all controller settings and profiles.
 */
data class ControllerConfig(
    val activeProfileId: String = "8bitdo_micro_k_default",
    val profiles: List<ControllerProfile> = emptyList(),
    val isControllerEnabled: Boolean = true
) {
    val activeProfile: ControllerProfile
        get() = profiles.firstOrNull { it.id == activeProfileId }
            ?: profiles.firstOrNull()
            ?: ControllerProfile(
                id = "8bitdo_micro_k_default",
                name = "8BitDo Micro (K-Mode Default)"
            )
}

enum class TestingMode(val label: String, val description: String) {
    K("K (Keyboard)", "8BitDo Micro Keyboard mode (Recommended)"),
    S("S (Gamepad)", "Standard Android Gamepad & Joystick mode"),
    D("D (DirectInput)", "DirectInput Gamepad mode"),
    RAW_HID("Raw HID", "Observe raw hardware HID key and motion events directly from input drivers"),
    ANDROID_KEY_EVENT("Android KeyEvent", "Observe processed Android KeyEvent and InputDevice structures"),
    MAPPED_ACTIONS("Mapped Actions", "Observe semantic actions dispatched to LearningEngine study and playback")
}

enum class EventOrigin {
    ACTIVITY,
    ACCESSIBILITY
}

sealed class ControllerActionResult {
    data class Executed(val action: ControllerAction, val target: String) : ControllerActionResult()
    data class UnavailableInContext(val action: ControllerAction, val reason: String) : ControllerActionResult()
    data class Unmapped(val gesture: ControllerGesture) : ControllerActionResult()
    data class Ignored(val reason: String) : ControllerActionResult()
    data class Disabled(val reason: String) : ControllerActionResult()
    data class DeferredAction(val action: ControllerAction, val note: String = "Deferred for future Shadowing feature") : ControllerActionResult()
}

data class InputDeviceInfo(
    val id: Int,
    val name: String,
    val descriptor: String,
    val vendorId: Int,
    val productId: Int,
    val sources: Int,
    val isGamepad: Boolean,
    val isJoystick: Boolean,
    val isDpad: Boolean,
    val isKeyboard: Boolean,
    val isExternal: Boolean
) {
    val sourceLabels: List<String>
        get() = ControllerInputDiagnostic.formatSource(sources).split("|")

    val isCandidateController: Boolean
        get() = ControllerInputDiagnostic.isCandidateControllerDevice(this)
}

data class ControllerInputEvent(
    val timestamp: Long,
    val deviceId: Int,
    val deviceName: String,
    val descriptor: String,
    val vendorId: Int,
    val productId: Int,
    val sources: Int,
    val source: Int,
    val sourceName: String,
    val action: Int,
    val actionName: String,
    val keyCode: Int,
    val keyCodeName: String,
    val scanCode: Int,
    val repeatCount: Int = 0,
    val metaState: Int = 0,
    val flags: Int = 0,
    val isCandidateController: Boolean = false,
    val origin: EventOrigin = EventOrigin.ACTIVITY,
    val testingMode: TestingMode = TestingMode.RAW_HID,
    val axis: Int? = null,
    val axisValue: Float? = null,
    val hatX: Float? = null,
    val hatY: Float? = null
) {
    fun toLogLine(): String =
        "[${testingMode.name.take(1)}][$origin] $actionName $keyCodeName (keyCode=$keyCode scanCode=$scanCode source=$sourceName device='$deviceName' vid=0x${vendorId.toString(16)} pid=0x${productId.toString(16)})"
}

data class ControllerKeyCount(
    val key: String,
    val count: Int = 0,
    val lastTimestamp: Long = 0L,
    val origin: EventOrigin = EventOrigin.ACTIVITY
)

data class ControllerRuntimeTraceEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val origin: EventOrigin,
    val buttonLabel: String,
    val keyCode: Int,
    val pressType: ControllerPressType,
    val lifecycleState: String = "UNKNOWN",
    val stateBefore: String,
    val contextBefore: ControllerContext,
    val resolvedMapping: String,
    val targetInvoked: String,
    val eventSubmitted: String?,
    val stateAfter: String,
    val contextAfter: ControllerContext,
    val dispatchResult: String,
    val latencyMs: Long = 0L
) {
    fun toLogLine(): String =
        "[$timestamp][lifecycle=$lifecycleState] origin=$origin button=$buttonLabel keyCode=$keyCode press=$pressType " +
        "stateBefore=$stateBefore contextBefore=$contextBefore mapping=$resolvedMapping " +
        "target=$targetInvoked event=$eventSubmitted stateAfter=$stateAfter contextAfter=$contextAfter " +
        "result=$dispatchResult latency=${latencyMs}ms"
}

data class ControllerDiagnosticsState(
    val isAccessibilityServiceConnected: Boolean = false,
    val isForeground: Boolean = true,
    val lifecycleState: String = "RESUMED",
    val isGlobalMuted: Boolean = false,
    val activeStudyPlayback: ActiveStudyPlayback? = null,
    val systemVolumeStatus: vn.loi.learning.android.media.SystemVolumeStatus? = null,
    val lastVolumeAction: String? = null,
    val connectedDevices: List<InputDeviceInfo> = emptyList(),
    val events: List<ControllerInputEvent> = emptyList(),
    val keyCounts: Map<String, Int> = emptyMap(),
    val runtimeTrace: List<ControllerRuntimeTraceEntry> = emptyList(),
    val audioTrace: List<StudyAudioTraceEntry> = emptyList(),
    val lastEvent: ControllerInputEvent? = null,
    val lastDispatchedResult: ControllerActionResult? = null,
    val testingMode: TestingMode = TestingMode.MAPPED_ACTIONS
)

enum class StudyAudioReason {
    ITEM_ENTRY,
    REVEAL,
    MANUAL_PLAY,
    MANUAL_LOOP,
    CONTINUE_EXIT,
    COMPOSE_AUTOPLAY
}

enum class StudyAudioEventKind {
    AUDIO_START,
    AUDIO_STOP
}

data class StudyAudioTraceEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val kind: StudyAudioEventKind,
    val itemKey: String?,
    val role: String?,
    val path: String?,
    val isLooping: Boolean,
    val reason: StudyAudioReason
) {
    fun toLogLine(): String =
        "[$timestamp][$kind] itemKey=$itemKey role=$role path=$path isLooping=$isLooping reason=$reason"
}
