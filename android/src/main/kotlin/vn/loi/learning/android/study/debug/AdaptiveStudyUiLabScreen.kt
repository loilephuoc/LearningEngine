package vn.loi.learning.android.study.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.partofspeech.PartOfSpeechExtractor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Adaptive Study UI Lab Launcher Screen.
 * Configures the data package, vocabulary item, and forced study mode,
 * then opens the full-screen canonical production [AdaptiveStudyUiPreviewScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveStudyUiLabScreen(
    engine: LearningApplicationContext?,
    onBack: () -> Unit,
    onOpenPreview: (packageId: String, itemIndex: Int, mode: LabStudyMode) -> Unit
) {
    val installedPackages = remember(engine) {
        engine?.installedPackageRepository?.findAll().orEmpty()
    }

    val demoPackageContents = remember { AdaptiveStudyUiLabStateFactory.createDemoPackageContents() }

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

    val pos = PartOfSpeechExtractor.primary(currentContent)?.value
    val ipa = currentContent.customFields[ContentFieldId("ipa")]?.value
        ?: currentContent.customFields[ContentFieldId("pronunciation")]?.value

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
                            text = "Bộ khởi chạy kiểm thử giao diện học thật · Debug Only",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Package Selector
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Gói dữ liệu từ vựng:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    var packageMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { packageMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
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
            }

            // Section 2: Vocabulary Item Selector
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Chọn từ vựng kiểm thử:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentItemIndex + 1} / ${activeContents.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Từ trước")
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentContent.text.primaryText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!ipa.isNullOrBlank() || !pos.isNullOrBlank()) {
                                    Text(
                                        text = listOfNotNull(ipa, pos).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                currentContent.text.translatedText?.let { tr ->
                                    Text(
                                        text = tr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (currentItemIndex < activeContents.size - 1) currentItemIndex++
                            },
                            enabled = currentItemIndex < activeContents.size - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Từ tiếp theo")
                        }
                    }
                }
            }

            // Section 3: Forced Study Mode Selection
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "3. Ép chế độ học (Forced Study Mode):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    LabStudyMode.entries.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMode = mode },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedMode = mode }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Primary Action Button to Open Fullscreen Production Preview
            Button(
                onClick = {
                    onOpenPreview(selectedPackageId, currentItemIndex, selectedMode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Mở giao diện học thật",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Section 5: Safety Guarantee Notice
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Giao diện học thật sẽ tải toàn màn hình với đầy đủ TopBar, HUD, IME Keyboard, âm thanh và đánh giá. Mọi tương tác đều là bộ nhớ tạm và không ghi vào dữ liệu FSRS thật.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
