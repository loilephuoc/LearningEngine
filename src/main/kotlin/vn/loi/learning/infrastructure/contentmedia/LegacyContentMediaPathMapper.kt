package vn.loi.learning.infrastructure.contentmedia

import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentMedia

class LegacyContentMediaPathMapper {

    fun map(
        contents: List<Content>,
        assets: List<ContentMediaAsset>
    ): List<Content> =
        mapWithWarnings(
            contents = contents,
            assets = assets
        ).contents

    fun mapWithWarnings(
        contents: List<Content>,
        assets: List<ContentMediaAsset>
    ): LegacyContentMediaMappingResult {
        if (contents.isEmpty()) {
            return LegacyContentMediaMappingResult(
                contents = contents
            )
        }

        val assetsByFileName =
            assets.associateBy { asset ->
                normalizePath(
                    asset.fileName
                )
            }

        val warnings =
            mutableListOf<String>()

        val mappedContents =
            contents.map { content ->
                content.copy(
                    media =
                        mapMedia(
                            content = content,
                            media = content.media,
                            assetsByFileName = assetsByFileName,
                            warnings = warnings
                        )
                )
            }

        return LegacyContentMediaMappingResult(
            contents = mappedContents,
            warnings = warnings
        )
    }

    private fun mapMedia(
        content: Content,
        media: ContentMedia,
        assetsByFileName: Map<String, ContentMediaAsset>,
        warnings: MutableList<String>
    ): ContentMedia =
        ContentMedia(
            primaryAudio =
                mapReference(
                    content = content,
                    mediaType = "primary audio",
                    reference = media.primaryAudio,
                    assetsByFileName = assetsByFileName,
                    warnings = warnings
                ),
            translatedAudio =
                mapReference(
                    content = content,
                    mediaType = "translated audio",
                    reference = media.translatedAudio,
                    assetsByFileName = assetsByFileName,
                    warnings = warnings
                ),
            image =
                mapReference(
                    content = content,
                    mediaType = "image",
                    reference = media.image,
                    assetsByFileName = assetsByFileName,
                    warnings = warnings
                ),
            exampleAudio =
                mapReference(
                    content = content,
                    mediaType = "example audio",
                    reference = media.exampleAudio,
                    assetsByFileName = assetsByFileName,
                    warnings = warnings
                ),
            exampleTranslatedAudio =
                mapReference(
                    content = content,
                    mediaType = "example translated audio",
                    reference = media.exampleTranslatedAudio,
                    assetsByFileName = assetsByFileName,
                    warnings = warnings
                )
        )

    private fun mapReference(
        content: Content,
        mediaType: String,
        reference: String?,
        assetsByFileName: Map<String, ContentMediaAsset>,
        warnings: MutableList<String>
    ): String? {
        if (reference == null) {
            return null
        }

        val mappedReference =
            assetsByFileName[
                normalizePath(
                    reference
                )
            ]?.relativePath

        if (mappedReference == null) {
            warnings +=
                "Missing $mediaType for content ${content.id}: $reference"
        }

        return mappedReference
    }

    private fun normalizePath(
        path: String
    ): String =
        path
            .trim()
            .replace('\\', '/')
            .removePrefix("./")
}