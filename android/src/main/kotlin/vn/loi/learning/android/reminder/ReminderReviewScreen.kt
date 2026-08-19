package vn.loi.learning.android.reminder

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
    runtime: AndroidVocabularyReminderRuntime? = null
) {
    val items = session.items
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reminder items available", style = MaterialTheme.typography.titleMedium)
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

    // Controller Bridge target integration
    DisposableEffect(pagerState.currentPage, currentItem) {
        val target = object : StudyControllerTarget {
            override fun currentContext(): ControllerContext = ControllerContext.STUDY_REVEALED
            override suspend fun revealAnswer(): Boolean = false
            override suspend fun rate(rating: ReviewRating): Boolean = false // Read-only!
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
                                contentDescription = "Back from Reminder Review"
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
                                val updated = markers.toggle(currentItem.contentId)
                                isDifficult = updated
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
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Previous")
                    }

                    Text(
                        "${pagerState.currentPage + 1} of ${items.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (pagerState.currentPage < items.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < items.size - 1
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val item = items[page]
            ReminderReviewItemContent(
                item = item,
                resolveMedia = resolveMedia,
                onPlayAudio = playAudio
            )
        }
    }
}

@Composable
private fun ReminderReviewItemContent(
    item: AndroidVocabularyCandidate,
    resolveMedia: (String) -> String?,
    onPlayAudio: (String?) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        val hasImage = !item.imageReference.isNullOrBlank()
        // Responsive allocation for image: ~44% of available height, bounded within 200..440dp
        val targetImageHeight = (availableHeight * 0.44f).coerceIn(200.dp, 440.dp)

        // Adaptive headword typography
        val headwordFontSize = when {
            item.primaryText.length <= 8 -> 44.sp
            item.primaryText.length <= 16 -> 36.sp
            item.primaryText.length <= 25 -> 28.sp
            else -> 22.sp
        }
        val headwordLineHeight = when {
            item.primaryText.length <= 8 -> 48.sp
            item.primaryText.length <= 16 -> 40.sp
            item.primaryText.length <= 25 -> 32.sp
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
                        onClickLabel = "Play question audio"
                    ) {
                        onPlayAudio(item.primaryAudioReference)
                    }
                    .semantics {
                        contentDescription = "Question: ${item.primaryText}. Tap to listen."
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
                            onClickLabel = "Play meaning audio"
                        ) {
                            onPlayAudio(audioRef)
                        }
                        .semantics {
                            contentDescription = "Meaning: $meaning. Tap to listen."
                        },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Meaning",
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
                            onClickLabel = "Play example audio"
                        ) {
                            onPlayAudio(item.exampleAudioReference)
                        }
                        .semantics {
                            contentDescription = "Example: ${item.example.orEmpty()} ${item.exampleTranslation.orEmpty()}. Tap to listen."
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
                            text = "Example",
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
            contentDescription = "Vocabulary image",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
