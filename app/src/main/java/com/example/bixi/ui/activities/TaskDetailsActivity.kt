package com.example.bixi.ui.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.AppSession
import com.example.bixi.R
import com.example.bixi.ui.adapters.AttachmentListAdapter
import com.example.bixi.ui.adapters.CheckListAdapter
import com.example.bixi.constants.AppConstants
import com.example.bixi.constants.NavigationConstants
import com.example.bixi.databinding.ActivityTaskDetailsBinding
import com.example.bixi.enums.FieldType
import com.example.bixi.enums.TaskActionType
import com.example.bixi.enums.TaskViewMode
import com.example.bixi.helper.AttachmentConverter
import com.example.bixi.helper.AttachmentOpenExternalHelper
import com.example.bixi.helper.AttachmentSelectionHelper
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.ExtensionHelper
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.helper.ResponseStatusHelper
import com.example.bixi.helper.Utils
import com.example.bixi.helper.ValidatorHelper
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.CheckItem
import com.example.bixi.models.api.CheckItemResponse
import com.example.bixi.models.api.CreateTaskRequest
import com.example.bixi.models.api.EditTaskRequest
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

    private lateinit var adapterChecks: CheckListAdapter

    private var resultReturnType: TaskActionType = TaskActionType.EMPTY

    var isShadowVisible = false

    // Sistem centralizat pentru atașamente
    private lateinit var attachmentHelper: AttachmentSelectionHelper

    private lateinit var itemTouchHelper: ItemTouchHelper

    private val descriptionEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedDescription = result.data?.getStringExtra(NavigationConstants.EDITTEXT_TEXT_NAV)
            binding.etDescription.setText(updatedDescription)
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
        setupLoadingOverlay()

        val taskId = intent.getStringExtra(NavigationConstants.TASK_ID_NAV)
        if(taskId.isNullOrBlank()){
            viewModel.viewMode = TaskViewMode.CREATE
        }
        else{
            // TODO: daca ii admin ii edit, altfel PREVIEW
            viewModel.viewMode = TaskViewMode.EDIT
            viewModel.taskId = taskId
        }

        onBackPressedDispatcher.addCallback(this) {
            onBack()
        }

        setupAttachmentHelper()
        setupInputValidations()
        initToolbar()
        initListeners()
        initCheckList()
        initAttachmentList()
        initResponsible()
        setStyles()
        setupViewModel()
        setupBindings()
        setUIMode()
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
            viewModel.addAttachmentBeforeLast(attachmentItem)
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

        binding.ivReset.setOnClickListener {
            animateResetFields()
        }
        binding.ivReset.visibility = View.GONE

        binding.ivSetDone.setOnClickListener {
            if(viewModel.viewMode == TaskViewMode.CREATE){
                createTask()
            }
            else{
                editTask()
            }
        }

        binding.ivChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(NavigationConstants.TASK_ID_NAV, viewModel.taskId)
            startActivity(intent)
        }

        binding.tlTitle.onFocusLostListener = { isValid ->
            setResetBtnVisibility()
        }
        binding.tlDescription.onFocusLostListener = { isValid ->
            setResetBtnVisibility()
        }

        binding.ivDelete.setOnClickListener {
            viewModel.delete({
                onSuccessfullyDeleted()
            })
        }

        binding.etCheckItem.doOnTextChanged { text, _, _, _ ->
            binding.btnAddCheckItem.isEnabled = !text.isNullOrEmpty()
        }

        // TODO: for open edittext window
