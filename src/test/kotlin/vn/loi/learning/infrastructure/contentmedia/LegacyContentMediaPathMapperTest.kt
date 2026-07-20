package vn.loi.learning.infrastructure.contentmedia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType

class LegacyContentMediaPathMapperTest {

    private val mapper =
        LegacyContentMediaPathMapper()

    @Test
    fun `maps legacy media file names to persisted relative paths`() {
        val content =
            Content(
                id =
                    ContentId(
                        "content-1"
                    ),
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Open the door."
                    ),
                media =
                    ContentMedia(
                        primaryAudio =
                            "audio/door.mp3",
                        translatedAudio =
                            "audio_vi/door.mp3",
                        image =
                            "images/door.png",
                        exampleAudio =
                            "examples/door.mp3",
                        exampleTranslatedAudio =
                            "examples_vi/door.mp3"
                    )
            )

        val assets =
            listOf(
                asset(
                    fileName =
                        "audio/door.mp3",
                    relativePath =
                        "lesson-package/audio/door.mp3"
                ),
                asset(
                    fileName =
                        "audio_vi/door.mp3",
                    relativePath =
                        "lesson-package/audio_vi/door.mp3"
                ),
                asset(
                    fileName =
                        "images/door.png",
                    relativePath =
                        "lesson-package/images/door.png"
                ),
                asset(
                    fileName =
                        "examples/door.mp3",
                    relativePath =
                        "lesson-package/examples/door.mp3"
                ),
                asset(
                    fileName =
                        "examples_vi/door.mp3",
                    relativePath =
                        "lesson-package/examples_vi/door.mp3"
                )
            )

        val mapped =
            mapper.map(
                contents =
                    listOf(
                        content
                    ),
                assets =
                    assets
            ).single()

        assertEquals(
            "lesson-package/audio/door.mp3",
            mapped.media.primaryAudio
        )

        assertEquals(
            "lesson-package/audio_vi/door.mp3",
            mapped.media.translatedAudio
        )

        assertEquals(
            "lesson-package/images/door.png",
            mapped.media.image
        )

        assertEquals(
            "lesson-package/examples/door.mp3",
            mapped.media.exampleAudio
        )

        assertEquals(
            "lesson-package/examples_vi/door.mp3",
            mapped.media.exampleTranslatedAudio
        )
    }

    @Test
    fun `removes missing primary media reference and reports warning`() {
        val content =
            Content(
                id =
                    ContentId(
                        "content-2"
                    ),
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "No extracted media."
                    ),
                media =
                    ContentMedia(
                        primaryAudio =
                            "missing.mp3"
                    )
            )

        val result =
            mapper.mapWithWarnings(
                contents =
                    listOf(
                        content
                    ),
                assets =
                    emptyList()
            )

        val mapped =
            result.contents.single()

        assertNull(
            mapped.media.primaryAudio
        )

        assertEquals(
            listOf(
                "Missing primary audio for content content-2: missing.mp3"
            ),
            result.warnings
        )
    }

    @Test
    fun `removes missing translated media reference`() {
        val content =
            Content(
                id =
                    ContentId(
                        "content-3"
                    ),
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Walk."
                    ),
                media =
                    ContentMedia(
                        exampleAudio =
                            "1743864287268.mp3",
                        exampleTranslatedAudio =
                            "1743864287268_vi.mp3"
                    )
            )

        val result =
            mapper.mapWithWarnings(
                contents =
                    listOf(
                        content
                    ),
                assets =
                    listOf(
                        asset(
                            fileName =
                                "1743864287268.mp3",
                            relativePath =
                                "OPD_2nd/1743864287268.mp3"
                        )
                    )
            )

        val mapped =
            result.contents.single()

        assertEquals(
            "OPD_2nd/1743864287268.mp3",
            mapped.media.exampleAudio
        )

        assertNull(
            mapped.media.exampleTranslatedAudio
        )

        assertEquals(
            listOf(
                "Missing example translated audio for content content-3: 1743864287268_vi.mp3"
            ),
            result.warnings
        )
    }

    @Test
    fun `normalizes windows path separators while matching assets`() {
        val content =
            Content(
                id =
                    ContentId(
                        "content-4"
                    ),
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Windows path."
                    ),
                media =
                    ContentMedia(
                        image =
                            "images\\example.png"
                    )
            )

        val mapped =
            mapper.map(
                contents =
                    listOf(
                        content
                    ),
                assets =
                    listOf(
                        asset(
                            fileName =
                                "images/example.png",
                            relativePath =
                                "lesson-package/images/example.png"
                        )
                    )
            ).single()

        assertEquals(
            "lesson-package/images/example.png",
            mapped.media.image
        )
    }

    private fun asset(
        fileName: String,
        relativePath: String
    ): ContentMediaAsset =
        ContentMediaAsset(
            packageName =
                "lesson-package",
            fileName =
                fileName,
            relativePath =
                relativePath
        )
}