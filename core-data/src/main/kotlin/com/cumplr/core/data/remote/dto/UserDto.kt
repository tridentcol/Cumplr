package com.cumplr.core.data.remote.dto

import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    @SerialName("company_id") val companyId: String,
    val name: String,
    val email: String,
    val role: String,
    val position: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val active: Boolean = true,
)

fun UserDto.toDomain() = User(
    id = id,
    companyId = companyId,
    name = name,
    email = email,
    role = UserRole.valueOf(role),
    position = position,
    avatarUrl = avatarUrl,
    active = active,
)
