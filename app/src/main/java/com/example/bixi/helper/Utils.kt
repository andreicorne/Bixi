package com.example.bixi.helper

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.bixi.models.AttachmentItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Utils {

    private val isoFormat: SimpleDateFormat
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf
        }

    fun calendarToUtcIsoString(calendar: Calendar): String {
        return isoFormat.format(calendar.time)
    }

    fun dateToUtcIsoString(date: Date): String {
        return isoFormat.format(date)
    }

    fun utcIsoStringToCalendar(isoString: String): Calendar {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = isoFormat.parse(isoString) ?: Date()
        return calendar
    }

    fun prepareAttachments(
        context: Context,
        attachments: List<AttachmentItem>
    ): List<MultipartBody.Part> {
        return attachments.mapNotNull { attachment ->
            val bytes = attachment.uri?.let { uriToByteArray(context, it) } ?: return@mapNotNull null
            val requestBody = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(
                name = "attachments",
                filename = getFileName(context, attachment.uri!!),
                body = requestBody
            )
        }
    }

    private fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result = "Document"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) result = it.getString(index)
            }
        }
        return result
    }
}