package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import vn.loi.learning.adapter.jvm.JvmJsonFileReader
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.LegacyPackageCandidate
import vn.loi.learning.application.contentpackaging.LegacyPackageContentImporter
import vn.loi.learning.infrastructure.contentmedia.ImportedMediaVerifier
import vn.loi.learning.infrastructure.contentmedia.LegacyContentMediaPathMapper
import vn.loi.learning.infrastructure.contentmedia.PackageMediaExtractor
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor

/**
 * Importer dành riêng cho package legacy gồm hai file:
 *
 * - <package-name>.json chứa metadata và nội dung học;
 * - <package-name>.pkg chứa media nhị phân theo format OPD3.
 *
 * Pipeline:
 *
 * 1. đọc JSON legacy;
 * 2. chuyển JSON thành Domain Content và LearningItem;
 * 3. extract media từ archive OPD3;
 * 4. ánh xạ media tồn tại sang đường dẫn đã lưu;
 * 5. loại bỏ reference media bị thiếu và ghi warning;
 * 6. xác thực các media reference còn lại;
 * 7. trả về ImportedPackageContent chưa được lưu repository.
 */
class LegacyOpd3PackageImporter(
    private val jsonImporter: LegacyJsonImporter,
    private val mediaExtractor: PackageMediaExtractor,
    private val mediaPathMapper: LegacyContentMediaPathMapper,
    private val mediaVerifier: ImportedMediaVerifier,
    private val jsonFileReader: JvmJsonFileReader =
        JvmJsonFileReader()
) : LegacyPackageContentImporter {

    override fun importContent(
        candidate: LegacyPackageCandidate
    ): ImportedPackageContent {
        val jsonPath =
            Paths.get(
                candidate.jsonSource
            )

        val mediaPath =
            Paths.get(
                candidate.mediaSource
            )

        val packageName =
            derivePackageName(
                jsonPath
            )

        val jsonText =
            jsonFileReader.read(
                jsonPath
            )

        val legacyResult =
            jsonImporter.import(
                sourceName =
                    jsonPath.fileName.toString(),
                jsonText =
                    jsonText
            )

        val extractedAssets =
            mediaExtractor.extract(
                packageFile = mediaPath,
                packageName = packageName
            )

        val mediaMappingResult =
            mediaPathMapper.mapWithWarnings(
                contents = legacyResult.contents,
                assets = extractedAssets
            )

        mediaVerifier.verify(
            mediaMappingResult.contents
        )

        val warnings =
            buildList {
                if (
                    legacyResult.skippedRecordCount > 0
                ) {
                    add(
                        "Skipped ${legacyResult.skippedRecordCount} legacy record(s)."
                    )
                }

                if (
                    legacyResult.duplicateCount > 0
                ) {
                    add(
                        "Detected ${legacyResult.duplicateCount} duplicate legacy record(s)."
                    )
                }

                addAll(
                    mediaMappingResult.warnings
                )
            }

        return ImportedPackageContent(
            contents = mediaMappingResult.contents,
            learningItems =
                legacyResult.learningItems,
            libraries =
                listOf(
                    ContentLibrary(
                        id = ContentLibraryId(createLibraryId(candidate.jsonSource)),
                        descriptor = LibraryDescriptor(name = packageName),
                        contentIds = mediaMappingResult.contents.map { it.id }.toSet()
                    )
                ),
            warnings = warnings
        )
    }

    fun import(
        candidate: LegacyPackageCandidate
    ): ImportedPackageContent =
        importContent(
            candidate
        )

    private fun derivePackageName(
        jsonPath: Path
    ): String {
        val fileName =
            jsonPath.fileName.toString()

        val extensionSeparatorIndex =
            fileName.lastIndexOf('.')

        require(
            extensionSeparatorIndex > 0
        ) {
            "Legacy JSON source must have a file extension: $jsonPath"
        }

        return fileName.substring(
            startIndex = 0,
            endIndex = extensionSeparatorIndex
        )
    }

    private fun createLibraryId(source: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(source.trim().toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(ID_HASH_LENGTH)
        return "legacy-library-$hash"
    }

    private companion object {
        const val ID_HASH_LENGTH = 24
    }
}
