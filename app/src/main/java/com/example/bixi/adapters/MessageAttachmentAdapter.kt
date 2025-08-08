package com.example.bixi.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.bixi.R
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.models.Attachment
import com.example.bixi.models.AttachmentType

class MessageAttachmentAdapter(
    private val attachments: List<Attachment>
) : RecyclerView.Adapter<MessageAttachmentAdapter.AttachmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_attachment, parent, false)
        return AttachmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        holder.bind(attachments[position])
    }

    override fun getItemCount(): Int = attachments.size

    class AttachmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val containerView: View = itemView.findViewById(R.id.attachmentContainer)
        private val imageView: ImageView = itemView.findViewById(R.id.ivAttachment)
        private val documentContainer: LinearLayout = itemView.findViewById(R.id.documentContainer)
        private val documentIcon: ImageView = itemView.findViewById(R.id.ivDocumentIcon)
        private val documentName: TextView = itemView.findViewById(R.id.tvDocumentName)

        fun bind(attachment: Attachment) {
            // Apply rounded background to container
            BackgroundStylerService.setRoundedBackground(
                view = containerView,
                backgroundColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceVariant),
                cornerRadius = 12f * itemView.context.resources.displayMetrics.density
            )

            when (attachment.type) {
                AttachmentType.IMAGE -> {
                    showImageAttachment(attachment)
                }
                else -> {
                    showDocumentAttachment(attachment)
                }
            }
        }

        private fun showImageAttachment(attachment: Attachment) {
            imageView.visibility = View.VISIBLE
            documentContainer.visibility = View.GONE

            try {
                val uri = Uri.parse(attachment.url)
                Glide.with(itemView.context)
                    .load(uri)
                    .transform(RoundedCorners(12))
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .override(200)
                    .priority(Priority.HIGH)
                    .into(imageView)
            } catch (e: Exception) {
                Glide.with(itemView.context)
                    .load(attachment.url)
                    .transform(RoundedCorners(12))
                    .placeholder(R.drawable.ic_image)
                    .override(200)
                    .priority(Priority.HIGH)
                    .error(R.drawable.ic_image)
                    .into(imageView)
            }
        }

        private fun showDocumentAttachment(attachment: Attachment) {
            imageView.visibility = View.GONE
            documentContainer.visibility = View.VISIBLE

            // Set appropriate icon based on file type
            val iconRes = when (attachment.type) {
                AttachmentType.VIDEO -> R.drawable.ic_video
                AttachmentType.AUDIO -> R.drawable.ic_document
                AttachmentType.DOCUMENT -> {
                    when {
                        attachment.name.endsWith(".pdf", ignoreCase = true) -> R.drawable.ic_document
                        attachment.name.endsWith(".doc", ignoreCase = true) ||
                                attachment.name.endsWith(".docx", ignoreCase = true) -> R.drawable.ic_document
                        attachment.name.endsWith(".xls", ignoreCase = true) ||
                                attachment.name.endsWith(".xlsx", ignoreCase = true) -> R.drawable.ic_document
                        attachment.name.endsWith(".ppt", ignoreCase = true) ||
                                attachment.name.endsWith(".pptx", ignoreCase = true) -> R.drawable.ic_document
                        attachment.name.endsWith(".zip", ignoreCase = true) ||
                                attachment.name.endsWith(".rar", ignoreCase = true) -> R.drawable.ic_document
                        else -> R.drawable.ic_document
                    }
                }
                else -> R.drawable.ic_document
            }

            documentIcon.setImageResource(iconRes)
            documentName.text = attachment.name
        }
    }
}