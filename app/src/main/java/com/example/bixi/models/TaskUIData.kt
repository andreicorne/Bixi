package com.example.bixi.models

import java.util.Calendar

data class TaskUIData(
    var title: String,
    var description: String,
    var startDate: Calendar,
    var endDate: Calendar,
    var attachments: List<AttachmentItem>,
    var responsible: Int?,
    var checks: List<CheckItem>
)