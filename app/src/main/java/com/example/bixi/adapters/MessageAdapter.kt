package com.example.bixi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bixi.R
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.models.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_chat, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val attachmentContainer: LinearLayout = itemView.findViewById(R.id.attachmentContainer)
        private val messageContainer: View = itemView.findViewById(R.id.messageContainer)

        fun bind(message: Message) {
            messageText.text = message.text
            messageTime.text = formatTime(message.timestamp)

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
                attachmentContainer.visibility = View.VISIBLE
                attachmentContainer.removeAllViews()

                message.attachments.forEach { attachment ->
                    val attachmentView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_attachment, attachmentContainer, false)

                    val attachmentName = attachmentView.findViewById<TextView>(R.id.attachmentName)
                    attachmentName.text = attachment.name

                    BackgroundStylerService.setRoundedBackground(
                        view = attachmentView,
                        backgroundColor = ContextCompat.getColor(attachmentView.context, R.color.md_theme_surfaceContainer_highContrast),
                        cornerRadius = 6f * attachmentView.context.resources.displayMetrics.density,
                    )

                    attachmentContainer.addView(attachmentView)
                }
            } else {
                attachmentContainer.visibility = View.GONE
            }
        }

        private fun formatTime(date: Date): String {
            val now = Date()
            val diff = now.time - date.time
            val minutes = diff / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24

            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min ago"
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