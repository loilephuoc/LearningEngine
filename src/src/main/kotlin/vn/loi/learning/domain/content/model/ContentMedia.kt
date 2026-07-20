package vn.loi.learning.domain.content.model

/**
 * Tham chiếu đến các tài nguyên media của Content.
 *
 * Engine chỉ lưu reference dưới dạng String.
 * Nó không biết reference đó là:
 * - Android asset
 * - file trên Windows
 * - URI trên iPhone
 * - URL trên server
 * - object trên cloud storage
 *
 * Việc mở và phát media thuộc Adapter/UI, không thuộc Domain.
 */
data class ContentMedia(
    val primaryAudio: String? = null,
    val translatedAudio: String? = null,
    val image: String? = null,
    val exampleAudio: String? = null,
    val exampleTranslatedAudio: String? = null
) {

    init {
        require(primaryAudio == null || primaryAudio.isNotBlank()) {
            "Primary audio reference must not be blank."
        }

        require(translatedAudio == null || translatedAudio.isNotBlank()) {
            "Translated audio reference must not be blank."
        }

        require(image == null || image.isNotBlank()) {
            "Image reference must not be blank."
        }

        require(exampleAudio == null || exampleAudio.isNotBlank()) {
            "Example audio reference must not be blank."
        }

        require(exampleTranslatedAudio == null || exampleTranslatedAudio.isNotBlank()) {
            "Example translated audio reference must not be blank."
        }
    }

    fun isEmpty(): Boolean =
        primaryAudio == null &&
                translatedAudio == null &&
                image == null &&
                exampleAudio == null &&
                exampleTranslatedAudio == null
}