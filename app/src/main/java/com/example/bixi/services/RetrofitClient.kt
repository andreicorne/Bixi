package com.example.bixi.services

import android.util.Log
import com.example.bixi.interfaces.AuthApi
import com.example.bixi.models.api.ApiResponse
import com.example.bixi.models.api.CreateTaskRequest
import com.example.bixi.models.api.ForgotPasswordRequest
import com.example.bixi.models.api.GetTasksRequest
import com.example.bixi.models.api.LoginRequest
import com.example.bixi.models.api.LoginResponse
import com.example.bixi.models.api.RefreshTokenRequest
import com.example.bixi.models.api.TaskDetailsResponse
import com.example.bixi.models.api.TaskListResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.bixi.be/api/"
    private const val TOKEN_EXPIRY_MINUTES = 14L

    // Stocare token-uri și timp
    private var currentAccessToken: String? = null
    private var currentRefreshToken: String? = null
    private var tokenTimestamp: Long = 0L

    val instance: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    // Salvează token-urile după login
    fun saveTokens(accessToken: String, refreshToken: String) {
        currentAccessToken = accessToken
        currentRefreshToken = refreshToken
        tokenTimestamp = System.currentTimeMillis()
    }

    // Verifică dacă token-ul a expirat (15 minute)
    private fun isTokenExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - tokenTimestamp
        val fifteenMinutesInMillis = TOKEN_EXPIRY_MINUTES * 60 * 1000
        return timeElapsed >= fifteenMinutesInMillis
    }

    // Refresh token folosind refresh token-ul curent
    private suspend fun refreshToken(): Boolean {
        return try {
            val refreshRequest = RefreshTokenRequest(currentRefreshToken!!)
            val response = instance.refreshToken(refreshRequest)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    // Actualizează token-urile cu cele noi
                    currentAccessToken = body.data?.access_token
                    currentRefreshToken = body.data?.refresh_token
                    tokenTimestamp = System.currentTimeMillis()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Verifică și refresh token dacă este necesar
    private suspend fun ensureValidToken(): String? {
        // Dacă nu avem token, returnează null
        if (currentAccessToken == null || currentRefreshToken == null) {
            return null
        }

        // Dacă token-ul a expirat, încearcă să-l refresh
        if (isTokenExpired()) {
            val refreshSuccess = refreshToken()
            if (!refreshSuccess) {
                // Refresh a eșuat, șterge token-urile
                currentAccessToken = null
                currentRefreshToken = null
                tokenTimestamp = 0L
                return null
            }
        }

        return currentAccessToken
    }

    private suspend fun <T> handleApiCall(call: suspend () -> Response<ApiResponse<T>>): ApiResponse<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()!!
                ApiResponse(body.success, body.statusCode, body.data)
            } else {
                val code = response.body()?.statusCode ?: -1
                ApiResponse(false, code, null)
            }
        } catch (e: Exception) {
            Log.e("### Retrofit", e.message.toString())
            ApiResponse(false, -1, null) // sau log/return e.message
        }
    }

    suspend fun login(parameter: LoginRequest): ApiResponse<LoginResponse> {
        val result = handleApiCall { instance.login(parameter) }

        // Dacă login-ul a reușit, salvează token-urile
        if (result.success && result.data != null) {
            saveTokens(result.data.access_token, result.data.refresh_token)
        }

        return result
    }

    suspend fun getMobileTasks(parameter: GetTasksRequest): ApiResponse<List<TaskListResponse>> {
        // Verifică și refresh token dacă este necesar
        val validToken = ensureValidToken()

        if (validToken == null) {
            // Token invalid sau refresh a eșuat
            return ApiResponse(false, -1, null)
        }

        val bearerToken = "Bearer $validToken"
        return handleApiCall { instance.getTasks(parameter, bearerToken) }
    }

    suspend fun getTaskById(taskId: String): ApiResponse<TaskDetailsResponse> {
        val validToken = ensureValidToken()

        if (validToken == null) {
            return ApiResponse(false, -1, null)
        }

        val bearerToken = "Bearer $validToken"
        return handleApiCall { instance.getTaskById(taskId, bearerToken) }
    }

    suspend fun createTask(parameter: CreateTaskRequest, attachments: List<MultipartBody.Part>): ApiResponse<Any> {
        // Verifică și refresh token dacă este necesar
        val validToken = ensureValidToken()

        if (validToken == null) {
            // Token invalid sau refresh a eșuat
            return ApiResponse(false, -1, null)
        }

        val mediaType = "text/plain".toMediaTypeOrNull()
        val titleBody = parameter.title.toRequestBody(mediaType)
        val assigneeBody = parameter.assigneeId.toRequestBody(mediaType)
        val startDateBody = parameter.startDate.toRequestBody(mediaType)
        val endDateBody = parameter.endDate.toRequestBody(mediaType)
        val descriptionBody = parameter.description.toRequestBody(mediaType)
        val creatorIdBody = parameter.creatorId.toRequestBody(mediaType)
        val checkListBody = parameter.checklist.toRequestBody(mediaType)

        val bearerToken = "Bearer $validToken"
        return handleApiCall { instance.createTask(titleBody, descriptionBody, creatorIdBody,
            checkListBody, assigneeBody, startDateBody, endDateBody, attachments, bearerToken) }
    }

    suspend fun forgotPassword(parameter: ForgotPasswordRequest): ApiResponse<Any> {
        return try {
            val response = instance.forgotPassword(parameter)
            if (response.isSuccessful) {
                val body = response.body()!!
                ApiResponse(body.success, body.statusCode, body.data)
            } else {
                val code = response.body()?.statusCode ?: -1
                ApiResponse(false, code, null)
            }
        } catch (e: Exception) {
            ApiResponse(false, -1, null) // sau log/return e.message
        }
    }

    // Metodă utilă pentru a verifica dacă utilizatorul este autentificat
    fun isLoggedIn(): Boolean {
        return currentAccessToken != null && currentRefreshToken != null
    }

    // Metodă pentru logout (șterge token-urile)
    fun logout() {
        currentAccessToken = null
        currentRefreshToken = null
        tokenTimestamp = 0L
    }
}