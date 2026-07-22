package vn.loi.learning.infrastructure.contentpackaging

import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipFile
import vn.loi.learning.application.contentpackaging.InvalidPackageArchiveStructureException
import vn.loi.learning.application.contentpackaging.PackageArchiveEntryCountExceededException
import vn.loi.learning.application.contentpackaging.PackageArchiveUncompressedSizeExceededException
import vn.loi.learning.application.contentpackaging.PackageImportBundle

class Opd3ArchiveStructureValidator(
    private val limits: Opd3ArchiveStructureLimits =
        Opd3ArchiveStructureLimits()
) {

    fun validate(
        archive: ZipFile
    ) {
        val exactNames = mutableSetOf<String>()
        val logicalNames = mutableMapOf<String, String>()
        val requiredLogicalNames = mutableMapOf<String, String>()
        var entryCount = 0
        var totalDeclaredUncompressedBytes = 0L

        val entries = archive.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            entryCount += 1

            if (entryCount > limits.maximumEntryCount) {
                throw PackageArchiveEntryCountExceededException(
                    limits.maximumEntryCount
                )
            }

            val declaredSize = entry.size

            if (declaredSize < 0) {
                invalid(
                    "entry '${entry.name}' has an unknown declared uncompressed size."
                )
            }

            if (
                declaredSize >
                limits.maximumDeclaredUncompressedBytes -
                    totalDeclaredUncompressedBytes
            ) {
                throw PackageArchiveUncompressedSizeExceededException(
                    limits.maximumDeclaredUncompressedBytes
                )
            }

            totalDeclaredUncompressedBytes += declaredSize

            val name = entry.name
            validateName(name)

            if (!exactNames.add(name)) {
                invalid("duplicate entry name '$name'.")
            }

            val logicalName = normalize(name)
            val previousLogicalName = logicalNames.putIfAbsent(
                logicalName,
                name
            )

            if (
                previousLogicalName != null &&
                previousLogicalName != name
            ) {
                invalid(
                    "entries '$previousLogicalName' and '$name' resolve to the same logical name."
                )
            }

            val requiredName = requiredNameFor(logicalName)

            if (requiredName != null) {
                val previousRequiredName =
                    requiredLogicalNames.putIfAbsent(
                        requiredName,
                        name
                    )

                if (
                    previousRequiredName != null &&
                    previousRequiredName != name
                ) {
                    invalid(
                        "required entry '$requiredName' is ambiguous between '$previousRequiredName' and '$name'."
                    )
                }

                if (name != requiredName) {
                    invalid(
                        "entry '$name' ambiguously matches required entry '$requiredName'."
                    )
                }
            }
        }
    }

    private fun validateName(
        name: String
    ) {
        if (name.isEmpty()) {
            invalid("entry name must not be empty.")
        }

        if (name != name.trim()) {
            invalid("entry name '$name' has leading or trailing whitespace.")
        }

        if (name.startsWith('/') || name.endsWith('/')) {
            invalid("entry name '$name' has an unsafe leading or trailing separator.")
        }

        if (WINDOWS_ABSOLUTE_PATH.matches(name)) {
            invalid("entry name '$name' is an absolute Windows path.")
        }

        if ('\\' in name) {
            invalid("entry name '$name' uses a backslash separator.")
        }

        val segments = name.split('/')

        if (segments.any(String::isEmpty)) {
            invalid("entry name '$name' contains an empty path segment.")
        }

        if (segments.any { segment -> segment == "." }) {
            invalid("entry name '$name' contains a dot path segment.")
        }

        if (segments.any { segment -> segment == ".." }) {
            invalid("entry name '$name' contains parent traversal.")
        }

        if (segments.any { segment -> segment != segment.trim() }) {
            invalid("entry name '$name' contains a path segment with surrounding whitespace.")
        }
    }

    private fun normalize(
        name: String
    ): String =
        Normalizer.normalize(
            name,
            Normalizer.Form.NFKC
        )

    private fun requiredNameFor(
        logicalName: String
    ): String? =
        REQUIRED_NAMES_BY_CASE_FOLDED[
            logicalName.lowercase(Locale.ROOT)
        ]

    private fun invalid(
        detail: String
    ): Nothing =
        throw InvalidPackageArchiveStructureException(
            detail
        )

    private companion object {
        val WINDOWS_ABSOLUTE_PATH =
            Regex("^[A-Za-z]:.*")

        val REQUIRED_NAMES_BY_CASE_FOLDED =
            PackageImportBundle.REQUIRED_FILES.associateBy { name ->
                name.lowercase(Locale.ROOT)
            }
    }
}
