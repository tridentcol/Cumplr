package com.cumplr.app.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val sessionManager: SessionManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val session = sessionManager.getSession().first() ?: return Result.success()
        return try {
            when (session.role) {
                UserRole.CHIEF, UserRole.ADMIN -> taskRepository.refreshCompanyTasks(session.companyId)
                UserRole.WORKER -> taskRepository.refresh(session.userId)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Background sync failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "cumplr_periodic_sync"
        private const val TAG = "SyncWorker"
    }
}
