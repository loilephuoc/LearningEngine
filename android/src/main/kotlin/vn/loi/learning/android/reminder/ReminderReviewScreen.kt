package vn.loi.learning.android.reminder

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.loi.learning.android.controller.ControllerContext
import vn.loi.learning.android.controller.StudyControllerBridge
import vn.loi.learning.android.controller.StudyControllerTarget
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.LearningEngineAudioPolicy
import vn.loi.learning.domain.study.memory.model.ReviewRating

@Composable
fun ReminderReviewScreen(
    session: AndroidReminderReviewSession,
    difficultMarkers: AndroidVocabularyReminderDifficultMarkers?,
    resolveMedia: (String) -> String?,
    onBack: () -> Unit,
    runtime: AndroidVocabularyReminderRuntime? = null,
    ratingBridge: AndroidReminderReviewRatingBridge? = null,
    fsrsInspectorQuery: AndroidReminderReviewFsrsInspectorQuery? = null
) {
    val items = session.items
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Không có mục nhắc học", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioController = remember { AndroidAudioController(context) }

    DisposableEffect(runtime) {
        runtime?.setReviewScreenActive(true)
        onDispose {
            runtime?.setReviewScreenActive(false)
            audioController.close()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = session.initialIndex.coerceIn(0, items.size - 1)
    ) {
        items.size
    }

    val currentItem = items[pagerState.currentPage]
    var isDifficult by remember(currentItem.contentId) {
        mutableStateOf(difficultMarkers?.isMarked(currentItem.contentId) == true)
    }

    // Rating and preview state
    var previewsByContentId by remember { mutableStateOf<Map<String, Map<ReviewRating, QuickReviewRatingPreview>>>(emptyMap()) }
    var ratedCandidateIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var submissionState by remember { mutableStateOf(QuickReviewSubmissionState.IDLE) }
    var successFeedback by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var inspectorByContentId by remember { mutableStateOf<Map<String, AndroidFsrsInspectorUiModel>>(emptyMap()) }

    // Asynchronous non-blocking preview loading for active candidate
    LaunchedEffect(currentItem.contentId) {
        errorMessage = null
        if (ratingBridge != null && !previewsByContentId.containsKey(currentItem.contentId.value)) {
            val previews = withContext(Dispatchers.IO) {
                ratingBridge.previewRatings(currentItem.contentId.value)
            }
            if (previews != null) {
                previewsByContentId = previewsByContentId + (currentItem.contentId.value to previews)
            }
        }
        fsrsInspectorQuery?.let { query ->
            val model = withContext(Dispatchers.IO) { query.query(currentItem.contentId.value) }
            inspectorByContentId = inspectorByContentId + (currentItem.contentId.value to model)
        }
    }

    // Play function that stops previous and plays new audio
    val playAudio: (String?) -> Unit = { audioRef ->
        audioController.stop()
        if (!audioRef.isNullOrBlank() && !LearningEngineAudioPolicy.isMuted.value) {
            val resolved = resolveMedia(audioRef)
            if (resolved != null) {
                audioController.replay(resolved)
            }
        }
    }

    // Canonical rating submission handler
    val handleRate: (ReviewRating) -> Unit = { rating ->
        if (ratingBridge != null &&
            submissionState != QuickReviewSubmissionState.SUBMITTING &&
            currentItem.contentId.value !in ratedCandidateIds
        ) {
            submissionState = QuickReviewSubmissionState.SUBMITTING
            errorMessage = null
            coroutineScope.launch {
                val candidateId = currentItem.contentId.value
                val result = withContext(Dispatchers.IO) {
                    ratingBridge.submitRating(candidateId, rating)
                }
                when (result) {
                    is QuickReviewRatingResult.Success -> {
                        fsrsInspectorQuery?.let { query ->
                            val refreshed = withContext(Dispatchers.IO) { query.query(candidateId) }
                            inspectorByContentId = inspectorByContentId + (candidateId to refreshed)
                        }
                        ratedCandidateIds = ratedCandidateIds + candidateId
                        submissionState = QuickReviewSubmissionState.SUCCESS
                        val intervalText = AndroidReminderReviewRatingBridge.formatTimeSpan(result.scheduledInterval)
                        val ratingLabel = result.rating.name.lowercase().replaceFirstChar { it.uppercase() }
                        successFeedback = "Rated $ratingLabel · $intervalText"

                        // Auto-advance to next candidate if available
                        if (pagerState.currentPage < items.size - 1) {
                            delay(600L)
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            submissionState = QuickReviewSubmissionState.IDLE
                        }
                    }
                    is QuickReviewRatingResult.Failure -> {
                        submissionState = QuickReviewSubmissionState.ERROR
                        errorMessage = result.cause.message ?: "Rating submission failed"
                    }
                    QuickReviewRatingResult.NotFound,
                    QuickReviewRatingResult.NotReviewable -> {
                        submissionState = QuickReviewSubmissionState.ERROR
                        errorMessage = "Item is not reviewable"
                    }
                    QuickReviewRatingResult.AlreadySubmitting -> {
                        // Already submitting, do nothing
                    }
                }
            }
        }
    }

    // Controller Bridge target integration
    DisposableEffect(pagerState.currentPage, currentItem, submissionState, ratedCandidateIds) {
        val target = object : StudyControllerTarget {
            override fun currentContext(): ControllerContext = ControllerContext.STUDY_REVEALED
            override suspend fun revealAnswer(): Boolean = false
            override suspend fun rate(rating: ReviewRating): Boolean {
                if (ratingBridge == null) return false
                if (submissionState == QuickReviewSubmissionState.SUBMITTING) return false
                if (currentItem.contentId.value in ratedCandidateIds) return false
                handleRate(rating)
                return true
            }
            override suspend fun next(): Boolean {
                if (pagerState.currentPage < items.size - 1) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    return true
                }
                return false
            }
            override suspend fun previous(): Boolean {
                if (pagerState.currentPage > 0) {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    return true
                }
                return false
            }
            override fun replayAudio(): Boolean {
                currentItem.primaryAudioReference?.let { playAudio(it); return true }
                return false
            }
            override fun exampleEnglishAudio(): String? = currentItem.exampleAudioReference?.let(resolveMedia)
            override fun primaryEnglishAudio(): String? = currentItem.primaryAudioReference?.let(resolveMedia)
            override fun describeState(): String = "ReminderReview"
        }
        StudyControllerBridge.register(target)
        onDispose {
            StudyControllerBridge.unregister(target)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại từ ôn tập nhắc học"
                            )
                        }
                        Column {
                            Text(
                                text = "Reminder Review",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "${pagerState.currentPage + 1} / ${items.size} · ${currentItem.packageName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Compact Difficult star icon button
                    IconButton(
                        onClick = {
                            difficultMarkers?.let { markers ->
                                val markedBefore = markers.isMarked(currentItem.contentId)
                                val updated = markers.toggle(currentItem.contentId)
                                isDifficult = updated
                                android.util.Log.i(
                                    "WidgetReviewDifficult",
                                    "[WidgetReviewDifficult] candidateId=${currentItem.contentId.value} markedBefore=$markedBefore markedAfter=$updated source=HOME_WIDGET_FULL_REVIEW"
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isDifficult) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isDifficult) "Difficult" else "Not difficult",
                            tint = if (isDifficult) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (pagerState.currentPage > 0 && submissionState != QuickReviewSubmissionState.SUBMITTING) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0 && submissionState != QuickReviewSubmissionState.SUBMITTING
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Trước")
                    }

                    Text(
                        "${pagerState.currentPage + 1} of ${items.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (pagerState.currentPage < items.size - 1 && submissionState != QuickReviewSubmissionState.SUBMITTING) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < items.size - 1 && submissionState != QuickReviewSubmissionState.SUBMITTING
                    ) {
                        Text("Tiếp")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = submissionState != QuickReviewSubmissionState.SUBMITTING,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val item = items[page]
            val previews = previewsByContentId[item.contentId.value]
            val isCurrentRated = item.contentId.value in ratedCandidateIds

            ReminderReviewItemContent(
                item = item,
                resolveMedia = resolveMedia,
                onPlayAudio = playAudio,
                ratingBridge = ratingBridge,
                previews = previews,
                isSubmitting = submissionState == QuickReviewSubmissionState.SUBMITTING,
                isCurrentRated = isCurrentRated,
                successFeedback = if (item.contentId.value == currentItem.contentId.value) successFeedback else null,
                errorMessage = if (item.contentId.value == currentItem.contentId.value) errorMessage else null,
                inspector = inspectorByContentId[item.contentId.value],
                inspectorEnabled = fsrsInspectorQuery != null,
                onRate = handleRate
            )
        }
    }
}

@Composable
private fun ReminderReviewItemContent(
    item: AndroidVocabularyCandidate,
    resolveMedia: (String) -> String?,
    onPlayAudio: (String?) -> Unit,
    ratingBridge: AndroidReminderReviewRatingBridge? = null,
    previews: Map<ReviewRating, QuickReviewRatingPreview>? = null,
    isSubmitting: Boolean = false,
    isCurrentRated: Boolean = false,
    successFeedback: String? = null,
    errorMessage: String? = null,
    inspector: AndroidFsrsInspectorUiModel? = null,
    inspectorEnabled: Boolean = false,
    onRate: (ReviewRating) -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        val hasImage = !item.imageReference.isNullOrBlank()
        // Responsive allocation for image: ~40% of available height, bounded within 180..420dp
        val targetImageHeight = (availableHeight * 0.40f).coerceIn(180.dp, 420.dp)

        // Adaptive headword typography
        val headwordFontSize = when {
            item.primaryText.length <= 8 -> 40.sp
            item.primaryText.length <= 16 -> 34.sp
            item.primaryText.length <= 25 -> 26.sp
            else -> 22.sp
        }
        val headwordLineHeight = when {
            item.primaryText.length <= 8 -> 44.sp
            item.primaryText.length <= 16 -> 38.sp
            item.primaryText.length <= 25 -> 30.sp
            else -> 26.sp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Prominent Vocabulary Image (Fit scale, whole image visible, responsive height allocation)
            item.imageReference?.let { imgRef ->
                ReminderImage(
                    imageRef = imgRef,
                    resolveMedia = resolveMedia,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(targetImageHeight)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            // 2. Question / Headword Card (Tap to play Question Audio)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Phát âm thanh câu hỏi"
                    ) {
                        onPlayAudio(item.primaryAudioReference)
                    }
                    .semantics {
                        contentDescription = "Câu hỏi: ${item.primaryText}. Chạm để nghe."
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.primaryText,
                        fontSize = headwordFontSize,
                        lineHeight = headwordLineHeight,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (!item.ipa.isNullOrBlank() || !item.partOfSpeech.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item.ipa?.takeIf { it.isNotBlank() }?.let { ipa ->
                                Text(
                                    text = "/$ipa/",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item.partOfSpeech?.takeIf { it.isNotBlank() }?.let { pos ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = pos,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Meaning / Translation Card (Tap to play Answer/Translation Audio)
            val meaning = item.translation?.takeIf { it.isNotBlank() } ?: item.answer.orEmpty()
            if (meaning.isNotBlank()) {
                val audioRef = item.translationAudioReference ?: item.answerAudioReference
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Phát âm thanh nghĩa"
                        ) {
                            onPlayAudio(audioRef)
                        }
                        .semantics {
                            contentDescription = "Nghĩa: $meaning. Chạm để nghe."
                        },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Nghĩa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = meaning,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 4. Example Card (English Example + Vietnamese Translation)
            if (!item.example.isNullOrBlank() || !item.exampleTranslation.isNullOrBlank()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Phát âm thanh ví dụ"
                        ) {
                            onPlayAudio(item.exampleAudioReference)
                        }
                        .semantics {
                            contentDescription = "Ví dụ: ${item.example.orEmpty()} ${item.exampleTranslation.orEmpty()}. Chạm để nghe."
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Ví dụ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!item.example.isNullOrBlank()) {
                            Text(
                                text = item.example,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!item.exampleTranslation.isNullOrBlank()) {
                            Text(
                                text = item.exampleTranslation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Four Quick FSRS Rating Controls
            if (ratingBridge != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Rating",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCurrentRated && successFeedback != null) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = successFeedback,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isSubmitting) {
                                Text(
                                    text = "Saving...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (!errorMessage.isNullOrBlank()) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Four Buttons in Responsive Width Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val ratingItems = listOf(
                                Triple(ReviewRating.AGAIN, "Again", vn.loi.learning.android.ui.StudyRatingColors.again),
                                Triple(ReviewRating.HARD, "Hard", vn.loi.learning.android.ui.StudyRatingColors.hard),
                                Triple(ReviewRating.GOOD, "Good", vn.loi.learning.android.ui.StudyRatingColors.good),
                                Triple(ReviewRating.EASY, "Easy", vn.loi.learning.android.ui.StudyRatingColors.easy)
                            )

                            for ((rating, label, palette) in ratingItems) {
                                val preview = previews?.get(rating)
                                val intervalText = preview?.formattedInterval ?: "-"
                                val isEnabled = !isSubmitting && !isCurrentRated && previews != null

                                OutlinedButton(
                                    onClick = { onRate(rating) },
                                    enabled = isEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .semantics {
                                            contentDescription = if (isEnabled) "$label, next review in $intervalText" else "$label unavailable"
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isEnabled) palette.border else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isEnabled) palette.background.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        contentColor = palette.content,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isEnabled) palette.content else androidx.compose.ui.graphics.Color.Unspecified,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = intervalText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isEnabled) palette.content.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (inspectorEnabled) {
                FsrsInspectorSection(inspector)
            }
        }
    }
}

@Composable
private fun FsrsInspectorSection(model: AndroidFsrsInspectorUiModel?) {
    var showAll by remember(model) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chi tiết FSRS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            when {
                model == null -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                !model.hasFsrsData -> Text("Không có dữ liệu FSRS", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    InspectorFieldGrid(model)
                    Text("Lịch sử ôn tập", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp).semantics { heading() })
                    if (model.history.isEmpty()) {
                        Text("Chưa có lượt ôn tập", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        model.recentHistory.forEach { ReviewHistoryRow(it) }
                        if (model.hasMoreHistory) {
                            TextButton(onClick = { showAll = true }) { Text("Xem tất cả (${model.history.size})") }
                        }
                    }
                }
            }
        }
    }
    if (showAll && model != null) {
        AlertDialog(
            onDismissRequest = { showAll = false },
            title = { Text("Lịch sử ôn tập") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(model.history) { ReviewHistoryRow(it) }
                }
            },
            confirmButton = { TextButton(onClick = { showAll = false }) { Text("Đóng") } }
        )
    }
}

@Composable
private fun InspectorFieldGrid(model: AndroidFsrsInspectorUiModel) {
    val fields = listOf(
        "Stage" to model.stage, "Due" to model.due, "Last reviewed" to model.lastReviewed,
        "Reviews" to model.reviewCount.toString(), "Lapses" to model.lapseCount.toString(),
        "Difficulty" to model.difficulty, "Stability" to model.stability
    )
    fields.forEach { (label, value) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
    if (model.reviewCount == 0) Text("Mục này chưa được ôn tập.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
}

@Composable
private fun ReviewHistoryRow(row: AndroidReviewHistoryRow) {
    val palette = when (row.rating) {
        ReviewRating.AGAIN -> vn.loi.learning.android.ui.StudyRatingColors.again
        ReviewRating.HARD -> vn.loi.learning.android.ui.StudyRatingColors.hard
        ReviewRating.GOOD -> vn.loi.learning.android.ui.StudyRatingColors.good
        ReviewRating.EASY -> vn.loi.learning.android.ui.StudyRatingColors.easy
    }
    Surface(shape = RoundedCornerShape(10.dp), color = palette.background.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border.copy(alpha = 0.6f))) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.rating.name, color = palette.content, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text(row.reviewedAt, style = MaterialTheme.typography.labelSmall)
            }
            Text(row.source, style = MaterialTheme.typography.bodySmall)
            Text(row.stageTransition, style = MaterialTheme.typography.bodySmall)
            Text("Độ ổn định ${row.stabilityTransition}", style = MaterialTheme.typography.labelSmall)
            Text("Độ khó ${row.difficultyTransition}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ReminderImage(
    imageRef: String,
    resolveMedia: (String) -> String?,
    modifier: Modifier = Modifier
) {
    val bitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, key1 = imageRef) {
        value = withContext(Dispatchers.IO) {
            val path = resolveMedia(imageRef) ?: return@withContext null
            val file = File(path)
            if (!file.exists() || !file.isFile) return@withContext null
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) {
                    sample *= 2
                }
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Ảnh từ vựng",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
