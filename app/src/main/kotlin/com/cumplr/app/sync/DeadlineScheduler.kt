package com.cumplr.app.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadlineScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(taskId: String, taskTitle: String, deadlineIso: String) {
        val deadline = runCatching { Instant.parse(deadlineIso) }.getOrNull() ?: return
        val now = Instant.now()

        listOf(24 to TimeUnit.HOURS.toMillis(24), 1 to TimeUnit.HOURS.toMillis(1))
            .forEach { (hours, offsetMillis) ->
                val fireAt = deadline.minusMillis(offsetMillis)
                val delayMs = Duration.between(now, fireAt).toMillis()
                if (delayMs <= 0) return@forEach

                val request = OneTimeWorkRequestBuilder<DeadlineReminderWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(
                        DeadlineReminderWorker.KEY_TASK_ID    to taskId,
                        DeadlineReminderWorker.KEY_TASK_TITLE to taskTitle,
                        DeadlineReminderWorker.KEY_HOURS_LEFT to hours,
                    ))
                    .addTag(taskId)
                    .build()

                workManager.enqueueUniqueWork(
                    "deadline_reminder_${taskId}_${hours}h",
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            }
    }

    fun cancel(taskId: String) = workManager.cancelAllWorkByTag(taskId)
}
