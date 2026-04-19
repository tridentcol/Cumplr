package com.cumplr.core.domain.model

data class Company(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val plan: String,
    val createdAt: String
)
