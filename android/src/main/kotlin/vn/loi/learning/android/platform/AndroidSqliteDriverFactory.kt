package vn.loi.learning.android.platform

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.File
import vn.loi.learning.infrastructure.persistence.sqlite.LearningEngineDatabase
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteDatabaseHandle
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteDriverFactory

class AndroidSqliteDriverFactory(
    private val context: Context
) : SqliteDriverFactory {

    override fun createDriver(databaseFile: File): SqlDriver {
        databaseFile.parentFile?.mkdirs()
        return AndroidSqliteDriver(
            schema = LearningEngineDatabase.Schema,
            context = context,
            name = databaseFile.absolutePath,
            callback = object : AndroidSqliteDriver.Callback(LearningEngineDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.enableWriteAheadLogging()
                    db.execSQL("PRAGMA synchronous = NORMAL;")
                    db.execSQL("PRAGMA busy_timeout = 5000;")
                }
            }
        )
    }

    fun createHandle(databaseFile: File): SqliteDatabaseHandle {
        val driver = createDriver(databaseFile)
        val database = LearningEngineDatabase(driver)
        return SqliteDatabaseHandle(database, driver)
    }
}
