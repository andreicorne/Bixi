package com.example.bixi.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bixi.R
import com.example.bixi.viewModels.TimekeepingViewModel

class TimekeepingFragment : Fragment() {

    companion object {
        fun newInstance() = TimekeepingFragment()
    }

    private val viewModel: TimekeepingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_timekeeping, container, false)
    }
}