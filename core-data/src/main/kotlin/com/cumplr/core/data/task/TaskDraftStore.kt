package com.cumplr.core.data.task

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskDraftStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("task_drafts")
    }

    private fun obsKey(taskId: String) = stringPreferencesKey("obs_$taskId")

    suspend fun saveObservations(taskId: String, text: String) {
        dataStore.edit { it[obsKey(taskId)] = text }
    }

    suspend fun loadObservations(taskId: String): String =
        dataStore.data.map { it[obsKey(taskId)] ?: "" }.first()

    suspend fun clearDraft(taskId: String) {
        dataStore.edit { it.remove(obsKey(taskId)) }
    }
}
