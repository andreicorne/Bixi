package com.example.bixi.ui.adapters

import android.content.Context
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
import com.example.bixi.interfaces.IMessage
import com.example.bixi.models.AttachmentItem
import com.example.bixi.models.MessageItem
import com.example.bixi.models.MessageTimeSeparator
import java.text.SimpleDateFormat
import java.util.*

class CommentsAdapter(private val openAttachmentListener: (attachment: AttachmentItem) -> Unit) :
    ListAdapter<IMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_MESSAGE = 1
        private const val VIEW_TYPE_TIME_SEPARATOR = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MessageItem -> VIEW_TYPE_MESSAGE
            is MessageTimeSeparator -> VIEW_TYPE_TIME_SEPARATOR
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_comment, parent, false)
                CommentsViewHolder(view)
            }
            VIEW_TYPE_TIME_SEPARATOR -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_date_separator, parent, false)
                TimeSeparatorViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CommentsViewHolder -> holder.bind(getItem(position) as MessageItem)
            is TimeSeparatorViewHolder -> holder.bind(getItem(position) as MessageTimeSeparator)
        }
    }

    inner class CommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val attachmentList: LinearLayout = itemView.findViewById(R.id.attachmentContainer)
        private val messageContainer: View = itemView.findViewById(R.id.messageContainer)

        fun bind(messageItem: MessageItem) {
            // Show/hide message text
            if (messageItem.text.isNotEmpty()) {
                messageText.visibility = View.VISIBLE
                messageText.text = messageItem.text
            } else {
                messageText.visibility = View.GONE
            }

            messageTime.text = formatTime(messageItem.timestamp, itemView.context)

            // Align message based on sender
            val params = messageContainer.layoutParams as FrameLayout.LayoutParams
            if (messageItem.isFromCurrentUser) {
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
            if (messageItem.attachments.isNotEmpty()) {
                attachmentList.visibility = View.VISIBLE
                displayAttachmentsHorizontal(messageItem)
            } else {
                attachmentList.visibility = View.GONE
            }
        }

        private fun displayAttachmentsHorizontal(messageItem: MessageItem) {
            // Clear previous attachments
            attachmentList.removeAllViews()

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
                        openAttachmentListener(messageItem.attachments[position])
                    },
                    removeListener = { position ->
                        // Handle remove if needed
                    }
                )
                isNestedScrollingEnabled = false // Disable nested scrolling pentru performance
            }

            (recyclerView.adapter as CommentsAttachmentAdapter).submitList(messageItem.attachments)
            attachmentList.addView(recyclerView)
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * itemView.context.resources.displayMetrics.density).toInt()
        }
    }

    inner class TimeSeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val separatorText: TextView = itemView.findViewById(R.id.dateSeparatorText)

        fun bind(timeSeparator: MessageTimeSeparator) {
            separatorText.text = formatDateSeparator(timeSeparator.timestamp, itemView.context)
        }

        private fun formatDateSeparator(date: Date, context: Context): String {
            val now = Date()
            val calendar = Calendar.getInstance()
            val today = calendar.apply { time = now }
            val yesterday = calendar.apply { add(Calendar.DAY_OF_YEAR, -1) }
            val messageCalendar = Calendar.getInstance().apply { time = date }

            return when {
                isSameDay(messageCalendar, today) -> context.getString(R.string.today)
                isSameDay(messageCalendar, yesterday) -> context.getString(R.string.yesterday)
                else -> SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(date)
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
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

    class MessageDiffCallback : DiffUtil.ItemCallback<IMessage>() {
        override fun areItemsTheSame(oldItem: IMessage, newItem: IMessage): Boolean {
            return when {
                oldItem is MessageItem && newItem is MessageItem -> oldItem.id == newItem.id
                oldItem is MessageTimeSeparator && newItem is MessageTimeSeparator ->
                    oldItem.timestamp == newItem.timestamp
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: IMessage, newItem: IMessage): Boolean {
            return when {
                oldItem is MessageItem && newItem is MessageItem -> {
                    oldItem.id == newItem.id &&
                            oldItem.text == newItem.text &&
                            oldItem.timestamp == newItem.timestamp &&
                            areAttachmentsEqual(oldItem.attachments, newItem.attachments) &&
                            oldItem.isFromCurrentUser == newItem.isFromCurrentUser
                }
                oldItem is MessageTimeSeparator && newItem is MessageTimeSeparator -> {
                    oldItem.timestamp == newItem.timestamp
                }
                else -> false
            }
        }

        private fun areAttachmentsEqual(oldAttachments: List<AttachmentItem>, newAttachments: List<AttachmentItem>): Boolean {
            if (oldAttachments.size != newAttachments.size) return false

            for (i in oldAttachments.indices) {
                val oldAttachment = oldAttachments[i]
                val newAttachment = newAttachments[i]

                if (oldAttachment.id != newAttachment.id ||
                    oldAttachment.uri?.toString() != newAttachment.uri?.toString()) {
                    return false
                }
            }

            return true
        }
    }
}