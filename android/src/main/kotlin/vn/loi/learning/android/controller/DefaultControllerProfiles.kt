package vn.loi.learning.android.controller

import android.view.KeyEvent

/**
 * Standard default controller profiles for 8BitDo Micro and supported controllers.
 * Fully user-editable, conservative, and transparent.
 *
 * Canonical 8BitDo Micro Physical mapping matrix:
 * - KEYCODE_K = Physical L1 (Left Bumper)
 * - KEYCODE_M = Physical R1 (Right Bumper)
 * - KEYCODE_L = Physical L2 (Left Trigger)
 * - KEYCODE_R = Physical R2 (Right Trigger)
 * - KEYCODE_G = Physical A (East Button)
 * - KEYCODE_J = Physical B (South Button)
 * - KEYCODE_H = Physical X (North Button)
 * - KEYCODE_I = Physical Y (West Button)
 * - KEYCODE_C = Physical D-Pad Up
 * - KEYCODE_D = Physical D-Pad Down
 * - KEYCODE_E = Physical D-Pad Left
 * - KEYCODE_F = Physical D-Pad Right
 * - KEYCODE_N = Physical Select / -
 * - KEYCODE_O = Physical Start / + (Modifier Chord Button)
 * - KEYCODE_S = Physical Star / Mode
 */
object DefaultControllerProfiles {

    const val DEFAULT_PROFILE_ID = "8bitdo_micro_k_default"
    const val VENDOR_8BITDO = 0x2dc8
    const val PRODUCT_MICRO_K = 0x9021

    fun defaultProfile(): ControllerProfile {
        val vid = VENDOR_8BITDO
        val pid = PRODUCT_MICRO_K

        val inputA = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_G) // Physical A
        val inputB = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_J) // Physical B
        val inputX = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_H) // Physical X
        val inputY = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_I) // Physical Y
        val inputDpadUp = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_C) // Physical D-Pad Up
        val inputDpadDown = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_D) // Physical D-Pad Down
        val inputDpadLeft = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_E) // Physical D-Pad Left
        val inputDpadRight = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_F) // Physical D-Pad Right
        val inputL1 = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_K) // Physical L1
        val inputL2 = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_L) // Physical L2
        val inputR1 = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_M) // Physical R1
        val inputR2 = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_R) // Physical R2
        val inputO = ControllerPhysicalInput(vid, pid, KeyEvent.KEYCODE_O) // Modifier button (Start / +)

        val mappings = listOf(
            // Basic Everywhere-Applicable Mappings (GLOBAL)
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(input = inputL1, pressType = ControllerPressType.PRESS), // L1
                action = ControllerAction.REPLAY_PRIMARY_AUDIO
            ),
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(input = inputL2, pressType = ControllerPressType.PRESS), // L2
                action = ControllerAction.PLAY_EXAMPLE_EN
            ),
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(input = inputR2, pressType = ControllerPressType.PRESS), // R2
                action = ControllerAction.PLAY_EXAMPLE_VI
            ),
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(input = inputA, pressType = ControllerPressType.PRESS), // A
                action = ControllerAction.PLAY_PAUSE
            ),
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(input = inputB, pressType = ControllerPressType.PRESS), // B
                action = ControllerAction.REVEAL_ANSWER
            ),
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(input = inputY, pressType = ControllerPressType.PRESS), // Y
                action = ControllerAction.START_AUTO_PLAY
            ),
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(input = inputDpadLeft, pressType = ControllerPressType.PRESS), // D-Pad Left
                action = ControllerAction.PREVIOUS_ITEM
            ),
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(input = inputDpadRight, pressType = ControllerPressType.PRESS), // D-Pad Right
                action = ControllerAction.NEXT_ITEM
            ),

            // Study Rating Context Overrides (A/B/X/Y map to FSRS 1/2/3/4 during rating)
            ControllerMapping(
                context = ControllerContext.STUDY_RATING,
                gesture = ControllerGesture(input = inputA, pressType = ControllerPressType.PRESS), // A -> Again
                action = ControllerAction.RATE_AGAIN
            ),
            ControllerMapping(
                context = ControllerContext.STUDY_RATING,
                gesture = ControllerGesture(input = inputB, pressType = ControllerPressType.PRESS), // B -> Hard
                action = ControllerAction.RATE_HARD
            ),
            ControllerMapping(
                context = ControllerContext.STUDY_RATING,
                gesture = ControllerGesture(input = inputX, pressType = ControllerPressType.PRESS), // X -> Good
                action = ControllerAction.RATE_GOOD
            ),
            ControllerMapping(
                context = ControllerContext.STUDY_RATING,
                gesture = ControllerGesture(input = inputY, pressType = ControllerPressType.PRESS), // Y -> Easy
                action = ControllerAction.RATE_EASY
            ),

            // Auto Play Context Overrides (Mute & Stop Auto Play)
            ControllerMapping(
                context = ControllerContext.AUTO_PLAY,
                gesture = ControllerGesture(input = inputX, pressType = ControllerPressType.PRESS), // X
                action = ControllerAction.MUTE_TOGGLE
            ),
            ControllerMapping(
                context = ControllerContext.AUTO_PLAY,
                gesture = ControllerGesture(input = inputY, pressType = ControllerPressType.PRESS), // Y
                action = ControllerAction.STOP_AUTO_PLAY
            ),

            // Modifier Chord Mapping: Modifier (Start/+) + A -> Mute Toggle
            ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(
                    input = inputA,
                    pressType = ControllerPressType.PRESS,
                    modifier = inputO
                ),
                action = ControllerAction.MUTE_TOGGLE
            )
        )

        return ControllerProfile(
            id = DEFAULT_PROFILE_ID,
            name = "8BitDo Micro (K-Mode Default)",
            enabled = true,
            deviceVendorId = vid,
            deviceProductId = pid,
            modifierInput = inputO,
            mappings = mappings
        )
    }

    fun defaultConfig(): ControllerConfig = ControllerConfig(
        activeProfileId = DEFAULT_PROFILE_ID,
        profiles = listOf(defaultProfile()),
        isControllerEnabled = true
    )
}
