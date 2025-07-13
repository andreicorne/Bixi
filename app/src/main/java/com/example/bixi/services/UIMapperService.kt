package com.example.bixi.services

import android.text.Html
import com.example.bixi.models.UITaskList
import com.example.bixi.models.api.TaskListResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object UIMapperService {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    fun mapToUiTask(task: TaskListResponse): UITaskList {
        return UITaskList(
            id = task.id,
            title = task.title,
            description = fromHtmlToPlainText(task.description),
            assigneeName = task.assigneeName,
            startDate = LocalDateTime.parse(task.startDate, formatter),
            endDate = LocalDateTime.parse(task.endDate, formatter)
        )
    }

    fun fromHtmlToPlainText(html: String): String {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }

    fun mapToUiTaskList(tasks: List<TaskListResponse>): List<UITaskList> {
        return tasks.map { mapToUiTask(it) }
    }
}