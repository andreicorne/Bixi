package com.example.bixi.models.api

import com.google.gson.annotations.SerializedName

data class TaskItemListResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("assigneeId")
    val assigneeId: String,
    @SerializedName("assigneeEntityType")
    val assigneeEntityType: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("status")
    val status: String,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("endDate")
    val endDate: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("assigneeName")
    val assigneeName: String
)