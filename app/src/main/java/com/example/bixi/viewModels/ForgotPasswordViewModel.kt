package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bixi.models.api.ForgotPasswordRequest
import com.example.bixi.services.AuthRepository
import com.example.bixi.services.RetrofitClient
import kotlinx.coroutines.launch

class ForgotPasswordViewModel() : BaseViewModel() {

    // Poți avea LiveData pentru starea login-ului
    private val _apiResult = MutableLiveData<Boolean>()
    val apiResult: LiveData<Boolean> = _apiResult

    fun forgotPassword(email: String) {
        setLoading(true)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.forgotPassword(ForgotPasswordRequest(email))
                if (response.success) {
                    _apiResult.postValue(true)
                } else {
                    _apiResult.postValue(false)
                }

            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
                _apiResult.postValue(false)
            }
        }
    }
}
