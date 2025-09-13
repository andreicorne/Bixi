package com.example.bixi.models

import com.example.bixi.interfaces.IMessage
import java.util.Date

data class MessageItem(
    val id: String,
    val text: String,
    val timestamp: Date,
    val attachments: List<AttachmentItem> = emptyList(),
    val isFromCurrentUser: Boolean = false
) : IMessage