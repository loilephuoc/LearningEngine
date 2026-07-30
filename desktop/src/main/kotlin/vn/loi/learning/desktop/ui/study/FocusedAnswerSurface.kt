package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.file.Files
import java.nio.file.Path
import org.jetbrains.skia.Image
import vn.loi.learning.desktop.ui.designsystem.LEBorder
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LEIcons
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LESpacing
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.theme.LETheme
import vn.loi.learning.desktop.ui.designsystem.pos.resolvePartOfSpeechPresentation

@Composable
fun FocusedAnswerSurface(
    model: FocusedVocabularyAnswerModel,
    disclosure: FullAnswerDisclosure,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    schedulerFeedback: StudySchedulerFeedback? = null,
    typography: StudyTypographyPresentation =
        StudyTypographyPresentationResolver.resolve(
            vn.loi.learning.desktop.runtime.StudyTypographyPreferences(),
            viewportWidthDp = 0
        ),
    layout: StudyVisualLayout? = null,
    availableBodyHeightDp: Int? = null,
    typingComparison: (@Composable () -> Unit)? = null,
    currentLearningItemId: String?,
    examplesDisclosureKeyboard: ExamplesDisclosureKeyboardController,
    modifier: Modifier = Modifier
) {
    val traits = remember(model, disclosure, schedulerFeedback) {
        StudyVisualContentTraits(
            hasImage = disclosure.imageAvailable && model.imagePath != null,
            hasPronunciation = !disclosure.ipa.isNullOrBlank() || model.primaryAudioPath != null,
            hasPartOfSpeech = !disclosure.partOfSpeech.isNullOrBlank(),
            hasExamples = disclosure.examples.isNotEmpty(),
            hasSchedulerFeedback = schedulerFeedback != null
        )
    }
    val resolvedLayout = layout ?: remember(traits) {
        StudyVisualLayoutResolver.resolve(680, 800, traits)
    }

    val measuredBodyHeightDp =
        availableBodyHeightDp ?: resolvedLayout.availableAnswerHeightDp.coerceAtLeast(1)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = resolvedLayout.contentMaxWidthDp.dp)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Revealed answer: ${disclosure.englishWord}. ${disclosure.vietnameseMeaning}."
            }
    ) {
        val availableContentWidthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val responsivePolicy =
            remember(availableContentWidthDp) {
                FullAnswerResponsivePolicyResolver.resolve(availableContentWidthDp)
            }
        FullAnswerFitLayout(
            availableHeightDp = measuredBodyHeightDp,
            layout = resolvedLayout,
            hasImage = disclosure.imageAvailable && model.imagePath != null,
            modifier = Modifier.fillMaxWidth(),
        identity = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LESpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                typingComparison?.invoke()
                VocabularyIdentitySurface(
                    word = disclosure.englishWord,
                    ipa = disclosure.ipa,
                    partOfSpeech = disclosure.partOfSpeech,
                    audioPath = model.primaryAudioPath,
                    audioController = audioController,
                    strings = strings,
                    layout = resolvedLayout
                )
            }
        },
        image = { measuredImageHeightDp ->
            if (disclosure.imageAvailable && model.imagePath != null) {
                VocabularyImageBlock(
                    imagePath = model.imagePath,
                    imageDescription = strings.imageDescription,
                    audioPath = model.primaryAudioPath,
                    audioController = audioController,
                    loops = true,
                    layout = resolvedLayout,
                    imageMaxHeightDp = measuredImageHeightDp
                )
            } else {
                Spacer(Modifier.height(0.dp))
            }
        },
        meaning = {
            ResponsiveAnswerSupportingRegion(
                policy = responsivePolicy,
                meaning = disclosure.vietnameseMeaning,
                meaningAudioPath = model.meaningAudioPath,
                examples = disclosure.examples,
                currentLearningItemId = currentLearningItemId,
                examplesDisclosureKeyboard = examplesDisclosureKeyboard,
                strings = strings,
                audioController = audioController,
                typography = typography,
                englishTarget = disclosure.englishWord,
                vietnameseTarget = disclosure.vietnameseMeaning
            )
        },
        requiredExample = {
            Spacer(Modifier.height(0.dp))
        },
        schedulerFeedback = schedulerFeedback?.let { feedback ->
            @Composable {
                CompactSchedulerFeedback(feedback = feedback)
            }
        },
            continuation = null
        )
    }
}

