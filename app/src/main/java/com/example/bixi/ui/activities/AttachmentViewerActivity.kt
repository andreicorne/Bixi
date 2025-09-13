package com.example.bixi.ui.activities

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import java.io.File
import androidx.core.net.toUri
import com.bumptech.glide.Priority
import com.example.bixi.helper.AttachmentOpenExternalHelper
import com.example.bixi.models.AttachmentItem

class AttachmentViewerActivity : BaseActivity() {

    private lateinit var binding: ActivityAttachmentViewerBinding

    private var attachmentUri: Uri? = null
    private var attachmentType: AttachmentType = AttachmentType.UNKNOWN
    private var attachmentServerData: AttachmentResponse? = null
    private var attachmentName: String = ""
    private var isFirstTime: Boolean = true

    private lateinit var imageZoomHelper: ImageZoomHelper

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
    }

    override fun onResume() {
        super.onResume()

        if(isFirstTime){
            isFirstTime = false
            loadAttachment()
        }
    }

    private fun setupFullscreenMode() {
        if (attachmentType == AttachmentType.IMAGE) {
            var isFullscreen = false

            binding.ivImage.setOnClickListener {
                isFullscreen = !isFullscreen
                if (isFullscreen) {
                    binding.toolbar.visibility = View.GONE
                    binding.vToolbarShadow.visibility = View.GONE
                    window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )
                } else {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.vToolbarShadow.visibility = View.VISIBLE
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    private fun getIntentData() {
        val uriString = intent.getStringExtra(NavigationConstants.ATTACHMENT_URI)
        attachmentUri = uriString?.toUri()

        val typeString = intent.getStringExtra(NavigationConstants.ATTACHMENT_TYPE)
        attachmentType = try {
            AttachmentType.valueOf(typeString ?: "UNKNOWN")
        } catch (e: Exception) {
            AttachmentType.UNKNOWN
        }

        attachmentName = intent.getStringExtra(NavigationConstants.ATTACHMENT_NAME) ?: "Document"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            attachmentServerData = intent.getParcelableExtra(NavigationConstants.ATTACHMENT_SERVER_DATA,
                AttachmentResponse::class.java)
        }
        else{
            attachmentServerData = intent.getParcelableExtra(NavigationConstants.ATTACHMENT_SERVER_DATA)
        }
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
            AttachmentOpenExternalHelper.open(this, AttachmentItem( uri = attachmentUri, attachmentType,
                attachmentServerData, false))
        }
    }

    private fun loadAttachment() {
        showLoading(true)

        when (attachmentType) {
            AttachmentType.IMAGE -> {
                loadImage()
            }
            AttachmentType.VIDEO,
            AttachmentType.AUDIO,
            AttachmentType.DOCUMENT -> {
                loadDocument()
            }
            AttachmentType.UNKNOWN -> {
                showError(getString(R.string.unknown_file_type))
            }
        }
    }

    private fun loadImage() {
        binding.imageContainer.visibility = View.VISIBLE
        binding.documentContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE

        imageZoomHelper = ImageZoomHelper(binding.ivImage)

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
            .priority(Priority.HIGH)
            .override(200)
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
                    showError(getString(R.string.couldnt_load_image))
                }
            })
    }

    private fun loadDocument() {
        binding.imageContainer.visibility = View.GONE
        binding.documentContainer.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE

        setupWebView()

        val uri = attachmentUri
        if (uri == null) {
            showError(getString(R.string.document_not_found))
            return
        }

        loadRemoteDocument(uri.toString())
    }

    private fun loadRemoteDocument(url: String) {
        runOnUiThread {
            val docHtml = """
        <html>
            <body style="margin:0;padding:0;">
                <iframe src="https://docs.google.com/viewer?url=$url&embedded=true" 
                        width="100%" height="100%" style="border:none;"></iframe>
            </body>
        </html>
    """.trimIndent()
            binding.webView.loadDataWithBaseURL(null, docHtml, "text/html",  "UTF-8", null);
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
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)

                    showLoading(false)
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)

                    showLoading(false)
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

        Log.e("AttachmentViewer", "Error: $message")
        Log.e("AttachmentViewer", "URI: $attachmentUri")
        Log.e("AttachmentViewer", "Type: $attachmentType")
        Log.e("AttachmentViewer", "Name: $attachmentName")
    }

    private fun downloadAttachment() {
        val uri = attachmentUri ?: return

        try {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

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

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)

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

    override fun onDestroy() {
        super.onDestroy()
        // Curăță fișierele temporare
        try {
            val tempDir = File(cacheDir, "temp_documents")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("temp_doc_")) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AttachmentViewer", "Error cleaning temp files", e)
        }
    }
}