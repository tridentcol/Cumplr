package com.cumplr.core.domain.repository

import com.cumplr.core.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<List<AppNotification>>
    fun getUnreadCount(userId: String): Flow<Int>
    suspend fun markAsRead(id: String)
    suspend fun markAllRead(userId: String)
}
