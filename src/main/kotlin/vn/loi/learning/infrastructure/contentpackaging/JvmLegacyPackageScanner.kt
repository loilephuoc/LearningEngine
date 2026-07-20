package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import vn.loi.learning.application.contentpackaging.LegacyPackageCandidate
import vn.loi.learning.application.contentpackaging.LegacyPackageScanner

/**
 * Scanner filesystem JVM dành riêng cho package legacy OPD3.
 *
 * Một candidate hợp lệ phải gồm hai file trực tiếp trong cùng thư mục:
 *
 * - <base-name>.json
 * - <base-name>.pkg
 *
 * Hai phần mở rộng và base name được so khớp không phân biệt
 * chữ hoa và chữ thường.
 *
 * Các file không có file đồng hành tương ứng sẽ bị bỏ qua.
 */
class JvmLegacyPackageScanner(
    private val directory: Path
) : LegacyPackageScanner {

    override fun scan(): List<LegacyPackageCandidate> {
        require(
            Files.isDirectory(
                directory
            )
        ) {
            "Legacy package scan source must be an existing directory: $directory"
        }

        val files =
            Files.list(
                directory
            ).use { paths ->
                paths
                    .filter(
                        Files::isRegularFile
                    )
                    .toList()
            }

        val jsonFilesByBaseName =
            files
                .filter { path ->
                    path.hasExtension(
                        JSON_EXTENSION
                    )
                }
                .associateBy { path ->
                    path.normalizedBaseName()
                }

        val mediaFilesByBaseName =
            files
                .filter { path ->
                    path.hasExtension(
                        PACKAGE_EXTENSION
                    )
                }
                .associateBy { path ->
                    path.normalizedBaseName()
                }

        return jsonFilesByBaseName
            .keys
            .intersect(
                mediaFilesByBaseName.keys
            )
            .sorted()
            .map { normalizedBaseName ->
                LegacyPackageCandidate(
                    jsonSource =
                        jsonFilesByBaseName
                            .getValue(
                                normalizedBaseName
                            )
                            .toString(),
                    mediaSource =
                        mediaFilesByBaseName
                            .getValue(
                                normalizedBaseName
                            )
                            .toString()
                )
            }
    }

    private fun Path.hasExtension(
        extension: String
    ): Boolean =
        fileName
            .toString()
            .endsWith(
                extension,
                ignoreCase = true
            )

    private fun Path.normalizedBaseName(): String {
        val fileName =
            fileName.toString()

        val extensionSeparatorIndex =
            fileName.lastIndexOf(
                '.'
            )

        return fileName
            .substring(
                startIndex = 0,
                endIndex = extensionSeparatorIndex
            )
            .lowercase(
                Locale.ROOT
            )
    }

    private companion object {

        const val JSON_EXTENSION =
            ".json"

        const val PACKAGE_EXTENSION =
            ".pkg"
    }
}