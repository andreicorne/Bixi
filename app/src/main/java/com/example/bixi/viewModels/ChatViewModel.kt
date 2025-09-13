package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bixi.helper.ApiStatus
import com.example.bixi.models.AttachmentHandler
import com.example.bixi.models.Message
import com.example.bixi.models.api.CommentResponse
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.UIMapperService
import kotlinx.coroutines.launch

class ChatViewModel : BaseViewModel() {

    var taskId: String = ""
    private val pageSize = 15 // NumÄƒrul de comentarii pe paginÄƒ
    private var currentPage = 1 // Pagina curentÄƒ (Ã®ncepe de la 1)
    private var isLoadingMore = false
    var shouldScrollToNewMessage: Boolean = false

    private val _attachments = MutableLiveData<List<AttachmentHandler>>(mutableListOf())
    val attachments: LiveData<List<AttachmentHandler>> = _attachments

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _hasMore = MutableLiveData<Boolean>(true)
    val hasMore: LiveData<Boolean> = _hasMore

    fun loadMessage(){
        setLoading(true)
        currentPage = 1 // Reset la pagina 1 pentru Ã®ncÄƒrcarea iniÈ›ialÄƒ
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getComments(taskId, pageSize, currentPage)
                if (response.success) {
                    val newMessages = UIMapperService.mapCommentsFromServer(response.data!!, false)
                    _messages.value = newMessages

                    // VerificÄƒ dacÄƒ mai sunt comentarii (dacÄƒ numÄƒrul returnat este egal cu pageSize)
                    _hasMore.value = newMessages.size == pageSize
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

    fun loadMoreMessages() {
        if (isLoadingMore || !_hasMore.value!! || _isLoading.value!!) {
            return
        }

        isLoadingMore = true
        currentPage++ // IncrementeazÄƒ la urmÄƒtoarea paginÄƒ

        viewModelScope.launch {
            try {
                val response = RetrofitClient.getComments(taskId, pageSize, currentPage)
                if (response.success) {
                    val newMessages = UIMapperService.mapCommentsFromServer(response.data!!, false)

                    if (newMessages.isNotEmpty()) {
                        // AdaugÄƒ noile mesaje la sfÃ¢rÈ™itul listei existente
                        val currentList = _messages.value?.toMutableList() ?: mutableListOf()
                        currentList.addAll(newMessages)
                        _messages.value = currentList

                        // VerificÄƒ dacÄƒ mai sunt comentarii de Ã®ncÄƒrcat
                        _hasMore.value = newMessages.size == pageSize
                    } else {
                        _hasMore.value = false
                    }
                } else {
                    Log.e("API", "Load more failed: ${response.statusCode}")
                    _hasMore.value = false
                    currentPage-- // Revenire la pagina anterioarÄƒ Ã®n caz de eroare
                }

            } catch (e: Exception) {
                Log.e("API", "Exception loading more: ${e.message}")
                _hasMore.value = false
                currentPage-- // Revenire la pagina anterioarÄƒ Ã®n caz de eroare
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun addMessageToListFromServer(comment: CommentResponse){
        shouldScrollToNewMessage = true
        val commentsForUI = UIMapperService.mapCommentsFromServer(listOf(comment), false)
        val currentList = _messages.value?.toMutableList() ?: mutableListOf()
        commentsForUI.forEach { message ->
            currentList.add(0, message)
        }
        _messages.value = currentList
    }

    fun addAttachment(item: AttachmentHandler) {
        val currentList = _attachments.value?.toMutableList() ?: mutableListOf()
        currentList.add(item)
        _attachments.value = currentList
    }

    fun clearAttachments() {
        _attachments.value = mutableListOf()
    }

    fun removeAttachment(item: AttachmentHandler) {
        val currentList = _attachments.value?.toMutableList() ?: return
        currentList.remove(item)
        _attachments.value = currentList
    }

    fun removeAttachmentById(id: String) {
        val currentList = _attachments.value?.toMutableList() ?: return
        currentList.removeAll { it.id == id }
        _attachments.value = currentList
    }
}