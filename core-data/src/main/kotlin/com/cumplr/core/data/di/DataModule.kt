package com.cumplr.core.data.di

import android.content.Context
import androidx.room.Room
import com.cumplr.core.data.local.CumplrDatabase
import com.cumplr.core.data.local.MIGRATION_1_2
import com.cumplr.core.data.local.MIGRATION_2_3
import com.cumplr.core.data.local.MIGRATION_3_4
import com.cumplr.core.data.repository.AuthRepositoryImpl
import com.cumplr.core.data.repository.NoteRepositoryImpl
import com.cumplr.core.data.repository.NotificationRepositoryImpl
import com.cumplr.core.data.repository.StorageRepositoryImpl
import com.cumplr.core.data.repository.TaskRepositoryImpl
import com.cumplr.core.data.repository.UserRepositoryImpl
import com.cumplr.core.domain.repository.AuthRepository
import com.cumplr.core.domain.repository.NoteRepository
import com.cumplr.core.domain.repository.NotificationRepository
import com.cumplr.core.domain.repository.StorageRepository
import com.cumplr.core.domain.repository.TaskRepository
import com.cumplr.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    companion object {

        @Provides @Singleton
        fun provideDatabase(@ApplicationContext context: Context): CumplrDatabase =
            Room.databaseBuilder(context, CumplrDatabase::class.java, "cumplr.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        @Provides @Singleton
        fun provideUserDao(db: CumplrDatabase) = db.userDao()

        @Provides @Singleton
        fun provideTaskDao(db: CumplrDatabase) = db.taskDao()

        @Provides @Singleton
        fun provideNotificationDao(db: CumplrDatabase) = db.notificationDao()

        @Provides @Singleton
        fun provideNoteDao(db: CumplrDatabase) = db.noteDao()
    }
}
