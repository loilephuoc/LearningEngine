package vn.loi.learning.android.study.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.ui.LearningContentTypography
import vn.loi.learning.android.ui.LearningEngineShapes

internal object StudySpacing {
    val micro = 6.dp
    val group = 12.dp
    val section = 18.dp
    val examplePairGap = 10.dp
    val answerMetadataGap = 6.dp
    val controlsGap = 10.dp
    val choiceGap = 10.dp
    val screenPadding = 16.dp
}

internal object StudyTypography {
    val prompt: TextStyle = LearningContentTypography.sectionTitle
    val targetAnswer: TextStyle = LearningContentTypography.vocabulary
    val meaning: TextStyle = LearningContentTypography.meaning
    val metadata: TextStyle = LearningContentTypography.pronunciation
    val exampleEnglish: TextStyle = LearningContentTypography.example
    val exampleVietnamese: TextStyle = LearningContentTypography.example
    val input: TextStyle = LearningContentTypography.example
    val choice: TextStyle = LearningContentTypography.example
    val feedback: TextStyle = LearningContentTypography.example
}

internal object StudyShapes {
    val stage = LearningEngineShapes.large
    val semanticSurface = LearningEngineShapes.medium
    val interactive = LearningEngineShapes.large
}

internal data class StudyFeedbackPalette(val container: Color, val border: Color, val content: Color)
