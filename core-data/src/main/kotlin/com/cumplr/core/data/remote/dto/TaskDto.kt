package com.cumplr.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    @SerialName("company_id") val companyId: String,
    val title: String,
    val description: String? = null,
    @SerialName("assigned_to") val assignedTo: String,
    @SerialName("assigned_by") val assignedBy: String,
    val status: String,
    val priority: String,
    val deadline: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("photo_start_url") val photoStartUrl: String? = null,
    @SerialName("photo_end_url") val photoEndUrl: String? = null,
    val observations: String? = null,
    val feedback: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
