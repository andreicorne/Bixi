package com.example.bixi.helper

import android.net.Uri
import com.example.bixi.enums.AttachmentType
import com.example.bixi.models.Attachment
import com.example.bixi.models.AttachmentItem
import java.util.*

object AttachmentConverter {

    /**
     * Convertește Attachment în AttachmentItem (pentru Task Details)
     */
    fun toAttachmentItem(attachment: Attachment): AttachmentItem {
        val uri = try {
            Uri.parse(attachment.url)
        } catch (e: Exception) {
            null
        }

        val type = when (attachment.type) {
            com.example.bixi.models.AttachmentType.IMAGE -> AttachmentType.IMAGE
            com.example.bixi.models.AttachmentType.DOCUMENT -> AttachmentType.DOCUMENT
            else -> AttachmentType.UNKNOWN
        }

        return AttachmentItem(
            uri = uri,
            type = type,
            serverData = null
        )
    }

    /**
     * Convertește AttachmentItem în Attachment (pentru Chat)
     */
    fun toAttachment(attachmentItem: AttachmentItem): Attachment? {
        val uri = attachmentItem.uri ?: return null

        val type = when (attachmentItem.type) {
            AttachmentType.IMAGE -> com.example.bixi.models.AttachmentType.IMAGE
            AttachmentType.DOCUMENT -> com.example.bixi.models.AttachmentType.DOCUMENT
            else -> com.example.bixi.models.AttachmentType.OTHER
        }

        // Extrage numele fișierului din serverData sau generează unul
        val fileName = attachmentItem.serverData?.name ?: "attachment_${System.currentTimeMillis()}"

        return Attachment(
            id = UUID.randomUUID().toString(),
            url = uri.toString(),
            type = type,
            name = fileName,
            size = null
        )
    }
}