package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bixi.services.RetrofitClient
import kotlinx.coroutines.launch

class TasksViewModel : ViewModel() {

    private val _serverStatusResponse = MutableLiveData<Boolean>()
    val serverStatusResponse: LiveData<Boolean> = _serverStatusResponse

    fun getList() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getTasks()
                if (response.isSuccessful) {
                    _serverStatusResponse.postValue(true)
                } else {
                    _serverStatusResponse.postValue(false)
                }

            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
                _serverStatusResponse.postValue(false)
            }
        }
    }

}