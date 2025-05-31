package com.example.bixi.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bixi.R
import com.example.bixi.adapters.AttachmentListAdapter
import com.example.bixi.adapters.CheckListAdapter
import com.example.bixi.databinding.ActivityTaskDetailsBinding
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.models.AttachmentItem
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.task_details_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                Toast.makeText(this, "Salvat!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initToolbar(){
        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_close)
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initCheckList(){
        val sampleList = listOf("Element 1", "Element 2", "Element 3", "Element 4")

        binding.rlCheckList.layoutManager = LinearLayoutManager(this)
        binding.rlCheckList.adapter = CheckListAdapter(sampleList)

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.rlCheckList.addItemDecoration(divider)
    }

    private fun initAttachmentList(){

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