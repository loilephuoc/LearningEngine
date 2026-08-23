package vn.loi.learning.desktop.tts.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchTtsWorkspaceLayoutTest {
    @Test
    fun `large window uses near fullscreen workspace without legacy cap`() {
        val size = BatchTtsWorkspaceLayout.resolve(1920f, 1080f)
        assertEquals(1824f, size.widthDp)
        assertEquals(1026f, size.heightDp)
        assertTrue(size.widthDp > 920f)
        assertTrue(size.heightDp > 800f)
    }

    @Test
    fun `standard window keeps balanced margins`() {
        assertEquals(BatchTtsWorkspaceSize(1034f, 752f), BatchTtsWorkspaceLayout.resolve(1100f, 800f))
    }

    @Test
    fun `narrow window does not enforce legacy minimum width`() {
        val size = BatchTtsWorkspaceLayout.resolve(600f, 700f)
        assertEquals(588f, size.widthDp)
        assertTrue(size.widthDp < 720f)
    }

    @Test
    fun `low height keeps footer inside available viewport`() {
        val size = BatchTtsWorkspaceLayout.resolve(1000f, 500f)
        assertEquals(490f, size.heightDp)
        assertTrue(size.heightDp < 500f)
    }
}
