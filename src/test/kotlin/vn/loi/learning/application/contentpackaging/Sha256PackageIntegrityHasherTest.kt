package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256PackageIntegrityHasherTest {

    private val hasher =
        Sha256PackageIntegrityHasher()

    @Test
    fun `hashes empty content`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hasher.hash(
                ""
            )
        )
    }

    @Test
    fun `hashes known text`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hasher.hash(
                "abc"
            )
        )
    }
}