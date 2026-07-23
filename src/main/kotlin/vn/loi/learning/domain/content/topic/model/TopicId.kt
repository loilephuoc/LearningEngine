package vn.loi.learning.domain.content.topic.model

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import vn.loi.learning.domain.content.model.ContentId

/**
 * Stable identity of an installed learning topic.
 *
 * A package release may change version, libraries, ordering, or display metadata while the
 * topic identity remains unchanged. Existing package records without a persisted topic ID use
 * the deterministic legacy derivation once and persist the value on their next write.
 */
@JvmInline
value class TopicId(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "Topic ID must not be blank."
        }
    }

    override fun toString(): String =
        value

    companion object {

        fun deriveForLegacyPackage(
            packageName: String,
            packageFormat: String
        ): TopicId {
            require(packageName.isNotBlank()) {
                "Package name must not be blank."
            }
            require(packageFormat.isNotBlank()) {
                "Package format must not be blank."
            }

            val seed =
                packageName.trim() +
                    "\u0000" +
                    packageFormat.trim().uppercase()

            val hash =
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                        seed.toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    .joinToString("") { byte ->
                        "%02x".format(byte)
                    }

            return TopicId(
                "topic-${hash.take(ID_HASH_LENGTH)}"
            )
        }

        fun deriveForUnpackagedContent(
            contentId: ContentId
        ): TopicId =
            derive(
                "unpackaged-content\u0000${contentId.value}"
            )

        private fun derive(seed: String): TopicId {
            val hash =
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                        seed.toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    .joinToString("") { byte ->
                        "%02x".format(byte)
                    }

            return TopicId(
                "topic-${hash.take(ID_HASH_LENGTH)}"
            )
        }

        private const val ID_HASH_LENGTH =
            24
    }
}
