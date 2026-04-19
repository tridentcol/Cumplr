package com.cumplr.core.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.domain.model.SessionData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "cumplr_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_USER_ID    = stringPreferencesKey("user_id")
        private val KEY_COMPANY_ID = stringPreferencesKey("company_id")
        private val KEY_ROLE       = stringPreferencesKey("role")
        private val KEY_NAME       = stringPreferencesKey("name")
    }

    suspend fun saveSession(data: SessionData) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_USER_ID]    = data.userId
            prefs[KEY_COMPANY_ID] = data.companyId
            prefs[KEY_ROLE]       = data.role.name
            prefs[KEY_NAME]       = data.name
        }
    }

    fun getSession(): Flow<SessionData?> = context.sessionDataStore.data.map { prefs ->
        val userId    = prefs[KEY_USER_ID]    ?: return@map null
        val companyId = prefs[KEY_COMPANY_ID] ?: return@map null
        val roleName  = prefs[KEY_ROLE]       ?: return@map null
        val name      = prefs[KEY_NAME]       ?: return@map null
        val role      = runCatching { UserRole.valueOf(roleName) }.getOrNull() ?: return@map null
        SessionData(userId = userId, companyId = companyId, role = role, name = name)
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }
}
