package com.cumplr.core.domain.repository

import com.cumplr.core.domain.model.SessionData
import com.cumplr.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    fun getCurrentSession(): Flow<SessionData?>
}