@Composable
private fun ResponsiveAnswerSupportingRegion(
    policy: FullAnswerResponsivePolicy,
    meaning: String,
    meaningAudioPath: Path?,
    examples: List<FocusedExampleItem>,
    currentLearningItemId: String?,
    examplesDisclosureKeyboard: ExamplesDisclosureKeyboardController,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    typography: StudyTypographyPresentation,
    englishTarget: String,
    vietnameseTarget: String
) {
    val meaningContent: @Composable () -> Unit = {
        MeaningCard(
            meaning = meaning,
            meaningAudioPath = meaningAudioPath,
            meaningLabel = strings.meaningSceneLabel,
            audioController = audioController
        )
    }
    val examplesContent: @Composable () -> Unit = {
        ResponsiveExamplesSection(
            policy = policy,
            examples = examples,
            exampleLabel = strings.exampleSceneLabel,
            audioController = audioController,
            strings = strings,
            typography = typography,
            englishTarget = englishTarget,
            vietnameseTarget = vietnameseTarget,
            typingComparison = null,
            currentLearningItemId = currentLearningItemId,
            examplesDisclosureKeyboard = examplesDisclosureKeyboard
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
    ) {
        when (policy.layout) {
            AnswerSurfaceLayout.WIDE ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(Modifier.weight(policy.translationWeight)) { meaningContent() }
                    Box(Modifier.weight(policy.examplesWeight)) { examplesContent() }
                }
            AnswerSurfaceLayout.MEDIUM,
            AnswerSurfaceLayout.NARROW -> {
                meaningContent()
                ResponsiveExamplesSection(
                    policy = policy,
                    examples = examples,
                    exampleLabel = strings.exampleSceneLabel,
                    audioController = audioController,
                    strings = strings,
                    typography = typography,
                    englishTarget = englishTarget,
                    vietnameseTarget = vietnameseTarget,
                    typingComparison = null,
                    currentLearningItemId = currentLearningItemId,
                    examplesDisclosureKeyboard = examplesDisclosureKeyboard
                )
            }
        }
    }
}

