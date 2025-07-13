package com.example.bixi.services

import com.example.bixi.AppSession
import com.example.bixi.models.api.LoginRequest

object AuthRepository {

    suspend fun login(username: String, password: String): Result<Boolean> {
        return try {
            val response = RetrofitClient.login(LoginRequest(username, password))

            if (response.success && response.data != null) {
                val user = response.data
                AppSession.user = user

                RetrofitClient.saveTokens(user.access_token, user.refresh_token)
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
