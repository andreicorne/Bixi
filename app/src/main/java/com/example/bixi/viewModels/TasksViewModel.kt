package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bixi.AppSession
import com.example.bixi.enums.TaskStatus
import com.example.bixi.helper.ApiStatus
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

    private val pageSize = 20 // Numărul de task-uri pe pagină
    private var currentPage = 1 // Pagina curentă (începe de la 1)
    private var isLoadingMore = false

    private val _tasks = MutableLiveData<List<UITaskList>>(emptyList())
    val tasks: LiveData<List<UITaskList>> = _tasks

    private val _hasMore = MutableLiveData<Boolean>(true)
    val hasMore: LiveData<Boolean> = _hasMore

    var shouldNavigatoToTop: Boolean = false

    fun setStatus(status: TaskStatus) {
        _selectedStatus.value = status
        getList(status)
    }

    fun getList(status: TaskStatus) {
        setLoading(true)
        currentPage = 1 // Reset la pagina 1 pentru încărcarea inițială
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getMobileTasks(GetTasksRequest(pageSize, currentPage, status.value))
                if (response.success) {

                    if(status == TaskStatus.NEW){
                        shouldNavigatoToTop = true
                    }

                    val newTasks = UIMapperService.mapToUiTaskList(response.data!!)
                    _tasks.value = newTasks

                    // Verifică dacă mai sunt task-uri (dacă numărul returnat este egal cu pageSize)
                    _hasMore.value = newTasks.size == pageSize
                } else {
                    _hasMore.value = false
                }

                _sendResponseCode.postValue(response.statusCode)

            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
                _sendResponseCode.postValue(ApiStatus.SERVER_ERROR.code)
                _hasMore.value = false
            } finally {
                setLoading(false)
            }
        }
    }

    fun loadMoreTasks() {
        if (isLoadingMore || !_hasMore.value!! || _isLoading.value!!) {
            return
        }

        isLoadingMore = true
        currentPage++ // Incrementează la următoarea pagină

        viewModelScope.launch {
            try {
                val response = RetrofitClient.getMobileTasks(GetTasksRequest(pageSize, currentPage, _selectedStatus.value.value))
                if (response.success) {
                    val newTasks = UIMapperService.mapToUiTaskList(response.data!!)

                    if (newTasks.isNotEmpty()) {
                        // Adaugă noile task-uri la sfârșitul listei existente
                        val currentList = _tasks.value?.toMutableList() ?: mutableListOf()
                        currentList.addAll(newTasks)
                        _tasks.value = currentList

                        // Verifică dacă mai sunt task-uri de încărcat
                        _hasMore.value = newTasks.size == pageSize
                    } else {
                        _hasMore.value = false
                    }
                } else {
                    Log.e("API", "Load more tasks failed: ${response.statusCode}")
                    _hasMore.value = false
                    currentPage-- // Revenire la pagina anterioară în caz de eroare
                }

            } catch (e: Exception) {
                Log.e("API", "Exception loading more tasks: ${e.message}")
                _hasMore.value = false
                currentPage-- // Revenire la pagina anterioară în caz de eroare
            } finally {
                isLoadingMore = false
            }
        }
    }
}