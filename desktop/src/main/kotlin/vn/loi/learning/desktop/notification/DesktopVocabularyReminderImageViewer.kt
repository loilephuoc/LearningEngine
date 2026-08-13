package vn.loi.learning.desktop.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import java.awt.GraphicsConfiguration
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import vn.loi.learning.application.port.ContentMediaStorage

@Composable
fun DesktopVocabularyReminderImageViewer(
    visible: DesktopVocabularyReminderPopupState.FullImage,
    controller: DesktopVocabularyReminderPopupController,
    storage: ContentMediaStorage,
    graphicsConfiguration: GraphicsConfiguration?
) {
    val geometry = DesktopVocabularyReminderPopupPositioning.resolveGeometry(graphicsConfiguration)
    val usableWidth = (geometry.deviceBounds.width - geometry.deviceInsets.left - geometry.deviceInsets.right) /
        geometry.scaleX
    val usableHeight = (geometry.deviceBounds.height - geometry.deviceInsets.top - geometry.deviceInsets.bottom) /
        geometry.scaleY
    val width = (usableWidth * 0.9).toInt().coerceAtLeast(320)
    val height = (usableHeight * 0.9).toInt().coerceAtLeast(240)
    val placement = DesktopVocabularyReminderPopupPositioning.bottomRight(geometry, width, height)
    val state = rememberWindowState(
        position = WindowPosition(placement.xDp.dp, placement.yDp.dp),
        size = DpSize(placement.widthDp.dp, placement.heightDp.dp)
    )
    Window(
        onCloseRequest = controller::closeFullImage,
        state = state,
        title = "Vocabulary image",
        alwaysOnTop = true
    ) {
        val bitmap by produceState<ImageBitmap?>(null, visible.generation) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val reference = visible.candidate.imageReference ?: return@runCatching null
                    val path = storage.resolve(reference) ?: return@runCatching null
                    SkiaImage.makeFromEncoded(Files.readAllBytes(path)).toComposeImageBitmap()
                }.getOrNull()
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
            bitmap?.let {
                Image(it, "Full vocabulary image", Modifier.fillMaxSize().padding(24.dp), contentScale = ContentScale.Fit)
            }
            IconButton(controller::closeFullImage, Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Icon(Icons.Default.Close, "Close full vocabulary image", tint = Color.White)
            }
        }
    }
}
