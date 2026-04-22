package com.cumplr.core.data.local.mapper

import com.cumplr.core.data.local.entity.NotificationEntity
import com.cumplr.core.domain.model.AppNotification

fun NotificationEntity.toDomain() = AppNotification(
    id        = id,
    userId    = userId,
    companyId = companyId,
    type      = type,
    taskId    = taskId,
    title     = title,
    body      = body,
    read      = read,
    createdAt = createdAt,
)
