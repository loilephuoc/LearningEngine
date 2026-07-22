package vn.loi.learning.desktop.runtime

import java.io.InputStream
import java.util.Properties

data class DesktopBuildMetadata(
    val applicationVersion: String,
    val buildChannel: String,
    val buildRevision: String,
    val buildNumber: String
) {
    init {
        require(applicationVersion.isNotBlank()) { "Application version must not be blank." }
        require(buildChannel.isNotBlank()) { "Build channel must not be blank." }
        require(buildRevision.isNotBlank()) { "Build revision must not be blank." }
        require(buildNumber.isNotBlank()) { "Build number must not be blank." }
    }

    val displayVersion: String
        get() = "$applicationVersion ($buildChannel $buildNumber, $buildRevision)"
}

object DesktopBuildMetadataLoader {
    private const val RESOURCE_NAME = "/learning-engine-build.properties"

    fun load(): DesktopBuildMetadata =
        checkNotNull(
            DesktopBuildMetadataLoader::class.java.getResourceAsStream(RESOURCE_NAME)
        ) {
            "Missing desktop build metadata resource: $RESOURCE_NAME"
        }.use(::load)

    internal fun load(input: InputStream): DesktopBuildMetadata {
        val properties =
            Properties().apply {
                load(input.reader(Charsets.UTF_8))
            }

        return DesktopBuildMetadata(
            applicationVersion = properties.required("application.version"),
            buildChannel = properties.required("build.channel"),
            buildRevision = properties.required("build.revision"),
            buildNumber = properties.required("build.number")
        )
    }

    private fun Properties.required(key: String): String =
        getProperty(key)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: throw IllegalStateException(
                "Missing desktop build metadata property: $key"
            )
}
