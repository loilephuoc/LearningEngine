package vn.loi.learning.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
private fun LearningEngineDesignCatalog() {
    Column(Modifier.padding(LearningSpacing.screen), verticalArrangement = Arrangement.spacedBy(LearningSpacing.section)) {
        LearningEngineSectionHeader("Learning Engine", "Material 3 foundation · Tiếng Việt & English")
        Text("Từ vựng", style = LearningContentTypography.vocabulary)
        Text("/təˈmɑːr.oʊ/", style = LearningContentTypography.pronunciation)
        Text("Nghĩa và văn bản ví dụ", style = LearningContentTypography.example)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            LearningEnginePrimaryButton("Primary", {})
            LearningEngineSecondaryButton("Tonal", {})
        }
        LearningEngineCard { LearningEngineSectionHeader("Card", "Calm layered surface") }
        LearningEngineProgress(0.64f, "64% complete")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            LearningEngineStatusBadge("Active", LearningStatusTone.ACTIVE)
            LearningEngineFeedbackBadge(LearningDifficultyTone.DIFFICULT)
            LearningEngineFeedbackBadge(LearningDifficultyTone.EASY)
        }
        LearningEngineEmptyState("Chưa có nội dung", "Mục học tiếp theo sẽ xuất hiện tại đây.")
        LearningEngineErrorState("Could not load", "Try again when you are ready.")
    }
}

@Preview(name = "Learning Engine Light", showBackground = true)
@Composable private fun LightCatalogPreview() = LearningEngineTheme(AndroidThemeMode.LIGHT) { LearningEngineDesignCatalog() }

@Preview(name = "Learning Engine Dark", showBackground = true)
@Composable private fun DarkCatalogPreview() = LearningEngineTheme(AndroidThemeMode.DARK) { LearningEngineDesignCatalog() }
