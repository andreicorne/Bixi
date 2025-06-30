package com.example.bixi.adapters

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.activities.TaskDetailsActivity
import com.example.bixi.models.api.TaskListItem

class TaskListAdapter(
    private val items: List<TaskListItem>
) : RecyclerView.Adapter<TaskListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTv: TextView = itemView.findViewById(R.id.tv_title)
        val detailsTv: TextView = itemView.findViewById(R.id.tv_details)
        val scheduleTv: TextView = itemView.findViewById(R.id.tv_schedule)
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
        holder.scheduleTv.text = item.dateRange

        // Click listener
        holder.itemView.setOnClickListener {
//            val options = ActivityOptions.makeSceneTransitionAnimation(
//                it.context as Activity,
//                holder.container, // View și tag-ul său
//                "task_details_transition"
//            )

            // Navigăm către activitate cu animația specificată
            val intent = Intent(it.context, TaskDetailsActivity::class.java).apply {
                putExtra("TASK_TITLE", item.title)
                putExtra("TASK_DETAILS", item.details)
                putExtra("TASK_DATE", item.dateRange)
            }

//            it.context.startActivity(intent, options.toBundle())
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}
