package com.example.bixi.ui.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.Message
import java.text.SimpleDateFormat
import java.util.*

class CommentsAdapter(private val openAttachmentListener: (attachment: AttachmentItem) -> Unit) : ListAdapter<Message, CommentsAdapter.CommentsViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_comment, parent, false)
        return CommentsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val attachmentList: LinearLayout = itemView.findViewById(R.id.attachmentContainer)
        private val messageContainer: View = itemView.findViewById(R.id.messageContainer)

        fun bind(message: Message) {
            // Show/hide message text
            if (message.text.isNotEmpty()) {
                messageText.visibility = View.VISIBLE
                messageText.text = message.text
            } else {
                messageText.visibility = View.GONE
            }

            messageTime.text = formatTime(message.timestamp, itemView.context)

            // Align message based on sender
            val params = messageContainer.layoutParams as FrameLayout.LayoutParams
            if (message.isFromCurrentUser) {
                params.gravity = android.view.Gravity.END
                params.marginStart = itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_large)
                params.marginEnd = itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_small)
                messageContainer.setBackgroundResource(R.drawable.message_background_sent)
                messageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_onPrimary))
                messageTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_onPrimary))
            } else {
                params.gravity = android.view.Gravity.START
                params.marginStart = itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_small)
                params.marginEnd = itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_large)
                messageContainer.setBackgroundResource(R.drawable.message_background_received)
                messageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_onBackground))
                messageTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_onBackground))
            }
            messageContainer.layoutParams = params

            // Handle attachments
            if (message.attachments.isNotEmpty()) {
                attachmentList.visibility = View.VISIBLE
                displayAttachmentsHorizontal(message)
            } else {
                attachmentList.visibility = View.GONE
            }
        }

        private fun displayAttachmentsHorizontal(message: Message) {
            val recyclerView = RecyclerView(itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(100)
                ).apply {
                    setMargins(0, dpToPx(4), 0, 0)
                }
                layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = CommentsAttachmentAdapter(
                    openAttachmentListener = { position ->
                        openAttachmentListener(message.attachments[position])
//                        openAttachment(position)
                    },
                    removeListener = { position ->
//                        onDeleteAttachment(position)
                    }
                )
                isNestedScrollingEnabled = false // Disable nested scrolling pentru performance
            }

            (recyclerView.adapter as CommentsAttachmentAdapter).submitList(message.attachments)

            attachmentList.addView(recyclerView)
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * itemView.context.resources.displayMetrics.density).toInt()
        }

        private fun formatTime(date: Date, context: Context): String {
            val now = Date()
            val diff = now.time - date.time
            val minutes = diff / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24

            return when {
                minutes < 1 -> context.getString(R.string.now)
                minutes < 60 -> "$minutes min"
                hours < 24 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                days < 7 -> SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}