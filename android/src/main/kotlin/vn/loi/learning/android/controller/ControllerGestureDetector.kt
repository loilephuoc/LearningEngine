package vn.loi.learning.android.controller

import android.view.KeyEvent
import android.view.ViewConfiguration

/**
 * Robust gesture detector handling event deduplication, hardware burst suppression,
 * chord modifiers, and multi-press arbitration (PRESS, DOUBLE_PRESS, LONG_PRESS).
 */
class ControllerGestureDetector(
    private val deduplicationWindowMs: Long = 80L,
    private val doubleTapTimeoutMs: Long = defaultDoubleTapTimeout(),
    private val longPressTimeoutMs: Long = defaultLongPressTimeout(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        fun defaultDoubleTapTimeout(): Long {
            val timeout = runCatching { ViewConfiguration.getDoubleTapTimeout().toLong() }.getOrNull()
            return if (timeout != null && timeout > 0L) timeout else 300L
        }

        fun defaultLongPressTimeout(): Long {
            val timeout = runCatching { ViewConfiguration.getLongPressTimeout().toLong() }.getOrNull()
            return if (timeout != null && timeout > 0L) timeout else 400L
        }
    }

    private val lastDownTimeByKey = mutableMapOf<ControllerPhysicalInput, Long>()
    private val lastUpTimeByKey = mutableMapOf<ControllerPhysicalInput, Long>()
    private val downTimestampByKey = mutableMapOf<ControllerPhysicalInput, Long>()
    private val longPressFiredByKey = mutableMapOf<ControllerPhysicalInput, Boolean>()
    private val doublePressFiredByKey = mutableMapOf<ControllerPhysicalInput, Boolean>()

    // Pending single press tracking for double-press arbitration
    private val pendingSinglePressTapTimeByKey = mutableMapOf<ControllerPhysicalInput, Long>()
    private val pendingSinglePressGestureByKey = mutableMapOf<ControllerPhysicalInput, ControllerGesture>()

    // Modifier tracking
    private var isModifierHeld = false
    private var chordFiredDuringModifierHold = false

    /**
     * Processes an incoming KeyEvent from either ACTIVITY or ACCESSIBILITY.
     * Returns a resolved ControllerGesture if a valid gesture occurred, or null if ignored/deduplicated/buffered.
     */
    @Synchronized
    fun processKeyEvent(
        event: KeyEvent,
        origin: EventOrigin,
        profile: ControllerProfile
    ): ControllerGesture? {
        val dev = event.device
        val vendorId = dev?.vendorId ?: 0
        val productId = dev?.productId ?: 0
        return processRawKeyEvent(
            vendorId = vendorId,
            productId = productId,
            keyCode = event.keyCode,
            action = event.action,
            repeatCount = event.repeatCount,
            origin = origin,
            profile = profile
        )
    }

    @Synchronized
    fun processRawKeyEvent(
        vendorId: Int,
        productId: Int,
        keyCode: Int,
        action: Int,
        repeatCount: Int,
        origin: EventOrigin,
        profile: ControllerProfile
    ): ControllerGesture? {
        val now = clock()
        val currentInput = ControllerPhysicalInput(vendorId, productId, keyCode)

        val modifierInput = profile.modifierInput
        val isModifierKey = modifierInput != null && modifierInput.matches(vendorId, productId, keyCode)
        val activeModifier = if (isModifierHeld && !isModifierKey) modifierInput else null
        val activeModifierKeyCode = activeModifier?.keyCode

        // 1. Deduplication and Hardware Burst Suppression Gate
        if (action == KeyEvent.ACTION_DOWN) {
            val lastDown = lastDownTimeByKey[currentInput]
            if (lastDown != null && (now - lastDown) < deduplicationWindowMs && repeatCount == 0) {
                // Drop duplicate event from another origin or rapid hardware burst
                return null
            }
            lastDownTimeByKey[currentInput] = now
            if (repeatCount == 0) {
                downTimestampByKey[currentInput] = now
                longPressFiredByKey[currentInput] = false
                doublePressFiredByKey[currentInput] = false
            }
        }

        // 2. Modifier Key State Management
        if (isModifierKey) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (repeatCount == 0) {
                    isModifierHeld = true
                    chordFiredDuringModifierHold = false
                }
                return null
            } else if (action == KeyEvent.ACTION_UP) {
                val lastUp = lastUpTimeByKey[currentInput]
                if (lastUp != null && (now - lastUp) < deduplicationWindowMs) {
                    return null
                }
                lastUpTimeByKey[currentInput] = now

                val wasChordFired = chordFiredDuringModifierHold
                val downTime = downTimestampByKey[currentInput] ?: now
                val downDuration = now - downTime
                isModifierHeld = false
                chordFiredDuringModifierHold = false

                if (!wasChordFired) {
                    if (downDuration >= longPressTimeoutMs) {
                        return ControllerGesture(
                            input = currentInput,
                            pressType = ControllerPressType.LONG_PRESS,
                            modifier = null
                        )
                    }
                    return ControllerGesture(
                        input = currentInput,
                        pressType = ControllerPressType.PRESS,
                        modifier = null
                    )
                }
                return null
            }
            return null
        }

        // 3. Normal / Chord Key Processing
        val hasDoubleMapping = hasMapping(profile, keyCode, ControllerPressType.DOUBLE_PRESS, activeModifierKeyCode)
        val hasLongMapping = hasMapping(profile, keyCode, ControllerPressType.LONG_PRESS, activeModifierKeyCode)
        val hasExplicitPressMapping = hasMapping(profile, keyCode, ControllerPressType.PRESS, activeModifierKeyCode)

        if (action == KeyEvent.ACTION_DOWN) {
            if (repeatCount > 0) {
                val downTime = downTimestampByKey[currentInput] ?: now
                if ((now - downTime) >= longPressTimeoutMs && longPressFiredByKey[currentInput] != true) {
                    longPressFiredByKey[currentInput] = true
                    pendingSinglePressTapTimeByKey.remove(currentInput)
                    pendingSinglePressGestureByKey.remove(currentInput)
                    if (activeModifier != null) chordFiredDuringModifierHold = true
                    if (hasLongMapping) {
                        return ControllerGesture(
                            input = currentInput,
                            pressType = ControllerPressType.LONG_PRESS,
                            modifier = activeModifier
                        )
                    }
                }
                return null
            }

            // repeatCount == 0 (Initial DOWN)
            if (hasDoubleMapping) {
                // Check if this is the 2nd tap of a double press
                val previousTapTime = pendingSinglePressTapTimeByKey[currentInput]
                if (previousTapTime != null && (now - previousTapTime) <= doubleTapTimeoutMs) {
                    pendingSinglePressTapTimeByKey.remove(currentInput)
                    pendingSinglePressGestureByKey.remove(currentInput)
                    doublePressFiredByKey[currentInput] = true
                    if (activeModifier != null) chordFiredDuringModifierHold = true
                    return ControllerGesture(
                        input = currentInput,
                        pressType = ControllerPressType.DOUBLE_PRESS,
                        modifier = activeModifier
                    )
                }
                // First press of a potential double press: buffer and wait
                return null
            } else if (hasLongMapping) {
                // If has LONG_PRESS, must wait for release or hold threshold (do not emit PRESS on initial down)
                return null
            } else {
                // PRESS only (or unmapped): zero latency dispatch on initial down
                if (activeModifier != null) chordFiredDuringModifierHold = true
                return ControllerGesture(
                    input = currentInput,
                    pressType = ControllerPressType.PRESS,
                    modifier = activeModifier
                )
            }
        } else if (action == KeyEvent.ACTION_UP) {
            val lastUp = lastUpTimeByKey[currentInput]
            if (lastUp != null && (now - lastUp) < deduplicationWindowMs) {
                return null
            }
            lastUpTimeByKey[currentInput] = now

            // If long press was already emitted during hold, consume UP without firing PRESS
            if (longPressFiredByKey[currentInput] == true) {
                longPressFiredByKey[currentInput] = false
                return null
            }

            // If double press was already emitted on DOWN of 2nd tap, consume UP
            if (doublePressFiredByKey[currentInput] == true) {
                doublePressFiredByKey[currentInput] = false
                return null
            }

            val downTime = downTimestampByKey[currentInput] ?: now
            val downDuration = now - downTime

            // Long hold release
            if (downDuration >= longPressTimeoutMs) {
                longPressFiredByKey[currentInput] = false
                pendingSinglePressTapTimeByKey.remove(currentInput)
                pendingSinglePressGestureByKey.remove(currentInput)
                if (activeModifier != null) chordFiredDuringModifierHold = true
                if (hasLongMapping) {
                    return ControllerGesture(
                        input = currentInput,
                        pressType = ControllerPressType.LONG_PRESS,
                        modifier = activeModifier
                    )
                }
                return null
            }

            // Short release (< longPressTimeoutMs)
            if (hasDoubleMapping) {
                // Register tap 1 timestamp for potential 2nd tap
                pendingSinglePressTapTimeByKey[currentInput] = now
                val shouldBufferGesture = hasExplicitPressMapping || (!hasLongMapping && profile.mappings.none { it.gesture.input.keyCode == keyCode })
                if (shouldBufferGesture) {
                    pendingSinglePressGestureByKey[currentInput] = ControllerGesture(
                        input = currentInput,
                        pressType = ControllerPressType.PRESS,
                        modifier = activeModifier
                    )
                }
                return null
            }

            if (hasLongMapping) {
                // Released before long press threshold and no double mapping configured
                if (activeModifier != null) chordFiredDuringModifierHold = true
                val shouldEmitPress = hasExplicitPressMapping || !hasLongMapping
                if (shouldEmitPress) {
                    return ControllerGesture(
                        input = currentInput,
                        pressType = ControllerPressType.PRESS,
                        modifier = activeModifier
                    )
                }
                return null
            }

            // PRESS-only already dispatched on ACTION_DOWN, UP produces null
            return null
        }

        return null
    }

    private fun hasMapping(
        profile: ControllerProfile,
        keyCode: Int,
        pressType: ControllerPressType,
        modifierKeyCode: Int?
    ): Boolean {
        return profile.mappings.any { mapping ->
            mapping.gesture.pressType == pressType &&
                mapping.gesture.input.keyCode == keyCode &&
                mapping.gesture.modifier?.keyCode == modifierKeyCode
        }
    }

    /**
     * Drains any buffered single-press gestures whose double-press recognition window has expired.
     */
    @Synchronized
    fun drainExpiredGestures(now: Long = clock()): List<ControllerGesture> {
        val expiredInputs = pendingSinglePressTapTimeByKey.filter { (_, tapTime) ->
            (now - tapTime) >= doubleTapTimeoutMs
        }.keys.toList()

        if (expiredInputs.isEmpty()) return emptyList()

        val drained = mutableListOf<ControllerGesture>()
        for (input in expiredInputs) {
            pendingSinglePressTapTimeByKey.remove(input)
            pendingSinglePressGestureByKey.remove(input)?.let { drained.add(it) }
        }
        return drained
    }

    @Synchronized
    fun clear() {
        lastDownTimeByKey.clear()
        lastUpTimeByKey.clear()
        downTimestampByKey.clear()
        longPressFiredByKey.clear()
        doublePressFiredByKey.clear()
        pendingSinglePressTapTimeByKey.clear()
        pendingSinglePressGestureByKey.clear()
        isModifierHeld = false
        chordFiredDuringModifierHold = false
    }

    @Synchronized
    fun reset() {
        clear()
    }
}
