package vn.loi.learning.android.platform

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.application.contentpackaging.*
import vn.loi.learning.application.contentpackaging.export.*
import vn.loi.learning.domain.library.model.InstalledPackageId

sealed interface AndroidPackageOperationResult {
    data class Success(val message: String) : AndroidPackageOperationResult
    data class Verification(val valid: Boolean, val errors: List<String>, val warnings: List<String>) : AndroidPackageOperationResult
    data class Failed(val message: String) : AndroidPackageOperationResult
}

/** SAF stream adapter only; package semantics stay in canonical Application use cases. */
class AndroidPackageOperations(private val graph: AndroidApplicationGraph) {
    suspend fun export(id: InstalledPackageId, open: () -> OutputStream?): AndroidPackageOperationResult = withContext(Dispatchers.IO) {
        val target=Files.createTempFile(graph.directories.rootDirectory,".export-",".opd3")
        try {
            when(val result=requireNotNull(graph.engine.exportContentPackage).execute(ExportContentPackageCommand(id,target))) {
                is ExportContentPackageResult.Success -> { open()?.use { out->Files.newInputStream(result.outputPath).use { it.copyTo(out) } } ?: return@withContext AndroidPackageOperationResult.Failed("Export destination unavailable."); AndroidPackageOperationResult.Success("Package exported.") }
                is ExportContentPackageResult.Failure -> AndroidPackageOperationResult.Failed(result.message)
            }
        } catch(_:SecurityException){ AndroidPackageOperationResult.Failed("Document permission was lost.") }
        finally { Files.deleteIfExists(target) }
    }
    suspend fun verify(open: () -> InputStream?): AndroidPackageOperationResult = withContext(Dispatchers.IO) {
        val source=Files.createTempFile(graph.directories.rootDirectory,".verify-",".opd3")
        try { open()?.use { Files.copy(it,source,java.nio.file.StandardCopyOption.REPLACE_EXISTING) } ?: return@withContext AndroidPackageOperationResult.Failed("Package unavailable.")
            val report=requireNotNull(graph.engine.packageVerifier).verify(source); AndroidPackageOperationResult.Verification(report.isValid,report.errors,report.warnings)
        } catch(e:Exception){AndroidPackageOperationResult.Failed(e.message ?: "Verification failed.")} finally {Files.deleteIfExists(source)}
    }
    suspend fun uninstall(id: InstalledPackageId): AndroidPackageOperationResult = withContext(Dispatchers.IO) {
        runCatching { val pkg=requireNotNull(graph.engine.libraryQuery?.getPackageSummary(id)); val catalog=requireNotNull(graph.engine.packageCatalog?.findAll()?.firstOrNull{it.contains(pkg.packageId)}); requireNotNull(graph.engine.uninstallContentPackage).execute(UninstallContentPackageCommand(catalog.id,pkg.packageId)); AndroidPackageOperationResult.Success("Package uninstalled.") }
            .getOrElse { AndroidPackageOperationResult.Failed(it.message ?: "Uninstall was denied.") }
    }
    suspend fun upgrade(command: UpgradeContentPackageCommand): AndroidPackageOperationResult = withContext(Dispatchers.IO) {
        runCatching { requireNotNull(graph.engine.upgradeContentPackage).execute(command); AndroidPackageOperationResult.Success("Package upgraded.") }
            .getOrElse { AndroidPackageOperationResult.Failed(it.message ?: "Upgrade failed.") }
    }
}
