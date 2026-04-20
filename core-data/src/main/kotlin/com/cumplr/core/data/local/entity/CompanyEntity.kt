package com.cumplr.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "logo_url") val logoUrl: String?,
    val plan: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
)
