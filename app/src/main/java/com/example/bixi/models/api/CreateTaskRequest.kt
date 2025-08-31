package com.example.bixi.models.api

data class CreateTaskRequest(
    val title: String,
    val description: String?,
    val creatorId: String,
    val assigneeId: String,
    val checklist: String,
    val startDate: String,
    val endDate: String
)