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
import com.example.bixi.models.api.LoginResponse
import com.example.bixi.services.AuthRepository
import com.example.bixi.services.JsonConverterService
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.SecureStorageService
import kotlinx.coroutines.launch

//class LoginViewModel(application: Application) : AndroidViewModel(application) {
class LoginViewModel() : BaseViewModel() {

        // Poți avea LiveData pentru starea login-ului
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun login(username: String, password: String) {
        setLoading(true)
        viewModelScope.launch {
            val result = AuthRepository.login(username, password)
            setLoading(false)

            if (result.isSuccess) {
                _loginSuccess.postValue(true)
            } else {
                Log.e("Login", "Error: ${result.exceptionOrNull()?.message}")
                _loginSuccess.postValue(false)
            }
        }
    }
}
