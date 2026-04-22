package com.cumplr.core.data.repository

import com.cumplr.core.data.local.dao.NotificationDao
import com.cumplr.core.data.local.mapper.toDomain
import com.cumplr.core.domain.model.AppNotification
import com.cumplr.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
) : NotificationRepository {

    override fun getNotifications(userId: String): Flow<List<AppNotification>> =
        notificationDao.getByUser(userId).map { list -> list.map { it.toDomain() } }

    override fun getUnreadCount(userId: String): Flow<Int> =
        notificationDao.getUnreadCount(userId)

    override suspend fun markAsRead(id: String) =
        notificationDao.markAsRead(id)

    override suspend fun markAllRead(userId: String) =
        notificationDao.markAllRead(userId)
}
