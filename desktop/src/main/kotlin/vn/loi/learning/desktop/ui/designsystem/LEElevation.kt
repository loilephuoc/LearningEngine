package vn.loi.learning.desktop.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp

object LEElevation {
    val none = 0.dp
    val flat = 1.dp
    val card = 2.dp
    val popup = 4.dp
    val modal = 8.dp
}

object LEBorder {
    val thin = 1.dp
    val medium = 2.dp

    val subtle = BorderStroke(1.dp, LEColors.borderSubtle)
    val default = BorderStroke(1.dp, LEColors.borderMedium)
    val focus = BorderStroke(1.5.dp, LEColors.borderFocus)
    val dashed = BorderStroke(1.dp, LEColors.dragDropBorder)
}
