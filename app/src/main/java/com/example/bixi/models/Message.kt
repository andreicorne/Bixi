package com.example.bixi.models

import java.util.Date

data class Message(
    val id: String,
    val text: String,
    val timestamp: Date,
    val attachments: List<AttachmentItem> = emptyList(),
    val isFromCurrentUser: Boolean = false
)