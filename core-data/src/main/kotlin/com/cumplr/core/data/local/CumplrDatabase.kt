package com.cumplr.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cumplr.core.data.local.dao.NoteDao
import com.cumplr.core.data.local.dao.NotificationDao
import com.cumplr.core.data.local.dao.TaskDao
import com.cumplr.core.data.local.dao.UserDao
import com.cumplr.core.data.local.entity.CompanyEntity
import com.cumplr.core.data.local.entity.NoteEntity
import com.cumplr.core.data.local.entity.NotificationEntity
import com.cumplr.core.data.local.entity.TaskEntity
import com.cumplr.core.data.local.entity.TaskEventEntity
import com.cumplr.core.data.local.entity.UserEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN location TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN pending_sync_op TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_company_id ON tasks (company_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_assigned_to ON tasks (assigned_to)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_sync_pending ON tasks (sync_pending)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS task_notes (
                id           TEXT PRIMARY KEY NOT NULL,
                task_id      TEXT NOT NULL,
                author_id    TEXT NOT NULL,
                author_name  TEXT NOT NULL,
                text         TEXT NOT NULL,
                created_at   TEXT NOT NULL,
                sync_pending INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_task_notes_task_id ON task_notes (task_id)")
    }
}

@Database(
    entities = [
        CompanyEntity::class,
        UserEntity::class,
        TaskEntity::class,
        TaskEventEntity::class,
        NotificationEntity::class,
        NoteEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class CumplrDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao
    abstract fun noteDao(): NoteDao
}
