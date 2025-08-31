package com.example.bixi.ui.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.models.CheckItem

class CheckListAdapter(
    private val listener: (item: CheckItem, isChecked: Boolean) -> Unit,
    private val deleteListener: (item: CheckItem) -> Unit,
    private val startDragListener: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<CheckItem, CheckListAdapter.ViewHolder>(CheckDiffCallback()) {


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_title)
        private val checkBox: com.google.android.material.checkbox.MaterialCheckBox =
            itemView.findViewById(R.id.cb_check)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete)
        private val dragHandle: ImageView = itemView.findViewById(R.id.iv_drag_handle)

        init {
            BackgroundStylerService.setRoundedBackground(
                view = deleteButton,
                backgroundColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceContainer_highContrast),
                cornerRadius = 48f * itemView.context.resources.displayMetrics.density,
                withRipple = true,
                rippleColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceVariant),
            )

            dragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startDragListener(this)
                }
                false
            }
        }

        fun bind(item: CheckItem) {
            textView.text = item.text

            // 🔧 Dezactivăm listenerul înainte să setăm isChecked
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = item.done
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                listener(item, isChecked)
            }

            // Strike-through și culoare
            if (item.done) {
                textView.paintFlags = textView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_tertiary))
            } else {
                textView.paintFlags = textView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_onSurface))
            }

            // Drag handle visibility logic
            dragHandle.visibility = if (!item.shouldDisplayDragHandle || item.done) {
                View.GONE
            } else {
                View.VISIBLE
            }

            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }

            deleteButton.setOnClickListener {
                deleteListener(item)
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
        return oldItem.done == newItem.done && oldItem.text == newItem.text && oldItem.shouldDisplayDragHandle == newItem.shouldDisplayDragHandle
    }

    override fun areContentsTheSame(oldItem: CheckItem, newItem: CheckItem): Boolean {
        return oldItem == newItem
    }
}