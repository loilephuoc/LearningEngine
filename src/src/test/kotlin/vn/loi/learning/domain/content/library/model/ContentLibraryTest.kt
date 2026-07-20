package vn.loi.learning.domain.content.library.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId

class ContentLibraryTest {

    private val libraryId =
        ContentLibraryId(
            "library-1"
        )

    private val descriptor =
        LibraryDescriptor(
            name = "Vocabulary In Use"
        )

    @Test
    fun `library ID rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            ContentLibraryId(" ")
        }
    }

    @Test
    fun `descriptor rejects blank name`() {
        assertFailsWith<IllegalArgumentException> {
            LibraryDescriptor(
                name = " "
            )
        }
    }

    @Test
    fun `new library is empty`() {
        val library =
            ContentLibrary(
                id = libraryId,
                descriptor = descriptor
            )

        assertEquals(
            "Vocabulary In Use",
            library.name
        )

        assertEquals(
            0,
            library.contentCount
        )

        assertTrue(
            library.isEmpty
        )
    }

    @Test
    fun `register adds content immutably`() {
        val contentId =
            ContentId(
                "content-1"
            )

        val original =
            ContentLibrary(
                id = libraryId,
                descriptor = descriptor
            )

        val updated =
            original.register(
                contentId
            )

        assertTrue(
            original.isEmpty
        )

        assertFalse(
            updated.isEmpty
        )

        assertTrue(
            updated.contains(
                contentId
            )
        )

        assertEquals(
            1,
            updated.contentCount
        )
    }

    @Test
    fun `register existing content returns same instance`() {
        val contentId =
            ContentId(
                "content-1"
            )

        val library =
            ContentLibrary(
                id = libraryId,
                descriptor = descriptor,
                contentIds = setOf(contentId)
            )

        assertSame(
            library,
            library.register(contentId)
        )
    }

    @Test
    fun `registerAll adds only missing content`() {
        val first =
            ContentId(
                "content-1"
            )

        val second =
            ContentId(
                "content-2"
            )

        val library =
            ContentLibrary(
                id = libraryId,
                descriptor = descriptor,
                contentIds = setOf(first)
            )

        val updated =
            library.registerAll(
                setOf(first, second)
            )

        assertEquals(
            setOf(first, second),
            updated.contentIds
        )

        assertEquals(
            2,
            updated.contentCount
        )
    }

    @Test
    fun `remove deletes content immutably`() {
        val contentId =
            ContentId(
                "content-1"
            )

        val original =
            ContentLibrary(
                id = libraryId,
                descriptor = descriptor,
                contentIds = setOf(contentId)
            )

        val updated =
            original.remove(
                contentId
            )

        assertTrue(
            original.contains(contentId)
        )

        assertFalse(
            updated.contains(contentId)
        )

        assertTrue(
            updated.isEmpty
        )
    }

    @Test
    fun `remove missing content returns same instance`() {
        val library =
            ContentLibrary(
                id = libraryId,
                descriptor = descriptor
            )

        assertSame(
            library,
            library.remove(
                ContentId(
                    "missing-content"
                )
            )
        )
    }
}
