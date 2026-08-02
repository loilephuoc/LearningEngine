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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.platform.LocalDensity
import java.nio.file.Files
import java.nio.file.Path
import org.jetbrains.skia.Image
import vn.loi.learning.desktop.ui.designsystem.LEBorder
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LEIcons
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LESpacing
import vn.loi.learning.desktop.ui.theme.LETheme
import vn.loi.learning.desktop.ui.designsystem.pos.resolvePartOfSpeechPresentation

@Composable
internal fun FocusedAnswerSurface(
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
    typingComparison: TypingRevealComparisonPresentation? = null,
    currentLearningItemId: String?,
    examplesDisclosureKeyboard: ExamplesDisclosureKeyboardController,
    revealProgress: Float = 1f,
    signaturePresentation: SignatureStudyPresentation =
        SignatureStudyPresentationResolver.resolve(800, 800),
    modifier: Modifier = Modifier
) {
    val revealVisual = StudyMicroInteractionResolver.reveal(revealProgress)
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
    val integratedComparison =
        typingComparisonForCanonicalWord(typingComparison, disclosure.englishWord)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = resolvedLayout.contentMaxWidthDp.dp)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    integratedComparison?.accessibilityDescription
                        ?: "Revealed answer: ${disclosure.englishWord}. ${disclosure.vietnameseMeaning}."
            }
    ) {
        val availableContentWidthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val responsivePolicy =
            remember(availableContentWidthDp) {
                FullAnswerResponsivePolicyResolver.resolve(availableContentWidthDp)
            }
        var examplesExpanded by remember(currentLearningItemId) {
            mutableStateOf(
                initialItemExamplesDisclosureState(currentLearningItemId, responsivePolicy)
                    .disclosure.expanded
            )
        }
        val spacePresentation = remember(
            availableContentWidthDp,
            measuredBodyHeightDp,
            resolvedLayout.ratingDockReservedHeightDp,
            disclosure.examples.size,
            examplesExpanded
        ) {
            AdaptiveStudySpacePresentationResolver.resolve(
                AdaptiveStudySpaceRequest(
                    isAnswer = true,
                    examplesExpanded = examplesExpanded,
                    hasExamples = disclosure.examples.isNotEmpty(),
                    viewportWidthDp = availableContentWidthDp,
                    viewportHeightDp = measuredBodyHeightDp,
                    bottomControlHeightDp = resolvedLayout.ratingDockReservedHeightDp,
                    intrinsicExampleHeightDp = disclosure.examples.size * 140
                )
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(spacePresentation.verticalSpacingDp.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space2)
            ) {
                AnswerConfirmationMarker(strings.flowAnswerReady)
                VocabularyIdentitySurface(
                    word = disclosure.englishWord,
                    ipa = disclosure.ipa,
                    partOfSpeech = disclosure.partOfSpeech,
                    audioPath = model.primaryAudioPath,
                    audioController = audioController,
                    strings = strings,
                    layout = resolvedLayout,
                    typingComparison = integratedComparison
                )
            }
            if (disclosure.imageAvailable && model.imagePath != null) {
                val signatureImageHeightDp =
                    spacePresentation.imageMaximumHeightDp
                val answerImageLayout = resolvedLayout.copy(
                    imageMaxWidthDp =
                        (resolvedLayout.contentMaxWidthDp * 0.98f).toInt().coerceAtLeast(1)
                )
                VocabularyImageBlock(
                    imagePath = model.imagePath,
                    imageDescription = strings.imageDescription,
                    audioPath = model.primaryAudioPath,
                    audioController = audioController,
                    loops = true,
                    layout = answerImageLayout,
                    imageMaxHeightDp = signatureImageHeightDp
                )
            }
            ResponsiveAnswerSupportingRegion(
                policy = responsivePolicy,
                meaning = disclosure.vietnameseMeaning,
                meaningAudioPath = model.meaningAudioPath,
                examples =
                    if (spacePresentation.showAllExampleContent) disclosure.examples
                    else disclosure.examples.take(signaturePresentation.maximumVisibleExamples),
                currentLearningItemId = currentLearningItemId,
                examplesDisclosureKeyboard = examplesDisclosureKeyboard,
                strings = strings,
                audioController = audioController,
                typography = typography,
                englishTarget = disclosure.englishWord,
                vietnameseTarget = disclosure.vietnameseMeaning,
                revealProgress = revealProgress,
                examplesExpanded = examplesExpanded,
                onExamplesExpandedChange = { examplesExpanded = it }
            )
            schedulerFeedback?.let { feedback ->
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = revealVisual.schedulerAlpha
                    }
                ) {
                    CompactSchedulerFeedback(
                        feedback = feedback,
                        context = SchedulerFeedbackContext.ACTIVE_ANSWER
                    )
                }
            }
        }
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
    vietnameseTarget: String,
    revealProgress: Float,
    examplesExpanded: Boolean,
    onExamplesExpandedChange: (Boolean) -> Unit
) {
    val revealVisual = StudyMicroInteractionResolver.reveal(revealProgress)
    val meaningContent: @Composable () -> Unit = {
        MeaningCard(
            meaning = meaning,
            meaningAudioPath = meaningAudioPath,
            audioController = audioController,
            modifier = Modifier.graphicsLayer { alpha = revealVisual.meaningAlpha }
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
            examplesDisclosureKeyboard = examplesDisclosureKeyboard,
            expanded = examplesExpanded,
            onExpandedChange = onExamplesExpandedChange
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space5)
    ) {
        meaningContent()
        examplesContent()
    }
}

