package com.example.bixi.ui.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.bixi.R
import com.example.bixi.databinding.FragmentTasksBinding
import com.example.bixi.viewModels.TasksViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.AppSession
import com.example.bixi.ui.activities.MainActivity
import com.example.bixi.ui.activities.TaskDetailsActivity
import com.example.bixi.ui.adapters.TaskListAdapter
import com.example.bixi.constants.NavigationConstants
import com.example.bixi.constants.StorageKeys
import com.example.bixi.enums.TaskActionType
import com.example.bixi.enums.TaskStatus
import com.example.bixi.helper.ApiStatus
import com.example.bixi.helper.ResponseStatusHelper
import com.example.bixi.services.JsonConverterService
import com.example.bixi.services.SecureStorageService

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var adapterTasks: TaskListAdapter

    companion object {
    }

    private val viewModel: TasksViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupList()
        setupBottomNavigation()
        setupEmptyTaskViewAnimation()
        setupViewModel()
        setupResultLauncher()
        viewModel.getList(viewModel.selectedStatus.value)

        binding.progressIndicator.setIndicatorColor(
            ContextCompat.getColor(requireContext(), R.color.md_theme_error),
            ContextCompat.getColor(requireContext(), R.color.md_theme_inversePrimary),
            ContextCompat.getColor(requireContext(), R.color.md_theme_onPrimaryFixed)
        )
    }

    private fun setupViewModel(){
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.sendResponseCode.observe(viewLifecycleOwner) { statusCode ->
            ResponseStatusHelper.showStatusMessage(requireContext(), statusCode)
            showLoading(false)
        }

        viewModel.tasks.observe(viewLifecycleOwner) { newList ->
            adapterTasks.submitList(newList){
                if(!viewModel.shouldNavigatoToTop){
                    return@submitList
                }
                viewModel.shouldNavigatoToTop = false
                activity?.runOnUiThread {
                    // Scroll doar la poziția 0 dacă este prima încărcare (lista goală)
                    if (adapterTasks.itemCount <= newList.size) {
                        binding.recyclerView.scrollToPosition(0)
                    }
                }
            }
            if(newList.isEmpty()){
                binding.ivEmpty.visibility = View.VISIBLE
                binding.ivEmpty.startAnimation()
            }
            else{
                binding.ivEmpty.visibility = View.GONE
            }
        }

        // Observer pentru hasMore status (opțional, pentru debugging)
        viewModel.hasMore.observe(viewLifecycleOwner) { hasMore ->
            Log.d("TasksFragment", "Has more tasks: $hasMore")
        }
    }

    private fun setupResultLauncher(){
        taskResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val taskActionReturnType = result.data?.getIntExtra(NavigationConstants.TASK_NAVIGATION_BACK,
                    TaskActionType.EMPTY.ordinal) ?: false
                when(taskActionReturnType){
                    TaskActionType.CREATE.ordinal -> {
                        binding.bottomNavigation.selectedItemId = R.id.menu_new
                    }
                    TaskActionType.DELETE.ordinal,
                    TaskActionType.EDIT.ordinal -> {
                        viewModel.getList(viewModel.selectedStatus.value)
                    }
                }
            }
        }
    }

    fun createTask(iconView: View){

        val intent = Intent(activity, TaskDetailsActivity::class.java)

        // Shared Element Transitions
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            iconView,
            "task_details_transition"
        )
        taskResultLauncher.launch(intent, options)
    }

    private fun setupList(){
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        adapterTasks = TaskListAdapter{ position ->
            val item = viewModel.tasks.value!![position]
            // Navigăm către activitate cu animația specificată
            val intent = Intent(context, TaskDetailsActivity::class.java).apply {
                putExtra(NavigationConstants.TASK_ID_NAV, item.id)
            }
            taskResultLauncher.launch(intent)
        }
        binding.recyclerView.adapter = adapterTasks

        // Adaugă scroll listener pentru paginare
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Verifică dacă trebuie să încarce mai multe task-uri
                // Threshold de 5 elemente înainte de sfârșitul listei
                if (!viewModel.isLoading.value!! &&
                    viewModel.hasMore.value!! &&
                    totalItemCount > 0 &&
                    lastVisibleItem + 5 >= totalItemCount) {

                    Log.d("TasksFragment", "Loading more tasks from next page...")
                    viewModel.loadMoreTasks()
                }
            }
        })
    }

    private fun setupEmptyTaskViewAnimation() {
        // Adaugă toate cele 20 de imagini
        val frameResources = listOf(
            R.drawable.empty_task_1,
            R.drawable.empty_task_2,
            R.drawable.empty_task_3,
            R.drawable.empty_task_4,
            R.drawable.empty_task_5,
            R.drawable.empty_task_6,
            R.drawable.empty_task_7,
            R.drawable.empty_task_8,
            R.drawable.empty_task_9,
            R.drawable.empty_task_10,
            R.drawable.empty_task_11,
            R.drawable.empty_task_12,
            R.drawable.empty_task_13,
            R.drawable.empty_task_14,
            R.drawable.empty_task_15,
            R.drawable.empty_task_16,
            R.drawable.empty_task_17,
            R.drawable.empty_task_18,
            R.drawable.empty_task_19,
            R.drawable.empty_task_19,
            R.drawable.empty_task_19,
            R.drawable.empty_task_18,
            R.drawable.empty_task_17,
            R.drawable.empty_task_16,
            R.drawable.empty_task_15,
            R.drawable.empty_task_14,
            R.drawable.empty_task_13,
            R.drawable.empty_task_14,
            R.drawable.empty_task_15,
            R.drawable.empty_task_15,
            R.drawable.empty_task_14,
            R.drawable.empty_task_13
        )

        binding.ivEmpty.addFrames(frameResources)
        binding.ivEmpty.setFrameDuration(30)
        binding.ivEmpty.setLooping(false)

        binding.ivEmpty.setOnAnimationCompleteListener {
        }
    }

    private fun showLoading(isLoading: Boolean, onFinish: (() -> Unit)? = null){
        (activity as MainActivity).showLoading(isLoading, onFinish)
    }

    private fun setupBottomNavigation(){

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_new -> viewModel.setStatus(TaskStatus.NEW)
                R.id.menu_in_progress -> viewModel.setStatus(TaskStatus.IN_PROGRESS)
                R.id.menu_overdue -> viewModel.setStatus(TaskStatus.ARCHIVED)
                R.id.menu_finished -> viewModel.setStatus(TaskStatus.DONE)
            }
            true
        }
    }
}