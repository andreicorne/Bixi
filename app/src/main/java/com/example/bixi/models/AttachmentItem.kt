package com.example.bixi.models

import android.net.Uri
import com.example.bixi.enums.AttachmentType
import com.example.bixi.models.api.AttachmentResponse
import java.util.UUID

data class AttachmentItem(
    var uri: Uri? = null,
    var type: AttachmentType = AttachmentType.UNKNOWN,
    var serverData: AttachmentResponse? = null,
    var isFromStorage: Boolean = true,
    var id: String = UUID.randomUUID().toString()
)