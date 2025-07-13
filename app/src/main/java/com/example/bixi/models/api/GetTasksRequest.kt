package com.example.bixi.models.api

data class GetTasksRequest(
    val pageSize: Int,
    val page: Int,
    val status: String
)