package com.example.bixi

import com.example.bixi.models.api.EmployeeResponse
import com.example.bixi.models.api.LoginResponse
import com.example.bixi.models.api.UserData

object AppSession {
    var user: LoginResponse? = null
    var employees: List<EmployeeResponse>? = null
    var token: String? = null
    var isDarkMode: Boolean = false
    var language: String = "ro"

    fun clear() {
        employees = null
        user = null
        token = null
    }
}