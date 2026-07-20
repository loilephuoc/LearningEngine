package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.TransactionRunner

class UninstallContentPackageUseCase(
    private val uninstallOperation: PackageUninstallOperation,
    private val transactionRunner: TransactionRunner
) {

    fun execute(
        command: UninstallContentPackageCommand
    ) {
        transactionRunner.runInTransaction {
            uninstallOperation.execute(command)
        }
    }
}
