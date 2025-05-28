package com.example.bixi.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bixi.R
import com.example.bixi.databinding.FragmentTasksBinding
import com.example.bixi.databinding.FragmentTimekeepingBinding
import com.example.bixi.viewModels.TasksViewModel
import com.example.bixi.viewModels.TimekeepingViewModel

class TimekeepingFragment : Fragment() {
    private var _binding: FragmentTimekeepingBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = TasksFragment()
    }

//    private val viewModel: TasksViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimekeepingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}