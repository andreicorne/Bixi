package com.example.bixi.models.api

data class TaskListResponse(
    val id: String,
    val assigneeId: String,
    val assigneeEntityType: String,
    val title: String,
    val description: String,
    val status: String,
    val startDate: String,
    val endDate: String,
    val createdAt: String,
    val assigneeName: String
)