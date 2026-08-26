package vn.loi.learning.infrastructure.persistence.sqlite

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.nio.file.Path

data class SqliteDatabaseHandle(
    val database: LearningEngineDatabase,
    val driver: SqlDriver
) : AutoCloseable {
    override fun close() {
        driver.close()
    }
}

object SqliteDatabaseFactory {

    fun createInMemory(): LearningEngineDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LearningEngineDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        return LearningEngineDatabase(driver)
    }

    fun createHandleFromFile(databaseFile: File): SqliteDatabaseHandle {
        databaseFile.parentFile?.mkdirs()
        val exists = databaseFile.exists() && databaseFile.length() > 0L
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath.replace('\\', '/')}")
        if (!exists) {
            LearningEngineDatabase.Schema.create(driver)
        }
        driver.execute(null, "PRAGMA journal_mode = WAL;", 0)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        driver.execute(null, "PRAGMA busy_timeout = 5000;", 0)
        driver.execute(null, "PRAGMA synchronous = NORMAL;", 0)
        val database = LearningEngineDatabase(driver)
        return SqliteDatabaseHandle(database, driver)
    }

    fun createFromFile(databaseFile: File): LearningEngineDatabase =
        createHandleFromFile(databaseFile).database

    fun createFromFile(databasePath: Path): LearningEngineDatabase =
        createFromFile(databasePath.toFile())

    fun createFromDriver(driver: SqlDriver): LearningEngineDatabase {
        return LearningEngineDatabase(driver)
    }
}
