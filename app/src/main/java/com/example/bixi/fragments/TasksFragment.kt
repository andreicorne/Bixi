package com.example.bixi.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bixi.R
import com.example.bixi.databinding.ActivityMainBinding
import com.example.bixi.databinding.FragmentTasksBinding
import com.example.bixi.viewModels.TasksViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bixi.models.api.TaskListItem
import com.example.bixi.adapters.ListAdapter

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = TasksFragment()
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
    }

    private fun setupList(){
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        val items = listOf(
            TaskListItem("Verificare armătură fundație – Șantier Cluj, Lot 3",
                "Verifică execuția armăturii pentru fundația contin...", "08:32 - 13:43"),
            TaskListItem("Verificare armătură fundație – Șantier Cluj, Lot 3",
                "Verifică execuția armăturii pentru fundația contin...", "08:32 - 13:43"),
            TaskListItem("Verificare armătură fundație – Șantier Cluj, Lot 3",
                "Verifică execuția armăturii pentru fundația contin...", "08:32 - 13:43"),
            TaskListItem(
                title = "Concediu",
                details = "Zile libere planificate",
                dateRange = "08:32 - 13:43"
            ),
            TaskListItem(
                title = "Eveniment",
                details = "Conferință Android",
                dateRange = "08:32 - 13:43"
            ),
            TaskListItem("Verificare armătură fundație – Șantier Cluj, Lot 3",
                "Verifică execuția armăturii pentru fundația contin...", "08:32 - 13:43"),
            TaskListItem(
                title = "Concediu",
                details = "Zile libere planificate",
                dateRange = "08:32 - 13:43"
            ),
            TaskListItem("Verificare armătură fundație – Șantier Cluj, Lot 3",
                "Verifică execuția armăturii pentru fundația contin...", "08:32 - 13:43"),
            TaskListItem(
                title = "Concediu",
                details = "Zile libere planificate",
                dateRange = "08:32 - 13:43"
            ),
        )

        val adapter = ListAdapter(items)
        binding.recyclerView.adapter = adapter
    }

    private fun setupBottomNavigation(){

        binding.toggleButton.check(binding.btnNew.id)

        binding.toggleButton.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_new -> showNewTasks()
                    R.id.btn_in_progress -> showNewTasks()
                    R.id.btn_overdue -> showNewTasks()
                }
            }
        }
    }

    private fun showNewTasks(){

    }
}