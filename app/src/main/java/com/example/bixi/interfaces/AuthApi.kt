package com.example.bixi.interfaces

import com.example.bixi.models.api.LoginRequest
import com.example.bixi.models.api.LoginResponse
import com.example.bixi.models.api.TaskItemListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Headers("Content-Type: application/json")
    @POST("hr/tasks/list")
    suspend fun getTasks(): Response<Any>
}