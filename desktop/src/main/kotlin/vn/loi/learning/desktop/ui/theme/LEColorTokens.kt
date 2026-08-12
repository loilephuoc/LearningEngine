package vn.loi.learning.desktop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic Color Tokens for Learning Engine 2.0 (PLE-028A Specification).
 * Components MUST consume semantic tokens via [LETheme.colors] and NEVER hardcode raw [Color] hex values.
 */
@Immutable
data class LEColors(
    // Canvas & Main Surfaces
    val windowBackground: Color,
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceMeaning: Color,
    val surfaceExample: Color,
    val surfaceScheduler: Color,
    val surfaceToolbar: Color,

    // Borders & Outlines
    val borderSubtle: Color,
    val borderMedium: Color,
    val borderFocus: Color,

    // Typography & Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,

    // Accent Colors
    val accentPrimary: Color,
    val accentHover: Color,
    val accentSoft: Color,

    // Status & Feedback Colors
    val danger: Color,
    val dangerContainer: Color,
    val dangerText: Color,

    val warning: Color,
    val warningContainer: Color,
    val warningText: Color,

    val success: Color,
    val successContainer: Color,
    val successText: Color,

    val info: Color,
    val infoContainer: Color,

    // Domain Learning Stages
    val stageNew: Color,
    val stageLearning: Color,
    val stageReview: Color,
    val stageMastered: Color,

    // Package Card & Progress Metrics
    val packageCardBackground: Color,
    val packageCardBorder: Color,
    val metricPurple: Color,
    val metricNeutral: Color,
    val metricOrange: Color,
    val metricRed: Color,
    val metricBlue: Color,
    val metricGreen: Color,
    val progressSurface: Color,
    val progressTrack: Color,

    // Waveform & Media
    val waveformActive: Color,
    val waveformInactive: Color,
    val dragDropBorder: Color
)

/**
 * Light Theme Color Palette Specification (PLE-028A Chapter 3).
 * Clean, pure crisp white canvas eliminating lavender/gray tints.
 */
val LightLEColors = LEColors(
    windowBackground = Color(0xFFF8FAFC),
    surfacePrimary = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFF1F5F9),
    surfaceMeaning = Color(0xFFF8FAF6),
    surfaceExample = Color(0xFFF8FAFC),
    surfaceScheduler = Color(0xFFF8FAFC),
    surfaceToolbar = Color(0xFFFFFFFF),

    borderSubtle = Color(0xFFE2E8F0),
    borderMedium = Color(0xFFCBD5E1),
    borderFocus = Color(0xFF3B82F6),

    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    textDisabled = Color(0xFFA1A1AA),

    accentPrimary = Color(0xFF2563EB),
    accentHover = Color(0xFF1D4ED8),
    accentSoft = Color(0xFFDBEAFE),

    danger = Color(0xFFDC2626),
    dangerContainer = Color(0xFFFEE2E2),
    dangerText = Color(0xFFB91C1C),

    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFFEF3C7),
    warningText = Color(0xFFB45309),

    success = Color(0xFF16A34A),
    successContainer = Color(0xFFDCFCE7),
    successText = Color(0xFF15803D),

    info = Color(0xFF2563EB),
    infoContainer = Color(0xFFDBEAFE),

    stageNew = Color(0xFF6366F1),
    stageLearning = Color(0xFFD97706),
    stageReview = Color(0xFF16A34A),
    stageMastered = Color(0xFF0284C7),

    packageCardBackground = Color(0xFFFFFFFF),
    packageCardBorder = Color(0xFFE2E8F0),
    metricPurple = Color(0xFF2563EB),
    metricNeutral = Color(0xFF475569),
    metricOrange = Color(0xFFEA580C),
    metricRed = Color(0xFFDC2626),
    metricBlue = Color(0xFF2563EB),
    metricGreen = Color(0xFF16A34A),
    progressSurface = Color(0xFFEFF6FF),
    progressTrack = Color(0xFFDBEAFE),

    waveformActive = Color(0xFF2563EB),
    waveformInactive = Color(0xFFCBD5E1),
    dragDropBorder = Color(0xFFCBD5E1)
)

/**
 * Dark Theme Color Palette Specification (PLE-028A Chapter 4).
 * Deep charcoal layering with WCAG AAA/AA high contrast remediation for IPA, meanings & examples.
 */
val DarkLEColors = LEColors(
    windowBackground = Color(0xFF090D16),
    surfacePrimary = Color(0xFF161B26),
    surfaceSecondary = Color(0xFF212838),
    surfaceMeaning = Color(0xFF0F291E),
    surfaceExample = Color(0xFF121824),
    surfaceScheduler = Color(0xFF161B26),
    surfaceToolbar = Color(0xFF161B26),

    borderSubtle = Color(0xFF273248),
    borderMedium = Color(0xFF3B4A6B),
    borderFocus = Color(0xFF60A5FA),

    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFE2E8F0),
    textMuted = Color(0xFF94A3B8),
    textDisabled = Color(0xFF64748B),

    accentPrimary = Color(0xFF60A5FA),
    accentHover = Color(0xFF93C5FD),
    accentSoft = Color(0xFF1E3A5F),

    danger = Color(0xFFEF4444),
    dangerContainer = Color(0xFF450A0A),
    dangerText = Color(0xFFFCA5A5),

    warning = Color(0xFFF59E0B),
    warningContainer = Color(0xFF451A03),
    warningText = Color(0xFFFCD34D),

    success = Color(0xFF22C55E),
    successContainer = Color(0xFF052E16),
    successText = Color(0xFF86EFAC),

    info = Color(0xFF3B82F6),
    infoContainer = Color(0xFF172554),

    stageNew = Color(0xFF818CF8),
    stageLearning = Color(0xFFF59E0B),
    stageReview = Color(0xFF22C55E),
    stageMastered = Color(0xFF38BDF8),

    packageCardBackground = Color(0xFF161B26),
    packageCardBorder = Color(0xFF273248),
    metricPurple = Color(0xFF60A5FA),
    metricNeutral = Color(0xFF94A3B8),
    metricOrange = Color(0xFFFB923C),
    metricRed = Color(0xFFF87171),
    metricBlue = Color(0xFF60A5FA),
    metricGreen = Color(0xFF4ADE80),
    progressSurface = Color(0xFF172554),
    progressTrack = Color(0xFF273248),

    waveformActive = Color(0xFF60A5FA),
    waveformInactive = Color(0xFF3B4A6B),
    dragDropBorder = Color(0xFF3B4A6B)
)
