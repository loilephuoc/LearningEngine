package vn.loi.learning.desktop.ui.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopLoadStatePresentationTest {
    @Test
    fun `ready state does not replace screen content`() {
        assertNull(
            resolveDesktopLoadStatePresentation(
                state = DesktopLoadState.Ready,
                screenName = "Dashboard"
            )
        )
    }

    @Test
    fun `loading state identifies screen and has no retry`() {
        val presentation =
            resolveDesktopLoadStatePresentation(
                state = DesktopLoadState.Loading,
                screenName = "Statistics"
            )

        assertEquals(
            "Loading Statistics",
            presentation?.title
        )
        assertNull(
            presentation?.actionLabel
        )
        assertEquals(
            "Statistics is loading. Please wait while the latest data is loaded.",
            presentation?.contentDescription
        )
    }

    @Test
    fun `failed state preserves detail and offers retry`() {
        val presentation =
            resolveDesktopLoadStatePresentation(
                state =
                    DesktopLoadState.Failed(
                        "Database unavailable"
                    ),
                screenName = "Review History"
            )

        assertEquals(
            "Review History could not be loaded",
            presentation?.title
        )
        assertEquals(
            "Retry",
            presentation?.actionLabel
        )
        assertEquals(
            "Database unavailable. Your previous data remains available when possible.",
            presentation?.description
        )
    }

    @Test
    fun `blank screen and failure text receive stable fallbacks`() {
        val presentation =
            resolveDesktopLoadStatePresentation(
                state = DesktopLoadState.Failed(" "),
                screenName = " "
            )

        assertEquals(
            "Screen could not be loaded",
            presentation?.title
        )
        assertEquals(
            "Unknown error. Your previous data remains available when possible.",
            presentation?.description
        )
    }

    @Test
    fun `throwable message normalization prefers detail then type`() {
        assertEquals(
            "Readable detail",
            IllegalStateException(
                "  Readable detail  "
            ).toDesktopFailureMessage()
        )

        assertEquals(
            "IllegalArgumentException",
            IllegalArgumentException()
                .toDesktopFailureMessage()
        )
    }
}
