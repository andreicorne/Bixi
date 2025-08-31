package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bixi.models.api.ForgotPasswordRequest
import com.example.bixi.services.RetrofitClient
import kotlinx.coroutines.launch

class ForgotPasswordViewModel() : BaseViewModel() {

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    fun setEmail(newTitle: String) {
        _email.value = newTitle
    }

    fun forgotPassword() {
        setLoading(true)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.forgotPassword(ForgotPasswordRequest(_email.value!!))
                if (response.success) {
                    _sendResponseCode.postValue(1)
                } else {
                    _sendResponseCode.postValue(1)
                }

            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
                _sendResponseCode.postValue(1)
            }
        }
    }
}
