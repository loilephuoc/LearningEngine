package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader

@Composable
fun MediaInspectorPane(
    uiState: PackageContentBrowserUiState,
    playbackCoordinator: PlaybackCoordinator? = null,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
    modifier: Modifier = Modifier
) {
    val selectedItem = uiState.selectedItemInView
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Media & Details Inspector",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (selectedItem == null) {
                Text(
                    text = "Select an item in Explorer to inspect media and quality checks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Surface
            }

            // 1. IMAGE ASSET CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Image Asset",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        AvailabilityBadge(isPresent = selectedItem.imageRef != null)
                    }

                    if (selectedItem.imageRef != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall),
                            contentAlignment = Alignment.Center
                        ) {
                            LessonThumbnail(selectedItem.imageRef, thumbnailLoader)
                        }
                        Text(
                            text = "File: ${selectedItem.imageRef}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No image file attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. AUDIO ASSETS CARDS
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Audio Assets",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                AudioInspectorCard(
                    title = "Question Audio",
                    audioRef = selectedItem.questionAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )
                AudioInspectorCard(
                    title = "Answer Audio",
                    audioRef = selectedItem.answerAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )
                AudioInspectorCard(
                    title = "Example Audio",
                    audioRef = selectedItem.exampleAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )
                AudioInspectorCard(
                    title = "Translation Audio",
                    audioRef = selectedItem.translationAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 3. QUALITY & VALIDATION PANEL
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quality Checks",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                QualityCheckRow(label = "Image Present", isPass = selectedItem.imageRef != null)
                QualityCheckRow(label = "Question Audio Present", isPass = selectedItem.questionAudioRef != null)
                QualityCheckRow(label = "Answer Audio Present", isPass = selectedItem.answerAudioRef != null)
                QualityCheckRow(label = "IPA Non-Empty", isPass = selectedItem.pronunciation.isNotBlank())
                QualityCheckRow(label = "Example Non-Empty", isPass = !selectedItem.exampleText.isNullOrBlank())
                QualityCheckRow(label = "Translation Non-Empty", isPass = !selectedItem.exampleTranslation.isNullOrBlank())

                // Requirement: Do NOT fake validation. If something has not been checked display "Not Evaluated".
                QualityCheckRow(label = "Duplicate Check", isPass = null)
                QualityCheckRow(label = "Audio Spectrum Quality", isPass = null)
            }
        }
    }
}

@Composable
private fun AudioInspectorCard(
    title: String,
    audioRef: String?,
    playbackCoordinator: PlaybackCoordinator?,
    onFallbackPlay: ((String) -> Unit)?,
    onFallbackStop: (() -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                AvailabilityBadge(isPresent = !audioRef.isNullOrBlank())
            }

            if (!audioRef.isNullOrBlank()) {
                Text(
                    text = audioRef,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Waveform Preview Graphic
                WaveformVisualizer(isPlaying = playbackCoordinator?.status is PlaybackStatus.Playing && (playbackCoordinator.status as PlaybackStatus.Playing).audioRef == audioRef)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    AudioStateButton(
                        audioRef = audioRef,
                        playbackCoordinator = playbackCoordinator,
                        onFallbackPlay = onFallbackPlay,
                        onFallbackStop = onFallbackStop
                    )
                }
            } else {
                Text(
                    text = "No audio reference",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun WaveformVisualizer(isPlaying: Boolean) {
    val barColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(vertical = 2.dp)
    ) {
        val count = 28
        val spacing = size.width / count
        val barWidth = spacing * 0.6f

        for (i in 0 until count) {
            val heightFactor = if (isPlaying) {
                0.2f + (0.8f * ((i * 17 + System.currentTimeMillis() / 100) % 100) / 100f)
            } else {
                0.2f + (0.6f * ((i * 13) % 10) / 10f)
            }
            val barHeight = size.height * heightFactor
            val x = i * spacing + spacing / 2
            val yTop = (size.height - barHeight) / 2
            drawLine(
                color = barColor,
                start = Offset(x, yTop),
                end = Offset(x, yTop + barHeight),
                strokeWidth = barWidth
            )
        }
    }
}

@Composable
private fun AvailabilityBadge(isPresent: Boolean) {
    Surface(
        color = if (isPresent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = if (isPresent) "✓ Available" else "✗ Missing",
            style = MaterialTheme.typography.labelSmall,
            color = if (isPresent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun QualityCheckRow(label: String, isPass: Boolean?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        when (isPass) {
            true -> Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                Text("✓ PASS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            false -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.extraSmall) {
                Text("✗ FAIL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            null -> Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) {
                Text("Not Evaluated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}
