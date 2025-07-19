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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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

                    setEndDateTimeFromServer(task.endDate)
                    setStartDateTimeFromServer(task.startDate)

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
            val fullUrl = "https://api.bixi.be/uploads/${serverAttachment.fileUrl}"
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
                serverData = serverAttachment
            )
        }.toMutableList()

        // Adaugă un item gol pentru noi attachments la sfârșit
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

    /**
     * Adaugă un atașament la sfârșitul listei (comportamentul vechi)
     */
    fun addAttachmentItem(item: AttachmentItem) {
        _attachments.value = _attachments.value?.plus(item)
    }

    /**
     * Adaugă un atașament înaintea ultimului element gol
     * Aceasta asigură că container-ul pentru adăugare rămâne mereu ultimul
     */
    fun addAttachmentBeforeLast(item: AttachmentItem) {
        val currentList = _attachments.value?.toMutableList() ?: mutableListOf()

        // Caută ultimul element gol (container pentru adăugare)
        val lastEmptyIndex = currentList.indexOfLast { it.uri == null }

        if (lastEmptyIndex != -1) {
            // Inserează înainte de ultimul element gol
            currentList.add(lastEmptyIndex, item)
            Log.d("ViewModel", "Added attachment before last empty at index: $lastEmptyIndex")
        } else {
            // Dacă nu există element gol, adaugă la sfârșit și apoi adaugă un element gol
            currentList.add(item)
            currentList.add(AttachmentItem()) // Adaugă container gol la sfârșit
            Log.d("ViewModel", "No empty container found, added attachment and new empty container")
        }

        _attachments.value = currentList
        Log.d("ViewModel", "Attachments list size: ${currentList.size}")
    }

    /**
     * Actualizează URI-ul unui atașament la un index specificat
     */
    fun updateAttachmentUri(index: Int, newUri: Uri) {
        val currentList = _attachments.value?.toMutableList() ?: return
        if (index !in currentList.indices) return

        val currentItem = currentList[index]
        currentList[index] = currentItem.copy(uri = newUri)

        // Dacă am actualizat ultimul element gol, adaugă un nou element gol la sfârșit
        if (index == currentList.size - 1 && currentItem.uri == null) {
            currentList.add(AttachmentItem())
            Log.d("ViewModel", "Updated last empty container, added new empty container")
        }

        _attachments.value = currentList
    }

    /**
     * Actualizează un atașament folosind o funcție de transformare
     */
    fun updateAttachmentAt(index: Int, update: (AttachmentItem) -> AttachmentItem) {
        val currentList = _attachments.value?.toMutableList() ?: return
        if (index !in currentList.indices) return

        val oldItem = currentList[index]
        val newItem = update(oldItem)

        if (newItem != oldItem) {
            currentList[index] = newItem

            // Dacă am actualizat ultimul element gol și acum are URI, adaugă un nou element gol
            if (index == currentList.size - 1 && oldItem.uri == null && newItem.uri != null) {
                currentList.add(AttachmentItem())
                Log.d("ViewModel", "Updated last empty container with content, added new empty container")
            }

            _attachments.value = currentList
        }
    }

    /**
     * Șterge un atașament din listă
     */
    fun removeAttachmentItem(item: AttachmentItem) {
        val currentList = _attachments.value?.toMutableList() ?: return
        val removed = currentList.remove(item)

        if (removed) {
            // Asigură-te că există întotdeauna un element gol la sfârșit
            ensureEmptyContainerAtEnd(currentList)
            _attachments.value = currentList
            Log.d("ViewModel", "Removed attachment item, list size: ${currentList.size}")
        }
    }

    /**
     * Șterge un atașament la un index specificat
     */
    fun removeAttachmentAt(index: Int) {
        val currentList = _attachments.value?.toMutableList() ?: return
        if (index !in currentList.indices) return

        currentList.removeAt(index)

        // Asigură-te că există întotdeauna un element gol la sfârșit
        ensureEmptyContainerAtEnd(currentList)

        _attachments.value = currentList
        Log.d("ViewModel", "Removed attachment at index: $index, list size: ${currentList.size}")
    }

    /**
     * Înlocuiește întreaga listă de atașamente
     */
    fun updateAttachmentList(newList: List<AttachmentItem>) {
        val mutableList = newList.toMutableList()

        // Asigură-te că există întotdeauna un element gol la sfârșit
        ensureEmptyContainerAtEnd(mutableList)

        _attachments.value = mutableList
    }

    /**
     * Metodă privată pentru a asigura că există un container gol la sfârșitul listei
     */
    private fun ensureEmptyContainerAtEnd(list: MutableList<AttachmentItem>) {
        if (list.isEmpty() || list.last().uri != null) {
            list.add(AttachmentItem())
            Log.d("ViewModel", "Added empty container at end")
        }
    }

    /**
     * Verifică dacă există un container gol la sfârșitul listei
     */
    fun hasEmptyContainerAtEnd(): Boolean {
        val list = _attachments.value ?: return false
        return list.isNotEmpty() && list.last().uri == null
    }

    /**
     * Returnează numărul de atașamente reale (exclude container-ul gol)
     */
    fun getRealAttachmentsCount(): Int {
        val list = _attachments.value ?: return 0
        return list.count { it.uri != null }
    }

    // Metodele pentru CheckItems rămân neschimbate
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

    fun setEndDateTimeFromServer(dateStr: String) {
        val calendar = parseIsoToCalendar(dateStr)
        _endDateTime.value = calendar
    }

    fun setStartDateTimeFromServer(dateStr: String) {
        val calendar = parseIsoToCalendar(dateStr)
        _startDateTime.value = calendar
    }

    fun parseIsoToCalendar(isoDate: String): Calendar {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        val date = format.parse(isoDate)

        return Calendar.getInstance().apply {
            time = date!!
        }
    }
}