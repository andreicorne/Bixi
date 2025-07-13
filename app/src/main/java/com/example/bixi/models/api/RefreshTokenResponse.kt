package com.example.bixi.models.api

data class RefreshTokenResponse(
    val access_token: String,
    val refresh_token: String
)