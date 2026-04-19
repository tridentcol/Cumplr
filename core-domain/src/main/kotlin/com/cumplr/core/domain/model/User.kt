package com.cumplr.core.domain.model

import com.cumplr.core.domain.enums.UserRole

data class User(
    val id: String,
    val companyId: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val position: String?,
    val avatarUrl: String?,
    val active: Boolean
)
