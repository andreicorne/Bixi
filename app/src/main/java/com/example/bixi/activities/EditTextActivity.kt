package com.example.bixi.activities

import android.content.Intent
import android.os.Bundle
import android.transition.Transition
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.TransitionInflater
import com.example.bixi.R
import com.example.bixi.constants.NavigationConstants
import com.example.bixi.databinding.ActivityEditTextBinding
import com.example.bixi.databinding.ActivityTaskDetailsBinding
import com.google.android.material.textfield.TextInputEditText

class EditTextActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before setContentView for shared element transition
//        window.sharedElementEnterTransition = TransitionInflater.from(this)
//            .inflateTransition(android.R.transition.move)

        super.onCreate(savedInstanceState)

        binding = ActivityEditTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val initialDescription = intent.getStringExtra(NavigationConstants.EDITTEXT_TEXT_NAV) ?: ""
        binding.etDescription.setText(initialDescription)
        binding.etDescription.requestFocus()
        binding.etDescription.setSelection(initialDescription.length)

        binding.root.setOnClickListener {
            returnWithDescription()
        }

        onBackPressedDispatcher.addCallback(this) {
            returnWithDescription()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        returnWithDescription()
    }

    private fun returnWithDescription(){
        val resultIntent = Intent().apply {
            putExtra(NavigationConstants.EDITTEXT_TEXT_NAV, binding.etDescription.text.toString())
        }
        setResult(RESULT_OK, resultIntent)
        supportFinishAfterTransition()
    }
}
