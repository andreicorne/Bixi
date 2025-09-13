package com.example.bixi.models.api

data class ApiResponse<T>(
    val success: Boolean,
    val statusCode: Int,
    val data: T?,
    val message: String? = null,
    val total: String? = null,
    val page: String? = null,
    val pageSize: String? = null
)