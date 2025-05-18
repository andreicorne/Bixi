package com.example.bixi.interfaces

import com.example.bixi.models.api.LoginRequest
import com.example.bixi.models.api.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}