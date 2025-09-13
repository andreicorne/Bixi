package com.example.bixi.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.AppSession
import com.example.bixi.R
import com.example.bixi.constants.NavigationConstants
import com.example.bixi.ui.adapters.AttachmentPreviewAdapter
import com.example.bixi.databinding.ActivityChatBinding
import com.example.bixi.helper.ApiStatus
import com.example.bixi.helper.AttachmentOpenExternalHelper
import com.example.bixi.helper.AttachmentSelectionHelper
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.ExtensionHelper
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.helper.ResponseStatusHelper
import com.example.bixi.helper.Utils
import com.example.bixi.models.AttachmentHandler
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.api.CreateCommentRequest
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.UIMapperService
import com.example.bixi.ui.adapters.CommentsAdapter
import com.example.bixi.viewModels.ChatViewModel
import kotlinx.coroutines.launch

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: CommentsAdapter
    private lateinit var attachmentPreviewAdapter: AttachmentPreviewAdapter

    // Sistem centralizat pentru ataÈ™amente
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

        viewModel.taskId = intent.getStringExtra(NavigationConstants.TASK_ID_NAV).toString()

        // IniÈ›ializeazÄƒ helper-ul pentru ataÈ™amente
        setupAttachmentHelper()

        setStyles()
        setupRecyclerView()
        setupAttachmentPreview()
        initListeners()

        setupViewModel()
    }

    private fun setupAttachmentHelper() {
        attachmentHelper = AttachmentSelectionHelper(this) { attachments ->
            handleAttachmentsFromHelper(attachments)
            updateAttachmentPreview()
        }
        attachmentHelper.initialize()
    }

    private fun handleAttachmentsFromHelper(attachments: List<AttachmentHandler>) {
        attachments.forEach { attachment ->
            viewModel.addAttachment(attachment)
        }
    }

    private fun setupViewModel(){
        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages){

                if(!viewModel.shouldScrollToNewMessage){
                    return@submitList
                }
                viewModel.shouldScrollToNewMessage = false
                runOnUiThread {
                    // Scroll doar la poziÈ›ia 0 dacÄƒ este prima Ã®ncÄƒrcare (lista goalÄƒ)
                    if (messageAdapter.itemCount <= messages.size) {
                        binding.rvMessages.scrollToPosition(0)
                    }
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.sendResponseCode.observe(this, Observer { statusCode ->
            ResponseStatusHelper.showStatusMessage(this, statusCode)
            showLoading(false)
        })

        // Observer pentru hasMore status (opÈ›ional, pentru debugging)
        viewModel.hasMore.observe(this) { hasMore ->
            Log.d("ChatActivity", "Has more messages: $hasMore")
        }

        viewModel.loadMessage()
    }

    private fun setupAttachmentPreview() {
        attachmentPreviewAdapter = AttachmentPreviewAdapter { attachment ->
            removeAttachment(attachment.id)
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
        messageAdapter = CommentsAdapter(
            openAttachmentListener = { attachment ->
                openAttachment(attachment)
            }
        )
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

                    // VerificÄƒ dacÄƒ trebuie sÄƒ Ã®ncarce mai multe mesaje
                    // Threshold de 5 elemente Ã®nainte de sfÃ¢rÈ™itul listei
                    if (!viewModel.isLoading.value!! &&
                        viewModel.hasMore.value!! &&
                        totalItemCount > 0 &&
                        lastVisibleItem + 5 >= totalItemCount) {

                        Log.d("ChatActivity", "Loading more messages from next page...")
                        viewModel.loadMoreMessages()
                    }
                }
            })
        }
    }

    fun openAttachment(attachment: AttachmentItem){
        if(attachment.isFromStorage){
            AttachmentOpenExternalHelper.open(this, attachment)
        }
        else{
            val fileName = attachment.serverData?.name ?: Utils.getFileName(this, attachment.uri!!)
            when (ExtensionHelper.getExtension(fileName)){
                "csv", "xlsx", "xls" -> AttachmentOpenExternalHelper.open(this, attachment)
                else -> {
                    val intent = Intent(this, AttachmentViewerActivity::class.java).apply {
                        putExtra(NavigationConstants.ATTACHMENT_URI, attachment.uri.toString())
                        putExtra(NavigationConstants.ATTACHMENT_TYPE, attachment.type.name)
                        putExtra(NavigationConstants.ATTACHMENT_NAME, fileName)
                        if (attachment.serverData != null) {
                            putExtra(NavigationConstants.ATTACHMENT_SERVER_DATA, attachment.serverData)
                        }
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun initListeners() {
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty() || viewModel.attachments.value.isNotEmpty()) {

                binding.etMessage.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etMessage.windowToken, 0)

                sendComment()
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

    private fun sendComment() {
//        if(!isValidFields()){
//            return
//        }

        showLoading(true)
        lifecycleScope.launch {
            try {
                val createCommentRequest = CreateCommentRequest(AppSession.user!!.user.id,
                    AppSession.user!!.user.username, "user",
                    binding.etMessage.text.toString())

                val attachmentsItems: MutableList<AttachmentItem> = mutableListOf()
                viewModel.attachments.value.forEach { attachment ->
                    attachmentsItems.add(UIMapperService.toAttachmentItem(attachment))
                }

                val parts = Utils.prepareAttachments(baseContext, attachmentsItems)
                val response = RetrofitClient.sendComment(viewModel.taskId, createCommentRequest, parts)
                if (response.success) {
                    viewModel.addMessageToListFromServer(response.data!!)
                    onSuccessSend()
                } else {
                    ResponseStatusHelper.showStatusMessage(applicationContext, response.statusCode)
                }
                showLoading(false)

            } catch (e: Exception) {
                ResponseStatusHelper.showStatusMessage(applicationContext, ApiStatus.SERVER_ERROR.code)
                showLoading(false)
                Log.e("API", "Exception: ${e.message}")
            }
        }
    }

    private fun onSuccessSend(){
        binding.etMessage.text.clear()
        viewModel.clearAttachments()
        updateAttachmentPreview()
    }

    private fun updateAttachmentPreview() {
        if (viewModel.attachments.value.isNotEmpty()) {
            binding.attachmentPreviewContainer.visibility = View.VISIBLE
            attachmentPreviewAdapter.submitList(viewModel.attachments.value)
        } else {
            binding.attachmentPreviewContainer.visibility = View.GONE
        }

        updateSendButtonState()
    }

    private fun removeAttachment(attachmentId: String) {
        viewModel.removeAttachmentById(attachmentId)
        updateAttachmentPreview()
    }

    private fun updateSendButtonState() {
        val hasText = binding.etMessage.text.toString().trim().isNotEmpty()
        val hasAttachments = viewModel.attachments.value.isNotEmpty()
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