package com.example.bixi.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.bixi.R
import com.example.bixi.models.Attachment
import com.example.bixi.models.AttachmentType

class AttachmentPreviewAdapter(
    private val onRemoveClick: (Attachment) -> Unit
) : ListAdapter<Attachment, AttachmentPreviewAdapter.PreviewViewHolder>(AttachmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attachment_preview, parent, false)
        return PreviewViewHolder(view, onRemoveClick)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PreviewViewHolder(
        itemView: View,
        private val onRemoveClick: (Attachment) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivPreview: ImageView = itemView.findViewById(R.id.ivPreview)
        private val documentContainer: LinearLayout = itemView.findViewById(R.id.documentContainer)
        private val ivDocumentIcon: ImageView = itemView.findViewById(R.id.ivDocumentIcon)
        private val tvDocumentName: TextView = itemView.findViewById(R.id.tvDocumentName)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(attachment: Attachment) {
            btnRemove.setOnClickListener {
                onRemoveClick(attachment)
            }

            when (attachment.type) {
                AttachmentType.IMAGE -> {
                    showImagePreview(attachment)
                }
                else -> {
                    showDocumentPreview(attachment)
                }
            }
        }

        private fun showImagePreview(attachment: Attachment) {
            ivPreview.visibility = View.VISIBLE
            documentContainer.visibility = View.GONE

            try {
                val uri = Uri.parse(attachment.url)
                Glide.with(itemView.context)
                    .load(uri)
                    .transform(RoundedCorners(12))
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .into(ivPreview)
            } catch (e: Exception) {
                Glide.with(itemView.context)
                    .load(attachment.url)
                    .transform(RoundedCorners(12))
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .into(ivPreview)
            }
        }

        private fun showDocumentPreview(attachment: Attachment) {
            ivPreview.visibility = View.GONE
            documentContainer.visibility = View.VISIBLE

            // Set appropriate icon
            val iconRes = when (attachment.type) {
                AttachmentType.VIDEO -> R.drawable.ic_video
                AttachmentType.AUDIO -> R.drawable.ic_audio_file
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

            ivDocumentIcon.setImageResource(iconRes)
            tvDocumentName.text = attachment.name
        }
    }

    class AttachmentDiffCallback : DiffUtil.ItemCallback<Attachment>() {
        override fun areItemsTheSame(oldItem: Attachment, newItem: Attachment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Attachment, newItem: Attachment): Boolean {
            return oldItem == newItem
        }
    }
}