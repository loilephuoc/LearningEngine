package vn.loi.learning.android.controller

/**
 * Deterministic mapping resolution and conflict detection engine.
 * Supports action-centric Everywhere-applicable (GLOBAL) bindings with exact context overrides.
 */
object ControllerMappingResolver {

    /**
     * Checks if two contexts represent the same runtime card stage (e.g. STUDY_RATING and STUDY_REVEALED).
     */
    fun areComplementaryContexts(ctx1: ControllerContext, ctx2: ControllerContext): Boolean {
        return (ctx1 == ControllerContext.STUDY_RATING && ctx2 == ControllerContext.STUDY_REVEALED) ||
               (ctx1 == ControllerContext.STUDY_REVEALED && ctx2 == ControllerContext.STUDY_RATING)
    }

    /**
     * Resolves a semantic ControllerAction given the configuration, active context, and gesture.
     *
     * Precedence:
     * 1. Active profile: Most-specific context mapping (e.g. STUDY_QUESTION, STUDY_RATING, AUTO_PLAY, SHADOWING)
     * 1b. Active profile: Complementary context fallback (STUDY_RATING <-> STUDY_REVEALED)
     * 2. Active profile: Everywhere-applicable (GLOBAL) mapping, filtered by Action applicability in the active context
     * 3. Default profile fallback: Specific context -> Complementary context -> Everywhere-applicable (GLOBAL)
     * 4. Unmapped (null)
     */
    fun resolve(
        config: ControllerConfig,
        context: ControllerContext,
        gesture: ControllerGesture
    ): ControllerAction? {
        if (!config.isControllerEnabled) return null

        val activeProfile = config.activeProfile
        if (!activeProfile.enabled) return null

        // 1. Most-specific context in active profile
        val specificAction = findActionInProfile(activeProfile, context, gesture)
        if (specificAction != null) return specificAction

        // 1b. Complementary context fallback (STUDY_RATING <-> STUDY_REVEALED)
        val complementaryContext = when (context) {
            ControllerContext.STUDY_RATING -> ControllerContext.STUDY_REVEALED
            ControllerContext.STUDY_REVEALED -> ControllerContext.STUDY_RATING
            else -> null
        }
        if (complementaryContext != null) {
            val compAction = findActionInProfile(activeProfile, complementaryContext, gesture)
            if (compAction != null) return compAction
        }

        // 2. Everywhere-applicable (GLOBAL) in active profile, validated by centralized Action applicability
        if (context != ControllerContext.GLOBAL) {
            val globalAction = findActionInProfile(activeProfile, ControllerContext.GLOBAL, gesture)
            if (globalAction != null && globalAction.isApplicableIn(context)) return globalAction
        }

        // 3. Default profile fallback (if custom active profile omits the mapping)
        if (activeProfile.id != DefaultControllerProfiles.DEFAULT_PROFILE_ID) {
            val defaultProfile = DefaultControllerProfiles.defaultProfile()
            val defSpecific = findActionInProfile(defaultProfile, context, gesture)
            if (defSpecific != null) return defSpecific

            if (complementaryContext != null) {
                val defComp = findActionInProfile(defaultProfile, complementaryContext, gesture)
                if (defComp != null) return defComp
            }

            if (context != ControllerContext.GLOBAL) {
                val defGlobal = findActionInProfile(defaultProfile, ControllerContext.GLOBAL, gesture)
                if (defGlobal != null && defGlobal.isApplicableIn(context)) return defGlobal
            }
        }

        return null
    }

    private fun findActionInProfile(
        profile: ControllerProfile,
        context: ControllerContext,
        gesture: ControllerGesture
    ): ControllerAction? {
        return profile.mappings.firstOrNull { mapping ->
            mapping.context == context && matchGesture(mapping.gesture, gesture)
        }?.action
    }

    fun matchGesture(mappingGesture: ControllerGesture, inputGesture: ControllerGesture): Boolean {
        if (mappingGesture.pressType != inputGesture.pressType) return false

        // Check primary input
        if (!mappingGesture.input.matches(
                inputGesture.input.vendorId,
                inputGesture.input.productId,
                inputGesture.input.keyCode
            )
        ) return false

        // Check modifier
        val mapMod = mappingGesture.modifier
        val inMod = inputGesture.modifier

        if (mapMod == null && inMod == null) return true
        if (mapMod != null && inMod != null) {
            return mapMod.matches(inMod.vendorId, inMod.productId, inMod.keyCode)
        }
        return false
    }

    /**
     * Checks if a mapping conflicts with an existing mapping in the profile (same context/complementary context + gesture).
     */
    fun findConflict(profile: ControllerProfile, mapping: ControllerMapping): ControllerMapping? {
        return profile.mappings.firstOrNull { existing ->
            existing != mapping &&
            (existing.context == mapping.context || areComplementaryContexts(existing.context, mapping.context)) &&
            matchGesture(existing.gesture, mapping.gesture)
        }
    }

    /**
     * Upserts a mapping into a profile, replacing any conflicting mapping for the same context (or complementary context) and gesture.
     */
    fun upsertMapping(profile: ControllerProfile, mapping: ControllerMapping): ControllerProfile {
        val filtered = profile.mappings.filterNot { existing ->
            (existing.context == mapping.context || areComplementaryContexts(existing.context, mapping.context)) &&
            matchGesture(existing.gesture, mapping.gesture)
        }
        return profile.copy(mappings = filtered + mapping)
    }

    /**
     * Replaces an existing mapping with an updated mapping.
     * Guarantees that the old mapping is removed even if the context or button/gesture changed.
     */
    fun replaceMapping(
        profile: ControllerProfile,
        oldMapping: ControllerMapping,
        newMapping: ControllerMapping
    ): ControllerProfile {
        val withoutOld = removeMapping(profile, oldMapping.context, oldMapping.gesture)
        return upsertMapping(withoutOld, newMapping)
    }

    /**
     * Removes a mapping matching context (or complementary context) and gesture from a profile.
     */
    fun removeMapping(
        profile: ControllerProfile,
        context: ControllerContext,
        gesture: ControllerGesture
    ): ControllerProfile {
        val filtered = profile.mappings.filterNot { existing ->
            (existing.context == context || areComplementaryContexts(existing.context, context)) &&
            matchGesture(existing.gesture, gesture)
        }
        return profile.copy(mappings = filtered)
    }
}
