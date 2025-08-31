package com.example.bixi.viewModels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bixi.enums.AttachmentType
import com.example.bixi.enums.TaskViewMode
import com.example.bixi.helper.ApiStatus
import com.example.bixi.helper.Utils
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.CheckItem
import com.example.bixi.models.TaskUIData
import com.example.bixi.models.api.AttachmentResponse
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.UIMapperService
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class TaskDetailsViewModel : BaseViewModel() {

    private var originalData: TaskUIData? = null

    lateinit var viewMode: TaskViewMode
    var taskId: String = ""

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _startDateTimeText = MutableLiveData<String>()
    val startDateTimeText: LiveData<String> = _startDateTimeText

    private val _endDateTimeText = MutableLiveData<String>()
    val endDateTimeText: LiveData<String> = _endDateTimeText

    private val _startDateTime = MutableLiveData<Calendar?>()
    val startDateTime: LiveData<Calendar?> = _startDateTime

    private val _endDateTime = MutableLiveData<Calendar?>()
    val endDateTime: LiveData<Calendar?> = _endDateTime

    private val _attachments = MutableLiveData<List<AttachmentItem>>(mutableListOf(AttachmentItem()))
    val attachments: LiveData<List<AttachmentItem>> = _attachments

    private val _responsibles = MutableLiveData<List<String>>(mutableListOf())
    val responsibles: LiveData<List<String>> = _responsibles

    private val _responsible = MutableLiveData<Int>()
    val responsible: LiveData<Int> = _responsible

    private val _checks = MutableLiveData<List<CheckItem>>(emptyList())
    val checks: LiveData<List<CheckItem>> = _checks

    init {
        _responsibles.value = listOf("Marius", "Cosmin", "Flavius")
    }

    fun getData(){
        _responsibles.value = listOf("Marius", "Cosmin", "Flavius")

        setLoading(true)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getTaskById(taskId)
                if (response.success) {
                    val task = response.data!!

                    _title.value = task.title
                    _description.value = UIMapperService.fromHtmlToPlainText(task.description)

                    //TODO: reset. need from server
                    _responsible.value = 1

                    setEndDateTimeFromServer(task.endDate)
                    setStartDateTimeFromServer(task.startDate)

                    val isMoreThanOneChecks = task.checklist.size > 1
                    _checks.value = task.checklist.map { item ->
                        CheckItem(text = item.text, done = item.done, id = UUID.randomUUID().toString(), shouldDisplayDragHandle = isMoreThanOneChecks)
                    }

                    _attachments.value = mapAttachmentsFromServer(task.attachments)

                    originalData = TaskUIData(title.value!!, description.value!!, startDateTime.value!!, endDateTime.value!!,
                        attachments.value!!, responsible.value, checks.value!!)

                }
                else{
                    _sendResponseCode.postValue(response.statusCode)
                }

            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
                _sendResponseCode.postValue(ApiStatus.SERVER_ERROR.code)
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        setLoading(true)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.delete(taskId)
                if (response.success) {
                    onSuccess()
                } else {
                    _sendResponseCode.postValue(1)
                }

            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
                _sendResponseCode.postValue(1)
            }
        }
    }

    fun taskHasChanged(): Boolean{
        if(originalData == null){
            return false
        }

        if(!title.value.equals(originalData!!.title)){
            return true
        }
        if(!description.value.equals(originalData!!.description)){
            return true
        }
        if(!responsible.value.equals(originalData!!.responsible)){
            return true
        }
        if(startDateTime.value!!.timeInMillis != originalData!!.startDate.timeInMillis){
            return true
        }
        if(endDateTime.value!!.timeInMillis != originalData!!.endDate.timeInMillis){
            return true
        }
        if(checks.value != originalData!!.checks){
            return true
        }
        if(attachments.value != originalData!!.attachments){
            return true
        }
        return false
    }

    fun resetChanges(){
        _title.value = originalData!!.title
        _description.value = originalData!!.description
        _responsible.value = if(originalData!!.responsible == null) 0 else originalData!!.responsible!!

        _startDateTime.value = originalData!!.startDate
        _endDateTime.value = originalData!!.endDate

        _checks.value = originalData!!.checks
        _attachments.value = originalData!!.attachments

        // TODO: set the original responsible
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
                serverData = serverAttachment,
                false
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

    fun setResponsible(pos: Int){
        _responsible.value = pos
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

    fun removeCheckAt(index: Int) {
        val updatedList = _checks.value?.toMutableList() ?: return
        if (index in updatedList.indices) {
            updatedList.removeAt(index)
            _checks.value = updatedList
        }
    }

    // Funcție pentru a actualiza "shouldDisplayDragHandle" pe toți itemii
    private fun updateDragHandles(items: List<CheckItem>): List<CheckItem> {
        val shouldShowDrag = items.count { !it.done } >= 2
        return items.map { item ->
            item.copy(shouldDisplayDragHandle = shouldShowDrag && !item.done)
        }
    }

    fun addCheckItem(newItem: CheckItem) {
        val currentList = _checks.value ?: emptyList()

        // Adăugăm noul item, care va avea drag handle doar dacă este permis
        val updatedList = (currentList + newItem).let { updateDragHandles(it) }

        _checks.value = updatedList
    }

    fun deleteCheckItem(id: String) {
        val currentList = _checks.value ?: return

        val updatedList = currentList.filter { it.id != id }

        // Actualizăm drag handles pe toți itemii
        val finalList = updateDragHandles(updatedList)

        _checks.value = finalList
    }

    fun moveUncheckedItem(from: Int, to: Int) {
        val currentList = _checks.value?.toMutableList() ?: return

        // doar pe cele nebifate
        val unchecked = currentList.filter { !it.done }.toMutableList()
        if (from >= unchecked.size || to >= unchecked.size) return

        val movedItem = unchecked.removeAt(from)
        unchecked.add(to, movedItem)

        // Actualizăm drag handles pe toate itemele (și cele bifate)
        val finalList = updateDragHandles(unchecked + currentList.filter { it.done })

        _checks.value = finalList
    }

    fun updateCheckItem(id: String, isChecked: Boolean) {
        val currentList = _checks.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            // actualizăm itemul bifat
            val updatedItem = currentList[index].copy(done = isChecked)
            currentList[index] = updatedItem

            // Actualizăm drag handles pe toți itemii
            val finalList = updateDragHandles(currentList)

            _checks.value = finalList
        }
    }

    fun setStartDateTime(dateStr: String) {
        _startDateTimeText.value = dateStr
    }

    fun setEndDateTime(dateStr: String) {
        _endDateTimeText.value = dateStr
    }

    fun setEndDateTimeFromServer(dateStr: String) {
        val calendar = Utils.utcIsoStringToCalendar(dateStr)
        _endDateTime.value = calendar
    }

    fun setStartDateTimeFromServer(dateStr: String) {
        val calendar = Utils.utcIsoStringToCalendar(dateStr)
        _startDateTime.value = calendar
    }
}