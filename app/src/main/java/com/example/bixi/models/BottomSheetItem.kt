package com.example.bixi.models

import android.net.Uri
import com.example.bixi.enums.AttachmentBottomSheetItemType
import com.example.bixi.enums.AttachmentType

data class BottomSheetItem(
    val name: String,
    var type: AttachmentBottomSheetItemType?
)