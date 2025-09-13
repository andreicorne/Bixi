package com.example.bixi.ui.adapters

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.bixi.R

class ResponsibleAdapter(
    context: Context,
    private val items: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, items) {

    var selectedIndex: Int = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView

        // textul afișat în TextView (cel din câmpul selectat)
        view.text = items[position]

        if (parent is AdapterView<*>) {
            if (position == selectedIndex) {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_surfaceContainer_highContrast))
                view.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurface))
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurface))
            }
        }

        return view
    }
}
