package vn.loi.learning.desktop.ui.library

import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.domain.library.model.LibraryId

enum class LibraryFailureCategory {
    MISCONFIGURED_SERVICE,
    LIBRARY_NOT_FOUND,
    UNEXPECTED_FAILURE
}

class LibraryServiceUnavailableException(
    message: String = "Library query or command service is misconfigured or unavailable."
) : RuntimeException(message)

class LibraryNotFoundException(
    val libraryId: LibraryId
) : RuntimeException("Library '${libraryId.value}' was not found.")

/**
 * Maps failure categories, exceptions, and typed LibraryCommandResult variants into
 * deterministic, user-facing Desktop Library error messages.
 * Never includes raw exception cause text, stack traces, paths, secrets, or unmapped internal details.
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

    fun forCommandResult(result: LibraryCommandResult<*>): String = when (result) {
        is LibraryCommandResult.Success -> ""
        is LibraryCommandResult.LibraryNotFound -> LIBRARY_NOT_FOUND_MESSAGE
        is LibraryCommandResult.PackageNotFound -> "The requested package was not found."
        is LibraryCommandResult.CollectionNotFound -> "The requested collection was not found."
        is LibraryCommandResult.InvalidState -> if (result.message.isNotBlank()) result.message else "The action cannot be performed in the current state."
        is LibraryCommandResult.DuplicateCollection -> "A collection with the name '${result.collectionName}' already exists."
        is LibraryCommandResult.AlreadyAssigned -> "This package is already assigned to the collection."
        is LibraryCommandResult.NotAssigned -> "This package is not assigned to the collection."
        is LibraryCommandResult.ActiveVersionConflict -> "Another active version of package '${result.packageId}' already exists in the library."
        is LibraryCommandResult.PackageNotRegisteredInLibrary -> "Package '${result.packageId}' is not registered in target library."
        is LibraryCommandResult.CrossLibraryConflict -> "Operations across different libraries are forbidden."
        is LibraryCommandResult.PersistenceFailure -> "A storage failure occurred. Please try again."
    }
}
