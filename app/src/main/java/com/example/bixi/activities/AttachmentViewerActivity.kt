// AttachmentViewerActivity.kt - Versiunea actualizată
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
import java.io.File
import java.io.FileOutputStream

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

        // Configurează vizibilitatea butoanelor bazat pe tipul de fișier
        setupButtonVisibility()
    }

    private fun setupButtonVisibility() {
        val uri = attachmentUri

        if (uri != null && isLocalUri(uri)) {
            // Pentru fișiere locale, ascunde butonul de download
            binding.ivDownload.visibility = View.GONE
            Log.d("AttachmentViewer", "Local file detected, hiding download button")
        } else {
            // Pentru fișiere de pe server, afișează butonul de download
            binding.ivDownload.visibility = View.VISIBLE
            Log.d("AttachmentViewer", "Remote file detected, showing download button")
        }

        // Butonul de share este întotdeauna vizibil
        binding.ivShare.visibility = View.VISIBLE

        // Butonul de deschidere externă este întotdeauna vizibil
        binding.ivOpenExternal.visibility = View.VISIBLE
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

        val uri = attachmentUri
        if (uri == null) {
            showError("URI invalid")
            return
        }

        // Verifică dacă este URI local sau de pe server
        if (isLocalUri(uri)) {
            loadLocalDocument(uri)
        } else {
            loadRemoteDocument(uri.toString())
        }
    }

    // Alternativă: Folosește Intent pentru PDF-uri
    private fun loadPdfWithIntent(uri: Uri) {
        try {
            // Creează o interfață care sugerează deschiderea externă
            val intentBasedHtml = createIntentBasedPdfViewer()
            val htmlFile = File(cacheDir, "intent_pdf_${System.currentTimeMillis()}.html")
            htmlFile.writeText(intentBasedHtml)

            binding.webView.loadUrl("file://${htmlFile.absolutePath}")
            showLoading(false)
        } catch (e: Exception) {
            Log.e("AttachmentViewer", "Error with intent-based PDF viewer", e)
            loadPdfWithCustomViewer(uri)
        }
    }

    private fun createIntentBasedPdfViewer(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$attachmentName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .container {
                    background: white;
                    border-radius: 20px;
                    padding: 40px;
                    text-align: center;
                    box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                    max-width: 450px;
                    color: #333;
                }
                .pdf-icon {
                    width: 100px;
                    height: 100px;
                    margin: 0 auto 30px;
                    background: linear-gradient(135deg, #dc2626, #ef4444);
                    border-radius: 20px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-size: 40px;
                    box-shadow: 0 10px 20px rgba(220, 38, 38, 0.3);
                }
                .title {
                    font-size: 28px;
                    font-weight: 700;
                    margin-bottom: 15px;
                    color: #1f2937;
                }
                .subtitle {
                    font-size: 18px;
                    color: #6b7280;
                    margin-bottom: 30px;
                    line-height: 1.6;
                }
                .file-name {
                    background: #f3f4f6;
                    padding: 20px;
                    border-radius: 12px;
                    font-family: 'SF Mono', 'Monaco', 'Cascadia Code', monospace;
                    word-break: break-all;
                    margin-bottom: 40px;
                    border: 2px solid #e5e7eb;
                    font-size: 14px;
                }
                .action-buttons {
                    display: flex;
                    gap: 15px;
                    justify-content: center;
                    flex-wrap: wrap;
                    margin-bottom: 30px;
                }
                .btn {
                    padding: 16px 32px;
                    border: none;
                    border-radius: 12px;
                    font-size: 16px;
                    font-weight: 600;
                    cursor: pointer;
                    text-decoration: none;
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;
                    transition: all 0.3s ease;
                    position: relative;
                    overflow: hidden;
                }
                .btn-primary {
                    background: linear-gradient(135deg, #dc2626, #ef4444);
                    color: white;
                    box-shadow: 0 8px 16px rgba(220, 38, 38, 0.3);
                }
                .btn-primary:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 12px 24px rgba(220, 38, 38, 0.4);
                }
                .btn-secondary {
                    background: #f8f9fa;
                    color: #374151;
                    border: 2px solid #e5e7eb;
                }
                .btn-secondary:hover {
                    background: #e5e7eb;
                    transform: translateY(-1px);
                }
                .feature-grid {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 15px;
                    margin-top: 30px;
                    text-align: left;
                }
                .feature-item {
                    padding: 15px;
                    background: #f8fafc;
                    border-radius: 8px;
                    border-left: 4px solid #dc2626;
                    font-size: 14px;
                    color: #374151;
                }
                .feature-icon {
                    margin-right: 8px;
                    font-size: 16px;
                }
                .note {
                    background: linear-gradient(135deg, #fef2f2, #fee2e2);
                    border: 1px solid #fca5a5;
                    border-radius: 12px;
                    padding: 20px;
                    margin-top: 30px;
                    color: #dc2626;
                    font-size: 14px;
                    line-height: 1.5;
                }
                .auto-open {
                    margin-top: 20px;
                    padding: 15px;
                    background: #e0f2fe;
                    border-radius: 8px;
                    color: #0369a1;
                    font-size: 14px;
                }
                .countdown {
                    font-weight: 600;
                    color: #dc2626;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="pdf-icon">📄</div>
                <div class="title">PDF Ready</div>
                <div class="subtitle">
                    Documentul tău PDF este pregătit pentru vizualizare într-o aplicație optimizată.
                </div>
                <div class="file-name">$attachmentName</div>
                
                <div class="action-buttons">
                    <button class="btn btn-primary" onclick="openPdfNow()">
                        <span class="feature-icon">🚀</span>
                        Deschide acum
                    </button>
                    <button class="btn btn-secondary" onclick="Android.shareFile()">
                        <span class="feature-icon">📤</span>
                        Partajează
                    </button>
                </div>
                
                <div class="auto-open">
                    <strong>⚡ Deschidere automată în <span class="countdown" id="countdown">5</span> secunde</strong>
                </div>
                
                <div class="feature-grid">
                    <div class="feature-item">
                        <span class="feature-icon">📖</span>
                        <strong>Vizualizare completă</strong><br>
                        Zoom, scroll și toate funcțiile PDF
                    </div>
                    <div class="feature-item">
                        <span class="feature-icon">⚡</span>
                        <strong>Performanță optimă</strong><br>
                        Încărcare rapidă și fluidă
                    </div>
                    <div class="feature-item">
                        <span class="feature-icon">🔍</span>
                        <strong>Căutare în text</strong><br>
                        Găsește rapid informația dorită
                    </div>
                    <div class="feature-item">
                        <span class="feature-icon">💾</span>
                        <strong>Salvare disponibilă</strong><br>
                        Salvează local pentru acces offline
                    </div>
                </div>
                
                <div class="note">
                    <strong>💡 De ce deschidere externă?</strong><br>
                    Aplicațiile dedicate PDF oferă o experiență superioară cu funcții avansate de zoom, căutare și navigare pe care WebView nu le poate egaliza.
                </div>
            </div>
            
            <script>
                let countdown = 5;
                const countdownElement = document.getElementById('countdown');
                
                const timer = setInterval(() => {
                    countdown--;
                    countdownElement.textContent = countdown;
                    
                    if (countdown <= 0) {
                        clearInterval(timer);
                        openPdfNow();
                    }
                }, 1000);
                
                function openPdfNow() {
                    clearInterval(timer);
                    Android.openExternal();
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun isLocalUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        return scheme == "content" || scheme == "file" ||
                uri.toString().startsWith("/") ||
                !uri.toString().startsWith("http")
    }

    private fun loadLocalDocument(uri: Uri) {
        try {
            val documentType = detectDocumentType(uri)
            Log.d("AttachmentViewer", "Detected document type: $documentType for file: $attachmentName")

            when (documentType) {
                DocumentType.PDF -> loadPdfDocument(uri)
                DocumentType.OFFICE_DOCUMENT -> loadOfficeDocument(uri)
                DocumentType.TEXT_DOCUMENT -> loadTextDocument(uri)
                DocumentType.IMAGE -> loadImageAsDocument(uri)
                DocumentType.UNSUPPORTED -> showUnsupportedDocument(uri)
            }

        } catch (e: Exception) {
            Log.e("AttachmentViewer", "Error loading local document", e)
            showError("Nu s-a putut încărca documentul: ${e.message}")
        }
    }

    private fun loadPdfDocument(uri: Uri) {
        try {
            // Încearcă mai multe metode pentru încărcarea PDF-ului
            val tempFile = createTempFileAndCopy(uri)

            // Metodă 1: Încearcă cu PDF.js (cea mai bună soluție)
            loadPdfWithPdfJs(tempFile)

        } catch (e: Exception) {
            Log.e("AttachmentViewer", "Error loading PDF", e)
            // Fallback la viewer HTML personalizat
            loadPdfWithCustomViewer(uri)
        }
    }

    private fun loadPdfWithPdfJs(file: File) {
        // Creează un viewer PDF.js pentru fișiere locale
        val pdfJsHtml = createPdfJsViewer(file)
        val htmlFile = File(cacheDir, "pdfjs_viewer_${System.currentTimeMillis()}.html")
        htmlFile.writeText(pdfJsHtml)

        binding.webView.loadUrl("file://${htmlFile.absolutePath}")
        showLoading(false)
    }

    private fun createPdfJsViewer(file: File): String {
        val fileSize = formatFileSize(file.length())
        val base64Data = encodeFileToBase64(file)

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$attachmentName</title>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background: #f5f5f5;
                    overflow-x: hidden;
                }
                .pdf-header {
                    background: white;
                    padding: 15px 20px;
                    border-bottom: 1px solid #e0e0e0;
                    position: sticky;
                    top: 0;
                    z-index: 100;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .pdf-info {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    flex-wrap: wrap;
                    gap: 10px;
                }
                .pdf-name {
                    font-weight: 600;
                    color: #dc2626;
                    font-size: 16px;
                }
                .pdf-details {
                    color: #666;
                    font-size: 14px;
                }
                .pdf-controls {
                    display: flex;
                    gap: 10px;
                    align-items: center;
                    margin-top: 10px;
                }
                .control-btn {
                    padding: 8px 12px;
                    border: 1px solid #ddd;
                    background: #f8f9fa;
                    border-radius: 4px;
                    cursor: pointer;
                    font-size: 14px;
                    transition: all 0.2s;
                }
                .control-btn:hover {
                    background: #e9ecef;
                }
                .control-btn:disabled {
                    opacity: 0.5;
                    cursor: not-allowed;
                }
                .page-info {
                    font-size: 14px;
                    color: #666;
                    margin: 0 10px;
                }
                .zoom-control {
                    display: flex;
                    gap: 5px;
                    align-items: center;
                }
                .pdf-container {
                    padding: 20px;
                    text-align: center;
                }
                .pdf-page {
                    background: white;
                    margin: 0 auto 20px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    border-radius: 4px;
                    max-width: 100%;
                    display: inline-block;
                }
                .loading {
                    text-align: center;
                    padding: 40px;
                    color: #666;
                }
                .error {
                    text-align: center;
                    padding: 40px;
                    color: #dc2626;
                    background: #fef2f2;
                    border: 1px solid #fecaca;
                    border-radius: 8px;
                    margin: 20px;
                }
                .fallback-actions {
                    display: flex;
                    gap: 15px;
                    justify-content: center;
                    margin-top: 20px;
                    flex-wrap: wrap;
                }
                .btn {
                    padding: 12px 24px;
                    border: none;
                    border-radius: 6px;
                    font-size: 16px;
                    cursor: pointer;
                    text-decoration: none;
                    display: inline-block;
                    transition: all 0.2s;
                    font-weight: 500;
                }
                .btn-primary {
                    background-color: #dc2626;
                    color: white;
                }
                .btn-secondary {
                    background-color: #f8f9fa;
                    color: #333;
                    border: 1px solid #dadce0;
                }
                .btn:hover {
                    transform: translateY(-1px);
                    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
                }
            </style>
        </head>
        <body>
            <div class="pdf-header">
                <div class="pdf-info">
                    <div>
                        <div class="pdf-name">📄 $attachmentName</div>
                        <div class="pdf-details">$fileSize • PDF Document</div>
                    </div>
                </div>
                <div class="pdf-controls">
                    <button class="control-btn" id="prevBtn" onclick="previousPage()">◀ Anterior</button>
                    <span class="page-info">
                        Pagina <span id="currentPage">1</span> din <span id="totalPages">-</span>
                    </span>
                    <button class="control-btn" id="nextBtn" onclick="nextPage()">Următor ▶</button>
                    <div class="zoom-control">
                        <button class="control-btn" onclick="zoomOut()">🔍-</button>
                        <span id="zoomLevel">100%</span>
                        <button class="control-btn" onclick="zoomIn()">🔍+</button>
                    </div>
                </div>
            </div>
            
            <div class="pdf-container" id="pdfContainer">
                <div class="loading" id="loadingDiv">
                    📄 Se încarcă PDF-ul...
                </div>
            </div>
            
            <script>
                let pdfDoc = null;
                let currentPage = 1;
                let totalPages = 0;
                let scale = 1.2;
                
                // Configurează PDF.js
                pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
                
                // Încarcă PDF-ul din date base64
                const pdfData = 'data:application/pdf;base64,$base64Data';
                
                pdfjsLib.getDocument(pdfData).promise.then(function(pdf) {
                    pdfDoc = pdf;
                    totalPages = pdf.numPages;
                    document.getElementById('totalPages').textContent = totalPages;
                    document.getElementById('loadingDiv').style.display = 'none';
                    
                    // Afișează prima pagină
                    renderPage(1);
                    updateControls();
                }).catch(function(error) {
                    console.error('Error loading PDF:', error);
                    showError();
                });
                
                function renderPage(pageNum) {
                    if (!pdfDoc) return;
                    
                    pdfDoc.getPage(pageNum).then(function(page) {
                        const canvas = document.createElement('canvas');
                        const ctx = canvas.getContext('2d');
                        
                        const viewport = page.getViewport({ scale: scale });
                        canvas.height = viewport.height;
                        canvas.width = viewport.width;
                        
                        canvas.className = 'pdf-page';
                        
                        const renderContext = {
                            canvasContext: ctx,
                            viewport: viewport
                        };
                        
                        // Șterge pagina anterioară
                        const container = document.getElementById('pdfContainer');
                        container.innerHTML = '';
                        container.appendChild(canvas);
                        
                        page.render(renderContext);
                    });
                }
                
                function previousPage() {
                    if (currentPage > 1) {
                        currentPage--;
                        renderPage(currentPage);
                        updateControls();
                    }
                }
                
                function nextPage() {
                    if (currentPage < totalPages) {
                        currentPage++;
                        renderPage(currentPage);
                        updateControls();
                    }
                }
                
                function zoomIn() {
                    scale += 0.2;
                    renderPage(currentPage);
                    updateZoomDisplay();
                }
                
                function zoomOut() {
                    if (scale > 0.4) {
                        scale -= 0.2;
                        renderPage(currentPage);
                        updateZoomDisplay();
                    }
                }
                
                function updateControls() {
                    document.getElementById('currentPage').textContent = currentPage;
                    document.getElementById('prevBtn').disabled = (currentPage === 1);
                    document.getElementById('nextBtn').disabled = (currentPage === totalPages);
                }
                
                function updateZoomDisplay() {
                    document.getElementById('zoomLevel').textContent = Math.round(scale * 100) + '%';
                }
                
                function showError() {
                    document.getElementById('pdfContainer').innerHTML = `
                        <div class="error">
                            <h3>⚠️ Nu s-a putut încărca PDF-ul</h3>
                            <p>PDF.js nu poate procesa acest fișier. Încearcă să îl deschizi într-o aplicație externă.</p>
                            <div class="fallback-actions">
                                <button class="btn btn-primary" onclick="Android.openExternal()">
                                    📖 Deschide extern
                                </button>
                                <button class="btn btn-secondary" onclick="Android.shareFile()">
                                    📤 Partajează
                                </button>
                            </div>
                        </div>
                    `;
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun encodeFileToBase64(file: File): String {
        return try {
            val bytes = file.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("AttachmentViewer", "Error encoding file to base64", e)
            ""
        }
    }

    private fun loadPdfWithCustomViewer(uri: Uri) {
        val htmlContent = createPdfInfoHtml(attachmentName)
        val htmlFile = File(cacheDir, "pdf_info_${System.currentTimeMillis()}.html")
        htmlFile.writeText(htmlContent)

        binding.webView.loadUrl("file://${htmlFile.absolutePath}")
        showLoading(false)
    }

    private fun loadOfficeDocument(uri: Uri) {
        // Pentru documente Office, încearcă mai multe metode
        val tempFile = createTempFileAndCopy(uri)

        // Metodă 1: Încearcă Google Docs Viewer cu o copie locală uploadată temporar
        // Această metodă nu va funcționa pentru fișiere locale, deci trecem la metodă 2

        // Metodă 2: Încarcă un viewer HTML personalizat
        loadOfficeDocumentWithCustomViewer(tempFile)
    }

    private fun loadOfficeDocumentWithCustomViewer(file: File) {
        val htmlContent = createOfficeViewerHtml(file)
        val htmlFile = File(cacheDir, "office_viewer_${System.currentTimeMillis()}.html")
        htmlFile.writeText(htmlContent)

        binding.webView.loadUrl("file://${htmlFile.absolutePath}")
        showLoading(false)
    }

    private fun loadTextDocument(uri: Uri) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            }

            if (content != null) {
                val htmlContent = createTextViewerHtml(content, attachmentName)
                val htmlFile = File(cacheDir, "text_viewer_${System.currentTimeMillis()}.html")
                htmlFile.writeText(htmlContent)

                binding.webView.loadUrl("file://${htmlFile.absolutePath}")
                showLoading(false)
            } else {
                showError("Nu s-a putut citi conținutul fișierului")
            }
        } catch (e: Exception) {
            Log.e("AttachmentViewer", "Error loading text document", e)
            showError("Nu s-a putut încărca documentul text")
        }
    }

    private fun loadImageAsDocument(uri: Uri) {
        // Dacă este o imagine, încarcă-o ca imagine, nu ca document
        attachmentType = AttachmentType.IMAGE
        loadImage()
    }

    private fun showUnsupportedDocument(uri: Uri) {
        val htmlContent = createUnsupportedFileHtml(attachmentName)
        val htmlFile = File(cacheDir, "unsupported_${System.currentTimeMillis()}.html")
        htmlFile.writeText(htmlContent)

        binding.webView.loadUrl("file://${htmlFile.absolutePath}")
        showLoading(false)
    }

    private fun createTempFileAndCopy(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream == null) {
            throw Exception("Nu s-a putut accesa fișierul")
        }

        val tempFile = createTempFile(uri)
        val outputStream = FileOutputStream(tempFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    private fun loadRemoteDocument(url: String) {
        when {
            url.contains(".pdf", ignoreCase = true) -> {
                val googleDocsUrl = "https://docs.google.com/gviewer?embedded=true&url=$url"
                binding.webView.loadUrl(googleDocsUrl)
            }
            url.contains(".doc", ignoreCase = true) ||
                    url.contains(".docx", ignoreCase = true) ||
                    url.contains(".xls", ignoreCase = true) ||
                    url.contains(".xlsx", ignoreCase = true) -> {
                val googleDocsUrl = "https://docs.google.com/gviewer?embedded=true&url=$url"
                binding.webView.loadUrl(googleDocsUrl)
            }
            else -> {
                binding.webView.loadUrl(url)
            }
        }
    }

    private fun createTempFile(uri: Uri): File {
        val extension = getFileExtension(uri)
        val tempDir = File(cacheDir, "temp_documents")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        return File.createTempFile(
            "temp_doc_${System.currentTimeMillis()}",
            ".$extension",
            tempDir
        )
    }

    private fun getFileExtension(uri: Uri): String {
        val fileName = attachmentName
        return if (fileName.contains('.')) {
            fileName.substringAfterLast('.').lowercase()
        } else {
            // Încearcă să detecteze din MIME type
            val mimeType = contentResolver.getType(uri)
            when {
                mimeType?.contains("pdf") == true -> "pdf"
                mimeType?.contains("msword") == true -> "doc"
                mimeType?.contains("wordprocessingml") == true -> "docx"
                mimeType?.contains("ms-excel") == true -> "xls"
                mimeType?.contains("spreadsheetml") == true -> "xlsx"
                mimeType?.contains("ms-powerpoint") == true -> "ppt"
                mimeType?.contains("presentationml") == true -> "pptx"
                mimeType?.contains("opendocument.text") == true -> "odt"
                mimeType?.contains("opendocument.spreadsheet") == true -> "ods"
                mimeType?.contains("opendocument.presentation") == true -> "odp"
                mimeType?.contains("rtf") == true -> "rtf"
                mimeType?.contains("plain") == true -> "txt"
                else -> "tmp"
            }
        }
    }

    // Enum pentru tipurile de documente suportate
    private enum class DocumentType {
        PDF,
        OFFICE_DOCUMENT,
        TEXT_DOCUMENT,
        IMAGE,
        UNSUPPORTED
    }

    private fun detectDocumentType(uri: Uri): DocumentType {
        val extension = getFileExtension(uri).lowercase()
        val mimeType = contentResolver.getType(uri)?.lowercase() ?: ""

        return when {
            // PDF
            extension == "pdf" || mimeType.contains("pdf") -> DocumentType.PDF

            // Microsoft Office Documents
            extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx") ||
                    mimeType.contains("msword") ||
                    mimeType.contains("wordprocessingml") ||
                    mimeType.contains("ms-excel") ||
                    mimeType.contains("spreadsheetml") ||
                    mimeType.contains("ms-powerpoint") ||
                    mimeType.contains("presentationml") -> DocumentType.OFFICE_DOCUMENT

            // OpenDocument formats
            extension in listOf("odt", "ods", "odp") ||
                    mimeType.contains("opendocument") -> DocumentType.OFFICE_DOCUMENT

            // Text documents
            extension in listOf("txt", "rtf", "csv", "xml", "json", "log", "md") ||
                    mimeType.contains("text/") ||
                    mimeType.contains("rtf") -> DocumentType.TEXT_DOCUMENT

            // Images
            extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg") ||
                    mimeType.contains("image/") -> DocumentType.IMAGE

            else -> DocumentType.UNSUPPORTED
        }
    }

    private fun createOfficeViewerHtml(file: File): String {
        val fileName = file.name
        val fileSize = formatFileSize(file.length())

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$attachmentName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                    color: #333;
                }
                .container {
                    max-width: 800px;
                    margin: 0 auto;
                    background: white;
                    border-radius: 8px;
                    padding: 30px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }
                .document-icon {
                    width: 80px;
                    height: 80px;
                    margin: 0 auto 20px;
                    background: #4285f4;
                    border-radius: 8px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-size: 24px;
                }
                .document-info {
                    text-align: center;
                }
                .document-name {
                    font-size: 24px;
                    font-weight: 600;
                    margin-bottom: 8px;
                    color: #1a73e8;
                }
                .document-details {
                    color: #666;
                    margin-bottom: 30px;
                }
                .action-buttons {
                    display: flex;
                    gap: 15px;
                    justify-content: center;
                    flex-wrap: wrap;
                }
                .btn {
                    padding: 12px 24px;
                    border: none;
                    border-radius: 6px;
                    font-size: 16px;
                    cursor: pointer;
                    text-decoration: none;
                    display: inline-block;
                    transition: background-color 0.2s;
                }
                .btn-primary {
                    background-color: #1a73e8;
                    color: white;
                }
                .btn-secondary {
                    background-color: #f8f9fa;
                    color: #333;
                    border: 1px solid #dadce0;
                }
                .btn:hover {
                    opacity: 0.9;
                }
                .info-section {
                    margin-top: 40px;
                    padding-top: 20px;
                    border-top: 1px solid #e0e0e0;
                }
                .info-item {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 10px;
                    padding: 8px 0;
                }
                .file-preview-note {
                    background: #e8f0fe;
                    border: 1px solid #d2e3fc;
                    border-radius: 4px;
                    padding: 16px;
                    margin-top: 20px;
                    text-align: center;
                    color: #1565c0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="document-icon">📄</div>
                <div class="document-info">
                    <div class="document-name">$attachmentName</div>
                    <div class="document-details">
                        Dimensiune: $fileSize
                    </div>
                </div>
                
                <div class="action-buttons">
                    <button class="btn btn-primary" onclick="openExternal()">
                        Deschide în aplicația externă
                    </button>
                    <button class="btn btn-secondary" onclick="shareFile()">
                        Partajează
                    </button>
                </div>
                
                <div class="file-preview-note">
                    <strong>📱 Notă:</strong> Pentru a vizualiza conținutul complet al documentului, folosește butonul "Deschide extern" din bara de sus pentru a-l deschide într-o aplicație compatibilă.
                </div>
                
                <div class="info-section">
                    <div class="info-item">
                        <span><strong>Tip fișier:</strong></span>
                        <span>${getDocumentTypeName(fileName)}</span>
                    </div>
                    <div class="info-item">
                        <span><strong>Dimensiune:</strong></span>
                        <span>$fileSize</span>
                    </div>
                    <div class="info-item">
                        <span><strong>Locație:</strong></span>
                        <span>Fișier local</span>
                    </div>
                </div>
            </div>
            
            <script>
                function openExternal() {
                    // Această funcție va fi apelată de aplicația Android
                    window.location.href = 'javascript:Android.openExternal()';
                }
                
                function shareFile() {
                    // Această funcție va fi apelată de aplicația Android
                    window.location.href = 'javascript:Android.shareFile()';
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun createTextViewerHtml(content: String, fileName: String): String {
        val fileSize = formatFileSize(content.toByteArray().size.toLong())
        val lineCount = content.lines().size

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$fileName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f8f9fa;
                    color: #333;
                    line-height: 1.6;
                }
                .header {
                    background: white;
                    padding: 20px;
                    border-radius: 8px;
                    margin-bottom: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .file-info {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    flex-wrap: wrap;
                    gap: 10px;
                }
                .file-name {
                    font-size: 20px;
                    font-weight: 600;
                    color: #1a73e8;
                }
                .file-details {
                    color: #666;
                    font-size: 14px;
                }
                .content-container {
                    background: white;
                    border-radius: 8px;
                    padding: 0;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    overflow: hidden;
                }
                .content {
                    padding: 20px;
                    white-space: pre-wrap;
                    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
                    font-size: 14px;
                    line-height: 1.5;
                    max-height: 70vh;
                    overflow-y: auto;
                    border: none;
                    resize: none;
                    background: #fafafa;
                    border-radius: 4px;
                }
                .line-numbers {
                    background: #f1f3f4;
                    padding: 20px 15px;
                    border-right: 1px solid #e0e0e0;
                    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
                    font-size: 12px;
                    color: #666;
                    user-select: none;
                    float: left;
                    width: 50px;
                    box-sizing: border-box;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="file-info">
                    <div>
                        <div class="file-name">📄 $fileName</div>
                        <div class="file-details">$lineCount linii • $fileSize</div>
                    </div>
                </div>
            </div>
            
            <div class="content-container">
                <div class="content">${content.take(50000)}</div>
            </div>
            
            ${if (content.length > 50000) "<p style='text-align: center; color: #666; margin-top: 20px;'>📄 Conținutul a fost trunchiat pentru performanță. Folosește o aplicație externă pentru vizualizarea completă.</p>" else ""}
        </body>
        </html>
        """.trimIndent()
    }

    private fun createUnsupportedFileHtml(fileName: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$fileName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 40px 20px;
                    background-color: #f8f9fa;
                    color: #333;
                    text-align: center;
                }
                .container {
                    max-width: 500px;
                    margin: 0 auto;
                    background: white;
                    border-radius: 12px;
                    padding: 40px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                }
                .icon {
                    font-size: 64px;
                    margin-bottom: 20px;
                }
                .title {
                    font-size: 24px;
                    font-weight: 600;
                    margin-bottom: 10px;
                    color: #ea4335;
                }
                .subtitle {
                    font-size: 16px;
                    color: #666;
                    margin-bottom: 30px;
                }
                .file-name {
                    background: #f8f9fa;
                    padding: 15px;
                    border-radius: 8px;
                    font-family: monospace;
                    word-break: break-all;
                    margin-bottom: 30px;
                }
                .suggestion {
                    background: #e8f0fe;
                    border: 1px solid #d2e3fc;
                    border-radius: 8px;
                    padding: 20px;
                    color: #1565c0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="icon">❓</div>
                <div class="title">Tip de fișier nesuportat</div>
                <div class="subtitle">Nu putem afișa conținutul acestui fișier în aplicație</div>
                <div class="file-name">$fileName</div>
                <div class="suggestion">
                    <strong>💡 Sugestie:</strong> Folosește butonul "Deschide extern" din bara de sus pentru a deschide fișierul într-o aplicație compatibilă de pe dispozitiv.
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun getDocumentTypeName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> "Document PDF"
            "doc" -> "Document Microsoft Word"
            "docx" -> "Document Microsoft Word (XML)"
            "xls" -> "Foaie de calcul Microsoft Excel"
            "xlsx" -> "Foaie de calcul Microsoft Excel (XML)"
            "ppt" -> "Prezentare Microsoft PowerPoint"
            "pptx" -> "Prezentare Microsoft PowerPoint (XML)"
            "odt" -> "Document OpenDocument Text"
            "ods" -> "Foaie de calcul OpenDocument"
            "odp" -> "Prezentare OpenDocument"
            "txt" -> "Document text"
            "rtf" -> "Rich Text Format"
            "csv" -> "Comma Separated Values"
            "xml" -> "Document XML"
            "json" -> "Document JSON"
            else -> "Document necunoscut"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun createPdfViewerHtml(file: File): String {
        val fileSize = formatFileSize(file.length())

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$attachmentName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 0;
                    background-color: #f5f5f5;
                    color: #333;
                }
                .pdf-container {
                    width: 100%;
                    height: 100vh;
                    display: flex;
                    flex-direction: column;
                }
                .pdf-header {
                    background: white;
                    padding: 15px 20px;
                    border-bottom: 1px solid #e0e0e0;
                    flex-shrink: 0;
                }
                .pdf-info {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                .pdf-name {
                    font-weight: 600;
                    color: #1a73e8;
                    font-size: 16px;
                }
                .pdf-details {
                    color: #666;
                    font-size: 14px;
                }
                .pdf-viewer {
                    flex: 1;
                    background: white;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 40px 20px;
                }
                .pdf-placeholder {
                    text-align: center;
                    max-width: 400px;
                }
                .pdf-icon {
                    width: 80px;
                    height: 80px;
                    margin: 0 auto 20px;
                    background: #dc2626;
                    border-radius: 8px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-size: 28px;
                }
                .pdf-message {
                    font-size: 18px;
                    font-weight: 600;
                    margin-bottom: 10px;
                    color: #333;
                }
                .pdf-description {
                    color: #666;
                    margin-bottom: 30px;
                    line-height: 1.5;
                }
                .action-buttons {
                    display: flex;
                    gap: 15px;
                    justify-content: center;
                    flex-wrap: wrap;
                }
                .btn {
                    padding: 12px 24px;
                    border: none;
                    border-radius: 6px;
                    font-size: 16px;
                    cursor: pointer;
                    text-decoration: none;
                    display: inline-block;
                    transition: all 0.2s;
                    font-weight: 500;
                }
                .btn-primary {
                    background-color: #1a73e8;
                    color: white;
                }
                .btn-secondary {
                    background-color: #f8f9fa;
                    color: #333;
                    border: 1px solid #dadce0;
                }
                .btn:hover {
                    transform: translateY(-1px);
                    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
                }
                .btn:active {
                    transform: translateY(0);
                }
                .note {
                    background: #e8f0fe;
                    border: 1px solid #d2e3fc;
                    border-radius: 6px;
                    padding: 16px;
                    margin-top: 20px;
                    color: #1565c0;
                    font-size: 14px;
                }
                .embedded-pdf {
                    width: 100%;
                    height: calc(100vh - 100px);
                    border: none;
                    background: white;
                }
            </style>
        </head>
        <body>
            <div class="pdf-container">
                <div class="pdf-header">
                    <div class="pdf-info">
                        <div class="pdf-name">📄 $attachmentName</div>
                        <div class="pdf-details">$fileSize • PDF Document</div>
                    </div>
                </div>
                
                <div class="pdf-viewer">
                    <!-- Încearcă să încărce PDF-ul direct -->
                    <embed src="file://${file.absolutePath}" type="application/pdf" class="embedded-pdf" />
                    
                    <!-- Fallback dacă embed nu funcționează -->
                    <div class="pdf-placeholder" style="display: none;" id="fallback">
                        <div class="pdf-icon">📄</div>
                        <div class="pdf-message">PDF Document</div>
                        <div class="pdf-description">
                            WebView nu poate afișa acest PDF direct. Folosește butoanele de mai jos pentru a-l deschide într-o aplicație externă.
                        </div>
                        
                        <div class="action-buttons">
                            <button class="btn btn-primary" onclick="Android.openExternal()">
                                📖 Deschide extern
                            </button>
                            <button class="btn btn-secondary" onclick="Android.shareFile()">
                                📤 Partajează
                            </button>
                        </div>
                        
                        <div class="note">
                            <strong>💡 Sfat:</strong> Pentru cea mai bună experiență de vizualizare PDF, recomandăm deschiderea într-o aplicație dedicată precum Adobe Reader sau Google PDF Viewer.
                        </div>
                    </div>
                </div>
            </div>
            
            <script>
                // Verifică dacă PDF-ul s-a încărcat
                setTimeout(function() {
                    const embed = document.querySelector('.embedded-pdf');
                    const fallback = document.getElementById('fallback');
                    
                    // Dacă embed nu și-a încărcat conținutul, afișează fallback
                    try {
                        if (!embed.contentDocument && !embed.getSVGDocument()) {
                            embed.style.display = 'none';
                            fallback.style.display = 'block';
                        }
                    } catch (e) {
                        // Dacă accesul este restricționat, afișează fallback
                        embed.style.display = 'none';
                        fallback.style.display = 'block';
                    }
                }, 2000);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun createPdfInfoHtml(fileName: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$fileName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 40px 20px;
                    background-color: #f8f9fa;
                    color: #333;
                    text-align: center;
                }
                .container {
                    max-width: 500px;
                    margin: 0 auto;
                    background: white;
                    border-radius: 12px;
                    padding: 40px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                }
                .pdf-icon {
                    width: 80px;
                    height: 80px;
                    margin: 0 auto 20px;
                    background: #dc2626;
                    border-radius: 12px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-size: 32px;
                }
                .title {
                    font-size: 24px;
                    font-weight: 600;
                    margin-bottom: 10px;
                    color: #dc2626;
                }
                .subtitle {
                    font-size: 16px;
                    color: #666;
                    margin-bottom: 30px;
                    line-height: 1.5;
                }
                .file-name {
                    background: #f8f9fa;
                    padding: 15px;
                    border-radius: 8px;
                    font-family: monospace;
                    word-break: break-all;
                    margin-bottom: 30px;
                    border: 1px solid #e0e0e0;
                }
                .action-buttons {
                    display: flex;
                    gap: 15px;
                    justify-content: center;
                    flex-wrap: wrap;
                    margin-bottom: 20px;
                }
                .btn {
                    padding: 14px 28px;
                    border: none;
                    border-radius: 8px;
                    font-size: 16px;
                    font-weight: 500;
                    cursor: pointer;
                    text-decoration: none;
                    display: inline-block;
                    transition: all 0.2s;
                }
                .btn-primary {
                    background-color: #dc2626;
                    color: white;
                }
                .btn-secondary {
                    background-color: #f8f9fa;
                    color: #333;
                    border: 1px solid #dadce0;
                }
                .btn:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                }
                .suggestion {
                    background: #fef2f2;
                    border: 1px solid #fecaca;
                    border-radius: 8px;
                    padding: 20px;
                    color: #dc2626;
                    margin-top: 20px;
                }
                .feature-list {
                    text-align: left;
                    margin-top: 20px;
                    background: #f8f9fa;
                    padding: 20px;
                    border-radius: 8px;
                }
                .feature-item {
                    margin: 8px 0;
                    color: #666;
                }
                .feature-item::before {
                    content: "✓ ";
                    color: #22c55e;
                    font-weight: bold;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="pdf-icon">📄</div>
                <div class="title">Document PDF</div>
                <div class="subtitle">
                    Pentru cea mai bună experiență de vizualizare, deschide acest PDF într-o aplicație dedicată.
                </div>
                <div class="file-name">$fileName</div>
                
                <div class="action-buttons">
                    <button class="btn btn-primary" onclick="Android.openExternal()">
                        📖 Deschide extern
                    </button>
                    <button class="btn btn-secondary" onclick="Android.shareFile()">
                        📤 Partajează
                    </button>
                </div>
                
                <div class="feature-list">
                    <div style="font-weight: 600; margin-bottom: 12px; color: #333;">Aplicații recomandate:</div>
                    <div class="feature-item">Adobe Acrobat Reader</div>
                    <div class="feature-item">Google PDF Viewer</div>
                    <div class="feature-item">Browser web (Chrome, Firefox)</div>
                    <div class="feature-item">Aplicația Files din Android</div>
                </div>
                
                <div class="suggestion">
                    <strong>📱 Informație:</strong> PDF-urile au nevoie de un viewer specializat pentru a fi afișate corect pe dispozitivele mobile.
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
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
                // Adaugă aceste setări pentru fișiere locale
                allowUniversalAccessFromFileURLs = true
                allowFileAccessFromFileURLs = true
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
                    Log.e("WebView", "Error loading: $description for URL: $failingUrl")

                    // Dacă Google Docs Viewer eșuează, încearcă încărcarea directă
                    if (failingUrl?.contains("docs.google.com") == true && attachmentUri != null) {
                        if (isLocalUri(attachmentUri!!)) {
                            // Pentru fișiere locale, încearcă încărcarea directă
                            try {
                                val tempFile = createTempFile(attachmentUri!!)
                                contentResolver.openInputStream(attachmentUri!!)?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                binding.webView.loadUrl("file://${tempFile.absolutePath}")
                            } catch (e: Exception) {
                                showError("Nu s-a putut încărca documentul")
                            }
                        }
                    } else {
                        showError("Nu s-a putut încărca documentul")
                    }
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

        // Verifică dacă este fișier local
        if (isLocalUri(uri)) {
            Toast.makeText(this, "Fișierul este deja local pe dispozitiv", Toast.LENGTH_SHORT).show()
            return
        }

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