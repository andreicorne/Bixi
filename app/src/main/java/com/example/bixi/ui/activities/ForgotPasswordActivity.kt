package com.example.bixi.ui.activities

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.bixi.databinding.ActivityForgotPasswordBinding
import com.example.bixi.enums.FieldType
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.helper.ResponseStatusHelper
import com.example.bixi.helper.ValidatorHelper
import com.example.bixi.viewModels.ForgotPasswordViewModel

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
        setupBindings()
        setupInputValidations()
    }

    private fun setupBindings(){
        binding.tlEmail.bindTo(viewModel::setEmail)
    }

    private fun setupInputValidations(){
        binding.tlEmail.setValidators(ValidatorHelper.getValidatorsFor(FieldType.EMAIL))
    }

    private fun initListeners(){
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
        binding.resetPasswordButton.setOnClickListener {
            viewModel.forgotPassword()
        }
    }

    private fun setupViewModel(){
        viewModel.sendResponseCode.observe(this, Observer { statusCode ->
            ResponseStatusHelper.showStatusMessage(this, statusCode)
            showLoading(false)
        })
    }
}