@Composable
private fun AnswerConfirmationMarker(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = label
        }
    ) {
        Icon(
            imageVector = LEIcons.Success,
            contentDescription = null,
            tint = LETheme.colors.success,
            modifier = Modifier.size(LETheme.spacing.space5)
        )
        Text(
            text = label.uppercase(),
            style = LETheme.typography.fieldLabel,
            color = LETheme.colors.successText,
            fontWeight = FontWeight.Bold
        )
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
    examplesDisclosureKeyboard: ExamplesDisclosureKeyboardController,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    if (examples.isEmpty() && typingComparison == null) return
    val disclosureAvailable =
        policy.layout == AnswerSurfaceLayout.NARROW && examples.isNotEmpty()
    DisposableEffect(
        examplesDisclosureKeyboard,
        disclosureAvailable,
        currentLearningItemId,
        expanded
    ) {
        if (disclosureAvailable) {
            examplesDisclosureKeyboard.bind { command ->
                val result =
                    applyExamplesDisclosureCommand(ExamplesDisclosureState(expanded), command)
                onExpandedChange(result.state.expanded)
                result.consumed
            }
        }
        onDispose { examplesDisclosureKeyboard.unbind() }
    }
    if (disclosureAvailable) {
        ExamplesDisclosureControl(
            label = "$exampleLabel (${examples.size})",
            expanded = expanded,
            expandedDescription = strings.examplesExpanded,
            collapsedDescription = strings.examplesCollapsed,
            collapsedTooltip = strings.examplesOpenTooltip,
            expandedTooltip = strings.examplesCloseTooltip,
            onToggle = {
                onExpandedChange(
                    toggleExamplesDisclosure(ExamplesDisclosureState(expanded)).expanded
                )
            }
        )
    }
    typingComparison?.invoke()
    if (expanded && examples.isNotEmpty()) {
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
internal fun VocabularyIdentitySurface(
    word: String,
    ipa: String?,
    partOfSpeech: String?,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    strings: LearningContentRendererStrings,
    layout: StudyVisualLayout? = null,
    typingComparison: TypingRevealComparisonPresentation? = null,
    stage: StudySurfaceStage = StudySurfaceStage.UNDERSTANDING,
    modifier: Modifier = Modifier
) {
    val surfacePresentation =
        StudySurfacePresentationResolver.resolve(
            stage,
            StudySurfaceRole.HERO
        )
    val heroPresentation = StudyHeroPresentationResolver.resolve(stage)
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
    val integratedComparison = typingComparisonForCanonicalWord(typingComparison, word)
    Surface(
        modifier = baseModifier,
        shape =
            if (heroPresentation.usesExpansiveShape) LETheme.shapes.radius2XL
            else LETheme.shapes.radiusL,
        color =
            if (heroPresentation.usesAccentTone) LETheme.colors.accentSoft
            else presentation.containerColor,
        border =
            if (
                surfacePresentation.borderProminence == StudyBorderProminence.NONE &&
                !focused &&
                !isLooping
            ) null else BorderStroke(presentation.borderWidth, presentation.borderColor),
        shadowElevation =
            if (heroPresentation.depth == FocusedImmersionDepth.HERO) {
                LETheme.elevation.elevation1
            } else {
                surfacePresentation.resolveElevation(LETheme.elevation)
            }
    ) {
        val headerTypography = StudyTypographyPresentationResolver.resolveAnswerHeader(layout)
        val wordSize = headerTypography.wordFontSize.sp
        val lineHeight = headerTypography.wordLineHeight.sp
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
            if (integratedComparison != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(identityPresentation.verticalGapDp.dp)
                ) {
                    Text(
                        text = integratedComparison.userAnswerLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = LETheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text =
                            typingComparisonAnnotatedText(
                                integratedComparison.userAnswer,
                                integratedComparison.userMismatchSpans,
                                LETheme.colors.danger
                            ),
                        style =
                            LETheme.typography.displayWord.copy(
                                fontSize = wordSize,
                                lineHeight = lineHeight,
                                color = presentation.primaryContentColor
                            ),
                        textAlign = TextAlign.Center,
                        softWrap = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(
                        modifier = Modifier.widthIn(max = 48.dp),
                        color = LETheme.colors.borderSubtle
                    )
                }
            }
            Text(
                text =
                    integratedComparison?.let {
                        typingComparisonAnnotatedText(
                            word,
                            it.expectedMismatchSpans,
                            LETheme.colors.success
                        )
                    } ?: androidx.compose.ui.text.AnnotatedString(word),
                style = LETheme.typography.displayWord.copy(
                    fontSize = wordSize,
                    lineHeight = lineHeight,
                    color = presentation.primaryContentColor
                ),
                textAlign = TextAlign.Center,
                softWrap = true,
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
    fontSizeSp: Int? = null,
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
            style =
                LETheme.typography.meaningPos.let { base ->
                    base.copy(
                        color = style.contentColor,
                        fontSize = fontSizeSp?.sp ?: base.fontSize
                    )
                },
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
    centered: Boolean = false,
    posFontSizeSp: Int? = null,
    meaningContent: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier.then(if (centered) Modifier.fillMaxWidth() else Modifier),
        horizontalArrangement =
            if (centered) {
                Arrangement.spacedBy(
                    LETheme.spacing.space2,
                    Alignment.CenterHorizontally
                )
            } else {
                Arrangement.spacedBy(LETheme.spacing.space2)
            },
        verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space2)
    ) {
        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
            meaningContent()
        }
        resolveStudyMeaningPos(partOfSpeech)?.let { meaningPos ->
            StudyPosBadge(
                partOfSpeech = meaningPos,
                fontSizeSp = posFontSizeSp,
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
    typingRequired: Boolean = false,
    inventoryVisible: Boolean = false,
    modifier: Modifier = Modifier
) = StudyVocabularyImageBlock(
    imagePath = imagePath,
    imageDescription = imageDescription,
    audioPath = audioPath,
    audioController = audioController,
    loops = loops,
    layout = layout,
    imageMaxHeightDp = imageMaxHeightDp,
    typingRequired = typingRequired,
    inventoryVisible = inventoryVisible,
    modifier = modifier,
    surfacePresentation =
        StudySurfacePresentationResolver.resolve(
            StudySurfaceStage.UNDERSTANDING,
            StudySurfaceRole.HERO_SUPPORT
        )
)

@Composable
internal fun StudyVocabularyImageBlock(
    imagePath: Path,
    imageDescription: String,
    audioPath: Path? = null,
    audioController: LearningContentAudioController? = null,
    loops: Boolean = false,
    layout: StudyVisualLayout,
    imageMaxHeightDp: Int = layout.imageMaxHeightDp,
    typingRequired: Boolean = false,
    inventoryVisible: Boolean = false,
    modifier: Modifier = Modifier,
    surfacePresentation: StudySurfacePresentation
) {
    val heroPresentation = StudyHeroPresentationResolver.resolve(surfacePresentation.stage)
    val bitmap = remember(imagePath) {
        runCatching {
            Image.makeFromEncoded(Files.readAllBytes(imagePath)).toComposeImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        val density = LocalDensity.current.density
        val intrinsicWidthDp = (bitmap.width / density).toInt().coerceAtLeast(1)
        val intrinsicHeightDp = (bitmap.height / density).toInt().coerceAtLeast(1)
        val aspectClass = AdaptiveStudyImagePresentationResolver.classify(
            intrinsicWidthDp,
            intrinsicHeightDp
        )
        val verticalAllocation = StudyVerticalSpaceAllocationResolver.resolve(
            StudyVerticalSpaceInput(
                viewportWidthDp = layout.contentMaxWidthDp,
                viewportHeightDp = layout.availableAnswerHeightDp.coerceAtLeast(1),
                imageAspectClass = aspectClass,
                typingRequired = typingRequired,
                externalReservedHeightDp = if (inventoryVisible) 76 else 0,
                examplesExpanded = false,
                typingOuterHeightDp =
                    TypingFieldLayoutMetricsResolver.resolve(layout.contentMaxWidthDp)
                        .outerMinimumHeightDp
            )
        )
        val allocatedHeightDp =
            if (typingRequired) verticalAllocation.imageMaxHeightDp
            else imageMaxHeightDp
        if (allocatedHeightDp <= 0) return
        val imagePresentation = remember(
            bitmap.width,
            bitmap.height,
            density,
            layout.imageMaxWidthDp,
            allocatedHeightDp,
            verticalAllocation.imageMaxWidthDp
        ) {
            AdaptiveStudyImagePresentationResolver.resolve(
                intrinsicWidthDp = intrinsicWidthDp,
                intrinsicHeightDp = intrinsicHeightDp,
                availableWidthDp = minOf(layout.imageMaxWidthDp, verticalAllocation.imageMaxWidthDp),
                heightBudgetDp = allocatedHeightDp
            )
        }
        val maxW = imagePresentation.maximumWidthDp.dp
        val maxH = imagePresentation.frameHeightDp.dp
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
                shape =
                    if (heroPresentation.usesExpansiveShape) LETheme.shapes.radius2XL
                    else LETheme.shapes.radiusL,
                color =
                    if (heroPresentation.usesAccentTone) LETheme.colors.accentSoft
                    else presentation.containerColor,
                border = presentation.border,
                shadowElevation =
                    if (heroPresentation.depth == FocusedImmersionDepth.HERO) {
                        if (surfacePresentation.stage == StudySurfaceStage.DISCOVERY) {
                            LETheme.elevation.elevation2
                        } else {
                            LETheme.elevation.elevation1
                        }
                    } else {
                        surfacePresentation.resolveElevation(LETheme.elevation)
                    }
            ) {
                Box {
                    Image(
                        bitmap = bitmap,
                        contentDescription = imageDescription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxH)
                            .clip(
                                if (heroPresentation.usesExpansiveShape) LETheme.shapes.radius2XL
                                else LETheme.shapes.radiusL
                            ),
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

    Row(
        modifier =
            surfaceModifier
                .padding(
                    horizontal = compactLayout.horizontalPaddingDp.dp,
                    vertical = compactLayout.verticalPaddingDp.dp
                ),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasAudio) {
            Surface(
                shape = LETheme.shapes.radiusPill,
                color = LETheme.colors.accentSoft,
                modifier = Modifier.size(compactLayout.iconSizeDp.dp)
            ) {
                Icon(
                    imageVector = LEIcons.Audio,
                    contentDescription = null,
                    tint = LETheme.colors.accentPrimary,
                    modifier = Modifier.padding(compactLayout.iconPaddingDp.dp)
                )
            }
        }
        Text(
            text = meaning,
            fontSize = compactLayout.textSizeSp.sp,
            lineHeight = compactLayout.textLineHeightSp.sp,
            fontWeight = FontWeight.SemiBold,
            style = LETheme.typography.meaningPrimary,
            maxLines = 1,
            softWrap = false
        )
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
    val visualFocus = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.EXAMPLE)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(LESpacing.md),
        verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space4)
    ) {
        Text(
            text = exampleLabel.uppercase(),
            style = LETheme.typography.sectionTitle,
            color = visualFocus.resolveContentColor(LETheme.colors),
            fontWeight = FontWeight.Bold
        )
        examples.forEachIndexed { index, example ->
            if (index > 0) {
                HorizontalDivider(color = LETheme.colors.borderSubtle)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnglishExampleAudioRow(
                    englishText = example.englishText,
                    target = englishTarget,
                    audioPath = example.englishAudioPath ?: example.audioPath,
                    audioController = audioController,
                    typography = typography
                )
                if (!example.vietnameseTranslation.isNullOrBlank()) {
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
    Row(
            modifier = rowModifier.padding(horizontal = 4.dp, vertical = 4.dp),
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
    Row(
            modifier = rowModifier.padding(horizontal = 4.dp, vertical = 2.dp),
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
