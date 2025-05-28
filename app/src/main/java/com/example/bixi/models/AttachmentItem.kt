package com.example.bixi.models

import android.net.Uri

data class AttachmentItem(
    val name: String,
    var uri: Uri? = null
)