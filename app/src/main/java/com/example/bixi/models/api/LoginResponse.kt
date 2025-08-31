package com.example.bixi.models.api

data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val user: UserData,
    val attendanceStatus: String
)

data class UserData(
    val id: String,
    val username: String,
    val roles: List<String>,
    val tenantId: String,
    val companyId: String,
    var password: String?
)