package com.example.bixi.adapters

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bixi.R
import com.example.bixi.activities.TaskDetailsActivity
import com.example.bixi.enums.AttachmentType
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.UITaskList
import com.example.bixi.models.api.TaskListItem

class TaskListAdapter(private val listener: (position: Int) -> Unit) :
    ListAdapter<UITaskList, TaskListAdapter.ViewHolder>(TasksDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTv: TextView = itemView.findViewById(R.id.tv_title)
        val detailsTv: TextView = itemView.findViewById(R.id.tv_details)
        val scheduleTv: TextView = itemView.findViewById(R.id.tv_schedule)

        init {
        }

        fun bind(item: UITaskList) {
            titleTv.text = item.title
            detailsTv.text = item.description
            scheduleTv.text = item.getFormattedPeriod()

            itemView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        // NU apelăm performClick aici
                    }
                }
                false // foarte important! permite sistemului să gestioneze click-ul nativ
            }

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
            .inflate(R.layout.row_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TasksDiffCallback : DiffUtil.ItemCallback<UITaskList>() {
    override fun areItemsTheSame(oldItem: UITaskList, newItem: UITaskList): Boolean {
        return oldItem.id == newItem.id && oldItem.title == newItem.title && oldItem.description == newItem.description &&
                oldItem.endDate == newItem.endDate && oldItem.assigneeName == newItem.assigneeName && oldItem.startDate == newItem.startDate
    }

    override fun areContentsTheSame(oldItem: UITaskList, newItem: UITaskList): Boolean {
        return oldItem == newItem
    }
}
