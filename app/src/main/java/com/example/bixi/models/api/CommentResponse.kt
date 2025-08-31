package com.example.bixi.models.api

data class CommentResponse(
    val id: String,
    val authorId: String,
    val authorEntityType: String,
    val authorName: String,
    val message: String,
    val createdAt: String,
    val updatedAt: String,
    val documents: List<AttachmentResponse>,
)
