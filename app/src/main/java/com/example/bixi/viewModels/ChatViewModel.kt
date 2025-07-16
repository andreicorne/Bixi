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

class ChatViewModel : BaseViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _hasMore = MutableLiveData<Boolean>(true)
    val hasMore: LiveData<Boolean> = _hasMore

    private var currentPage = 0
    private val allMessages = mutableListOf<Message>()

    fun loadMessages() {
        if (isLoading.value == true) return

        viewModelScope.launch {
            setLoading(true)

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
                setLoading(false)
            }
        }
    }

    fun loadMoreMessages() {
        if (isLoading.value == true || _hasMore.value == false) return

        viewModelScope.launch {
            setLoading(true)

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
                setLoading(false)
            }
        }
    }

    fun sendMessage(text: String, attachments: List<Attachment> = emptyList()) {
        viewModelScope.launch {
            val newMessage = Message(
                id = UUID.randomUUID().toString(),
                text = text,
                timestamp = Date(),
                attachments = attachments,
                isFromCurrentUser = true
            )

            // Add message optimistically
            allMessages.add(0, newMessage)
            _messages.value = allMessages.toList()

            // Simulate sending to server
            try {
                delay(500)
                // API call to send message with attachments
                // Here you would upload the attachments first, then send the message
                uploadAttachments(attachments)
            } catch (e: Exception) {
                // Handle error - remove message or show retry
                allMessages.removeAt(0)
                _messages.value = allMessages.toList()
            }
        }
    }

    private suspend fun uploadAttachments(attachments: List<Attachment>) {
        // Simulate attachment upload
        attachments.forEach { attachment ->
            delay(200) // Simulate upload time per attachment
            // Here you would upload each attachment to your server
            // and update the attachment URL with the server URL
        }
    }

    // Simulate server response - replace with actual API call
    private fun fetchMessagesFromServer(page: Int): MessagePage {
        val messagesPerPage = 20
        val totalMessages = 100
        val start = page * messagesPerPage
        val end = minOf(start + messagesPerPage, totalMessages)

        val messages = (start until end).map { index ->
            val attachments = when {
                index % 8 == 0 -> listOf(
                    Attachment(
                        id = "att_img_$index",
                        url = "https://picsum.photos/400/300?random=$index",
                        type = AttachmentType.IMAGE,
                        name = "image_$index.jpg",
                        size = 1024 * 200
                    )
                )
                index % 12 == 0 -> listOf(
                    Attachment(
                        id = "att_doc_$index",
                        url = "https://example.com/document$index.pdf",
                        type = AttachmentType.DOCUMENT,
                        name = "Document_$index.pdf",
                        size = 1024 * 500
                    )
                )
                index % 15 == 0 -> listOf(
                    Attachment(
                        id = "att_img1_$index",
                        url = "https://picsum.photos/400/300?random=${index}a",
                        type = AttachmentType.IMAGE,
                        name = "image1_$index.jpg",
                        size = 1024 * 180
                    ),
                    Attachment(
                        id = "att_img2_$index",
                        url = "https://picsum.photos/400/300?random=${index}b",
                        type = AttachmentType.IMAGE,
                        name = "image2_$index.jpg",
                        size = 1024 * 220
                    )
                )
                else -> emptyList()
            }

            Message(
                id = "msg_$index",
                text = if (attachments.isNotEmpty()) {
                    when {
                        attachments.size > 1 -> "In mesajul asta am trimis niste poze"
                        attachments.first().type == AttachmentType.IMAGE -> "Iti trimit poza asta"
                        else -> "Aici iti trimit documentul"
                    }
                } else {
                    "Mesaj $index: Mesaj simplu aici. Ii un simplu mesaj, fara continut special"
                },
                timestamp = Date(System.currentTimeMillis() - (index * 60000)),
                attachments = attachments,
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