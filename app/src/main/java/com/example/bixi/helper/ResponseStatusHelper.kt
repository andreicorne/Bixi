package com.example.bixi.helper

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.example.bixi.R

enum class ApiStatus(val code: Int, @StringRes val messageRes: Int) {
    SERVER_ERROR(1, R.string.server_error_generic_message),
    SERVER_SUCCESS(4, 0),
    BAD_REQUEST(2, R.string.server_forgot_password_success);

    companion object {
        fun fromCode(code: Int): ApiStatus {
            return values().find { it.code == code } ?: SERVER_ERROR
        }
    }
}

object ResponseStatusHelper {
    fun showStatusMessage(context: Context, statusCode: Int) {
        val status = ApiStatus.fromCode(statusCode)
        if(status == ApiStatus.SERVER_SUCCESS){
            return
        }

        try{
            val message = context.getString(status.messageRes)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception){
        }
    }
}