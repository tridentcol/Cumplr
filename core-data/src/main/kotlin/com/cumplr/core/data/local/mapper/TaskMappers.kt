package com.cumplr.core.data.local.mapper

import com.cumplr.core.data.local.entity.TaskEntity
import com.cumplr.core.data.remote.dto.TaskDto
import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task

fun TaskEntity.toDomain() = Task(
    id              = id,
    companyId       = companyId,
    title           = title,
    description     = description,
    assignedTo      = assignedTo,
    assignedBy      = assignedBy,
    status          = TaskStatus.valueOf(status),
    priority        = TaskPriority.valueOf(priority),
    deadline        = deadline,
    startTime       = startTime,
    endTime         = endTime,
    photoStartUrl   = photoStartUrl,
    photoEndUrl     = photoEndUrl,
    observations    = observations,
    feedback        = feedback,
    rejectionReason = rejectionReason,
    createdAt       = createdAt,
    updatedAt       = updatedAt,
)

fun Task.toEntity(syncPending: Boolean = false) = TaskEntity(
    id              = id,
    companyId       = companyId,
    title           = title,
    description     = description,
    assignedTo      = assignedTo,
    assignedBy      = assignedBy,
    status          = status.name,
    priority        = priority.name,
    deadline        = deadline,
    startTime       = startTime,
    endTime         = endTime,
    photoStartUrl   = photoStartUrl,
    photoEndUrl     = photoEndUrl,
    observations    = observations,
    feedback        = feedback,
    rejectionReason = rejectionReason,
    createdAt       = createdAt,
    updatedAt       = updatedAt,
    syncPending     = syncPending,
)

fun TaskDto.toEntity() = TaskEntity(
    id              = id,
    companyId       = companyId,
    title           = title,
    description     = description,
    assignedTo      = assignedTo,
    assignedBy      = assignedBy,
    status          = status,
    priority        = priority,
    deadline        = deadline,
    startTime       = startTime,
    endTime         = endTime,
    photoStartUrl   = photoStartUrl,
    photoEndUrl     = photoEndUrl,
    observations    = observations,
    feedback        = feedback,
    rejectionReason = rejectionReason,
    createdAt       = createdAt,
    updatedAt       = updatedAt,
    syncPending     = false,
)
