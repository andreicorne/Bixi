package com.example.bixi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.models.CheckItem

class CheckListAdapter(private val listener: (position: Int) -> Unit) :
    ListAdapter<CheckItem, CheckListAdapter.ViewHolder>(CheckDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_title)

        init {
//            BackgroundStylerService.setRoundedBackground(
//                view = itemView,
//                backgroundColor = ContextCompat.getColor(itemView.context, R.color.md_theme_background),
//                cornerRadius = 10f * itemView.context.resources.displayMetrics.density,
//                withRipple = true,
//                rippleColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceVariant),
//            )
        }

        fun bind(item: CheckItem) {
            textView.text = item.text
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    listener(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_check_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CheckDiffCallback : DiffUtil.ItemCallback<CheckItem>() {
    override fun areItemsTheSame(oldItem: CheckItem, newItem: CheckItem): Boolean {
        return oldItem.done == newItem.done && oldItem.text == newItem.text
    }

    override fun areContentsTheSame(oldItem: CheckItem, newItem: CheckItem): Boolean {
        return oldItem == newItem
    }
}