@Composable
private fun ResponsiveExamplesSection(
    policy: FullAnswerResponsivePolicy,
    examples: List<FocusedExampleItem>,
    exampleLabel: String,
    audioController: LearningContentAudioController,
    strings: LearningContentRendererStrings,
    typography: StudyTypographyPresentation,
    englishTarget: String,
    vietnameseTarget: String,
    typingComparison: (@Composable () -> Unit)?,
    currentLearningItemId: String?,
    examplesDisclosureKeyboard: ExamplesDisclosureKeyboardController
) {
    if (examples.isEmpty() && typingComparison == null) return
    var itemDisclosureState by remember(currentLearningItemId, policy.layout) {
        mutableStateOf(initialItemExamplesDisclosureState(currentLearningItemId, policy))
    }
    val disclosureAvailable =
        policy.layout == AnswerSurfaceLayout.NARROW && examples.isNotEmpty()
    DisposableEffect(
        examplesDisclosureKeyboard,
        disclosureAvailable,
        currentLearningItemId
    ) {
        if (disclosureAvailable) {
            examplesDisclosureKeyboard.bind { command ->
                val result =
                    applyExamplesDisclosureCommand(itemDisclosureState.disclosure, command)
                itemDisclosureState = itemDisclosureState.copy(disclosure = result.state)
                result.consumed
            }
        }
        onDispose { examplesDisclosureKeyboard.unbind() }
    }
    if (disclosureAvailable) {
        ExamplesDisclosureControl(
            label = "$exampleLabel (${examples.size})",
            expanded = itemDisclosureState.disclosure.expanded,
            expandedDescription = strings.examplesExpanded,
            collapsedDescription = strings.examplesCollapsed,
            collapsedTooltip = strings.examplesOpenTooltip,
            expandedTooltip = strings.examplesCloseTooltip,
            onToggle = {
                itemDisclosureState =
                    itemDisclosureState.copy(
                        disclosure = toggleExamplesDisclosure(itemDisclosureState.disclosure)
                    )
            }
        )
    }
    typingComparison?.invoke()
    if (itemDisclosureState.disclosure.expanded && examples.isNotEmpty()) {
        ExampleCard(
            examples = examples,
            exampleLabel = exampleLabel,
            audioController = audioController,
            strings = strings,
            typography = typography,
            englishTarget = englishTarget,
            vietnameseTarget = vietnameseTarget
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamplesDisclosureControl(
    label: String,
    expanded: Boolean,
    expandedDescription: String,
    collapsedDescription: String,
    collapsedTooltip: String,
    expandedTooltip: String,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val tooltip = if (expanded) expandedTooltip else collapsedTooltip
    val tooltipState = rememberTooltipState()
    LaunchedEffect(focused) {
        if (focused) tooltipState.show() else tooltipState.dismiss()
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = tooltipState
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        contentDescription = "$label. $tooltip"
                        stateDescription =
                            if (expanded) expandedDescription else collapsedDescription
                    }
                    .hoverable(interactionSource)
                    .clickable(interactionSource = interactionSource, onClick = onToggle)
                    .onPreviewKeyEvent { event ->
                        when {
                            event.key != Key.Enter && event.key != Key.Spacebar -> false
                            event.type == KeyEventType.KeyDown -> true
                            event.type == KeyEventType.KeyUp -> {
                                onToggle()
                                true
                            }
                            else -> false
                        }
                    }
                    .focusable(interactionSource = interactionSource),
            shape = LETheme.shapes.radiusM,
            color = LETheme.colors.surfaceSecondary,
            border =
                if (focused) {
                    BorderStroke(LETheme.borders.thick, LETheme.colors.borderFocus)
                } else {
                    LETheme.borders.subtle
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = LESpacing.md, vertical = LESpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = LETheme.typography.sectionTitle, fontWeight = FontWeight.Bold)
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
fun VocabularyIdentitySurface(
    word: String,
    ipa: String?,
    partOfSpeech: String?,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    strings: LearningContentRendererStrings,
    layout: StudyVisualLayout? = null,
    modifier: Modifier = Modifier
) {
    val hasAudio = audioPath != null
    val interactionSource = remember { MutableInteractionSource() }
    val isLooping = hasAudio && audioController.activeLoopPath == audioPath

    val baseModifier = if (hasAudio) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = if (isLooping) "Dừng phát lặp từ tiếng Anh: $word" else "Phát lặp từ tiếng Anh: $word"
                stateDescription = if (isLooping) "Đang phát lặp" else "Chưa phát lặp"
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {
                audioController.toggleLoop(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                    audioController.toggleLoop(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val presentation = resolveStudyAnswerInteractionStyle(
        colors = LETheme.colors,
        borders = LETheme.borders,
        enabled = hasAudio,
        hovered = hovered,
        pressed = pressed,
        focused = focused,
        activeLoop = isLooping
    )
    Surface(
        modifier = baseModifier,
        shape = LETheme.shapes.radiusL,
        color = presentation.containerColor,
        border = BorderStroke(presentation.borderWidth, presentation.borderColor)
    ) {
        val wordSize = (layout?.identityWordFontSizeSp ?: 52).sp
        val lineHeight = (layout?.identityWordLineHeightSp ?: 58).sp
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val identityPresentation =
                resolveStudyIdentityPresentation(maxWidth.value.toInt().coerceAtLeast(1))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(identityPresentation.cardPaddingDp.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(identityPresentation.verticalGapDp.dp)
            ) {
            Text(
                text = word,
                style = LETheme.typography.displayWord.copy(
                    fontSize = wordSize,
                    lineHeight = lineHeight,
                    color = presentation.primaryContentColor
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            InlinePronunciationRow(
                ipa = ipa,
                partOfSpeech = partOfSpeech,
                audioPath = audioPath,
                audioController = audioController,
                strings = strings,
                presentation = identityPresentation
            )
            }
        }
    }
}

@Composable
fun InlinePronunciationRow(
    ipa: String?,
    partOfSpeech: String?,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    strings: LearningContentRendererStrings,
    presentation: StudyIdentityPresentation =
        resolveStudyIdentityPresentation(COMFORTABLE_IDENTITY_CARD_WIDTH_DP),
    modifier: Modifier = Modifier
) {
    val hasIpa = !ipa.isNullOrBlank()
    val hasPos = !partOfSpeech.isNullOrBlank()
    val hasAudio = audioPath != null

    if (!hasIpa && !hasPos && !hasAudio) return

    val isStacked = presentation.composition == StudyIdentityComposition.STACKED

    if (isStacked) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(presentation.verticalGapDp.dp)
        ) {
            if (hasAudio) {
                CompactAudioReplayButton(
                    path = audioPath!!,
                    audioController = audioController,
                    description = strings.promptAudioLabel,
                    isPrimary = true,
                    buttonSizeDp = presentation.speakerButtonSizeDp,
                    iconSizeDp = presentation.speakerIconSizeDp
                )
            }

            if (!ipa.isNullOrBlank()) {
                val formattedIpa = if (ipa.startsWith("/") && ipa.endsWith("/")) ipa else "/$ipa/"
                Text(
                    text = formattedIpa,
                    fontSize = presentation.ipaFontSizeSp.sp,
                    fontStyle = FontStyle.Italic,
                    style = LETheme.typography.metadataIpa
                )
            }

            if (!partOfSpeech.isNullOrBlank()) {
                StudyPosBadge(partOfSpeech, identityPresentation = presentation)
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(presentation.horizontalGapDp.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasAudio) {
                CompactAudioReplayButton(
                    path = audioPath!!,
                    audioController = audioController,
                    description = strings.promptAudioLabel,
                    isPrimary = true,
                    buttonSizeDp = presentation.speakerButtonSizeDp,
                    iconSizeDp = presentation.speakerIconSizeDp
                )
            }

            if (!ipa.isNullOrBlank()) {
                val formattedIpa = if (ipa.startsWith("/") && ipa.endsWith("/")) ipa else "/$ipa/"
                Text(
                    text = formattedIpa,
                    fontSize = presentation.ipaFontSizeSp.sp,
                    fontStyle = FontStyle.Italic,
                    style = LETheme.typography.metadataIpa
                )
            }

            if (!partOfSpeech.isNullOrBlank()) {
                StudyPosBadge(partOfSpeech, identityPresentation = presentation)
            }
        }
    }
}

@Composable
internal fun StudyPosBadge(
    partOfSpeech: String,
    identityPresentation: StudyIdentityPresentation? = null,
    modifier: Modifier = Modifier
) {
    val resolved = resolvePartOfSpeechPresentation(partOfSpeech, LETheme.partOfSpeech) ?: return
    val style = resolved.style
    Surface(
        modifier = modifier,
        color = style.containerColor,
        contentColor = style.contentColor,
        border = BorderStroke(LETheme.borders.thin, style.borderColor),
        shape = LETheme.shapes.radiusS
    ) {
        Text(
            text = resolved.canonicalLabel,
            style = LETheme.typography.meaningPos.copy(color = style.contentColor),
            modifier = Modifier.padding(
                horizontal =
                    identityPresentation?.posHorizontalPaddingDp?.dp
                        ?: LETheme.spacing.space3,
                vertical =
                    identityPresentation?.posVerticalPaddingDp?.dp
                        ?: LETheme.spacing.space2
            )
        )
    }
}

@Composable
internal fun StudyMeaningPosGroup(
    partOfSpeech: String?,
    modifier: Modifier = Modifier,
    meaningContent: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space2),
        verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space2)
    ) {
        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
            meaningContent()
        }
        resolveStudyMeaningPos(partOfSpeech)?.let { meaningPos ->
            StudyPosBadge(
                partOfSpeech = meaningPos,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun CompactAudioReplayButton(
    path: Path,
    audioController: LearningContentAudioController,
    description: String,
    isPrimary: Boolean = false,
    loops: Boolean = true,
    buttonSizeDp: Int = 40,
    iconSizeDp: Int = 22,
    modifier: Modifier = Modifier
) {
    val isLooping = audioController.activeLoopPath == path

    IconButton(
        onClick = {
            if (loops) audioController.toggleLoop(path) else audioController.playOnce(path)
        },
        modifier = modifier
            .size(buttonSizeDp.dp)
            .semantics {
                contentDescription = if (isLooping) "Stop loop: $description" else "Play audio: $description" + (if (isPrimary) " [R]" else "")
                stateDescription = if (isLooping) "Loop active" else "Loop inactive"
            }
    ) {
        Icon(
            imageVector = if (isLooping) LEIcons.Stop else LEIcons.Audio,
            contentDescription = null,
            tint = if (isLooping) LETheme.colors.accentPrimary else LETheme.colors.textSecondary,
            modifier = Modifier.size(iconSizeDp.dp)
        )
    }
}

@Composable
fun VocabularyImageBlock(
    imagePath: Path,
    imageDescription: String,
    audioPath: Path? = null,
    audioController: LearningContentAudioController? = null,
    loops: Boolean = false,
    layout: StudyVisualLayout,
    imageMaxHeightDp: Int = layout.imageMaxHeightDp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imagePath) {
        runCatching {
            Image.makeFromEncoded(Files.readAllBytes(imagePath)).toComposeImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        val maxW = layout.imageMaxWidthDp.dp
        val maxH = imageMaxHeightDp.dp
        val enabled = audioPath != null && audioController != null
        val interactionSource = remember { MutableInteractionSource() }
        val isLooping = enabled && loops && audioController?.activeLoopPath == audioPath
        val presentation = rememberAudioInteractionPresentation(
            interactionSource = interactionSource,
            enabled = enabled,
            activeLoop = isLooping
        )
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .widthIn(max = maxW)
                    .fillMaxWidth()
                    .height(maxH)
                    .audioPressable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    description = when {
                        isLooping -> "D\u1eebng ph\u00e1t l\u1eb7p t\u1eeb ti\u1ebfng Anh"
                        loops -> "Ph\u00e1t l\u1eb7p t\u1eeb ti\u1ebfng Anh"
                        else -> "Nghe t\u1eeb ti\u1ebfng Anh"
                    },
                    state = if (loops) {
                        if (isLooping) "\u0110ang ph\u00e1t l\u1eb7p" else "Ch\u01b0a ph\u00e1t l\u1eb7p"
                    } else null
                ) {
                    if (loops) {
                        audioController!!.toggleLoop(audioPath!!)
                    } else {
                        audioController!!.playOnce(audioPath!!)
                    }
                    },
                shape = LETheme.shapes.radiusL,
                color = presentation.containerColor,
                border = presentation.border
            ) {
                Box {
                    Image(
                        bitmap = bitmap,
                        contentDescription = imageDescription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxH)
                            .clip(LETheme.shapes.radiusL),
                        contentScale = ContentScale.Fit
                    )
                    if (enabled) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(42.dp),
                            shape = LETheme.shapes.radiusPill,
                            color = LETheme.colors.accentSoft
                        ) {
                            Icon(
                                imageVector = if (isLooping) LEIcons.Stop else LEIcons.Audio,
                                contentDescription = null,
                                tint = presentation.iconColor,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeaningCard(
    meaning: String,
    meaningAudioPath: Path? = null,
    meaningLabel: String = "Meaning",
    audioController: LearningContentAudioController? = null,
    modifier: Modifier = Modifier
) {
    val compactLayout = CompactMeaningLayout()
    val hasAudio = meaningAudioPath != null && audioController != null
    val interactionSource = remember { MutableInteractionSource() }

    val surfaceModifier = if (hasAudio) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Phát nghĩa tiếng Việt: $meaning"
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {
                audioController!!.playOnce(meaningAudioPath!!)
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                    audioController!!.playOnce(meaningAudioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    LESurface(
        variant = StudySurfaceRoles.meaning,
        modifier = surfaceModifier,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = compactLayout.horizontalPaddingDp.dp,
                    vertical = compactLayout.verticalPaddingDp.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = LETheme.shapes.radiusM,
                color = if (hasAudio) LETheme.colors.accentSoft else LETheme.colors.surfaceSecondary,
                modifier = Modifier.size(compactLayout.iconSizeDp.dp)
            ) {
                Icon(
                    imageVector = if (hasAudio) LEIcons.Audio else LEIcons.Help,
                    contentDescription = null,
                    tint = if (hasAudio) LETheme.colors.accentPrimary else LETheme.colors.textMuted,
                    modifier = Modifier.padding(compactLayout.iconPaddingDp.dp)
                )
            }
            Text(
                text = meaning,
                fontSize = compactLayout.textSizeSp.sp,
                lineHeight = compactLayout.textLineHeightSp.sp,
                fontWeight = FontWeight.SemiBold,
                style = LETheme.typography.meaningPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ExampleCard(
    examples: List<FocusedExampleItem>,
    exampleLabel: String = "Examples",
    audioController: LearningContentAudioController,
    strings: LearningContentRendererStrings,
    typography: StudyTypographyPresentation,
    englishTarget: String,
    vietnameseTarget: String,
    modifier: Modifier = Modifier
) {
    LESurface(
        variant = StudySurfaceRoles.example,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.md),
            verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            Text(
                text = exampleLabel.uppercase(),
                style = LETheme.typography.sectionTitle,
                color = LETheme.colors.accentPrimary,
                fontWeight = FontWeight.Bold
            )
            examples.forEach { example ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LETheme.shapes.radiusM,
                    color = LETheme.colors.surfacePrimary,
                    border = LETheme.borders.subtle
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    // English Example Row
                    EnglishExampleAudioRow(
                        englishText = example.englishText,
                        target = englishTarget,
                        audioPath = example.englishAudioPath ?: example.audioPath,
                        audioController = audioController,
                        typography = typography
                    )
                    // Vietnamese Translation Row
                    if (
                        !example.vietnameseTranslation.isNullOrBlank()
                    ) {
                        VietnameseExampleAudioRow(
                            vietnameseTranslation = example.vietnameseTranslation,
                            target = vietnameseTarget,
                            audioPath = example.vietnameseAudioPath,
                            audioController = audioController,
                            typography = typography
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun EnglishExampleAudioRow(
    englishText: String,
    target: String,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    typography: StudyTypographyPresentation,
    modifier: Modifier = Modifier
) {
    val hasAudio = audioPath != null
    val interactionSource = remember { MutableInteractionSource() }
    val isLooping = hasAudio && audioController.activeLoopPath == audioPath

    val rowModifier = if (hasAudio) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = if (isLooping) "Dừng phát lặp ví dụ tiếng Anh: $englishText" else "Phát lặp ví dụ tiếng Anh: $englishText"
                stateDescription = if (isLooping) "Loop active" else "Loop inactive"
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {
                audioController.toggleLoop(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                    audioController.toggleLoop(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    val exampleHovered by interactionSource.collectIsHoveredAsState()
    val examplePressed by interactionSource.collectIsPressedAsState()
    val exampleFocused by interactionSource.collectIsFocusedAsState()
    val presentation = resolveStudyExampleInteractionStyle(
        colors = LETheme.colors,
        borders = LETheme.borders,
        kind = StudyExampleRowKind.ENGLISH,
        enabled = hasAudio,
        hovered = exampleHovered,
        pressed = examplePressed,
        focused = exampleFocused,
        activeLoop = isLooping
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LETheme.shapes.radiusM,
        color = presentation.containerColor,
        border = BorderStroke(presentation.borderWidth, presentation.borderColor)
    ) {
        Row(
            modifier = rowModifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasAudio) {
                Icon(
                    if (isLooping) LEIcons.Stop else LEIcons.Audio,
                    contentDescription = null,
                    tint = presentation.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = highlightedExampleText(
                    text = englishText,
                    target = target,
                    language = ExampleTargetLanguage.ENGLISH,
                    highlightStyle = SpanStyle(
                        color = presentation.highlightColor,
                        fontWeight = FontWeight.Bold
                    )
                ),
                fontSize = typography.exampleEnglishFontSize.sp,
                lineHeight = typography.exampleEnglishLineHeight.sp,
                fontWeight = FontWeight.SemiBold,
                color = presentation.contentColor,
                softWrap = typography.softWrap,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VietnameseExampleAudioRow(
    vietnameseTranslation: String,
    target: String,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    typography: StudyTypographyPresentation,
    modifier: Modifier = Modifier
) {
    val hasAudio = audioPath != null
    val interactionSource = remember { MutableInteractionSource() }

    val rowModifier = if (hasAudio) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Phát bản dịch tiếng Việt: $vietnameseTranslation"
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {
                audioController.playOnce(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                    audioController.playOnce(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    val exampleHovered by interactionSource.collectIsHoveredAsState()
    val examplePressed by interactionSource.collectIsPressedAsState()
    val exampleFocused by interactionSource.collectIsFocusedAsState()
    val presentation = resolveStudyExampleInteractionStyle(
        colors = LETheme.colors,
        borders = LETheme.borders,
        kind = StudyExampleRowKind.VIETNAMESE,
        enabled = hasAudio,
        hovered = exampleHovered,
        pressed = examplePressed,
        focused = exampleFocused,
        activeLoop = false
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LETheme.shapes.radiusM,
        color = presentation.containerColor,
        border = BorderStroke(presentation.borderWidth, presentation.borderColor)
    ) {
        Row(
            modifier = rowModifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasAudio) {
                Icon(
                    LEIcons.Audio,
                    contentDescription = null,
                    tint = presentation.iconColor,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Spacer(Modifier.size(22.dp))
            }
            Text(
                text = highlightedExampleText(
                    text = vietnameseTranslation,
                    target = target,
                    language = ExampleTargetLanguage.VIETNAMESE,
                    highlightStyle = SpanStyle(
                        color = presentation.highlightColor,
                        fontWeight = FontWeight.Bold
                    )
                ),
                fontSize = typography.exampleVietnameseFontSize.sp,
                lineHeight = typography.exampleVietnameseLineHeight.sp,
                fontWeight = FontWeight.Normal,
                color = presentation.contentColor,
                softWrap = typography.softWrap,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
