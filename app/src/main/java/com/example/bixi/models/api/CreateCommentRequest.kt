package com.example.bixi.models.api

data class CreateCommentRequest(
    val authorId: String,
    val authorName: String?,
    val authorEntityType: String,
    val message: String
)