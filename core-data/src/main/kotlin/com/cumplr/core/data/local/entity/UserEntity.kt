package com.cumplr.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "company_id") val companyId: String,
    val name: String,
    val email: String,
    val role: String,
    val position: String?,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?,
    val active: Boolean,
    @ColumnInfo(name = "fcm_token") val fcmToken: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
)
