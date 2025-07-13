package com.example.bixi.models

import java.util.Date

data class Message(
    val id: String,
    val text: String,
    val timestamp: Date,
    val attachments: List<Attachment> = emptyList(),
    val isFromCurrentUser: Boolean = false
)

data class Attachment(
    val id: String,
    val url: String,
    val type: AttachmentType,
    val name: String,
    val size: Long? = null
)

enum class AttachmentType {
    IMAGE,
    VIDEO,
    DOCUMENT,
    AUDIO,
    OTHER
}

data class MessagePage(
    val messages: List<Message>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean
)