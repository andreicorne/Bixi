package com.example.bixi.helper

import android.graphics.Color

object ExtensionHelper {

    fun getColorByExtension(fileName: String): Int {
        val extensionColor = when (getExtension(fileName).lowercase()) {
            "pdf" -> Color.rgb(234, 67, 53)
            "doc", "docx" -> Color.rgb(78, 140, 244)
            "csv", "xlsx", "xls" -> Color.rgb(52, 168, 83)
            "ppt", "pptx" -> Color.rgb(247, 195, 36)
            else -> Color.GRAY
        }
        return extensionColor
    }

    fun getExtension(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return extension
    }
}