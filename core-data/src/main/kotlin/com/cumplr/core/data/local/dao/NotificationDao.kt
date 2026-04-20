package com.cumplr.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cumplr.core.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Upsert
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND read = 0")
    fun getUnreadCount(userId: String): Flow<Int>

    @Query("UPDATE notifications SET read = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUser(userId: String): Flow<List<NotificationEntity>>
}
