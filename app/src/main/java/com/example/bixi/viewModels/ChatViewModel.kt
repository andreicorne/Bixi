package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bixi.helper.ApiStatus
import com.example.bixi.interfaces.IMessage
import com.example.bixi.models.AttachmentHandler
import com.example.bixi.models.MessageItem
import com.example.bixi.models.MessageTimeSeparator
import com.example.bixi.models.api.CommentResponse
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.UIMapperService
import kotlinx.coroutines.launch
import java.util.Date

class ChatViewModel : BaseViewModel() {

    var taskId: String = ""
    private val pageSize = 15 // NumÄƒrul de comentarii pe paginÄƒ
    private var currentPage = 1 // Pagina curentÄƒ (Ã®ncepe de la 1)
    private var isLoadingMore = false
    var shouldScrollToNewMessage: Boolean = false

    private val _attachments = MutableLiveData<List<AttachmentHandler>>(mutableListOf())
    val attachments: LiveData<List<AttachmentHandler>> = _attachments

    private val _messages = MutableLiveData<List<IMessage>>()
    val messages: LiveData<List<IMessage>> = _messages

    private val _hasMore = MutableLiveData<Boolean>(true)
    val hasMore: LiveData<Boolean> = _hasMore

    fun loadMessage(){
        setLoading(true)
        currentPage = 1 // Reset la pagina 1 pentru încărcarea inițială
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getComments(taskId, pageSize, currentPage)
                if (response.success) {
                    val newMessages = UIMapperService.mapCommentsFromServer(response.data!!, false)
                    // Adaugă separatorii de timp înainte de a seta lista
                    val messagesWithSeparators = addTimeSeparators(newMessages)
                    _messages.value = messagesWithSeparators

                    // Verifică dacă mai sunt comentarii (dacă numărul returnat este egal cu pageSize)
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
        currentPage++ // Incrementează la următoarea pagină

        viewModelScope.launch {
            try {
                val response = RetrofitClient.getComments(taskId, pageSize, currentPage)
                if (response.success) {
                    val newMessages = UIMapperService.mapCommentsFromServer(response.data!!, false)

                    if (newMessages.isNotEmpty()) {
                        // Obține lista curentă fără separatori
                        val currentList = _messages.value?.filterIsInstance<MessageItem>()?.toMutableList()
                            ?: mutableListOf()
                        // Adaugă noile mesaje
                        currentList.addAll(newMessages)

                        // Reaplică separatorii de timp pentru întreaga listă
                        val messagesWithSeparators = addTimeSeparators(currentList)
                        _messages.value = messagesWithSeparators

                        // Verifică dacă mai sunt comentarii de încărcat
                        _hasMore.value = newMessages.size == pageSize
                    } else {
                        _hasMore.value = false
                    }
                } else {
                    Log.e("API", "Load more failed: ${response.statusCode}")
                    _hasMore.value = false
                    currentPage-- // Revenire la pagina anterioară în caz de eroare
                }

            } catch (e: Exception) {
                Log.e("API", "Exception loading more: ${e.message}")
                _hasMore.value = false
                currentPage-- // Revenire la pagina anterioară în caz de eroare
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun addMessageToListFromServer(comment: CommentResponse){
        shouldScrollToNewMessage = true
        val newMessage = UIMapperService.mapCommentsFromServer(listOf(comment), false)

        // Obține doar mesajele (fără separatori) din lista curentă
        val currentMessages = _messages.value?.filterIsInstance<MessageItem>()?.toMutableList()
            ?: mutableListOf()

        // Adaugă noul mesaj la începutul listei
        newMessage.forEach { message ->
            if (message is MessageItem) {
                currentMessages.add(0, message)
            }
        }

        // Reaplică separatorii de timp
        val messagesWithSeparators = addTimeSeparators(currentMessages)
        _messages.value = messagesWithSeparators
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

    private fun addTimeSeparators(messages: List<IMessage>): List<IMessage> {
        if (messages.isEmpty()) return messages

        val result = mutableListOf<IMessage>()
        var lastDate: Date? = null

        // Sortează mesajele după timestamp (cel mai nou primul)
//        val sortedMessages = messages.sortedByDescending { message ->
//            when (message) {
//                is MessageItem -> message.timestamp
//                is MessageTimeSeparator -> message.timestamp
//                else -> Date(0) // fallback pentru alte tipuri
//            }
//        }

        messages.forEachIndexed { index, message ->
            val currentDate = when (message) {
                is MessageItem -> message.timestamp
                is MessageTimeSeparator -> message.timestamp
                else -> null
            }

            currentDate?.let { current ->
                // Verifică dacă data curentă este diferită de cea anterioară
                lastDate?.let { last ->
                    if (!isSameDay(current, last)) {
                        // Adaugă separatorul ÎNAINTE de mesajele din noua zi
                        // Separatorul arată data pentru mesajele care urmează
                        result.add(MessageTimeSeparator(last))
                    }
                }

                lastDate = current
            }

            // Adaugă mesajul doar dacă nu este deja un MessageTimeSeparator duplicat
            if (message !is MessageTimeSeparator) {
                result.add(message)
            }

            val isLast = index == messages.lastIndex
            if(isLast && !isSameDay(currentDate!!, Date(System.currentTimeMillis()))){
                result.add(MessageTimeSeparator(currentDate!!))
            }
        }

        return result
    }

    // Funcție helper pentru a verifica dacă două Date-uri sunt în aceeași zi
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance()
        val cal2 = java.util.Calendar.getInstance()

        cal1.time = date1
        cal2.time = date2

        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH) &&
                cal1.get(java.util.Calendar.DAY_OF_MONTH) == cal2.get(java.util.Calendar.DAY_OF_MONTH)
    }
}