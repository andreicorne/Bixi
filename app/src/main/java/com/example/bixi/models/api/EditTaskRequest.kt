package com.example.bixi.models.api

data class EditTaskRequest(
    val id: String,
    val title: String,
    val description: String,
//    val assigneeId: String,
    val checklist: String,
//    val startDate: String,
//    val endDate: String
)