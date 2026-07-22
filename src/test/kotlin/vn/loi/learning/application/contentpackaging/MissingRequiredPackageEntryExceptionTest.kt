package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MissingRequiredPackageEntryExceptionTest {

    @Test
    fun `bundle entry exception preserves legacy argument and message contracts`() {
        val exception = MissingRequiredPackageEntryException(
            "metadata.json"
        )

        assertIs<PackageImportException>(exception)
        assertIs<IllegalArgumentException>(exception)
        assertEquals("metadata.json", exception.entryName)
        assertEquals(
            "Missing package file: metadata.json",
            exception.message
        )
    }

    @Test
    fun `legacy specialized exceptions share required entry contract`() {
        val manifest = MissingPackageManifestException(
            "manifest.json"
        )
        val content = MissingPackageContentException(
            "content.json"
        )

        assertIs<MissingRequiredPackageEntryException>(manifest)
        assertIs<MissingRequiredPackageEntryException>(content)
        assertEquals("manifest.json", manifest.entryName)
        assertEquals("content.json", content.entryName)
        assertEquals(
            "Missing package manifest: manifest.json",
            manifest.message
        )
        assertEquals(
            "Missing package content entry: content.json",
            content.message
        )
    }

    @Test
    fun `blank required entry name is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            MissingRequiredPackageEntryException(" ")
        }
    }
}
