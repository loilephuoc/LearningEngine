package vn.loi.learning.application.contentpackaging

/**
 * Kết quả xác thực an toàn đường dẫn.
 */
sealed interface PathValidationResult {
    object Valid : PathValidationResult
    data class Invalid(val reason: String) : PathValidationResult
}

/**
 * Validator phòng chống các lỗ hổng path traversal, Zip Slip, đường dẫn tuyệt đối,
 * ký tự lạ và đường dẫn nằm ngoài bố cục cho phép của OPD3 package.
 */
object Opd3PathValidator {

    private val ALLOWED_TOP_LEVEL_FILES = setOf(
        "metadata.json",
        "contents.json",
        "learning-items.json",
        "media-manifest.json",
        "manifest.json"
    )

    fun validateArchivePath(path: String): PathValidationResult {
        if (path.isBlank()) {
            return PathValidationResult.Invalid("Entry name must not be blank.")
        }
        if (path.contains("\\")) {
            return PathValidationResult.Invalid("Path contains backslash separator: '$path'. Only '/' is allowed.")
        }
        if (path.startsWith("/")) {
            return PathValidationResult.Invalid("Absolute path starting with '/': '$path'.")
        }
        if (path.contains(":")) {
            return PathValidationResult.Invalid("Path contains drive letter or colon: '$path'.")
        }
        if (path.startsWith("//") || path.contains("//")) {
            return PathValidationResult.Invalid("Path contains repeated separators: '$path'.")
        }

        val segments = path.split('/')
        for (segment in segments) {
            if (segment == ".") {
                return PathValidationResult.Invalid("Path contains dot segment '.': '$path'.")
            }
            if (segment == "..") {
                return PathValidationResult.Invalid("Path contains parent directory segment '..': '$path'.")
            }
        }

        if (path !in ALLOWED_TOP_LEVEL_FILES && !path.startsWith("media/")) {
            return PathValidationResult.Invalid("Entry '$path' is outside allowed OPD3 layout.")
        }

        if (path.startsWith("media/") && path.trimEnd('/') == "media") {
            return PathValidationResult.Invalid("Media path must specify a subpath under media/.")
        }

        return PathValidationResult.Valid
    }

    fun isSafeMediaPath(logicalPath: String): Boolean {
        if (logicalPath.isBlank()) return false
        if (logicalPath.contains("\\") || logicalPath.contains(":") || logicalPath.startsWith("/")) return false
        val segments = logicalPath.split('/')
        return segments.none { it == "." || it == ".." }
    }
}
