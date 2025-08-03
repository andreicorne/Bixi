package com.example.bixi.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.AppSession
import com.example.bixi.R
import com.example.bixi.adapters.AttachmentListAdapter
import com.example.bixi.adapters.CheckListAdapter
import com.example.bixi.constants.AppConstants
import com.example.bixi.constants.NavigationConstants
import com.example.bixi.customViews.CustomBottomSheetFragment
import com.example.bixi.databinding.ActivityTaskDetailsBinding
import com.example.bixi.enums.AttachmentBottomSheetItemType
import com.example.bixi.helper.AttachmentConverter
import com.example.bixi.helper.AttachmentSelectionHelper
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.helper.Utils
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.BottomSheetItem
import com.example.bixi.models.CheckItem
import com.example.bixi.models.api.CheckItemResponse
import com.example.bixi.models.api.CreateTaskRequest
import com.example.bixi.services.RetrofitClient
import com.example.bixi.viewModels.TaskDetailsViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityTaskDetailsBinding
    private val viewModel: TaskDetailsViewModel by viewModels()

    private lateinit var adapterAttachments: AttachmentListAdapter
    private var selectedAttachmentPosition: Int = -1

    private lateinit var adapterChecks: CheckListAdapter

    private var taskWasCreated: Boolean = false
    var isShadowVisible = false

    // Sistem centralizat pentru atașamente
    private lateinit var attachmentHelper: AttachmentSelectionHelper

    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTaskDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLoadingOverlay()

        val taskId = intent.getStringExtra(NavigationConstants.TASK_ID_NAV)
        if(taskId.isNullOrBlank()){
            viewModel.isCreateMode = true
        }
        else{
            viewModel.taskId = taskId
        }

        // Inițializează helper-ul pentru atașamente
        setupAttachmentHelper()

        initToolbar()
        initListeners()
        initCheckList()
        initAttachmentList()
        initResponsible()
        setStyles()
        setupViewModel()
        setupBindings()
    }

    private fun setupAttachmentHelper() {
        attachmentHelper = AttachmentSelectionHelper(this) { attachments ->
            handleAttachmentsFromHelper(attachments)
        }
        attachmentHelper.initialize()
    }

    private fun handleAttachmentsFromHelper(attachments: List<com.example.bixi.models.Attachment>) {
        attachments.forEach { attachment ->
            val attachmentItem = AttachmentConverter.toAttachmentItem(attachment)

            if (selectedAttachmentPosition != -1) {
                viewModel.updateAttachmentAt(selectedAttachmentPosition) {
                    attachmentItem
                }
                selectedAttachmentPosition = -1
            } else {
                // Adaugă atașamentul înaintea ultimului element (care este container-ul gol)
                viewModel.addAttachmentBeforeLast(attachmentItem)
            }
        }
    }

    private fun initListeners(){
        binding.btnAddCheckItem.setOnClickListener {
            addCheckItem()
        }

        binding.etCheckItem.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_NEXT ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                addCheckItem()
                true
            } else {
                false
            }
        }

        binding.etStartDate.setOnClickListener {
            showDateTimePicker(isStart = true)
        }

        binding.etEndDate.setOnClickListener {
            showDateTimePicker(isStart = false)
        }

        binding.svScrollview.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 0 && !isShadowVisible) {
                binding.vToolbarShadow.visibility = View.VISIBLE
                isShadowVisible = true
            } else if (scrollY == 0 && isShadowVisible) {
                binding.vToolbarShadow.visibility = View.GONE
                isShadowVisible = false
            }
        }

        binding.ivDelete.setOnClickListener {
            onBack()
        }

        binding.ivReset.setOnClickListener {
            animateResetFields()
        }

        binding.ivSetDone.setOnClickListener {
            createTask()
        }

        binding.ivChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createTask() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val checkItemResponses: List<CheckItemResponse> = viewModel.checks.value?.map { item ->
                    CheckItemResponse(
                        text = item.text,
                        done = item.done
                    )
                } ?: emptyList()

                val gson = Gson()
                val checksJson = gson.toJson(checkItemResponses)

                val createTaskRequest = CreateTaskRequest(viewModel.title.value!!, viewModel.description.value!!,
                    AppSession.user!!.user.id, "98112922-056b-4aa9-834b-f56004b43bc5", checksJson,
                    Utils.calendarToUtcIsoString(viewModel.startDateTime.value!!),
                    Utils.calendarToUtcIsoString(viewModel.endDateTime.value!!))

                val parts = prepareAttachments(baseContext, viewModel.attachments.value!!)
                val response = RetrofitClient.createTask(createTaskRequest, parts)
                if (response.success) {
                    onSuccessfullyCreated()
                } else {
                }
                showLoading(false)

            } catch (e: Exception) {
                showLoading(false)
                Log.e("API", "Exception: ${e.message}")
            }
        }
    }

    private fun onSuccessfullyCreated(){
        taskWasCreated = true
        onBack()
    }

    private fun prepareAttachments(
        context: Context,
        attachments: List<AttachmentItem>
    ): List<MultipartBody.Part> {
        return attachments.mapNotNull { attachment ->
            val bytes = attachment.uri?.let { uriToByteArray(context, it) } ?: return@mapNotNull null
            val requestBody = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(
                name = "attachments",
                filename = getFileName(attachment.uri!!),
                body = requestBody
            )
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "Document"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) result = it.getString(index)
            }
        }
        return result
    }

    fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showDateTimePicker(isStart: Boolean) {
        val currentCalendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDateTime = Calendar.getInstance()
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, minute)
                        selectedDateTime.set(Calendar.SECOND, 0)
                        selectedDateTime.set(Calendar.MILLISECOND, 0)

                        val allowedDelayMs = 60 * 1000 // 60 secunde
                        if (selectedDateTime.timeInMillis < currentCalendar.timeInMillis - allowedDelayMs) {
                            Toast.makeText(this, getString(R.string.cannot_select_a_past_date), Toast.LENGTH_SHORT).show()
                            return@TimePickerDialog
                        }

                        if (isStart) {
                            viewModel.setStartDateTime(selectedDateTime)

                            if (viewModel.endDateTime.value == null) {
                                val endDate = Calendar.getInstance()
                                endDate.timeInMillis = selectedDateTime.timeInMillis + 60 * 60 * 1000
                                viewModel.setEndDateTime(endDate)
                            } else {
                                viewModel.endDateTime.value?.let { endDate ->
                                    if (endDate.before(selectedDateTime)) {
                                        val newEnd = Calendar.getInstance()
                                        newEnd.timeInMillis = selectedDateTime.timeInMillis + 60 * 60 * 1000
                                        viewModel.setEndDateTime(newEnd)
                                    }
                                }
                            }
                        }
                        else {
                            viewModel.setEndDateTime(selectedDateTime)

                            viewModel.startDateTime.value?.let { startDate ->
                                if (startDate.after(selectedDateTime)) {
                                    val newStart = Calendar.getInstance()
                                    newStart.timeInMillis = selectedDateTime.timeInMillis - 60 * 60 * 1000
                                    viewModel.setStartDateTime(newStart)
                                }
                            }
                        }

                    },
                    currentCalendar.get(Calendar.HOUR_OF_DAY),
                    currentCalendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.minDate = currentCalendar.timeInMillis
        datePicker.show()
    }

    private fun addCheckItem(){
        viewModel.addCheckItem(CheckItem(text =  binding.etCheckItem.text.toString(), done = false))
        binding.etCheckItem.text.clear()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etCheckItem.windowToken, 0)
        binding.etCheckItem.clearFocus()
    }

    private fun initCheckList(){
        binding.rlCheckList.layoutManager = LinearLayoutManager(this)

        viewModel.checks.observe(this) { newList ->
            val sortedList = newList.sortedBy { it.done } // false (nebifat) vine înainte de true (bifat)
            adapterChecks.submitList(sortedList)
        }

        adapterChecks = CheckListAdapter(
            listener = { item, isChecked ->
                viewModel.updateCheckItem(item.id!!, isChecked)
            },
            deleteListener = { item ->
                viewModel.deleteCheckItem(item.id!!)
            },
            startDragListener = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )

        binding.rlCheckList.adapter = adapterChecks
        binding.rlCheckList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        itemTouchHelper = ItemTouchHelper(checkItemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rlCheckList)
    }

    private fun initAttachmentList(){
        viewModel.attachments.observe(this) { newList ->
            adapterAttachments.submitList(newList)
        }

        adapterAttachments = AttachmentListAdapter { position ->
            val attachment = viewModel.attachments.value?.get(position)

            if (attachment?.uri == null) {
                // Pentru empty container, deschide selecția de atașamente
                selectedAttachmentPosition = position
                attachmentHelper.showAttachmentPicker()
            } else {
                // Pentru attachments existente, afișează bottom sheet pentru opțiuni
                runOnUiThread{
                    val bottomSheetItems = mutableListOf(
                        BottomSheetItem(getString(R.string.view), AttachmentBottomSheetItemType.VIEW),
                        BottomSheetItem(getString(R.string.edit), AttachmentBottomSheetItemType.EDIT),
                        BottomSheetItem(getString(R.string.remove), AttachmentBottomSheetItemType.REMOVE)
                    )
                    val bottomSheetFragment = CustomBottomSheetFragment()
                    bottomSheetFragment.setOptions(bottomSheetItems) { selectedOption ->
                        when (selectedOption.type) {
                            AttachmentBottomSheetItemType.VIEW -> {
                                val fileName = attachment.serverData?.name ?: "Document"
                                AttachmentViewerActivity.startActivity(
                                    context = this@TaskDetailsActivity,
                                    uri = attachment.uri,
                                    type = attachment.type,
                                    serverData = attachment.serverData,
                                    name = fileName
                                )
                            }
                            AttachmentBottomSheetItemType.EDIT -> {
                                selectedAttachmentPosition = position
                                attachmentHelper.showAttachmentPicker()
                            }
                            AttachmentBottomSheetItemType.REMOVE -> {
                                viewModel.removeAttachmentAt(position)
                            }
                            null -> TODO()
                        }
                    }
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }
            }
        }
        binding.rlAttachments.adapter = adapterAttachments
        binding.rlAttachments.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun initToolbar(){
        binding.ivBack.setOnClickListener {
            onBack()
        }
    }

    private fun setupViewModel(){
        viewModel.serverStatusResponse.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Login reușit!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.server_error_generic_message), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.responsible.observe(this) { list ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
            binding.tvResponsible.setAdapter(adapter)
        }

        viewModel.title.observe(this) { newText ->
            if (binding.titleLayout.editText?.text.toString() != newText) {
                binding.titleLayout.editText?.setText(newText)
            }
        }

        viewModel.description.observe(this) { newText ->
            if (binding.descriptionInputLayout.editText?.text.toString() != newText) {
                binding.descriptionInputLayout.editText?.setText(newText)
            }
        }

        viewModel.startDate.observe(this) { startDate ->
            binding.etStartDate.setText(startDate.toString())
        }

        viewModel.endDate.observe(this) { endDate ->
            binding.etEndDate.setText(endDate.toString())
        }

        viewModel.startDateTime.observe(this) { date ->
            date?.let {
                binding.etStartDate.setText(SimpleDateFormat(AppConstants.DATE_FORMAT, Locale.getDefault()).format(it.time))
            }
        }

        viewModel.endDateTime.observe(this) { date ->
            date?.let {
                binding.etEndDate.setText(SimpleDateFormat(AppConstants.DATE_FORMAT, Locale.getDefault()).format(it.time))
            }
        }

        if(!viewModel.isCreateMode){
            viewModel.getData()
        }
    }

    private fun setupBindings(){
        binding.titleLayout.bindTo(viewModel::setTitle)
        binding.descriptionInputLayout.bindTo(viewModel::setDescription)
    }

    private fun initResponsible(){
        binding.tvResponsible.setOnItemClickListener { _, _, position, _ ->
            val selected = viewModel.responsible.value.get(position)
            binding.tvResponsible.clearFocus()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.tvResponsible.windowToken, 0)
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
            view = binding.ivReset,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )

        BackgroundStylerService.setRoundedBackground(
            view = binding.ivDelete,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )

        BackgroundStylerService.setRoundedBackground(
            view = binding.ivChat,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )

        BackgroundStylerService.setRoundedBackground(
            view = binding.ivSetDone,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 48f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )
    }

    private fun animateResetFields() {
        binding.svScrollview.animate()
            .translationY(dpToPx(-20f))
            .alpha(0f)
            .setDuration(150L)
            .withEndAction {
                binding.svScrollview.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(150L)
                    .start()
            }
            .start()
    }

    private fun onBack(){
        val resultIntent = Intent()
        resultIntent.putExtra("task_created", taskWasCreated)
        setResult(Activity.RESULT_OK, resultIntent)
        onBackPressedDispatcher.onBackPressed()
    }

    fun dpToPx(dp: Float): Float {
        val density = resources.displayMetrics.density
        return dp * density
    }

    val checkItemTouchHelperCallback = object : ItemTouchHelper.Callback() {

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val item = adapterChecks.currentList[viewHolder.adapterPosition]

            // Permite glisarea doar dacă itemul nu este bifat
            val dragFlags = if (item.done) 0 else ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlags = 0
            return makeMovementFlags(dragFlags, swipeFlags)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)

            // Verifică dacă un item este selectat (este apasat pentru glisare)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder?.itemView?.elevation = 8f // Setează elevation pentru a ieși în evidență
            }
        }

        // Se apelează după ce itemul a fost eliberat și mutat
        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.elevation = 0f // Resetează elevation la 0 după mutare
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val item = adapterChecks.currentList[viewHolder.adapterPosition]
            // Permite mutarea doar pentru itemii nebifați
            if (item.done) return false
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            viewModel.moveUncheckedItem(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // Nu permite swipe pentru niciun item
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true // Permite glisarea lungă
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false // Dezactivează swipe-ul
        }
    }
}