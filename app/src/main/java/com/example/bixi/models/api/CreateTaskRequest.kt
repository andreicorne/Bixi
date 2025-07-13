package com.example.bixi.models.api

data class CreateTaskRequest(
    val title: String,
    val description: String,
    val creatorId: String,
    val checklist: String,
    val attachments: ByteArray?
)