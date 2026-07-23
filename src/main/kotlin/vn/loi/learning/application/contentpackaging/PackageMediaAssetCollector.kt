package vn.loi.learning.application.contentpackaging

import java.util.Locale
import vn.loi.learning.domain.content.model.ContentId

/**
 * Cổng đọc byte tài nguyên media từ file nguồn legacy PKG.
 */
fun interface LegacyMediaByteReader {
    fun readAssetBytes(packageSource: String, assetPath: String): ByteArray?
}

/**
 * Thu thập và đóng gói các byte tài nguyên media cho một CanonicalTopicPackage.
 *
 * Nhiệm vụ:
 * - Thu thập dữ liệu byte của các tệp media được tham chiếu;
 * - Loại bỏ tài nguyên trùng lặp (duplicate asset elimination);
 * - Tính toán checksum SHA-256 cho từng tệp media;
 * - Xây dựng CanonicalMediaManifest sắp xếp đinh ninh;
 * - Ghi chẩn đoán warning cho các media không giải quyết được (unresolved assets).
 *
 * Tuyệt đối không chỉnh sửa trạng thái học tập của người học.
 */
class PackageMediaAssetCollector(
    private val integrityHasher: PackageIntegrityHasher = Sha256PackageIntegrityHasher(),
    private val mediaByteReader: LegacyMediaByteReader = LegacyMediaByteReader { _, _ -> null }
) {

    fun collect(
        canonicalPackage: CanonicalTopicPackage,
        customByteReader: LegacyMediaByteReader? = null
    ): CanonicalMediaBundle {
        val reader = customByteReader ?: mediaByteReader
        val packageSource = canonicalPackage.sourceMetadata.packageSource
        val diagnostics = mutableListOf<CanonicalConversionDiagnostic>()

        val collectedAssetsByPath = mutableMapOf<String, CanonicalMediaAssetBytes>()
        val contentOwnersByPath = mutableMapOf<String, MutableSet<ContentId>>()
        val mediaTypeByPath = mutableMapOf<String, CanonicalMediaType>()

        canonicalPackage.mediaReferences.forEach { mediaRef ->
            val path = mediaRef.logicalPath
            contentOwnersByPath.getOrPut(path) { mutableSetOf() }.add(mediaRef.owningContentId)
            mediaTypeByPath[path] = mediaRef.mediaType

            if (mediaRef.status == CanonicalMediaStatus.MISSING) {
                diagnostics += CanonicalConversionDiagnostic(
                    code = CanonicalConversionDiagnosticCode.UNRESOLVED_MEDIA_REFERENCE,
                    severity = CanonicalConversionDiagnosticSeverity.WARNING,
                    message = "Unresolved media asset referenced by content ${mediaRef.owningContentId.value}: ${mediaRef.referencedAsset}",
                    contentId = mediaRef.owningContentId,
                    source = packageSource
                )
                return@forEach
            }

            if (path !in collectedAssetsByPath) {
                val bytes = try {
                    reader.readAssetBytes(packageSource, mediaRef.referencedAsset)
                        ?: reader.readAssetBytes(packageSource, path)
                } catch (exception: Exception) {
                    null
                }

                if (bytes == null) {
                    diagnostics += CanonicalConversionDiagnostic(
                        code = CanonicalConversionDiagnosticCode.UNRESOLVED_MEDIA_REFERENCE,
                        severity = CanonicalConversionDiagnosticSeverity.WARNING,
                        message = "Could not read media bytes for asset ${mediaRef.referencedAsset} (content ${mediaRef.owningContentId.value}).",
                        contentId = mediaRef.owningContentId,
                        source = packageSource
                    )
                } else {
                    val hash = integrityHasher.hash(bytes)
                    collectedAssetsByPath[path] = CanonicalMediaAssetBytes(
                        logicalPath = path,
                        mediaType = mediaRef.mediaType,
                        bytes = bytes,
                        sha256 = hash
                    )
                }
            }
        }

        // Sắp xếp đinh ninh theo logicalPath
        val sortedAssetPaths = collectedAssetsByPath.keys.sortedWith(String.CASE_INSENSITIVE_ORDER)

        val assets = sortedAssetPaths.mapNotNull { collectedAssetsByPath[it] }

        val manifestEntries = sortedAssetPaths.map { path ->
            val asset = collectedAssetsByPath.getValue(path)
            val owners = contentOwnersByPath[path]?.sortedBy { it.value } ?: emptyList()
            CanonicalMediaManifestEntry(
                logicalPath = path,
                mediaType = asset.mediaType,
                size = asset.size,
                sha256 = asset.sha256,
                owningContentIds = owners
            )
        }

        val sortedDiagnostics = diagnostics.sortedWith(diagnosticComparator)

        return CanonicalMediaBundle(
            manifest = CanonicalMediaManifest(entries = manifestEntries),
            assets = assets,
            diagnostics = sortedDiagnostics
        )
    }

    private companion object {
        val diagnosticComparator: Comparator<CanonicalConversionDiagnostic> =
            compareBy<CanonicalConversionDiagnostic> {
                if (it.severity == CanonicalConversionDiagnosticSeverity.FATAL) 0 else 1
            }
                .thenBy { it.code.name }
                .thenBy { it.contentId?.value.orEmpty() }
                .thenBy { it.message.lowercase(Locale.ROOT) }
    }
}
