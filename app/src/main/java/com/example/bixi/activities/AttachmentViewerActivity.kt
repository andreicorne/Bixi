package com.example.bixi.activities

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.bixi.R
import com.example.bixi.constants.NavigationConstants
import com.example.bixi.databinding.ActivityAttachmentViewerBinding
import com.example.bixi.enums.AttachmentType
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.ImageZoomHelper
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.models.api.AttachmentResponse

class AttachmentViewerActivity : BaseActivity() {

    private lateinit var binding: ActivityAttachmentViewerBinding

    private var attachmentUri: Uri? = null
    private var attachmentType: AttachmentType = AttachmentType.UNKNOWN
    private var attachmentServerData: AttachmentResponse? = null
    private var attachmentName: String = ""

    private lateinit var imageZoomHelper: ImageZoomHelper

    companion object {
        fun startActivity(
            context: Context,
            uri: Uri?,
            type: AttachmentType,
            serverData: AttachmentResponse? = null,
            name: String = ""
        ) {
            val intent = Intent(context, AttachmentViewerActivity::class.java).apply {
                putExtra(NavigationConstants.ATTACHMENT_URI, uri.toString())
                putExtra(NavigationConstants.ATTACHMENT_TYPE, type.name)
                putExtra(NavigationConstants.ATTACHMENT_NAME, name)
                if (serverData != null) {
                    putExtra(NavigationConstants.ATTACHMENT_SERVER_DATA, serverData)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAttachmentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLoadingOverlay()

        getIntentData()
        initToolbar()
        initListeners()
        setStyles()
        setupFullscreenMode()
        loadAttachment()
    }

    private fun setupFullscreenMode() {
        // Pentru imagini, permite full screen la tap pe toolbar
        if (attachmentType == AttachmentType.IMAGE) {
            var isFullscreen = false

            binding.ivImage.setOnClickListener {
                isFullscreen = !isFullscreen
                if (isFullscreen) {
                    // Hide toolbar
                    binding.toolbar.visibility = View.GONE
                    binding.vToolbarShadow.visibility = View.GONE
                    window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )
                } else {
                    // Show toolbar
                    binding.toolbar.visibility = View.VISIBLE
                    binding.vToolbarShadow.visibility = View.VISIBLE
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    private fun getIntentData() {
        val uriString = intent.getStringExtra(NavigationConstants.ATTACHMENT_URI)
        attachmentUri = if (uriString != null) Uri.parse(uriString) else null

        val typeString = intent.getStringExtra(NavigationConstants.ATTACHMENT_TYPE)
        attachmentType = try {
            AttachmentType.valueOf(typeString ?: "UNKNOWN")
        } catch (e: Exception) {
            AttachmentType.UNKNOWN
        }

        attachmentName = intent.getStringExtra(NavigationConstants.ATTACHMENT_NAME) ?: "Document"
        attachmentServerData = intent.getParcelableExtra(NavigationConstants.ATTACHMENT_SERVER_DATA)
    }

    private fun initToolbar() {
        binding.tvTitle.text = attachmentName
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initListeners() {
        binding.ivDownload.setOnClickListener {
            downloadAttachment()
        }

        binding.ivShare.setOnClickListener {
            shareAttachment()
        }

        binding.ivOpenExternal.setOnClickListener {
            openInExternalApp()
        }
    }

    private fun loadAttachment() {
        showLoading(true)

        when (attachmentType) {
            AttachmentType.IMAGE -> {
                loadImage()
            }
            AttachmentType.DOCUMENT -> {
                loadDocument()
            }
            AttachmentType.UNKNOWN -> {
                showError("Tip de fișier necunoscut")
            }
        }
    }

    private fun loadImage() {
        binding.imageContainer.visibility = View.VISIBLE
        binding.documentContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE

        // Initialize zoom helper
        imageZoomHelper = ImageZoomHelper(binding.ivImage)

        // Wait for ImageView to be laid out before loading image
        if (binding.ivImage.width == 0 || binding.ivImage.height == 0) {
            binding.ivImage.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.ivImage.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    loadImageWithGlide()
                }
            })
        } else {
            loadImageWithGlide()
        }
    }

    private fun loadImageWithGlide() {
        Glide.with(this)
            .asDrawable()
            .load(attachmentUri)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_error)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    imageZoomHelper.setImage(resource)
                    showLoading(false)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    imageZoomHelper.setImage(placeholder)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    showError("Nu s-a putut încărca imaginea")
                }
            })
    }

