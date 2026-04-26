package com.cumplr.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cumplr.core.data.local.dao.NotificationDao
import com.cumplr.core.data.local.dao.TaskDao
import com.cumplr.core.data.local.entity.NotificationEntity
import com.cumplr.core.data.session.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID

@HiltWorker
class DeadlineReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationDao: NotificationDao,
    private val taskDao: TaskDao,
    private val sessionManager: SessionManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId    = inputData.getString(KEY_TASK_ID)    ?: return Result.failure()
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: return Result.failure()
        val hoursLeft = inputData.getInt(KEY_HOURS_LEFT, 24)
        val session   = sessionManager.getSession().first() ?: return Result.failure()

        val task = taskDao.getTaskById(taskId).first()
        if (task == null ||
            task.status == "APPROVED" ||
            task.status == "SUBMITTED" ||
            task.status == "UNDER_REVIEW") return Result.success()

        val now = Instant.now().toString()
        notificationDao.upsertNotifications(listOf(
            NotificationEntity(
                id        = UUID.randomUUID().toString(),
                userId    = session.userId,
                companyId = session.companyId,
                type      = "DEADLINE_REMINDER",
                taskId    = taskId,
                title     = if (hoursLeft == 1) "Tarea vence en 1 hora" else "Tarea vence en 24 horas",
                body      = "\"$taskTitle\" tiene un deadline próximo.",
                read      = false,
                createdAt = now,
            )
        ))
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID    = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_HOURS_LEFT = "hours_left"
    }
}
