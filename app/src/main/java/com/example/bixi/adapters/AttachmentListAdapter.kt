package com.example.bixi.adapters

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bixi.R
import com.example.bixi.enums.AttachmentType
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.models.AttachmentItem
import androidx.core.graphics.toColorInt

class AttachmentListAdapter(private val listener: (position: Int) -> Unit) :
    ListAdapter<AttachmentItem, AttachmentListAdapter.ViewHolder>(AttachmentDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageIv: ImageView = itemView.findViewById(R.id.iv_photo)
        private val fileNameView: TextView = itemView.findViewById(R.id.tv_file_name)
        private val emptyContainer: View = itemView.findViewById(R.id.rl_empty_container)
        private val documentContainer: View = itemView.findViewById(R.id.rl_document_container)
        private val photoContainer: View = itemView.findViewById(R.id.rl_photo_container)
        private val editContainer: View = itemView.findViewById(R.id.fl_edit_container)

        init {
            BackgroundStylerService.setRoundedBackground(
                view = itemView,
                backgroundColor = ContextCompat.getColor(itemView.context, R.color.md_theme_background),
                cornerRadius = 10f * itemView.context.resources.displayMetrics.density,
                withRipple = true,
                rippleColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceVariant),
            )

            BackgroundStylerService.setRoundedBackground(
                view = editContainer,
                backgroundColor = "#60000000".toColorInt(),
                cornerRadius = 10f * itemView.context.resources.displayMetrics.density,
            )

            BackgroundStylerService.setRoundedBackground(
                view = documentContainer,
                backgroundColor = ContextCompat.getColor(itemView.context, R.color.md_theme_background),
                cornerRadius = 10f * itemView.context.resources.displayMetrics.density,
                withRipple = true,
                rippleColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceVariant),
                strokeWidth = (1 * itemView.context.resources.displayMetrics.density).toInt(),
                strokeColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceVariant)
            )
        }

        fun bind(item: AttachmentItem) {
            if (item.uri != null) {
                // Verifică dacă este imagine folosind type-ul din AttachmentItem sau serverData
                val isImage = when {
                    item.type == AttachmentType.IMAGE -> true
                    item.serverData?.type?.startsWith("image/") == true -> true
                    else -> {
                        // Fallback: verifică MIME type-ul local pentru URI-uri locale
                        val mimeType = itemView.context.contentResolver.getType(item.uri!!)
                        mimeType?.startsWith("image/") == true
                    }
                }

                if (isImage) {
                    Glide.with(itemView.context).load(item.uri).into(imageIv)
                    photoContainer.visibility = View.VISIBLE
                    documentContainer.visibility = View.GONE
                    item.type = AttachmentType.IMAGE
                } else {
                    photoContainer.visibility = View.GONE
                    documentContainer.visibility = View.VISIBLE

                    // Folosește numele din serverData dacă există, altfel extrage din URI
                    val fileName = item.serverData?.name ?: getFileName(itemView.context, item.uri!!)
                    fileNameView.text = fileName
                    item.type = AttachmentType.DOCUMENT
                }
                emptyContainer.visibility = View.GONE
                editContainer.visibility = View.VISIBLE
            } else {
                emptyContainer.visibility = View.VISIBLE
                documentContainer.visibility = View.GONE
                photoContainer.visibility = View.GONE
                editContainer.visibility = View.GONE
            }

            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    // Dacă nu este empty container, deschide viewer-ul
                    if (item.uri != null) {
                        val fileName = item.serverData?.name ?: getFileName(itemView.context, item.uri!!)
                        com.example.bixi.activities.AttachmentViewerActivity.startActivity(
                            context = itemView.context,
                            uri = item.uri,
                            type = item.type,
                            serverData = item.serverData,
                            name = fileName
                        )
                    } else {
                        // Pentru empty container, apelează listener-ul original
                        listener(pos)
                    }
                }
            }
        }

        private fun getFileName(context: Context, uri: Uri): String {
            var result = "Document"
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = it.getString(index)
                }
            }
            return result
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_attachment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class AttachmentDiffCallback : DiffUtil.ItemCallback<AttachmentItem>() {
    override fun areItemsTheSame(oldItem: AttachmentItem, newItem: AttachmentItem): Boolean {
        return oldItem.id == newItem.id && oldItem.type == newItem.type && oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: AttachmentItem, newItem: AttachmentItem): Boolean {
        return oldItem == newItem
    }
}