package com.example.bixi.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.bixi.R
import com.example.bixi.enums.AttachmentType
import com.example.bixi.models.AttachmentItem

object AttachmentOpenExternalHelper {

    fun open(context: Context, attachment: AttachmentItem) {
        try {
            if(isLocalUri(attachment.uri!!)){
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(attachment.uri, getAttachmentMimeType(context, attachment))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
            else{
                val intent = Intent(Intent.ACTION_VIEW, attachment.uri)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.no_application_found_to_open_file),
                Toast.LENGTH_SHORT
            ).show()
            Log.e("AttachmentViewer", "Error opening external app", e)
        }
    }

    private fun getAttachmentMimeType(context: Context, attachment: AttachmentItem): String {
        return when (attachment.type) {
            AttachmentType.IMAGE -> "image/*"
            AttachmentType.DOCUMENT -> {
                attachment.serverData?.type ?: context.contentResolver.getType(attachment.uri!!) ?: "*/*"
            }
            else -> "*/*"
        }
    }

    private fun isLocalUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        return scheme == "content" || scheme == "file" ||
                uri.toString().startsWith("/") ||
                !uri.toString().startsWith("http")
    }

}