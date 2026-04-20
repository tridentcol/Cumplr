package com.cumplr.core.data.di

import android.content.Context
import androidx.room.Room
import com.cumplr.core.data.local.CumplrDatabase
import com.cumplr.core.data.repository.AuthRepositoryImpl
import com.cumplr.core.domain.repository.AuthRepository
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

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): CumplrDatabase =
            Room.databaseBuilder(context, CumplrDatabase::class.java, "cumplr.db").build()

        @Provides
        @Singleton
        fun provideUserDao(db: CumplrDatabase) = db.userDao()

        @Provides
        @Singleton
        fun provideTaskDao(db: CumplrDatabase) = db.taskDao()

        @Provides
        @Singleton
        fun provideNotificationDao(db: CumplrDatabase) = db.notificationDao()
    }
}
