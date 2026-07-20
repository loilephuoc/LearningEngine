package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.packaging.model.PackageCatalog

/**
 * Đăng ký một ContentPackage vào PackageCatalog.
 */
class RegisterContentPackageUseCase(
    private val registrationOperation: PackageRegistrationOperation,
    private val transactionRunner: TransactionRunner
) {

    fun execute(
        command: RegisterContentPackageCommand
    ): PackageCatalog =
        transactionRunner.runInTransaction {
            registrationOperation.execute(command)
        }
}
