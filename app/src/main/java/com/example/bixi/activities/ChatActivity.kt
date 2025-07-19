package com.example.bixi.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.adapters.AttachmentPreviewAdapter
import com.example.bixi.adapters.MessageAdapter
import com.example.bixi.databinding.ActivityChatBinding
import com.example.bixi.helper.AttachmentSelectionHelper
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.models.Attachment
import com.example.bixi.viewModels.ChatViewModel

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var attachmentPreviewAdapter: AttachmentPreviewAdapter
    private val selectedAttachments = mutableListOf<Attachment>()

    // Sistem centralizat pentru atașamente
    private lateinit var attachmentHelper: AttachmentSelectionHelper

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLoadingOverlay()

        // Inițializează helper-ul pentru atașamente
        setupAttachmentHelper()

        setStyles()
        setupRecyclerView()
        setupAttachmentPreview()
        initListeners()

        observeMessages()
        setupViewModel()
    }

    private fun setupAttachmentHelper() {
        attachmentHelper = AttachmentSelectionHelper(this) { attachments ->
            selectedAttachments.addAll(attachments)
            updateAttachmentPreview()
        }
        attachmentHelper.initialize()
    }

    private fun setupViewModel(){
        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.loadMessages()
    }

    private fun setupAttachmentPreview() {
        attachmentPreviewAdapter = AttachmentPreviewAdapter { attachment ->
            removeAttachment(attachment)
        }

        binding.rvAttachmentPreview.apply {
            adapter = attachmentPreviewAdapter
            layoutManager = LinearLayoutManager(
                this@ChatActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                reverseLayout = true
                stackFromEnd = false
            }
            adapter = messageAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (!viewModel.isLoading.value!! && viewModel.hasMore.value!! &&
                        lastVisibleItem + 5 >= totalItemCount) {
                        viewModel.loadMoreMessages()
                    }
                }
            })
        }
    }

    private fun initListeners() {
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty() || selectedAttachments.isNotEmpty()) {
                viewModel.sendMessage(messageText, selectedAttachments.toList())
                binding.etMessage.text.clear()
                selectedAttachments.clear()
                updateAttachmentPreview()
            }
        }

        binding.btnAttachment.setOnClickListener {
            attachmentHelper.showAttachmentPicker()
        }

        // Add text watcher to update send button state
        binding.etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateSendButtonState()
            }
        })
    }

    private fun observeMessages() {
        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator
        }
    }

    private fun updateAttachmentPreview() {
        if (selectedAttachments.isNotEmpty()) {
            binding.attachmentPreviewContainer.visibility = View.VISIBLE
            attachmentPreviewAdapter.submitList(selectedAttachments.toList())
        } else {
            binding.attachmentPreviewContainer.visibility = View.GONE
        }

        updateSendButtonState()
    }

    private fun removeAttachment(attachment: Attachment) {
        selectedAttachments.remove(attachment)
        updateAttachmentPreview()
    }

    private fun updateSendButtonState() {
        val hasText = binding.etMessage.text.toString().trim().isNotEmpty()
        val hasAttachments = selectedAttachments.isNotEmpty()
        binding.btnSend.isEnabled = hasText || hasAttachments

        binding.btnSend.alpha = if (hasText || hasAttachments) 1.0f else 0.5f
    }

    private fun setStyles(){
        BackgroundStylerService.setRoundedBackground(
            view = binding.btnSend,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_surfaceContainer_highContrast),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )
        BackgroundStylerService.setRoundedBackground(
            view = binding.btnAttachment,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_surfaceContainer_highContrast),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )

        BackgroundStylerService.setRoundedBackground(
            view = binding.etMessage,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_surfaceContainer_highContrast),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )
    }
}