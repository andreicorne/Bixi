package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bixi.models.api.LoginRequest
import com.example.bixi.services.RetrofitClient
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

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
