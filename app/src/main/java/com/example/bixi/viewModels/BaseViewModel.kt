package com.example.bixi.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {

    protected val _sendResponseCode = MutableLiveData<Int>()
    val sendResponseCode: LiveData<Int> = _sendResponseCode

    protected val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    protected fun setLoading(value: Boolean) {
        _isLoading.value = value
    }
}