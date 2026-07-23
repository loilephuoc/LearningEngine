package vn.loi.learning.desktop.ui.library

import vn.loi.learning.domain.library.model.LibraryId

enum class LibraryFailureCategory {
    MISCONFIGURED_SERVICE,
    LIBRARY_NOT_FOUND,
    UNEXPECTED_FAILURE
}

class LibraryServiceUnavailableException(
    message: String = "Library query service is misconfigured or unavailable."
) : RuntimeException(message)

class LibraryNotFoundException(
    val libraryId: LibraryId
) : RuntimeException("Library '${libraryId.value}' was not found.")

/**
 * Maps failure categories and exceptions into deterministic, user-facing Desktop Library error messages.
 * Never includes raw exception messages, cause text, stack traces, paths, secrets, or internal IDs.
 */
internal object LibraryFailureMessage {

    const val SERVICE_UNAVAILABLE_MESSAGE = "Library service is unavailable. Please restart the application."
    const val LIBRARY_NOT_FOUND_MESSAGE = "The selected library is unavailable."
    const val UNEXPECTED_FAILURE_MESSAGE = "Unable to load the library. Please try again."

    fun forCategory(category: LibraryFailureCategory): String = when (category) {
        LibraryFailureCategory.MISCONFIGURED_SERVICE -> SERVICE_UNAVAILABLE_MESSAGE
        LibraryFailureCategory.LIBRARY_NOT_FOUND -> LIBRARY_NOT_FOUND_MESSAGE
        LibraryFailureCategory.UNEXPECTED_FAILURE -> UNEXPECTED_FAILURE_MESSAGE
    }

    fun forFailure(failure: Throwable): String = when (failure) {
        is LibraryServiceUnavailableException -> forCategory(LibraryFailureCategory.MISCONFIGURED_SERVICE)
        is LibraryNotFoundException -> forCategory(LibraryFailureCategory.LIBRARY_NOT_FOUND)
        else -> forCategory(LibraryFailureCategory.UNEXPECTED_FAILURE)
    }
}
