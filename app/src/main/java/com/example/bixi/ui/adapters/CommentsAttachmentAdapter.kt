package com.example.bixi.ui.adapters

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bixi.R
import com.example.bixi.enums.AttachmentType
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.models.AttachmentItem
import com.bumptech.glide.Priority
import com.example.bixi.helper.ExtensionHelper

class CommentsAttachmentAdapter(private val openAttachmentListener: (position: Int) -> Unit,
                            private val removeListener: (position: Int) -> Unit) :
    ListAdapter<AttachmentItem, CommentsAttachmentAdapter.ViewHolder>(AttachmentDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageIv: ImageView = itemView.findViewById(R.id.iv_photo)
        private val fileNameView: TextView = itemView.findViewById(R.id.tv_file_name)
        private val extensionTextView: TextView = itemView.findViewById(R.id.tv_extension)
        private val extensionView: View = itemView.findViewById(R.id.ll_extension_container)
        private val documentContainer: View = itemView.findViewById(R.id.ll_document_container)
        private val photoContainer: View = itemView.findViewById(R.id.rl_photo_container)

        init {
            BackgroundStylerService.setRoundedBackground(
                view = itemView,
                backgroundColor = ContextCompat.getColor(itemView.context, R.color.md_theme_background),
                cornerRadius = 10f * itemView.context.resources.displayMetrics.density,
                withRipple = true,
                rippleColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceVariant),
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
                    item.serverData?.fileUrl?.lowercase()?.matches(Regex(""".*\.(jpg|jpeg|png|gif|webp|bmp)$""")) == true -> true

                    else -> {
                        // Fallback: verifică MIME type-ul local pentru URI-uri locale
                        val mimeType = itemView.context.contentResolver.getType(item.uri!!)
                        mimeType?.startsWith("image/") == true
                    }
                }

                if (isImage) {
                    Glide.with(itemView.context)
                        .load(item.uri)
                        .priority(Priority.HIGH)
                        .override(200)
                        .into(imageIv)
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

                    extensionTextView.text = ExtensionHelper.getExtension(fileName).uppercase()
                    BackgroundStylerService.setRoundedBackground(
                        view = extensionView,
                        backgroundColor = ExtensionHelper.getColorByExtension(fileName),
                        cornerRadius = 4f * itemView.context.resources.displayMetrics.density,
                    )
                }
            } else {
                documentContainer.visibility = View.GONE
                photoContainer.visibility = View.GONE
            }

            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    openAttachmentListener(pos)
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
            .inflate(R.layout.row_comment_attachment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}