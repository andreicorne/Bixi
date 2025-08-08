package com.example.bixi.interfaces

import com.example.bixi.models.api.ApiResponse
import com.example.bixi.models.api.CreateTaskRequest
import com.example.bixi.models.api.ForgotPasswordRequest
import com.example.bixi.models.api.GetTasksRequest
import com.example.bixi.models.api.LoginRequest
import com.example.bixi.models.api.LoginResponse
import com.example.bixi.models.api.RefreshTokenRequest
import com.example.bixi.models.api.RefreshTokenResponse
import com.example.bixi.models.api.TaskDetailsResponse
import com.example.bixi.models.api.TaskListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

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
        @Part("assigneeId") assigneeId: RequestBody,
        @Part("startDate") startDate: RequestBody,
        @Part("endDate") endDate: RequestBody,
        @Part attachments: List<MultipartBody.Part>,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<Any>>

    @Multipart
    @PATCH("hr/mobile-tasks/{taskId}/with-files")
    suspend fun editTask(
        @Path("taskId") taskId: String,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
//        @Part("creatorId") creatorId: RequestBody,
        @Part("checklist") checklist: RequestBody,
//        @Part("assigneeId") assigneeId: RequestBody,
//        @Part("startDate") startDate: RequestBody,
//        @Part("endDate") endDate: RequestBody,
        @Part attachments: List<MultipartBody.Part>,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<Any>>


    @POST("auth/mobile-forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<Any>>

    @GET("hr/mobile-tasks/{taskId}")
    suspend fun getTaskById(
        @Path("taskId") taskId: String,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<TaskDetailsResponse>>

}