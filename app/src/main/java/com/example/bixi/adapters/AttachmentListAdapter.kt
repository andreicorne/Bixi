package com.example.bixi.adapters

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bixi.R
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.models.AttachmentItem

class AttachmentListAdapter(private val items: List<AttachmentItem>, private val listener: (position: Int) -> Unit) :
    RecyclerView.Adapter<AttachmentListAdapter.ViewHolder>() {

//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
////        val textView: TextView = itemView.findViewById(R.id.tv_title)
//        val tapAreaView: FrameLayout = itemView.findViewById(R.id.fl_tap_area)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.row_attachment, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
////        holder.textView.text = items[position]
//        BackgroundStylerService.setRoundedBackground(
//            view = holder.tapAreaView,
//            backgroundColor = ContextCompat.getColor(holder.itemView.context, R.color.md_theme_background),
//            cornerRadius = 8f * holder.itemView.context.resources.displayMetrics.density,
//            withRipple = true,
//            rippleColor = ContextCompat.getColor(holder.itemView.context, R.color.md_theme_surfaceVariant),
////            strokeWidth = (1 * holder.itemView.context.resources.displayMetrics.density).toInt(), // 2dp în px
////            strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.md_theme_onBackground)
//        )
//
//        holder.tapAreaView.setOnClickListener{
//            listener.onAttachmentButtonClicked(position)
//        }
//    }
//
//    override fun getItemCount(): Int = items.size



    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageIv: ImageView = itemView.findViewById(R.id.iv_photo)
        private val documentIv: ImageView = itemView.findViewById(R.id.iv_document)
        private val textView: TextView = itemView.findViewById(R.id.tv_file_name)
        private val tapAreaView: RelativeLayout = itemView.findViewById(R.id.rl_tap_area)
        private val emptyContainer: View = itemView.findViewById(R.id.ll_empty_container)

        fun bind(item: AttachmentItem, position: Int) {
            if (item.uri != null) {
                val mimeType = itemView.context.contentResolver.getType(item.uri!!)
                if (mimeType?.startsWith("image/") == true) {
                    Glide.with(itemView.context).load(item.uri).into(imageIv)
                    textView.text = ""
                    documentIv.visibility = View.GONE
                    imageIv.visibility = View.VISIBLE
                } else {
//                    imageView.setImageResource(R.drawable.ic_document_generic)
                    textView.text = getFileName(itemView.context, item.uri!!)
                    imageIv.visibility = View.GONE
                    documentIv.visibility = View.VISIBLE
                }
                emptyContainer.visibility = View.GONE
            } else {
//                imageView.setImageResource(R.drawable.ic_placeholder)
                textView.text = ""
                imageIv.visibility = View.GONE
                documentIv.visibility = View.GONE
                emptyContainer.visibility = View.VISIBLE
            }

            tapAreaView.setOnClickListener {
                listener(adapterPosition)
            }

            BackgroundStylerService.setRoundedBackground(
                view = tapAreaView,
                backgroundColor = ContextCompat.getColor(itemView.context, R.color.md_theme_background),
                cornerRadius = 8f * itemView.context.resources.displayMetrics.density,
                withRipple = true,
                rippleColor = ContextCompat.getColor(itemView.context, R.color.md_theme_surfaceVariant),
    //            strokeWidth = (1 * holder.itemView.context.resources.displayMetrics.density).toInt(), // 2dp în px
    //            strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.md_theme_onBackground)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_attachment, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
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
