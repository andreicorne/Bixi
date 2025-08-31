package com.example.bixi.models.api

data class AttendanceRequest(
    val employeeId: String,
    val action: String,
    val position: Position
)