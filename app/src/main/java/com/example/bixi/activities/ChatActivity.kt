package com.example.bixi.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.adapters.AttachmentPreviewAdapter
import com.example.bixi.adapters.MessageAdapter
import com.example.bixi.databinding.ActivityChatBinding
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.models.Attachment
import com.example.bixi.models.AttachmentType
import com.example.bixi.viewModels.ChatViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var attachmentPreviewAdapter: AttachmentPreviewAdapter
    private var photoFile: File? = null
    private val selectedAttachments = mutableListOf<Attachment>()

    // Activity Result Launchers
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedImages(uris)
        }
    }

    private val documentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleSelectedDocument(it) }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoFile?.let { file ->
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                handleSelectedImages(listOf(uri))
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraPermission = permissions[android.Manifest.permission.CAMERA] ?: false
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }

        if (!cameraPermission && !storagePermission) {
            Toast.makeText(this, "Permissions are required for attachments", Toast.LENGTH_SHORT).show()
        }
    }

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
        setupAttachmentPreview()
        initListeners()
        requestPermissions()

        // Observe messages
        observeMessages()
        setupViewModel()
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
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@ChatActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
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
            openAttachmentPicker()
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

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun openAttachmentPicker() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_attachment_picker, null)
        bottomSheetDialog.setContentView(view)

        view.findViewById<androidx.cardview.widget.CardView>(R.id.cardCamera)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            openCamera()
        }

        view.findViewById<androidx.cardview.widget.CardView>(R.id.cardGallery)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            openGallery()
        }

        view.findViewById<androidx.cardview.widget.CardView>(R.id.cardDocument)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            openDocumentPicker()
        }

        bottomSheetDialog.show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFileName = "photo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"

        // Creează directorul pentru photos în storage-ul intern
        val photoDir = File(filesDir, "photos")
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }

        photoFile = File(photoDir, photoFileName)

        try {
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile!!
            )
            cameraLauncher.launch(photoURI)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Error creating photo URI: ${e.message}", Toast.LENGTH_SHORT).show()
            // Log pentru debugging
            android.util.Log.e("ChatActivity", "FileProvider error", e)
            android.util.Log.e("ChatActivity", "Photo file path: ${photoFile?.absolutePath}")
        }
    }

    private fun openGallery() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show()
            return
        }

        galleryLauncher.launch("image/*")
    }

    private fun openDocumentPicker() {
        documentLauncher.launch("*/*")
    }

    private fun handleSelectedImages(uris: List<Uri>) {
        uris.forEach { uri ->
            val fileName = getFileName(uri) ?: "image_${System.currentTimeMillis()}.jpg"
            val fileSize = getFileSize(uri)

            val attachment = Attachment(
                id = UUID.randomUUID().toString(),
                url = uri.toString(),
                type = AttachmentType.IMAGE,
                name = fileName,
                size = fileSize
            )

            selectedAttachments.add(attachment)
        }

        updateAttachmentPreview()
    }

    private fun handleSelectedDocument(uri: Uri) {
        val fileName = getFileName(uri) ?: "document_${System.currentTimeMillis()}"
        val fileSize = getFileSize(uri)
        val mimeType = contentResolver.getType(uri)

        val attachmentType = when {
            mimeType?.startsWith("image/") == true -> AttachmentType.IMAGE
            mimeType?.startsWith("video/") == true -> AttachmentType.VIDEO
            mimeType?.startsWith("audio/") == true -> AttachmentType.AUDIO
            else -> AttachmentType.DOCUMENT
        }

        val attachment = Attachment(
            id = UUID.randomUUID().toString(),
            url = uri.toString(),
            type = attachmentType,
            name = fileName,
            size = fileSize
        )

        selectedAttachments.add(attachment)
        updateAttachmentPreview()
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun getFileSize(uri: Uri): Long? {
        var fileSize: Long? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = it.getLong(sizeIndex)
                }
            }
        }
        return fileSize
    }

    private fun updateAttachmentPreview() {
        if (selectedAttachments.isNotEmpty()) {
            binding.attachmentPreviewContainer.visibility = View.VISIBLE
            attachmentPreviewAdapter.submitList(selectedAttachments.toList())
        } else {
            binding.attachmentPreviewContainer.visibility = View.GONE
        }

        // Update send button state
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

        // Optional: Change send button appearance based on state
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