package com.example.bixi.models

import android.net.Uri
import com.example.bixi.enums.AttachmentType

data class AttachmentHandler(
    val id: String,
    val uri: Uri,
    val type: AttachmentType,
    val name: String,
    val size: Long? = null
)