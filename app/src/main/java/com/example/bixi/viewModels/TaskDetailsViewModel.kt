package com.example.bixi.viewModels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bixi.AppSession
import com.example.bixi.enums.AttachmentType
import com.example.bixi.enums.TaskStatus
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.CheckItem
import com.example.bixi.models.api.AttachmentResponse
import com.example.bixi.models.api.CreateTaskRequest
import com.example.bixi.models.api.GetTasksRequest
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.UIMapperService
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskDetailsViewModel : BaseViewModel() {

    var isCreateMode: Boolean = false
    var taskId: String = ""

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _startDateTime = MutableLiveData<Calendar?>()
    val startDateTime: LiveData<Calendar?> = _startDateTime

    private val _endDateTime = MutableLiveData<Calendar?>()
    val endDateTime: LiveData<Calendar?> = _endDateTime

    private val _serverStatusResponse = MutableLiveData<Boolean>()
    val serverStatusResponse: LiveData<Boolean> = _serverStatusResponse

    private val _attachments = MutableLiveData<List<AttachmentItem>>(mutableListOf(AttachmentItem()))
    val attachments: LiveData<List<AttachmentItem>> = _attachments

    private val _responsible = MutableLiveData<List<String>>(mutableListOf())
    val responsible: LiveData<List<String>> = _responsible

    private val _checks = MutableLiveData<List<CheckItem>>(emptyList())
    val checks: LiveData<List<CheckItem>> = _checks

    private val _startDate = MutableLiveData<String>()
    val startDate: LiveData<String> = _startDate

    private val _endDate = MutableLiveData<String>()
    val endDate: LiveData<String> = _endDate

    fun getData(){
        _responsible.value = listOf("Marius", "Cosmin", "Flavius")

        setLoading(true)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getTaskById(taskId)
                if (response.success) {
                    val task = response.data!!
                    _title.value = task.title

                    _description.value = UIMapperService.fromHtmlToPlainText(task.description)

                    _checks.value = task.checklist

                    _attachments.value = mapAttachmentsFromServer(task.attachments)

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

    private fun mapAttachmentsFromServer(serverAttachments: List<AttachmentResponse>): List<AttachmentItem> {
        val mappedAttachments = serverAttachments.map { serverAttachment ->
            val fullUrl = "https://api.bixi.be/uploads/${serverAttachment.fileUrl}" // Înlocuiește cu domeniul tău
            val uri = Uri.parse(fullUrl)

            val attachmentType = when {
                serverAttachment.type.startsWith("image/") -> AttachmentType.IMAGE
                serverAttachment.type.contains("pdf") -> AttachmentType.DOCUMENT
                serverAttachment.type.contains("word") -> AttachmentType.DOCUMENT
                else -> AttachmentType.DOCUMENT
            }

            AttachmentItem(
                uri = uri,
                type = attachmentType,
                serverData = serverAttachment // Păstrează datele originale
            )
        }.toMutableList()

        // Adaugă un item gol pentru noi attachments
        mappedAttachments.add(AttachmentItem())

        return mappedAttachments
    }

    fun setTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun setDescription(newDescription: String) {
        _description.value = newDescription
    }

    fun setStartDateTime(calendar: Calendar) {
        _startDateTime.value = calendar
    }

    fun setEndDateTime(calendar: Calendar) {
        _endDateTime.value = calendar
    }

    fun addAttachmentItem(item: AttachmentItem) {
        _attachments.value = _attachments.value?.plus(item)
    }

    fun updateAttachmentUri(index: Int, newUri: Uri) {
        val currentList = _attachments.value?.toMutableList() ?: return
        val currentItem = currentList[index]
        currentList[index] = currentItem.copy(uri = newUri)
        _attachments.value = currentList
    }

    fun updateAttachmentAt(index: Int, update: (AttachmentItem) -> AttachmentItem) {
        val currentList = _attachments.value?.toMutableList() ?: return
        if (index in currentList.indices) {
            val oldItem = currentList[index]
            val newItem = update(oldItem)
            if (newItem != oldItem) { // doar dacă e un obiect diferit
                currentList[index] = newItem
                _attachments.value = currentList
            }
        }
    }

    fun removeAttachmentItem(item: AttachmentItem) {
        _attachments.value = _attachments.value?.filter { it != item }
    }

    fun removeAttachmentAt(index: Int) {
        val updatedList = _attachments.value?.toMutableList() ?: return
        if (index in updatedList.indices) {
            updatedList.removeAt(index)
            _attachments.value = updatedList
        }
    }

    fun updateAttachmentList(newList: List<AttachmentItem>) {
        _attachments.value = newList
    }

    fun addCheckItem(item: CheckItem) {
        _checks.value = _checks.value?.plus(item)
    }

    fun removeCheckAt(index: Int) {
        val updatedList = _checks.value?.toMutableList() ?: return
        if (index in updatedList.indices) {
            updatedList.removeAt(index)
            _checks.value = updatedList
        }
    }
}