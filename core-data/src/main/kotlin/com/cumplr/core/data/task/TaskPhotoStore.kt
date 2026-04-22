package com.cumplr.core.data.task

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File
        get() = File(context.filesDir, "task_execution").also { it.mkdirs() }

    fun save(taskId: String, type: String, bytes: ByteArray) {
        File(dir, "${taskId}_$type.jpg").writeBytes(bytes)
    }

    fun load(taskId: String, type: String): ByteArray? =
        File(dir, "${taskId}_$type.jpg").takeIf { it.exists() }?.readBytes()

    fun delete(taskId: String, type: String) {
        File(dir, "${taskId}_$type.jpg").delete()
    }

    fun clear(taskId: String) {
        delete(taskId, "start")
        delete(taskId, "end")
    }
}
