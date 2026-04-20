package com.cumplr.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cumplr.core.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE company_id = :companyId")
    fun getUsersByCompany(companyId: String): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users WHERE company_id = :companyId AND active = 1")
    fun getActiveWorkersCount(companyId: String): Flow<Int>
}
