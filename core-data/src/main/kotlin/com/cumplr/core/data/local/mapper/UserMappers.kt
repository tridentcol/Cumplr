package com.cumplr.core.data.local.mapper

import com.cumplr.core.data.local.entity.UserEntity
import com.cumplr.core.data.remote.dto.UserDto
import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.domain.model.User

fun UserEntity.toDomain() = User(
    id        = id,
    companyId = companyId,
    name      = name,
    email     = email,
    role      = UserRole.valueOf(role),
    position  = position,
    avatarUrl = avatarUrl,
    active    = active,
)

fun UserDto.toEntity() = UserEntity(
    id        = id,
    companyId = companyId,
    name      = name,
    email     = email,
    role      = role,
    position  = position,
    avatarUrl = avatarUrl,
    active    = active,
    fcmToken  = fcmToken,
    createdAt = createdAt,
)
