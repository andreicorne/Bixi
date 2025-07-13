package com.example.bixi.activities

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.adapters.MessageAdapter
import com.example.bixi.databinding.ActivityChatBinding
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.viewModels.ChatViewModel

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()

    private lateinit var messageAdapter: MessageAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLoadingOverlay()

        setStyles()
        setupRecyclerView()
        initListeners()

        // Observe messages
        observeMessages()

        // Load initial messages
        viewModel.loadMessages()
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
            if (messageText.isNotEmpty()) {
                viewModel.sendMessage(messageText)
                binding.etMessage.text.clear()
            }
        }

        binding.btnAttachment.setOnClickListener {
            // Open attachment picker
            openAttachmentPicker()
        }
    }

    private fun observeMessages() {
        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator
        }
    }

    private fun openAttachmentPicker() {
        // TODO: Implement attachment picker
        // This could open a dialog or bottom sheet to select files/images
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