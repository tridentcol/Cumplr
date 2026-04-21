package com.cumplr.core.domain.repository

import com.cumplr.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getWorkersByCompany(companyId: String): Flow<List<User>>
    fun getUser(userId: String): Flow<User?>
    suspend fun refreshCompanyUsers(companyId: String): Result<Unit>
}
