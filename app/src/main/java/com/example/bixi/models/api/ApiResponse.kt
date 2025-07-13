package com.example.bixi.models.api

data class ApiResponse<T>(
    val success: Boolean,
    val statusCode: Int,
    val data: T?,
    val message: String? = null,
    val total: Int? = null,
    val page: Int? = null,
    val pageSize: Int? = null
)