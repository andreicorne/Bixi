package com.example.bixi.helper

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.bixi.R
import com.example.bixi.models.Attachment
import com.example.bixi.models.AttachmentType
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AttachmentSelectionHelper(
    private val activity: ComponentActivity,
    private val onAttachmentsSelected: (List<Attachment>) -> Unit
) {

    private var photoFile: File? = null

    // Activity Result Launchers
    private val galleryLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedImages(uris)
        }
    }

    private val documentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleSelectedDocument(it) }
    }

    private val cameraLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoFile?.let { file ->
                val uri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    file
                )
                handleSelectedImages(listOf(uri))
            }
        }
    }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraPermission = permissions[android.Manifest.permission.CAMERA] ?: false
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }

        if (!cameraPermission && !storagePermission) {
            Toast.makeText(activity, activity.getString(R.string.permissions_required_for_attachments), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inițializează helper-ul și verifică permisiunile
     */
    fun initialize() {
        requestPermissions()
    }

    /**
     * Deschide bottom sheet-ul pentru selecția atașamentelor
     */
    fun showAttachmentPicker() {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.bottom_sheet_attachment_picker, null)
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
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, activity.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
            return
        }

        val photoFileName = "photo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"

        val photoDir = File(activity.filesDir, "photos")
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }

        photoFile = File(photoDir, photoFileName)

        try {
            val photoURI = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                photoFile!!
            )
            cameraLauncher.launch(photoURI)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(activity, activity.getString(R.string.couldnt_take_photo), Toast.LENGTH_SHORT).show()
            android.util.Log.e("AttachmentHelper", "FileProvider error", e)
        }
    }

    private fun openGallery() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            Toast.makeText(activity, activity.getString(R.string.storage_permission_required), Toast.LENGTH_SHORT).show()
            return
        }

        galleryLauncher.launch("image/*")
    }

    private fun openDocumentPicker() {
        documentLauncher.launch("*/*")
    }

    private fun handleSelectedImages(uris: List<Uri>) {
        val attachments = uris.mapNotNull { uri ->
            val fileName = getFileName(uri) ?: "image_${System.currentTimeMillis()}.jpg"
            val fileSize = getFileSize(uri)

            Attachment(
                id = UUID.randomUUID().toString(),
                url = uri.toString(),
                type = AttachmentType.IMAGE,
                name = fileName,
                size = fileSize
            )
        }

        onAttachmentsSelected(attachments)
    }

    private fun handleSelectedDocument(uri: Uri) {
        val fileName = getFileName(uri) ?: "document_${System.currentTimeMillis()}"
        val fileSize = getFileSize(uri)
        val mimeType = activity.contentResolver.getType(uri)

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

        onAttachmentsSelected(listOf(attachment))
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor = activity.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun getFileSize(uri: Uri): Long? {
        var fileSize: Long? = null
        val cursor = activity.contentResolver.query(uri, null, null, null, null)
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

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}