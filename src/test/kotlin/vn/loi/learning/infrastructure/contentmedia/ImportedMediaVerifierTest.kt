package vn.loi.learning.infrastructure.contentmedia

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType

class ImportedMediaVerifierTest {

    @Test
    fun `verify succeeds when all media exist`() {

        val root =
            Files.createTempDirectory(
                "media-storage"
            )

        val storage =
            JvmContentMediaStorage(
                root
            )

        storage.store(
            "lesson",
            "audio/test.mp3",
            byteArrayOf(1)
        )

        val verifier =
            ImportedMediaVerifier(
                storage
            )

        verifier.verify(
            listOf(
                content(
                    "lesson/audio/test.mp3"
                )
            )
        )
    }

    @Test
    fun `verify fails when media missing`() {

        val root =
            Files.createTempDirectory(
                "media-storage"
            )

        val verifier =
            ImportedMediaVerifier(
                JvmContentMediaStorage(root)
            )

        assertFailsWith<IllegalArgumentException> {

            verifier.verify(
                listOf(
                    content(
                        "lesson/audio/test.mp3"
                    )
                )
            )
        }
    }

    private fun content(
        audio: String
    ) =
        Content(
            id = ContentId("1"),
            type = ContentType.SENTENCE,
            text =
                ContentText(
                    primaryText = "hello"
                ),
            media =
                ContentMedia(
                    primaryAudio = audio
                )
        )
}