package com.cumplr.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cumplr.core.data.local.dao.NotificationDao
import com.cumplr.core.data.local.dao.TaskDao
import com.cumplr.core.data.local.dao.UserDao
import com.cumplr.core.data.local.entity.CompanyEntity
import com.cumplr.core.data.local.entity.NotificationEntity
import com.cumplr.core.data.local.entity.TaskEntity
import com.cumplr.core.data.local.entity.TaskEventEntity
import com.cumplr.core.data.local.entity.UserEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN location TEXT")
    }
}

@Database(
    entities = [
        CompanyEntity::class,
        UserEntity::class,
        TaskEntity::class,
        TaskEventEntity::class,
        NotificationEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class CumplrDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao
}
