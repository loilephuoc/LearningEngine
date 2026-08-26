package vn.loi.learning.infrastructure.persistence.sqlite

import app.cash.sqldelight.db.SqlDriver
import java.io.File
import java.nio.file.Path

fun interface SqliteDriverFactory {
    fun createDriver(databaseFile: File): SqlDriver
}

data class SqliteDatabaseHandle(
    val database: LearningEngineDatabase,
    val driver: SqlDriver
) : AutoCloseable {
    override fun close() {
        driver.close()
    }
}

object SqliteDatabaseFactory {

    @Volatile
    var defaultDriverFactory: SqliteDriverFactory? = null

    fun createHandleFromFile(
        databaseFile: File,
        driverFactory: SqliteDriverFactory? = null
    ): SqliteDatabaseHandle {
        val factory = driverFactory ?: defaultDriverFactory ?: resolveDefaultDriverFactory()
        databaseFile.parentFile?.mkdirs()
        val driver = factory.createDriver(databaseFile)
        val database = LearningEngineDatabase(driver)
        return SqliteDatabaseHandle(database, driver)
    }

    fun createFromFile(
        databaseFile: File,
        driverFactory: SqliteDriverFactory? = null
    ): LearningEngineDatabase =
        createHandleFromFile(databaseFile, driverFactory).database

    fun createFromFile(
        databasePath: Path,
        driverFactory: SqliteDriverFactory? = null
    ): LearningEngineDatabase =
        createFromFile(databasePath.toFile(), driverFactory)

    fun createFromDriver(driver: SqlDriver): LearningEngineDatabase {
        return LearningEngineDatabase(driver)
    }

    fun createInMemory(): LearningEngineDatabase {
        val driver = JvmSqliteDriverFactory.createInMemoryDriver()
        return LearningEngineDatabase(driver)
    }

    private fun resolveDefaultDriverFactory(): SqliteDriverFactory {
        return try {
            val clazz = Class.forName("vn.loi.learning.infrastructure.persistence.sqlite.JvmSqliteDriverFactory")
            val field = clazz.getField("INSTANCE")
            field.get(null) as SqliteDriverFactory
        } catch (ex: Throwable) {
            throw IllegalStateException(
                "No SQLite driver factory configured and JVM JDBC driver is unavailable. " +
                "Please configure SqliteDatabaseFactory.defaultDriverFactory (e.g. AndroidSqliteDriverFactory on Android).",
                ex
            )
        }
    }
}

object JvmSqliteDriverFactory : SqliteDriverFactory {
    override fun createDriver(databaseFile: File): SqlDriver {
        databaseFile.parentFile?.mkdirs()
        val exists = databaseFile.exists() && databaseFile.length() > 0L
        val driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(
            "jdbc:sqlite:${databaseFile.absolutePath.replace('\\', '/')}"
        )
        if (!exists) {
            LearningEngineDatabase.Schema.create(driver)
        }
        driver.execute(null, "PRAGMA journal_mode = WAL;", 0)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        driver.execute(null, "PRAGMA busy_timeout = 5000;", 0)
        driver.execute(null, "PRAGMA synchronous = NORMAL;", 0)
        return driver
    }

    fun createInMemoryDriver(): SqlDriver {
        val driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(
            app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY
        )
        LearningEngineDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        return driver
    }
}
