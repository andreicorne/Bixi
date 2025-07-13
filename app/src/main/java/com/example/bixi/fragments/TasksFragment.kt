package com.example.bixi.fragments

import android.content.Intent
import android.graphics.Color
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.example.bixi.R
import com.example.bixi.databinding.FragmentTasksBinding
import com.example.bixi.viewModels.TasksViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bixi.activities.MainActivity
import com.example.bixi.activities.TaskDetailsActivity
import com.example.bixi.models.api.TaskListItem
import com.example.bixi.adapters.TaskListAdapter
import com.example.bixi.enums.TaskStatus

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

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
        viewModel.getList(viewModel.selectedStatus.value)

        binding.progressIndicator.setIndicatorColor(
            ContextCompat.getColor(context, R.color.md_theme_error),
            ContextCompat.getColor(context, R.color.md_theme_inversePrimary),
            ContextCompat.getColor(context, R.color.md_theme_onPrimaryFixed)
        )
    }

    private fun setupViewModel(){
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.serverStatusResponse.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Toast.makeText(context, "Eroare", Toast.LENGTH_SHORT).show()
            }
            showLoading(false)
        }

        viewModel.tasks.observe(viewLifecycleOwner) { newList ->
            adapterTasks.submitList(newList)
            if(newList.isEmpty()){
                binding.ivEmpty.visibility = View.VISIBLE
                binding.ivEmpty.startAnimation()
            }
            else{
                binding.ivEmpty.visibility = View.GONE
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
        startActivity(intent, options.toBundle())
    }

    private fun setupList(){
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        adapterTasks = TaskListAdapter{ position ->
//            val options = ActivityOptions.makeSceneTransitionAnimation(
//                it.context as Activity,
//                holder.container, // View și tag-ul său
//                "task_details_transition"
//            )

            val item = viewModel.tasks.value.get(position)
            // Navigăm către activitate cu animația specificată
            val intent = Intent(context, TaskDetailsActivity::class.java).apply {
                putExtra("TASK_TITLE", item.title)
                putExtra("TASK_DETAILS", item.description)
                putExtra("TASK_DATE", item.getFormattedPeriod())
            }

//            it.context.startActivity(intent, options.toBundle())
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapterTasks
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
            R.drawable.empty_task_13,
//            R.drawable.empty_task_16,
//            R.drawable.empty_task_15,
//            R.drawable.empty_task_14,
//            R.drawable.empty_task_13,
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