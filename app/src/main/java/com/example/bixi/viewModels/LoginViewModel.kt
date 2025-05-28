package com.example.bixi.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bixi.AppSession
import com.example.bixi.constants.StorageKeys
import com.example.bixi.models.api.LoginRequest
import com.example.bixi.services.JsonConverterService
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.SecureStorageService
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    // Poți avea LiveData pentru starea login-ului
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun login(username: String, password: String) {
        val request = LoginRequest(username, password)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.login(request)
                if (response.isSuccessful) {
                    val user = response.body()?.user
                    Log.d("API", "Login successful: ${user?.username}")
                    // TODO: Salvează tokenul sau datele userului
                    AppSession.user = user
                    SecureStorageService.putString(getApplication(), StorageKeys.USER_TOKEN, JsonConverterService.toJson(user))
                    _loginSuccess.postValue(true)
                } else {
                    Log.e("API", "Login error: ${response.code()} - ${response.message()}")
                    _loginSuccess.postValue(false)
                }

            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
                _loginSuccess.postValue(false)
            }
        }
    }

}
