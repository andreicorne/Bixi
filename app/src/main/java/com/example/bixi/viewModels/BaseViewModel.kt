package com.example.bixi.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {

    // LiveData vizibilă pentru Activity/Fragment
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Metode pe care copilul le poate apela
    protected fun setLoading(value: Boolean) {
        _isLoading.value = value
    }
}