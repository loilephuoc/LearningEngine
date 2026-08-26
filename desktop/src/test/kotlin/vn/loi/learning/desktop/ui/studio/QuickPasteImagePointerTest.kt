package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class QuickPasteImagePointerTest {
    @Test
    fun `one secondary click invokes quick paste exactly once`() = runComposeUiTest {
        var pasteRequests = 0
        setContent {
            Box(
                Modifier
                    .size(200.dp)
                    .testTag("image-area")
                    .quickPasteImageOnSecondaryClick { pasteRequests++ }
            )
        }

        onNodeWithTag("image-area").performMouseInput { click(button = MouseButton.Secondary) }

        assertEquals(1, pasteRequests)
    }

    @Test
    fun `primary click does not invoke image quick paste`() = runComposeUiTest {
        var pasteRequests = 0
        setContent {
            Box(
                Modifier
                    .size(200.dp)
                    .testTag("image-area")
                    .quickPasteImageOnSecondaryClick { pasteRequests++ }
            )
        }

        onNodeWithTag("image-area").performMouseInput { click(button = MouseButton.Primary) }

        assertEquals(0, pasteRequests)
    }
}
