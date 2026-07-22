package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentTextFormat

class ContentFingerprintFactory(
    private val hasher: PackageIntegrityHasher =
        Sha256PackageIntegrityHasher()
) {

    fun create(
        content: Content
    ): ContentFingerprint =
        ContentFingerprint(
            hasher.hash(
                canonicalize(
                    content
                )
            )
        )

    private fun canonicalize(
        content: Content
    ): String =
        buildString {
            appendValue(
                content.type.name
            )

            appendValue(
                content.text.primaryText
            )

            appendValue(
                content.text.translatedText
            )

            appendValue(
                content.text.pronunciation
            )

            appendValue(
                content.text.exampleText
            )

            appendValue(
                content.text.exampleTranslation
            )
            val formats = listOf(
                content.text.primaryFormat,
                content.text.translatedFormat,
                content.text.exampleFormat,
                content.text.exampleTranslationFormat
            )
            if (formats.any { format -> format != ContentTextFormat.PLAIN_TEXT }) {
                appendValue("content-text-formats-v1")
                formats.forEach { format -> appendValue(format.name) }
            }

            appendValue(
                content.media.primaryAudio
            )

            appendValue(
                content.media.translatedAudio
            )

            appendValue(
                content.media.image
            )

            appendValue(
                content.media.exampleAudio
            )

            appendValue(
                content.media.exampleTranslatedAudio
            )

            appendValue(
                content.metadata.title
            )

            appendValue(
                content.metadata.group
            )

            appendValue(
                content.metadata.section
            )

            appendValue(
                content.metadata.lesson
            )

            appendValues(
                content.metadata.tags
                    .sorted()
            )

            appendValue(
                content.metadata.source
            )

            appendValues(
                content.customFields.fields
                    .sortedBy { field ->
                        field.id.value
                    }
                    .flatMap { field ->
                        listOf(
                            field.id.value,
                            field.value
                        )
                    }
            )
        }

    private fun StringBuilder.appendValues(
        values: List<String>
    ) {
        appendValue(
            values.size.toString()
        )

        values.forEach { value ->
            appendValue(
                value
            )
        }
    }

    private fun StringBuilder.appendValue(
        value: String?
    ) {
        if (value == null) {
            append("-1:")
            return
        }

        append(value.length)
        append(':')
        append(value)
    }
}
