package com.example.bixi.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bixi.models.Attachment
import com.example.bixi.models.AttachmentType
import com.example.bixi.models.Message
import com.example.bixi.models.MessagePage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasMore = MutableLiveData<Boolean>(true)
    val hasMore: LiveData<Boolean> = _hasMore

    private var currentPage = 0
    private val allMessages = mutableListOf<Message>()

    fun loadMessages() {
        if (_isLoading.value == true) return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Simulate API call
                delay(1000)

                val newMessages = fetchMessagesFromServer(0)
                allMessages.clear()
                allMessages.addAll(newMessages.messages)
                _messages.value = allMessages.toList()
                _hasMore.value = newMessages.hasNext
                currentPage = 0
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreMessages() {
        if (_isLoading.value == true || _hasMore.value == false) return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Simulate API call
                delay(1000)

                val nextPage = currentPage + 1
                val newMessages = fetchMessagesFromServer(nextPage)
                allMessages.addAll(newMessages.messages)
                _messages.value = allMessages.toList()
                _hasMore.value = newMessages.hasNext
                currentPage = nextPage
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val newMessage = Message(
                id = UUID.randomUUID().toString(),
                text = text,
                timestamp = Date(),
                attachments = emptyList(),
                isFromCurrentUser = true
            )

            // Add message optimistically
            allMessages.add(0, newMessage)
            _messages.value = allMessages.toList()

            // Simulate sending to server
            try {
                delay(500)
                // API call to send message
            } catch (e: Exception) {
                // Handle error - remove message or show retry
            }
        }
    }

    // Simulate server response - replace with actual API call
    private fun fetchMessagesFromServer(page: Int): MessagePage {
        val messagesPerPage = 20
        val totalMessages = 100
        val start = page * messagesPerPage
        val end = minOf(start + messagesPerPage, totalMessages)

        val messages = (start until end).map { index ->
            Message(
                id = "msg_$index",
                text = "Message $index: This is a sample message with some content",
                timestamp = Date(System.currentTimeMillis() - (index * 60000)),
                attachments = if (index % 5 == 0) {
                    listOf(
                        Attachment(
                            id = "att_$index",
                            url = "https://example.com/file$index.pdf",
                            type = AttachmentType.DOCUMENT,
                            name = "Document_$index.pdf",
                            size = 1024 * 100
                        )
                    )
                } else emptyList(),
                isFromCurrentUser = index % 3 == 0
            )
        }

        return MessagePage(
            messages = messages,
            currentPage = page,
            totalPages = (totalMessages + messagesPerPage - 1) / messagesPerPage,
            hasNext = end < totalMessages
        )
    }
}