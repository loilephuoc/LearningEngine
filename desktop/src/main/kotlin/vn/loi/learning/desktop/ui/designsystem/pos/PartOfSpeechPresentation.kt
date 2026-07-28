package vn.loi.learning.desktop.ui.designsystem.pos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry
import vn.loi.learning.desktop.ui.theme.LEPartOfSpeechTokens
import vn.loi.learning.desktop.ui.theme.LEPosBadgeStyle
import vn.loi.learning.desktop.ui.theme.LEPosColorFamily

internal val LocalPartOfSpeechRegistry =
    staticCompositionLocalOf { PartOfSpeechSemanticRegistry() }

@Immutable
data class PartOfSpeechPresentation(
    val canonicalLabel: String,
    val semanticIdentity: String,
    val style: LEPosBadgeStyle
)

@Composable
fun ProvidePartOfSpeechRegistry(
    registry: PartOfSpeechSemanticRegistry,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPartOfSpeechRegistry provides registry, content = content)
}

@Composable
fun resolvePartOfSpeechPresentation(
    raw: String?,
    tokens: LEPartOfSpeechTokens
): PartOfSpeechPresentation? =
    LocalPartOfSpeechRegistry.current.resolve(raw)?.let { identity ->
        PartOfSpeechPresentation(
            canonicalLabel = identity.canonical.value,
            semanticIdentity = identity.colorKey.stableId,
            style = tokens.resolve(
                family = LEPosColorFamily.valueOf(identity.colorKey.family.name),
                visualSlot = identity.colorKey.visualSlot
            )
        )
    }
