package com.example.bixi.services

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogService {

    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "OK",
        negativeText: String = "Anulează",
        iconResId: Int? = null,
        onConfirmed: () -> Unit,
        onCancelled: (() -> Unit)? = null
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { dialog, _ ->
                dialog.dismiss()
                onConfirmed()
            }
            .setNegativeButton(negativeText) { dialog, _ ->
                dialog.dismiss()
                onCancelled?.invoke()
            }

        iconResId?.let {
            builder.setIcon(it)
        }

        builder.show()
    }

    fun showMaterialDialog(
        context: Context,
        title: String,
        options: List<String>,
        onOptionSelected: (index: Int, option: String) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(options.toTypedArray()) { dialog, which ->
                onOptionSelected(which, options[which])
            }
            .show()
    }
}