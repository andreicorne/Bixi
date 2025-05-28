package com.example.bixi.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bixi.R
import com.example.bixi.adapters.AttachmentListAdapter
import com.example.bixi.adapters.CheckListAdapter
import com.example.bixi.databinding.ActivityLoginBinding
import com.example.bixi.databinding.ActivityTaskDetailsBinding
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.models.AttachmentItem
import com.example.bixi.viewModels.LoginViewModel
import com.example.bixi.viewModels.TaskDetailsViewModel

class TaskDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailsBinding
    private val viewModel: TaskDetailsViewModel by viewModels()

    private lateinit var adapter: AttachmentListAdapter
    private var items = mutableListOf<AttachmentItem>()
    private var selectedPosition: Int = -1

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            items[selectedPosition].uri = uri
            adapter.notifyItemChanged(selectedPosition)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTaskDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initCheckList()
        initAttachmentList()
        setStyles()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed() // modern back navigation
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

//    private fun initAttachmentLauncherResult(){
//        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val uri: Uri? = result.data?.data
//                uri?.let {
//                    // Ai acum fișierul ales (poză sau document)
//                    handleAttachment(uri)
//                }
//            }
//        }
//    }
//
//    private fun handleAttachment(uri: Uri) {
//        // Poți deschide fișierul, citi conținutul sau salva referința
//
//        val mimeType = contentResolver.getType(uri)
//        if (mimeType?.startsWith("image/") == true) {
//            Glide.with(this)
//                .load(uri)
//                .into(imageView)
//        }
//        else {
//            val fileName = getFileNameFromUri(uri)
//            textView.text = "Fișier selectat: $fileName"
//            imageView.setImageResource(R.drawable.ic_document_generic) // un icon document
//        }
//
//        val fileName = uri.lastPathSegment ?: "Document"
//        Toast.makeText(this, "Fișier selectat: $fileName", Toast.LENGTH_SHORT).show()
//    }

//    fun Context.getFileNameFromUri(uri: Uri): String {
//        var name = "Fișier necunoscut"
//        val cursor = contentResolver.query(uri, null, null, null, null)
//        cursor?.use {
//            if (it.moveToFirst()) {
//                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//                if (index >= 0) {
//                    name = it.getString(index)
//                }
//            }
//        }
//        return name
//    }

    private fun initToolbar(){
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initCheckList(){
        val sampleList = listOf("Element 1", "Element 2", "Element 3", "Element 4")

        binding.rlCheckList.layoutManager = LinearLayoutManager(this)
        binding.rlCheckList.adapter = CheckListAdapter(sampleList)

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.rlCheckList.addItemDecoration(divider)
    }

    private fun initAttachmentList(){
//        val sampleList = listOf("Element 1", "Element 2", "Element 3", "Element 4")
//
//        binding.rlAttachments.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        binding.rlAttachments.adapter = AttachmentListAdapter(sampleList, this)

        items = mutableListOf(
            AttachmentItem("Contract de muncă"),
            AttachmentItem("Poză profil"),
            AttachmentItem("Copie buletin"),
            AttachmentItem("Adeverință medicală"),
            AttachmentItem("Diplomă de studii")
        )

        adapter = AttachmentListAdapter(items) { position ->
            selectedPosition = position
            openFilePicker()
        }
        binding.rlAttachments.adapter = adapter
        binding.rlAttachments.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf", "application/msword"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun setStyles(){
//        BackgroundStylerService.setRoundedBackground(
//            view = binding.rlAddCheckContainer,
//            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
//            cornerRadius = 50f * resources.displayMetrics.density,
////            withRipple = true,
////            rippleColor = ContextCompat.getColor(this, R.color.ic_launcher_logo),
//            strokeWidth = (1 * resources.displayMetrics.density).toInt(), // 2dp în px
//            strokeColor = ContextCompat.getColor(this, R.color.md_theme_onBackground)
//        )
    }
}