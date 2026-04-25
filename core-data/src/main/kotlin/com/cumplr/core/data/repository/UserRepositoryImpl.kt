package com.cumplr.core.data.repository

import android.util.Log
import com.cumplr.core.data.local.dao.UserDao
import com.cumplr.core.data.local.mapper.toDomain
import com.cumplr.core.data.local.mapper.toEntity
import com.cumplr.core.data.remote.SupabaseRestClient
import com.cumplr.core.data.session.TokenAuthGuard
import com.cumplr.core.domain.model.User
import com.cumplr.core.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepo"

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val restClient: SupabaseRestClient,
    private val auth: TokenAuthGuard,
) : UserRepository {

    override fun getWorkersByCompany(companyId: String): Flow<List<User>> =
        userDao.getUsersByCompany(companyId).map { list -> list.map { it.toDomain() } }

    override fun getUser(userId: String): Flow<User?> =
        userDao.getUserById(userId).map { it?.toDomain() }

    override suspend fun refreshCompanyUsers(companyId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dtos = auth.withValidToken { token -> restClient.getCompanyUsers(token, companyId) }
                dtos.forEach { userDao.upsertUser(it.toEntity()) }
                Log.d(TAG, "refreshCompanyUsers OK — ${dtos.size} users")
            }.onFailure { Log.w(TAG, "refreshCompanyUsers failed: ${it.message}") }
        }
}
