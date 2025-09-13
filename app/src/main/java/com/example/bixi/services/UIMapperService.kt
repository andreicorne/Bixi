package com.example.bixi.services

import android.net.Uri
import android.text.Html
import com.example.bixi.AppSession
import com.example.bixi.enums.AttachmentType
import com.example.bixi.helper.Utils
import com.example.bixi.models.AttachmentHandler
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.MessageItem
import com.example.bixi.models.UITaskList
import com.example.bixi.models.api.AttachmentResponse
import com.example.bixi.models.api.CommentResponse
import com.example.bixi.models.api.TaskListResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object UIMapperService {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    fun mapToUiTask(task: TaskListResponse): UITaskList {
        return UITaskList(
            id = task.id,
            title = task.title,
            description = fromHtmlToPlainText(task.description),
            assigneeName = task.assigneeName ?: "N/A",
            startDate = task.startDate?.let { LocalDateTime.parse(it, formatter) } ?: LocalDateTime.MIN,
            endDate = task.endDate?.let { LocalDateTime.parse(it, formatter) } ?: LocalDateTime.MIN
        )
    }

    fun fromHtmlToPlainText(html: String): String {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }

    fun mapToUiTaskList(tasks: List<TaskListResponse>): List<UITaskList> {
        return tasks.map { mapToUiTask(it) }
    }

    fun mapCommentsFromServer(serverComments: List<CommentResponse>, shouldAddEmptyAttachmentAtTheEnd: Boolean): List<MessageItem> {
        val mappedComments = serverComments.map { serverComment ->

            MessageItem(serverComment.id, serverComment.message,
                Utils.utcIsoStringToCalendar(serverComment.updatedAt).time,
                mapAttachmentsFromServer(serverComment.documents, shouldAddEmptyAttachmentAtTheEnd),
                if(AppSession.user!!.user.id.equals(serverComment.authorId)) true else false)
        }.toMutableList()

        return mappedComments
    }

    fun mapAttachmentsFromServer(serverAttachments: List<AttachmentResponse>, shouldAddEmptyAtTheEnd: Boolean): List<AttachmentItem> {
        val mappedAttachments = serverAttachments.map { serverAttachment ->
            val fullUrl = "https://api.bixi.be/uploads/${serverAttachment.fileUrl}"
            val uri = Uri.parse(fullUrl)

            val attachmentType = when {
                serverAttachment.type.startsWith("image/") -> AttachmentType.IMAGE
                serverAttachment.type.contains("pdf") -> AttachmentType.DOCUMENT
                serverAttachment.type.contains("word") -> AttachmentType.DOCUMENT
                else -> AttachmentType.DOCUMENT
            }

            AttachmentItem(
                id = serverAttachment.id,
                uri = uri,
                type = attachmentType,
                serverData = serverAttachment,
                isFromStorage = false
            )
        }.toMutableList()

        if(shouldAddEmptyAtTheEnd){
            mappedAttachments.add(AttachmentItem())
        }
        return mappedAttachments
    }

    fun toAttachmentItem(attachment: AttachmentHandler): AttachmentItem {
        val type = when (attachment.type) {
            AttachmentType.IMAGE -> AttachmentType.IMAGE
            AttachmentType.DOCUMENT -> AttachmentType.DOCUMENT
            else -> AttachmentType.UNKNOWN
        }

        return AttachmentItem(
            id = UUID.randomUUID().toString(),
            uri = attachment.uri,
            type = type,
            serverData = null
        )
    }
}