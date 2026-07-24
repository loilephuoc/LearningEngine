package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PackageImportSourceResolverTest {

    @Test
    fun `1 single opd3 selection is accepted`() {
        val tempDir = Files.createTempDirectory("opd3-selection-test")
        try {
            val opd3File = tempDir.resolve("package.opd3")
            Files.writeString(opd3File, "opd3 content")

            val source = PackageImportSourceResolver.resolve(listOf(opd3File))
            val opd3Source = assertIs<PackageImportSource.Opd3File>(source)
            assertEquals(opd3File, opd3Source.file)
            assertEquals("package.opd3", opd3Source.displayName)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `2 matching JSON and PKG pair selected together is accepted and produces LegacyPair`() {
        val tempDir = Files.createTempDirectory("pair-selection-test")
        try {
            val jsonFile = tempDir.resolve("2000Cau.json")
            val pkgFile = tempDir.resolve("2000Cau.pkg")
            Files.writeString(jsonFile, "[]")
            Files.writeString(pkgFile, "OPD3")

            val source = PackageImportSourceResolver.resolve(listOf(jsonFile, pkgFile))
            val legacySource = assertIs<PackageImportSource.LegacyPair>(source)
            assertEquals(jsonFile, legacySource.jsonFile)
            assertEquals(pkgFile, legacySource.packageFile)
            assertEquals("2000Cau", legacySource.displayName)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `3 mismatched basenames are rejected before import starts`() {
        val tempDir = Files.createTempDirectory("mismatch-selection-test")
        try {
            val jsonFile = tempDir.resolve("TopicA.json")
            val pkgFile = tempDir.resolve("TopicB.pkg")
            Files.writeString(jsonFile, "[]")
            Files.writeString(pkgFile, "OPD3")

            val ex = assertFailsWith<InvalidImportSelectionException> {
                PackageImportSourceResolver.resolve(listOf(jsonFile, pkgFile))
            }
            assertEquals("To import a legacy topic, select its matching .json and .pkg files together.", ex.message)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `4 JSON-only selection without companion shows actionable validation`() {
        val tempDir = Files.createTempDirectory("json-only-test")
        try {
            val jsonFile = tempDir.resolve("Solo.json")
            Files.writeString(jsonFile, "[]")

            val ex = assertFailsWith<InvalidImportSelectionException> {
                PackageImportSourceResolver.resolve(listOf(jsonFile))
            }
            assertEquals("To import a legacy topic, select its matching .json and .pkg files together.", ex.message)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `5 PKG-only selection without companion shows actionable validation`() {
        val tempDir = Files.createTempDirectory("pkg-only-test")
        try {
            val pkgFile = tempDir.resolve("Solo.pkg")
            Files.writeString(pkgFile, "OPD3")

            val ex = assertFailsWith<InvalidImportSelectionException> {
                PackageImportSourceResolver.resolve(listOf(pkgFile))
            }
            assertEquals("To import a legacy topic, select its matching .json and .pkg files together.", ex.message)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `6 two topic pairs selected together are rejected`() {
        val tempDir = Files.createTempDirectory("two-pairs-test")
        try {
            val jsonA = tempDir.resolve("PairA.json")
            val pkgA = tempDir.resolve("PairA.pkg")
            val jsonB = tempDir.resolve("PairB.json")
            val pkgB = tempDir.resolve("PairB.pkg")
            Files.writeString(jsonA, "[]")
            Files.writeString(pkgA, "OPD3")
            Files.writeString(jsonB, "[]")
            Files.writeString(pkgB, "OPD3")

            val ex = assertFailsWith<InvalidImportSelectionException> {
                PackageImportSourceResolver.resolve(listOf(jsonA, pkgA, jsonB, pkgB))
            }
            assertEquals("To import a legacy topic, select its matching .json and .pkg files together.", ex.message)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `7 single JSON with companion present auto-resolves LegacyPair`() {
        val tempDir = Files.createTempDirectory("auto-pair-test")
        try {
            val jsonFile = tempDir.resolve("2000Cau.json")
            val pkgFile = tempDir.resolve("2000Cau.pkg")
            Files.writeString(jsonFile, "[]")
            Files.writeString(pkgFile, "OPD3")

            val source = PackageImportSourceResolver.resolve(listOf(jsonFile))
            val legacySource = assertIs<PackageImportSource.LegacyPair>(source)
            assertEquals(jsonFile, legacySource.jsonFile)
            assertEquals(pkgFile, legacySource.packageFile)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
