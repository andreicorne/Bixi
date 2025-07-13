package com.example.bixi.interfaces

import com.example.bixi.models.api.ApiResponse
import com.example.bixi.models.api.CreateTaskRequest
import com.example.bixi.models.api.ForgotPasswordRequest
import com.example.bixi.models.api.GetTasksRequest
import com.example.bixi.models.api.LoginRequest
import com.example.bixi.models.api.LoginResponse
import com.example.bixi.models.api.RefreshTokenRequest
import com.example.bixi.models.api.RefreshTokenResponse
import com.example.bixi.models.api.TaskListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthApi {
    @POST("auth/mobile-login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("hr/mobile-tasks/list")
    suspend fun getTasks(
        @Body request: GetTasksRequest,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<List<TaskListResponse>>>

    @POST("auth/mobile-refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<RefreshTokenResponse>>

    @Multipart
    @POST("hr/mobile-tasks")
    suspend fun createTask(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("creatorId") creatorId: RequestBody,
        @Part("checklist") checklist: RequestBody,
        @Part attachments: List<MultipartBody.Part>,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<Any>>

    @POST("auth/mobile-forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<Any>>
}