//        binding.etDescription.setOnClickListener {
//            openDescriptionEdit()
//        }

        binding.etDescription.setOnTouchListener { v, event ->
            if (v.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)

                if (event.action == MotionEvent.ACTION_UP) {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    // TODO: for open edittext window
    private fun openDescriptionEdit() {
        val intent = Intent(this, EditTextActivity::class.java)
        intent.putExtra(NavigationConstants.EDITTEXT_TEXT_NAV, binding.etDescription.text.toString())

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            binding.etDescription,
            "task_details_description_edittext_transition"
        )
        descriptionEditLauncher.launch(intent, options)
    }

    private fun setResetBtnVisibility(){
        val hasChanges = viewModel.taskHasChanged()
        binding.ivReset.visibility = if(hasChanges) View.VISIBLE else View.GONE
    }

    private fun setupBindings(){
        binding.tlTitle.bindTo(viewModel::setTitle)
        binding.tlDescription.bindTo(viewModel::setDescription)
        binding.tlStartDate.bindTo(viewModel::setEndDateTime)
        binding.tlEndDate.bindTo(viewModel::setStartDateTime)
    }

    private fun setupInputValidations(){
        binding.tlTitle.setValidators(ValidatorHelper.getValidatorsFor(FieldType.TASK_TITLE))
        binding.tlDescription.setValidators(ValidatorHelper.getValidatorsFor(FieldType.TASK_DETAILS))
        binding.tlResponsible.setValidators(ValidatorHelper.getValidatorsFor(FieldType.TASK_RESPONSIBLE))
        binding.tlStartDate.setValidators(ValidatorHelper.getValidatorsFor(FieldType.TASK_START_DATE))
        binding.tlEndDate.setValidators(ValidatorHelper.getValidatorsFor(FieldType.TASK_END_DATE))
    }

    private fun createTask() {
        if(!isValidFields()){
            return
        }

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

                val createTaskRequest = CreateTaskRequest(viewModel.title.value!!, viewModel.description.value,
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

    private fun editTask() {
        if(!isValidFields()){
            return
        }

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

                val editTaskRequest = EditTaskRequest(viewModel.taskId,viewModel.title.value!!, viewModel.description.value!!,
                    checksJson, Utils.calendarToUtcIsoString(viewModel.startDateTime.value!!),
                    Utils.calendarToUtcIsoString(viewModel.endDateTime.value!!))

                val parts = prepareAttachments(baseContext, viewModel.attachments.value!!)
                val response = RetrofitClient.editTask(editTaskRequest, parts)
                if (response.success) {
                    onSuccessfullyEdited()
                } else {
                }
                showLoading(false)

            } catch (e: Exception) {
                showLoading(false)
                Log.e("API", "Exception: ${e.message}")
            }
        }
    }

    private fun isValidFields() : Boolean{
        val isValid = listOf(
            binding.tlTitle.validate(),
            binding.tlDescription.validate(),
            binding.tlResponsible.validate(),
            binding.tlStartDate.validate(),
            binding.tlEndDate.validate()
        ).all { it }
        return isValid
    }

    private fun onSuccessfullyDeleted(){
        resultReturnType = TaskActionType.DELETE
        onBack()
    }

    private fun onSuccessfullyEdited(){
        resultReturnType = TaskActionType.EDIT
        onBack()
    }

    private fun onSuccessfullyCreated(){
        resultReturnType = TaskActionType.CREATE
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
        if(binding.etCheckItem.text.isNullOrBlank()){
            return
        }

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
            setResetBtnVisibility()
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

        adapterAttachments = AttachmentListAdapter(
            openAttachmentListener = { position ->
                openAttachment(position)
            },
            removeListener = { position ->
                onDeleteAttachment(position)
            }
        )

        viewModel.attachments.observe(this) { newList ->
            adapterAttachments.submitList(newList)
            setResetBtnVisibility()
        }

        binding.rlAttachments.adapter = adapterAttachments
        binding.rlAttachments.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun openAttachment(pos: Int){
        val attachment = viewModel.attachments.value!![pos]
        if (attachment.uri != null) {

            if(attachment.isFromStorage){
                AttachmentOpenExternalHelper.open(this, attachment)
            }
            else{
                val fileName = attachment.serverData?.name ?: getFileName(attachment.uri!!)
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
        } else {
            onAddAttachment()
        }
    }

    private fun onDeleteAttachment(position: Int){
        viewModel.removeAttachmentAt(position)
    }

    private fun onAddAttachment(){
        attachmentHelper.showAttachmentPicker()
    }

    private fun initToolbar(){
        binding.ivBack.setOnClickListener {
            onBack()
        }
    }

    private fun setupViewModel(){

        viewModel.sendResponseCode.observe(this, Observer { statusCode ->
            ResponseStatusHelper.showStatusMessage(this, statusCode)
            showLoading(false)
        })

        viewModel.responsibles.observe(this) { list ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
            binding.tvResponsible.setAdapter(adapter)
        }

        viewModel.title.observe(this) { newText ->
            if (binding.tlTitle.editText?.text.toString() != newText) {
                binding.tlTitle.editText?.setText(newText)
            }
        }

        viewModel.description.observe(this) { newText ->
            if (binding.tlDescription.editText?.text.toString() != newText) {
                binding.tlDescription.editText?.setText(newText)
            }
        }

        viewModel.responsible.observe(this) { pos ->
            val adapter = binding.tvResponsible.adapter
            if (adapter != null && pos in 0 until adapter.count) {
                val item = adapter.getItem(pos)
                binding.tvResponsible.setText(item.toString(), false)
            }
        }

        viewModel.startDateTime.observe(this) { date ->
            date?.let {
                binding.etStartDate.setText(SimpleDateFormat(AppConstants.DATE_FORMAT, Locale.getDefault()).format(it.time))
                setResetBtnVisibility()
            }
        }

        viewModel.endDateTime.observe(this) { date ->
            date?.let {
                binding.etEndDate.setText(SimpleDateFormat(AppConstants.DATE_FORMAT, Locale.getDefault()).format(it.time))
                setResetBtnVisibility()
            }
        }

        if(viewModel.viewMode != TaskViewMode.CREATE){
            viewModel.getData()
        }
    }

    private fun initResponsible(){
        binding.tvResponsible.setOnItemClickListener { _, _, position, _ ->
            viewModel.setResponsible(position)
            binding.tvResponsible.clearFocus()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.tvResponsible.windowToken, 0)

            setResetBtnVisibility()
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
                    .withEndAction {
                        viewModel.resetChanges()
                        binding.ivReset.visibility = View.GONE
                    }
                    .start()
            }
            .start()
    }

    fun setUIMode(){

        when(viewModel.viewMode){
            TaskViewMode.CREATE -> {
                binding.ivChat.visibility = View.GONE
                binding.ivDelete.visibility = View.GONE
            }
            TaskViewMode.EDIT -> {

            }
            TaskViewMode.PREVIEW -> {
                binding.ivDelete.visibility = View.GONE
            }
        }
    }

    private fun onBack(){
        val resultIntent = Intent()
        resultIntent.putExtra(NavigationConstants.TASK_NAVIGATION_BACK, resultReturnType.ordinal)
        setResult(RESULT_OK, resultIntent)
        supportFinishAfterTransition()
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