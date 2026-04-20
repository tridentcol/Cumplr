package com.cumplr.core.data.repository

import android.util.Log
import com.cumplr.core.data.local.dao.UserDao
import com.cumplr.core.data.local.mapper.toDomain
import com.cumplr.core.data.local.mapper.toEntity
import com.cumplr.core.data.remote.SupabaseRestClient
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.model.User
import com.cumplr.core.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepo"

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val restClient: SupabaseRestClient,
    private val sessionManager: SessionManager,
) : UserRepository {

    override fun getWorkersByCompany(companyId: String): Flow<List<User>> =
        userDao.getUsersByCompany(companyId).map { list -> list.map { it.toDomain() } }

    override fun getUser(userId: String): Flow<User?> =
        userDao.getUserById(userId).map { it?.toDomain() }

    override suspend fun refreshCompanyUsers(companyId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val session = sessionManager.getSession().first()
                    ?: return@withContext Result.failure(Exception("Sin sesión activa"))
                val dtos = restClient.getCompanyUsers(session.accessToken, companyId)
                dtos.forEach { userDao.upsertUser(it.toEntity()) }
                Log.d(TAG, "refreshCompanyUsers OK — ${dtos.size} users")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "refreshCompanyUsers failed: ${e.message}")
                Result.failure(e)
            }
        }
}
