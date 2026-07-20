package vn.loi.learning.infrastructure.persistence.json

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Ghi toàn bộ nội dung JSON qua một file tạm nằm cùng thư mục
 * với file đích.
 *
 * Quy trình:
 * - tạo thư mục cha nếu cần;
 * - tạo file tạm duy nhất trong cùng filesystem;
 * - ghi đầy đủ nội dung UTF-8 vào file tạm;
 * - force dữ liệu và metadata của file tạm xuống storage;
 * - ưu tiên ATOMIC_MOVE để thay thế snapshot cũ;
 * - fallback sang move thay thế nếu filesystem không hỗ trợ atomic move;
 * - sync thư mục cha trên platform hỗ trợ;
 * - luôn dọn file tạm còn sót.
 */
internal object JsonFileWriter {

    fun write(
        filePath: Path,
        content: String,
        directorySynchronizer: JsonDirectorySynchronizer =
            PlatformJsonDirectorySynchronizer
    ) {
        val normalizedFilePath =
            filePath
                .toAbsolutePath()
                .normalize()

        val parentDirectory =
            normalizedFilePath.parent
                ?: throw IllegalArgumentException(
                    "JSON file path must have a parent directory: $filePath"
                )

        Files.createDirectories(
            parentDirectory
        )

        val temporaryFile =
            Files.createTempFile(
                parentDirectory,
                "${normalizedFilePath.fileName}.",
                ".tmp"
            )

        try {
            writeDurably(
                filePath =
                    temporaryFile,
                content =
                    content
            )

            replaceFile(
                temporaryFile =
                    temporaryFile,
                filePath =
                    normalizedFilePath
            )

            directorySynchronizer.sync(
                directory =
                    parentDirectory
            )
        } finally {
            Files.deleteIfExists(
                temporaryFile
            )
        }
    }

    private fun writeDurably(
        filePath: Path,
        content: String
    ) {
        val bytes =
            content.toByteArray(
                StandardCharsets.UTF_8
            )

        FileChannel.open(
            filePath,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
        ).use { channel ->
            val buffer =
                ByteBuffer.wrap(
                    bytes
                )

            while (
                buffer.hasRemaining()
            ) {
                channel.write(
                    buffer
                )
            }

            channel.force(
                true
            )
        }
    }

    private fun replaceFile(
        temporaryFile: Path,
        filePath: Path
    ) {
        try {
            Files.move(
                temporaryFile,
                filePath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (
            atomicMoveFailure: AtomicMoveNotSupportedException
        ) {
            fallbackMove(
                temporaryFile =
                    temporaryFile,
                filePath =
                    filePath,
                atomicMoveFailure =
                    atomicMoveFailure
            )
        } catch (
            atomicMoveFailure: IOException
        ) {
            fallbackMove(
                temporaryFile =
                    temporaryFile,
                filePath =
                    filePath,
                atomicMoveFailure =
                    atomicMoveFailure
            )
        }
    }

    private fun fallbackMove(
        temporaryFile: Path,
        filePath: Path,
        atomicMoveFailure: IOException
    ) {
        try {
            Files.move(
                temporaryFile,
                filePath,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (
            fallbackFailure: IOException
        ) {
            fallbackFailure.addSuppressed(
                atomicMoveFailure
            )

            throw fallbackFailure
        }
    }
}

internal fun interface JsonDirectorySynchronizer {

    fun sync(
        directory: Path
    )
}

private object PlatformJsonDirectorySynchronizer :
    JsonDirectorySynchronizer {

    override fun sync(
        directory: Path
    ) {
        if (
            isWindows()
        ) {
            return
        }

        FileChannel.open(
            directory,
            StandardOpenOption.READ
        ).use { channel ->
            channel.force(
                true
            )
        }
    }

    private fun isWindows(): Boolean =
        System.getProperty(
            "os.name",
            ""
        ).startsWith(
            prefix =
                "Windows",
            ignoreCase =
                true
        )
}