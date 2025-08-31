package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bixi.helper.ApiStatus
import com.example.bixi.services.AuthRepository
import kotlinx.coroutines.launch

//class LoginViewModel(application: Application) : AndroidViewModel(application) {
class LoginViewModel() : BaseViewModel() {

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    fun setEmail(newTitle: String) {
        _email.value = newTitle
    }

    fun setPassword(newTitle: String) {
        _password.value = newTitle
    }

    fun login() {
        setLoading(true)
        viewModelScope.launch {
            val result = AuthRepository.login(_email.value!!, _password.value!!)
            setLoading(false)

            if (result.isSuccess) {
                _sendResponseCode.postValue(ApiStatus.SERVER_SUCCESS.code)
            } else {
                Log.e("Login", "Error: ${result.exceptionOrNull()?.message}")
                _sendResponseCode.postValue(ApiStatus.SERVER_ERROR.code)
            }
        }
    }
}
