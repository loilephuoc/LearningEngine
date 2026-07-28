package vn.loi.learning.desktop.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Iconography System Tokens for Learning Engine 2.0 (PLE-028A Chapter 15).
 * Encapsulates vector icon roles across primary, secondary, metadata, status and scheduler components.
 */
@Immutable
data class LEIconsTokens(
    val New: ImageVector = Icons.Default.Add,
    val Save: ImageVector = Icons.Default.Save,
    val Discard: ImageVector = Icons.Default.Refresh,
    val Delete: ImageVector = Icons.Default.Delete,
    val Search: ImageVector = Icons.Default.Search,
    val Filter: ImageVector = Icons.Default.FilterList,
    val Image: ImageVector = Icons.Default.Image,
    val Audio: ImageVector = Icons.Default.VolumeUp,
    val Play: ImageVector = Icons.Default.PlayArrow,
    val Stop: ImageVector = Icons.Default.Stop,
    val Replace: ImageVector = Icons.Default.SwapHoriz,
    val Open: ImageVector = Icons.Default.OpenInNew,
    val Remove: ImageVector = Icons.Default.Close,
    val Fullscreen: ImageVector = Icons.Default.OpenInFull,
    val ZoomIn: ImageVector = Icons.Default.ZoomIn,
    val ZoomOut: ImageVector = Icons.Default.ZoomOut,
    val FitWidth: ImageVector = Icons.Default.FitScreen,
    val FitHeight: ImageVector = Icons.Default.AspectRatio,
    val Success: ImageVector = Icons.Default.CheckCircle,
    val Warning: ImageVector = Icons.Default.Warning,
    val Missing: ImageVector = Icons.Default.ErrorOutline,
    val Keyboard: ImageVector = Icons.Default.Keyboard,
    val Help: ImageVector = Icons.Default.HelpOutline,
    val Settings: ImageVector = Icons.Default.Settings,
    val Learning: ImageVector = Icons.Default.School,
    val Scheduler: ImageVector = Icons.Default.Schedule,
    val StatisticsTotal: ImageVector = Icons.Default.Layers,
    val StatisticsNew: ImageVector = Icons.Default.AddCircle,
    val StatisticsReview: ImageVector = Icons.Default.Refresh,
    val StatisticsDue: ImageVector = Icons.Default.Schedule,
    val StatisticsAgain: ImageVector = Icons.Default.Replay,
    val StatisticsHard: ImageVector = Icons.Default.FitnessCenter,
    val StatisticsGood: ImageVector = Icons.Default.ThumbUp,
    val StatisticsEasy: ImageVector = Icons.Default.Star
)

/** Default singleton instance of [LEIconsTokens] */
val DefaultLEIcons = LEIconsTokens()
