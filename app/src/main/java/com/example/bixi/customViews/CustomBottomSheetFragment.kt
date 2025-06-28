package com.example.bixi.customViews

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bixi.R
import com.example.bixi.adapters.BottomSheetOptionsAdapter
import com.example.bixi.databinding.BottomSheetBinding
import com.example.bixi.models.BottomSheetItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CustomBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var options: List<BottomSheetItem>
    private var listener: ((BottomSheetItem) -> Unit)? = null

    // Setter pentru lista de opțiuni și event listener
    fun setOptions(options: List<BottomSheetItem>, listener: (BottomSheetItem) -> Unit) {
        this.options = options
        this.listener = listener
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Activează drag handle-ul
        dialog.behavior.isDraggable = true
        dialog.behavior.isFitToContents = true

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = BottomSheetBinding.inflate(inflater, container, false)

        // Setează un adapter pentru RecyclerView
        val adapter = BottomSheetOptionsAdapter(options) { option ->
            listener?.invoke(option)
            dismiss()  // Închide BottomSheet după ce o opțiune este selectată
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(inflater.context, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter

        return binding.root
    }
}
