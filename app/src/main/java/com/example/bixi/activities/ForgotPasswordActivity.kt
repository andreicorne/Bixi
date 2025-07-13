package com.example.bixi.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bixi.AppSession
import com.example.bixi.R
import com.example.bixi.constants.StorageKeys
import com.example.bixi.databinding.ActivityForgotPasswordBinding
import com.example.bixi.databinding.ActivityLoginBinding
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.services.JsonConverterService
import com.example.bixi.services.SecureStorageService
import com.example.bixi.viewModels.ForgotPasswordViewModel
import com.example.bixi.viewModels.LoginViewModel

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLoadingOverlay()

        setupViewModel()
        initListeners()
    }

    private fun initListeners(){
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViewModel(){

        binding.resetPasswordButton.setOnClickListener {
            viewModel.forgotPassword(binding.etEmail.text.toString())
        }

        viewModel.apiResult.observe(this) { success ->
            if (success) {
                showLoading(false, {
                    Toast.makeText(this, getString(R.string.server_forgot_password_success), Toast.LENGTH_SHORT).show()
                })
            } else {
                Toast.makeText(this, getString(R.string.server_error_generic_message), Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }
}