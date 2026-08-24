package vn.loi.learning.android.study.debug

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.study.AndroidStudyEvent
import vn.loi.learning.android.study.AndroidStudyState
import vn.loi.learning.android.study.AudioRole
import vn.loi.learning.android.study.design.StudyContentDensity
import vn.loi.learning.android.study.modes.ExampleCompletionStudyStage
import vn.loi.learning.android.study.modes.ImageRecallStudyStage
import vn.loi.learning.android.study.modes.ListeningStudyStage
import vn.loi.learning.android.study.modes.MultipleChoiceStudyStage
import vn.loi.learning.android.study.modes.TypingStudyStage
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveStudyUiLabScreen(
    engine: LearningApplicationContext?,
    resolveMedia: (String) -> String?,
    onBack: () -> Unit
) {
    // 1. Prepare Content Packages (Installed + Fallback Demo Package)
    val installedPackages = remember(engine) {
        engine?.installedPackageRepository?.findAll().orEmpty()
    }

    val demoPackageContents = remember { createDemoPackageContents() }

    var selectedPackageId by remember {
        mutableStateOf(installedPackages.firstOrNull()?.id?.value ?: "demo-package")
    }

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

    var currentItemIndex by remember(selectedPackageId) { mutableIntStateOf(0) }
    val currentContent = activeContents.getOrElse(currentItemIndex.coerceIn(0, (activeContents.size - 1).coerceAtLeast(0))) {
        demoPackageContents.first()
    }

    var selectedMode by remember { mutableStateOf(LabStudyMode.TYPING) }
    var currentInput by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf("") }
    var selectedChoiceId by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf<String?>(null) }
    var isRevealed by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf(false) }
    var isCompleted by remember(currentItemIndex, selectedMode, selectedPackageId) { mutableStateOf(false) }

    // Media player for isolated testing
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val playAudio: (AudioRole, String?, Boolean) -> Unit = { _, path, _ ->
        if (!path.isNullOrBlank()) {
            runCatching {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    start()
                }
            }
        }
    }

    // Build Current Ephemeral State (Zero FSRS / Zero Mutation)
    val studyState = remember(
        currentContent,
        activeContents,
        selectedMode,
        currentInput,
        selectedChoiceId,
        isRevealed,
        isCompleted,
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
            currentIndex = currentItemIndex,
            totalCount = activeContents.size,
            packageTitle = activePackageTitle
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Adaptive Study UI Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Debug Mode · Zero FSRS Side-Effects",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Bounded layout: Root Column without verticalScroll ensures finite constraints for child Stage
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Controls Header: Package & Item Selector
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Package selector
                    var packageMenuExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gói dữ liệu:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Box {
                            OutlinedButton(onClick = { packageMenuExpanded = true }) {
                                Text(activePackageTitle, maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = packageMenuExpanded,
                                onDismissRequest = { packageMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Gói mẫu kiểm thử (Demo Vocabulary)") },
                                    onClick = {
                                        selectedPackageId = "demo-package"
                                        currentItemIndex = 0
                                        packageMenuExpanded = false
                                    }
                                )
                                installedPackages.forEach { pkg ->
                                    DropdownMenuItem(
                                        text = { Text(pkg.name.value) },
                                        onClick = {
                                            selectedPackageId = pkg.id.value
                                            currentItemIndex = 0
                                            packageMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Item Navigator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentItemIndex > 0) currentItemIndex--
                            },
                            enabled = currentItemIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mục trước")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Từ ${currentItemIndex + 1} / ${activeContents.size}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = currentContent.text.primaryText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentItemIndex < activeContents.size - 1) currentItemIndex++
                            },
                            enabled = currentItemIndex < activeContents.size - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Mục tiếp theo")
                        }
                    }
                }
            }

            // 2. Mode Selector Chips (Horizontal Scroll only)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LabStudyMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            // 3. Stage Simulation Actions / Test Bar (Horizontal Scroll only)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        currentInput = ""
                        selectedChoiceId = null
                        isRevealed = false
                        isCompleted = false
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Làm lại")
                }

                OutlinedButton(
                    onClick = { isRevealed = !isRevealed }
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isRevealed) "Ẩn đáp án" else "Hiện đáp án")
                }

                when (selectedMode) {
                    LabStudyMode.TYPING,
                    LabStudyMode.LISTENING,
                    LabStudyMode.IMAGE_RECALL,
                    LabStudyMode.EXAMPLE_COMPLETION -> {
                        Button(
                            onClick = {
                                currentInput = currentContent.text.primaryText
                                isCompleted = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Mẫu Đúng")
                        }
                        Button(
                            onClick = {
                                currentInput = "incorrect_sample"
                                isCompleted = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Mẫu Sai")
                        }
                    }
                    LabStudyMode.MULTIPLE_CHOICE -> {
                        Button(
                            onClick = {
                                selectedChoiceId = "choice-target-${currentContent.id.value}"
                                isCompleted = true
                            }
                        ) {
                            Text("Chọn Đúng")
                        }
                        Button(
                            onClick = {
                                selectedChoiceId = "choice-distractor-0"
                                isCompleted = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Chọn Sai")
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // 4. Bounded Real Stage Composable Container (weight(1f) provides finite max height)
            val stageScrollState = rememberScrollState()
            LaunchedEffect(currentItemIndex, selectedMode, selectedPackageId) {
                stageScrollState.scrollTo(0)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val dummyFeedback: @Composable () -> Unit = {}
                val stageModifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(stageScrollState)

                when (val s = studyState) {
                    is AndroidStudyState.Typing -> {
                        TypingStudyStage(
                            state = s,
                            activeRole = null,
                            baseDensity = StudyContentDensity.STANDARD,
                            availableMediaHeightDp = 200,
                            playAudio = playAudio,
                            onEvent = { event ->
                                when (event) {
                                    is AndroidStudyEvent.AnswerChanged -> currentInput = event.value
                                    is AndroidStudyEvent.Submit -> isCompleted = true
                                    is AndroidStudyEvent.Reveal -> isRevealed = true
                                    else -> Unit
                                }
                            },
                            onOpenFullscreenImage = {},
                            feedbackContent = dummyFeedback,
                            modifier = stageModifier
                        )
                    }
                    is AndroidStudyState.Listening -> {
                        ListeningStudyStage(
                            state = s,
                            activeRole = null,
                            baseDensity = StudyContentDensity.STANDARD,
                            playAudio = playAudio,
                            onEvent = { event ->
                                when (event) {
                                    is AndroidStudyEvent.AnswerChanged -> currentInput = event.value
                                    is AndroidStudyEvent.Submit -> isCompleted = true
                                    is AndroidStudyEvent.Reveal -> isRevealed = true
                                    else -> Unit
                                }
                            },
                            feedbackContent = dummyFeedback,
                            modifier = stageModifier
                        )
                    }
                    is AndroidStudyState.MultipleChoice -> {
                        MultipleChoiceStudyStage(
                            state = s,
                            activeRole = null,
                            baseDensity = StudyContentDensity.STANDARD,
                            availableMediaHeightDp = 200,
                            playAudio = playAudio,
                            onEvent = { event ->
                                when (event) {
                                    is AndroidStudyEvent.Choose -> {
                                        selectedChoiceId = event.choiceId
                                        isCompleted = true
                                    }
                                    is AndroidStudyEvent.Reveal -> isRevealed = true
                                    else -> Unit
                                }
                            },
                            onOpenFullscreenImage = {},
                            feedbackContent = dummyFeedback,
                            modifier = stageModifier
                        )
                    }
                    is AndroidStudyState.ImageRecall -> {
                        ImageRecallStudyStage(
                            state = s,
                            baseDensity = StudyContentDensity.STANDARD,
                            availableMediaHeightDp = 200,
                            onEvent = { event ->
                                when (event) {
                                    is AndroidStudyEvent.AnswerChanged -> currentInput = event.value
                                    is AndroidStudyEvent.Submit -> isCompleted = true
                                    is AndroidStudyEvent.Reveal -> isRevealed = true
                                    else -> Unit
                                }
                            },
                            onOpenFullscreenImage = {},
                            feedbackContent = dummyFeedback,
                            modifier = stageModifier
                        )
                    }
                    is AndroidStudyState.ExampleCompletion -> {
                        ExampleCompletionStudyStage(
                            state = s,
                            activeRole = null,
                            baseDensity = StudyContentDensity.STANDARD,
                            playAudio = playAudio,
                            onEvent = { event ->
                                when (event) {
                                    is AndroidStudyEvent.AnswerChanged -> currentInput = event.value
                                    is AndroidStudyEvent.Submit -> isCompleted = true
                                    is AndroidStudyEvent.Reveal -> isRevealed = true
                                    else -> Unit
                                }
                            },
                            feedbackContent = dummyFeedback,
                            modifier = stageModifier
                        )
                    }
                    else -> {
                        Text("Trạng thái kiểm thử không khả dụng.")
                    }
                }
            }
        }
    }
}

private fun createDemoPackageContents(): List<Content> {
    return listOf(
        Content(
            id = ContentId("demo-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "resilience",
                translatedText = "khả năng phục hồi nhanh chóng",
                exampleText = "Her mental resilience helped her overcome the severe hardship."
            ),
            media = ContentMedia(
                primaryAudio = "resilience.mp3",
                translatedAudio = "resilience_vi.mp3",
                image = "resilience.png"
            ),
            metadata = ContentMetadata(lesson = "Unit 1: Mindset"),
            customFields = ContentCustomFields(
                setOf(
                    ContentCustomField(ContentFieldId("ipa"), "rɪˈzɪl.jəns"),
                    ContentCustomField(ContentFieldId("partOfSpeech"), "noun")
                )
            )
        ),
        Content(
            id = ContentId("demo-2"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "meticulous",
                translatedText = "tỉ mỉ, cẩn trọng",
                exampleText = "He is always meticulous about keeping his laboratory notes clean."
            ),
            media = ContentMedia(
                primaryAudio = "meticulous.mp3",
                translatedAudio = "meticulous_vi.mp3",
                image = null
            ),
            metadata = ContentMetadata(lesson = "Unit 1: Mindset"),
            customFields = ContentCustomFields(
                setOf(
                    ContentCustomField(ContentFieldId("ipa"), "məˈtɪk.jə.ləs"),
                    ContentCustomField(ContentFieldId("partOfSpeech"), "adjective")
                )
            )
        ),
        Content(
            id = ContentId("demo-3"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "illuminate",
                translatedText = "soi sáng, làm sáng tỏ",
                exampleText = "A single beam of sunlight illuminated the ancient painting."
            ),
            media = ContentMedia(
                primaryAudio = "illuminate.mp3",
                translatedAudio = null,
                image = "illuminate.jpg"
            ),
            metadata = ContentMetadata(lesson = "Unit 2: Science"),
            customFields = ContentCustomFields(
                setOf(
                    ContentCustomField(ContentFieldId("ipa"), "ɪˈluː.mə.neɪt"),
                    ContentCustomField(ContentFieldId("partOfSpeech"), "verb")
                )
            )
        ),
        Content(
            id = ContentId("demo-4"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "ambiguity",
                translatedText = "sự mơ hồ, không rõ ràng",
                exampleText = "There was some ambiguity in the contract wording that required clarification."
            ),
            media = ContentMedia(
                primaryAudio = null,
                translatedAudio = null,
                image = null
            ),
            metadata = ContentMetadata(lesson = "Unit 2: Science"),
            customFields = ContentCustomFields(
                setOf(
                    ContentCustomField(ContentFieldId("ipa"), "ˌæm.bɪˈɡjuː.ə.ti"),
                    ContentCustomField(ContentFieldId("partOfSpeech"), "noun")
                )
            )
        )
    )
}
