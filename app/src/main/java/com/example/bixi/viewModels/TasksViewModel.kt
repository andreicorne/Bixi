package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bixi.AppSession
import com.example.bixi.enums.TaskStatus
import com.example.bixi.models.CheckItem
import com.example.bixi.models.UITaskList
import com.example.bixi.models.api.GetTasksRequest
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.UIMapperService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TasksViewModel : BaseViewModel() {

    private val _selectedStatus = MutableStateFlow(TaskStatus.NEW)
    val selectedStatus: StateFlow<TaskStatus> = _selectedStatus.asStateFlow()

    private val _serverStatusResponse = MutableLiveData<Boolean>()
    val serverStatusResponse: LiveData<Boolean> = _serverStatusResponse

    private val _tasks = MutableLiveData<List<UITaskList>>(emptyList())
    //    private val _checks = MutableLiveData<List<CheckItem>>(mutableListOf(CheckItem(title = "test", isChecked = false)))
    val tasks: LiveData<List<UITaskList>> = _tasks

    fun setStatus(status: TaskStatus) {
        _selectedStatus.value = status
        getList(status)
    }

    fun getList(status: TaskStatus) {
        setLoading(true)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getMobileTasks(GetTasksRequest(10, 0, status.value))
                if (response.success) {
                    _tasks.value = UIMapperService.mapToUiTaskList(response.data!!)
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