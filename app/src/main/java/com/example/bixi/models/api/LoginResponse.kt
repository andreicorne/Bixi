package com.example.bixi.models.api

data class LoginResponse(
    val user: UserData
)

data class UserData(
    val id: String,
    val username: String,
    val roles: List<String>,
    val tenantId: String,
    val companyId: String
)