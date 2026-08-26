package vn.loi.learning.android.study.debug

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import vn.loi.learning.android.study.AndroidStudyEvent
import vn.loi.learning.android.study.StudyScreen
import vn.loi.learning.android.study.AndroidTypingSuccessPresentationPolicy
import kotlinx.coroutines.delay
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Full-screen production preview for Adaptive Study UI Lab.
 * Directly renders the canonical production [StudyScreen] without embedding it into any debug Column/layout.
 * Intercepts all study events locally in memory to guarantee zero FSRS / persistence side-effects.
 */
@Composable
fun AdaptiveStudyUiPreviewScreen(
    engine: LearningApplicationContext?,
    initialPackageId: String,
    initialItemIndex: Int,
    initialMode: LabStudyMode,
    resolveMedia: (String) -> String?,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val installedPackages = remember(engine) {
        engine?.installedPackageRepository?.findAll().orEmpty()
    }

    val demoPackageContents = remember { AdaptiveStudyUiLabStateFactory.createDemoPackageContents() }

    val selectedPackageId = initialPackageId
    val selectedMode = initialMode

    val activeContents: List<Content> = remember(selectedPackageId, installedPackages) {
        if (selectedPackageId == "demo-package") {
            demoPackageContents
        } else {
            val pkgId = InstalledPackageId(selectedPackageId)
            val items = engine?.packageContentQuery?.getContentsForPackage(pkgId).orEmpty()
            val contents = items.mapNotNull { item ->
                engine?.contentRepository?.findById(ContentId(item.id))
            }
            if (contents.isNotEmpty()) contents else demoPackageContents
        }
    }

    val activePackageTitle = remember(selectedPackageId, installedPackages) {
        if (selectedPackageId == "demo-package") {
            "Gói kiểm thử mẫu (Demo Vocabulary)"
        } else {
            installedPackages.find { it.id.value == selectedPackageId }?.name?.value ?: "Gói từ vựng"
        }
    }

    var currentItemIndex by remember(selectedPackageId) {
        mutableIntStateOf(initialItemIndex.coerceIn(0, (activeContents.size - 1).coerceAtLeast(0)))
    }

    val currentContent = activeContents.getOrElse(currentItemIndex.coerceIn(0, (activeContents.size - 1).coerceAtLeast(0))) {
        demoPackageContents.first()
    }

    // Local-only ephemeral interaction state (Zero mutation / Zero FSRS)
    var currentInput by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf("") }
    var selectedChoiceId by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf<String?>(null) }
    var isRevealed by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf(false) }
    var isCompleted by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf(false) }
    var listeningCompletionPending by remember(currentItemIndex, selectedMode, selectedPackageId) {
        mutableStateOf(false)
    }
    var adaptiveTypingCompletionPending by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf(false) }
    var adaptiveTypingAudioCompleted by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf(false) }
    var imageRecallCompletionPending by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf(false) }

    val studyState = remember(
        currentContent,
        activeContents,
        selectedMode,
        currentInput,
        selectedChoiceId,
        isRevealed,
        isCompleted,
        listeningCompletionPending,
        adaptiveTypingCompletionPending,
        imageRecallCompletionPending,
        currentItemIndex,
        activePackageTitle
    ) {
        AdaptiveStudyUiLabStateFactory.buildState(
            content = currentContent,
            allPackageContents = activeContents,
            mode = selectedMode,
            mediaResolver = resolveMedia,
            currentInput = currentInput,
            selectedChoiceId = selectedChoiceId,
            isRevealed = isRevealed,
            isCompleted = isCompleted,
            listeningCompletionPending = listeningCompletionPending,
            adaptiveTypingCompletionPending = adaptiveTypingCompletionPending,
            imageRecallCompletionPending = imageRecallCompletionPending,
            currentIndex = currentItemIndex,
            totalCount = activeContents.size,
            packageTitle = activePackageTitle
        )
    }

    LaunchedEffect(adaptiveTypingCompletionPending, adaptiveTypingAudioCompleted, currentItemIndex) {
        if (!adaptiveTypingCompletionPending || !adaptiveTypingAudioCompleted) return@LaunchedEffect
        delay(AndroidTypingSuccessPresentationPolicy.minimumDwellMillis)
        currentItemIndex = if (currentItemIndex < activeContents.size - 1) currentItemIndex + 1 else 0
        currentInput = ""
        selectedChoiceId = null
        isRevealed = false
        isCompleted = false
        adaptiveTypingCompletionPending = false
        adaptiveTypingAudioCompleted = false
    }

    // This is deliberately the root composable: the preview inherits the exact production
    // constraints, chrome, scrolling, focus and audio behavior from StudyScreen.
    StudyScreen(
            state = studyState,
            onEvent = { event ->
                when (event) {
                    is AndroidStudyEvent.Home -> onBack()
                    is AndroidStudyEvent.AnswerChanged -> {
                        currentInput = event.value
                        if (selectedMode == LabStudyMode.LISTENING &&
                            isExactListeningPreviewAnswer(event.value, currentContent.text.primaryText)
                        ) {
                            listeningCompletionPending = true
                            isCompleted = true
                        }
                        if (selectedMode == LabStudyMode.TYPING &&
                            isExactListeningPreviewAnswer(event.value, currentContent.text.primaryText)
                        ) {
                            adaptiveTypingCompletionPending = true
                            isCompleted = true
                        }
                        if (selectedMode == LabStudyMode.IMAGE_RECALL &&
                            isExactListeningPreviewAnswer(event.value, currentContent.text.primaryText)
                        ) {
                            imageRecallCompletionPending = true
                            isCompleted = true
                        }
                    }
                    is AndroidStudyEvent.Submit -> isCompleted = true
                    AndroidStudyEvent.TypingSuccessAudioCompleted -> adaptiveTypingAudioCompleted = true
                    is AndroidStudyEvent.Reveal -> {
                        listeningCompletionPending = false
                        adaptiveTypingCompletionPending = false
                        imageRecallCompletionPending = false
                        isRevealed = true
                    }
                    is AndroidStudyEvent.Choose -> {
                        selectedChoiceId = event.choiceId
                        isCompleted = true
                    }
                    is AndroidStudyEvent.RateIntroduction,
                    is AndroidStudyEvent.OverrideRating,
                    is AndroidStudyEvent.SelectTypingRatingOverride,
                    is AndroidStudyEvent.Next -> {
                        // Simulated local rating/advance: ZERO FSRS writes!
                        if (currentItemIndex < activeContents.size - 1) {
                            currentItemIndex++
                        } else {
                            currentItemIndex = 0
                        }
                        currentInput = ""
                        selectedChoiceId = null
                        isRevealed = false
                        isCompleted = false
                        listeningCompletionPending = false
                        adaptiveTypingCompletionPending = false
                        imageRecallCompletionPending = false
                    }
                    is AndroidStudyEvent.Retry -> {
                        currentInput = ""
                        selectedChoiceId = null
                        isRevealed = false
                        isCompleted = false
                        listeningCompletionPending = false
                        adaptiveTypingCompletionPending = false
                        imageRecallCompletionPending = false
                    }
                    is AndroidStudyEvent.NextVisited -> {
                        if (selectedMode == LabStudyMode.LISTENING || selectedMode == LabStudyMode.IMAGE_RECALL) {
                            currentItemIndex = if (currentItemIndex < activeContents.size - 1) {
                                currentItemIndex + 1
                            } else {
                                0
                            }
                            currentInput = ""
                            selectedChoiceId = null
                            isRevealed = false
                            isCompleted = false
                            listeningCompletionPending = false
                            adaptiveTypingCompletionPending = false
                            imageRecallCompletionPending = false
                        } else if (currentItemIndex < activeContents.size - 1) {
                            currentItemIndex++
                            currentInput = ""
                            selectedChoiceId = null
                            isRevealed = false
                            isCompleted = false
                        }
                    }
                    is AndroidStudyEvent.PreviousVisited -> {
                        listeningCompletionPending = false
                        adaptiveTypingCompletionPending = false
                        imageRecallCompletionPending = false
                        if (currentItemIndex > 0) {
                            currentItemIndex--
                            currentInput = ""
                            selectedChoiceId = null
                            isRevealed = false
                            isCompleted = false
                        }
                    }
                    else -> Unit
                }
            },
            onAutoPlay = null,
            isDifficult = { false },
            onToggleDifficult = null,
            onSaveQuickEdit = null
    )
}

internal fun isExactListeningPreviewAnswer(input: String, expected: String): Boolean =
    input.trim().equals(expected.trim(), ignoreCase = true)
