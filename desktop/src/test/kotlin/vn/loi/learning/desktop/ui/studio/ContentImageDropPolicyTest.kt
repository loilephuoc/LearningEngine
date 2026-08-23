package vn.loi.learning.desktop.ui.studio

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ContentImageDropPolicyTest {
    @Test fun `supported image routes to one canonical import decision`() {
        val image = Files.createTempFile("content-image-drop-", ".png").toFile()
        try {
            val decision = assertIs<ContentImageDropDecision.Import>(ContentImageDropPolicy.evaluate(image))
            assertEquals(image, decision.file)
        } finally { image.delete() }
    }

    @Test fun `invalid extension is safely rejected with supported formats`() {
        val audio = Files.createTempFile("content-image-drop-", ".mp3").toFile()
        try {
            val decision = assertIs<ContentImageDropDecision.Reject>(ContentImageDropPolicy.evaluate(audio))
            assertTrue(decision.message.contains("PNG"))
        } finally { audio.delete() }
    }

    @Test fun `missing file is rejected before import`() {
        assertIs<ContentImageDropDecision.Reject>(ContentImageDropPolicy.evaluate(null))
    }
}
