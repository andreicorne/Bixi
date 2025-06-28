package com.example.bixi.models

import android.net.Uri
import com.example.bixi.enums.AttachmentType
import java.util.UUID

data class AttachmentItem(
    var uri: Uri? = null,
    var type: AttachmentType = AttachmentType.UNKNOWN
){
    val id: UUID = UUID.randomUUID()
}