package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.study.learning.model.LearningItem

class LearningItemFingerprintFactory(
    private val contentFingerprintFactory: ContentFingerprintFactory =
        ContentFingerprintFactory(),
    private val hasher: PackageIntegrityHasher =
        Sha256PackageIntegrityHasher()
) {

    fun create(
        learningItem: LearningItem,
        content: Content
    ): LearningItemFingerprint {
        require(
            learningItem.contentId == content.id
        ) {
            "Learning item ${learningItem.id} does not reference content ${content.id}."
        }

        return LearningItemFingerprint(
            hasher.hash(
                buildString {
                    appendValue(
                        contentFingerprintFactory
                            .create(
                                content
                            )
                            .value
                    )

                    appendValue(
                        learningItem.mode.name
                    )
                }
            )
        )
    }

    private fun StringBuilder.appendValue(
        value: String
    ) {
        append(value.length)
        append(':')
        append(value)
    }
}