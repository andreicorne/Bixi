package com.example.bixi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.databinding.BottomSheetItemBinding
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.models.BottomSheetItem

class BottomSheetOptionsAdapter(
    private val options: List<BottomSheetItem>,
    private val itemClick: (BottomSheetItem) -> Unit
) : RecyclerView.Adapter<BottomSheetOptionsAdapter.OptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = BottomSheetItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        BackgroundStylerService.setRoundedBackground(
            view = binding.root,
            backgroundColor = ContextCompat.getColor(binding.root.context, R.color.md_theme_surfaceContainerLow),
            cornerRadius = 10f * binding.root.context.resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(binding.root.context, R.color.md_theme_surfaceVariant),
        )

        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size

    inner class OptionViewHolder(private val binding: BottomSheetItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: BottomSheetItem) {
            binding.optionText.text = option.name
            binding.root.setOnClickListener { itemClick(option) }
        }
    }
}