package com.example.bixi.models.api

import com.example.bixi.models.CheckItem

data class TaskDetailsResponse(
    val id: String,
    val tenantId: String,
    val creatorId: String,
    val creatorEntityType: String,
    val assigneeId: String,
    val assigneeEntityType: String,
    val title: String,
    val isTemplate: Boolean,
    val description: String,
    val status: String,
    val checklist: List<CheckItem>,
    val startDate: String,
    val endDate: String,
    val linkedEntityId: String?,
    val linkedEntityType: String?,
    val createdAt: String,
    val updatedAt: String,
    val attachments: List<AttachmentResponse>,
)
