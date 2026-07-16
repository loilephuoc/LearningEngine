package vn.loi.learning.infrastructure.importer.legacy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO phản ánh format JSON của ứng dụng Android cũ.
 *
 * DTO chỉ thuộc Infrastructure.
 * Domain không được phụ thuộc class này.
 */
@Serializable
data class LegacySentenceJsonDto(
    val group: String? = null,
    val section: String? = null,
    val lesson: String? = null,

    val en: String? = null,
    val vi: String? = null,
    val ipa: String? = null,

    val image: String? = null,
    val audio: String? = null,

    @SerialName("audio_vi")
    val audioVi: String? = null,

    val example: String? = null,

    @SerialName("example_vi")
    val exampleVi: String? = null,

    @SerialName("example_audio")
    val exampleAudio: String? = null,

    @SerialName("example_audio_vi")
    val exampleAudioVi: String? = null
)