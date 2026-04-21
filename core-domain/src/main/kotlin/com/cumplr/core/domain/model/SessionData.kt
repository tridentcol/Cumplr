package com.cumplr.core.domain.model

import com.cumplr.core.domain.enums.UserRole

data class SessionData(
    val userId: String,
    val companyId: String,
    val role: UserRole,
    val name: String,
    val accessToken: String = "",
    val refreshToken: String = "",
)
