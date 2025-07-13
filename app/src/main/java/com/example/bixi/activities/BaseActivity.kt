package com.example.bixi.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.bixi.R

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var loadingOverlay: View
    private var contentView: View? = null

    fun setupLoadingOverlay() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)

        contentView = rootView

        loadingOverlay = layoutInflater.inflate(R.layout.view_loading, rootView, false)
        loadingOverlay.visibility = View.GONE

        rootView.addView(loadingOverlay)
        loadingOverlay.alpha = 0.0f
    }

    fun showLoading(show: Boolean, onFinish: (() -> Unit)? = null) {
        val duration = 300L

        if (show) {
            loadingOverlay.visibility = View.VISIBLE
            loadingOverlay.animate()
                .alpha(1f)
                .setDuration(duration)
                .start()

            contentView?.animate()
                ?.setDuration(duration)
                ?.start()

        } else {
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction {
                    loadingOverlay.visibility = View.GONE
                    onFinish?.invoke()
                }
                .start()

            contentView?.animate()
                ?.alpha(1f)
                ?.setDuration(duration)
                ?.start()
        }
    }

}
