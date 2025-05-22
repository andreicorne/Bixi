package com.example.bixi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.models.api.TaskListItem

class ListAdapter(private val items: List<TaskListItem>) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTv: TextView = itemView.findViewById(R.id.tv_title)
        val detailsTv: TextView = itemView.findViewById(R.id.tv_details)
        val intervalTextView: TextView = itemView.findViewById(R.id.tv_interval)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.titleTv.text = item.title
        holder.detailsTv.text = item.details
        holder.intervalTextView.text = item.dateRange
    }

    override fun getItemCount(): Int = items.size
}