    private fun loadDocument() {
        binding.imageContainer.visibility = View.GONE
        binding.documentContainer.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE

        setupWebView()

        // Pentru PDF-uri și documente, încercăm să le încărcăm în WebView
        val url = attachmentUri.toString()

        when {
            url.contains(".pdf", ignoreCase = true) -> {
                // Pentru PDF-uri, folosim Google Docs Viewer
                val googleDocsUrl = "https://docs.google.com/gviewer?embedded=true&url=$url"
                binding.webView.loadUrl(googleDocsUrl)
            }
            url.contains(".doc", ignoreCase = true) ||
                    url.contains(".docx", ignoreCase = true) ||
                    url.contains(".xls", ignoreCase = true) ||
                    url.contains(".xlsx", ignoreCase = true) -> {
                // Pentru documente Office, folosim Google Docs Viewer
                val googleDocsUrl = "https://docs.google.com/gviewer?embedded=true&url=$url"
                binding.webView.loadUrl(googleDocsUrl)
            }
            else -> {
                // Pentru alte tipuri, încarcă direct
                binding.webView.loadUrl(url)
            }
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    showLoading(false)
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    showError("Nu s-a putut încărca documentul")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressBar.progress = newProgress
                    binding.progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showError(message: String) {
        showLoading(false)
        binding.imageContainer.visibility = View.GONE
        binding.documentContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun downloadAttachment() {
        try {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val uri = attachmentUri ?: return
            val request = DownloadManager.Request(uri).apply {
                setTitle(attachmentName)
                setDescription("Se descarcă $attachmentName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, attachmentName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            downloadManager.enqueue(request)
            Toast.makeText(this, "Descărcarea a început", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("AttachmentViewer", "Error downloading file", e)
            Toast.makeText(this, "Nu s-a putut descărca fișierul", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareAttachment() {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = when (attachmentType) {
                    AttachmentType.IMAGE -> "image/*"
                    AttachmentType.DOCUMENT -> "*/*"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_STREAM, attachmentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Partajează fișierul"))
        } catch (e: Exception) {
            Toast.makeText(this, "Nu s-a putut partaja fișierul", Toast.LENGTH_SHORT).show()
            Log.e("AttachmentViewer", "Error sharing file", e)
        }
    }

    private fun openInExternalApp() {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(attachmentUri, getMimeType())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Nu s-a găsit o aplicație pentru a deschide acest fișier", Toast.LENGTH_SHORT).show()
            Log.e("AttachmentViewer", "Error opening external app", e)
        }
    }

    private fun getMimeType(): String {
        return when (attachmentType) {
            AttachmentType.IMAGE -> "image/*"
            AttachmentType.DOCUMENT -> {
                attachmentServerData?.type ?: contentResolver.getType(attachmentUri!!) ?: "*/*"
            }
            else -> "*/*"
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)

        // Notify zoom helper about orientation change
        if (attachmentType == AttachmentType.IMAGE && ::imageZoomHelper.isInitialized) {
            binding.ivImage.post {
                imageZoomHelper.onViewSizeChanged()
            }
        }
    }

    private fun setStyles(){
        BackgroundStylerService.setRoundedBackground(
            view = binding.ivBack,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )

        BackgroundStylerService.setRoundedBackground(
            view = binding.ivDownload,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )

        BackgroundStylerService.setRoundedBackground(
            view = binding.ivShare,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )

        BackgroundStylerService.setRoundedBackground(
            view = binding.ivOpenExternal,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )
    }
}