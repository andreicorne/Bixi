package com.example.bixi.fragments

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bixi.R
import com.example.bixi.databinding.FragmentTasksBinding
import com.example.bixi.viewModels.TasksViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bixi.activities.TaskDetailsActivity
import com.example.bixi.models.api.TaskListItem
import com.example.bixi.adapters.TaskListAdapter

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

    fun createTask(){
        val intent = Intent(activity, TaskDetailsActivity::class.java)
        startActivity(intent)
    }

    private fun setupList(){
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        val items = listOf(
            TaskListItem(
                title = "Turnare fundație",
                details = "Echipa va pregăti terenul, va monta cofrajele și va turna betonul pentru fundația principală a clădirii. Asigurați-vă că totul este aliniat și nivelat înainte de turnare.",
                dateRange = "07:00 - 12:00"
            ),
            TaskListItem(
                title = "Montare armătură",
                details = "Montarea barelor de oțel în cofrajele fundației. Este important să fie respectate distanțele de acoperire și poziționarea conform planului tehnic.",
                dateRange = "08:00 - 11:30"
            ),
            TaskListItem(
                title = "Zidărie parter",
                details = "Zidirea pereților exteriori și interiori de la parter cu blocuri ceramice. Se va verifica periodic alinierea cu firul cu plumb.",
                dateRange = "09:00 - 14:00"
            ),
            TaskListItem(
                title = "Izolație termică fațadă",
                details = "Aplicarea panourilor de polistiren expandat pe exteriorul clădirii, urmată de plasă de fibră și strat de adeziv. Asigurați o lipire uniformă și evitarea punților termice.",
                dateRange = "10:00 - 15:00"
            ),
            TaskListItem(
                title = "Montaj ferestre termopan",
                details = "Echipa va monta ferestrele din PVC cu geam termopan la etajul 1. Se va verifica etanșarea și alinierea corectă a fiecărei unități.",
                dateRange = "08:30 - 13:00"
            ),
            TaskListItem(
                title = "Instalații electrice interioare",
                details = "Tragerea cablurilor electrice prin pereți, amplasarea dozelor și pregătirea pentru tabloul electric. Trebuie respectate toate normele de siguranță în vigoare.",
                dateRange = "07:30 - 12:30"
            ),
            TaskListItem(
                title = "Turnare șapă autonivelantă",
                details = "Aplicarea unui strat de șapă autonivelantă pe pardoselile camerelor de la parter. Se va lucra în echipe mici pentru acuratețe.",
                dateRange = "09:00 - 11:00"
            ),
            TaskListItem(
                title = "Montaj structură acoperiș",
                details = "Montarea grinzilor și căpriorilor acoperișului. Este necesară atenție sporită la unghiuri și fixarea elementelor structurale pentru siguranță.",
                dateRange = "08:00 - 14:00"
            ),
            TaskListItem(
                title = "Vopsire interioară camere",
                details = "Aplicarea vopselei lavabile în camerele deja tencuite. Se va folosi grund și două straturi de lavabilă, cu verificare vizuală pentru uniformitate.",
                dateRange = "10:00 - 16:00"
            ),
            TaskListItem(
                title = "Curățenie finală de șantier",
                details = "Colectarea și evacuarea resturilor de materiale, măturarea și curățarea generală a spațiilor de lucru pentru predarea către beneficiar.",
                dateRange = "14:00 - 17:00"
            )
        )

        val adapter = TaskListAdapter(items)
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