package com.example.bixi.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.bixi.AppSession
import com.example.bixi.R
import com.example.bixi.constants.StorageKeys
import com.example.bixi.databinding.ActivityLoginBinding
import com.example.bixi.enums.FieldType
import com.example.bixi.helper.ApiStatus
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.helper.ResponseStatusHelper
import com.example.bixi.helper.ValidatorHelper
import com.example.bixi.services.JsonConverterService
import com.example.bixi.services.SecureStorageService
import com.example.bixi.viewModels.LoginViewModel

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLoadingOverlay()

        setupViewModel()
        setupInputValidations()
        setupBindings()
        setupClickListeners()
        setStyles()

        viewModel.setEmail("info+test@extravel.be")
        viewModel.setPassword("dcttest123")
    }

    private fun setupBindings(){
        binding.tlUsername.bindTo(viewModel::setEmail)
        binding.tlPassword.bindTo(viewModel::setPassword)
    }

    private fun setupInputValidations(){
        binding.tlUsername.setValidators(ValidatorHelper.getValidatorsFor(FieldType.EMAIL))
        binding.tlPassword.setValidators(ValidatorHelper.getValidatorsFor(FieldType.LOGIN_PASSWORD))
    }

    private fun setupClickListeners(){
        binding.btnLogin.setOnClickListener{
            if(!isValidFields()){
                return@setOnClickListener
            }

            showLoading(true)
            viewModel.login()
        }

        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isValidFields() : Boolean{
        val isValid = listOf(
            binding.tlUsername.validate(),
            binding.tlPassword.validate()
        ).all { it }
        return isValid
    }

    private fun setupViewModel(){
        viewModel.email.observe(this) { newText ->
            if (binding.tlUsername.editText?.text.toString() != newText) {
                binding.tlUsername.editText?.setText(newText)
            }
        }

        viewModel.password.observe(this) { newText ->
            if (binding.tlPassword.editText?.text.toString() != newText) {
                binding.tlPassword.editText?.setText(newText)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.sendResponseCode.observe(this, Observer { statusCode ->
            if(ApiStatus.fromCode(statusCode) == ApiStatus.SERVER_SUCCESS){
                AppSession.user!!.user.password = viewModel.password.value
                SecureStorageService.putString(this, StorageKeys.USER_TOKEN, JsonConverterService.toJson(AppSession.user))
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                return@Observer
            }

            ResponseStatusHelper.showStatusMessage(this, statusCode)
            showLoading(false)
        })
    }

    private fun setStyles(){
//        Glide.with(this)
//            .load(R.drawable.ic_logo) // sau URL
//            .apply(RequestOptions.bitmapTransform(RoundedCorners(32))) // 32 = raza colțurilor
//            .into(binding.ivLogo)

        BackgroundStylerService.setRoundedBackground(
            view = binding.flLogoFrame,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 24f * resources.displayMetrics.density,
//            withRipple = true,
//            rippleColor = ContextCompat.getColor(this, R.color.ic_launcher_logo),
            strokeWidth = (2 * resources.displayMetrics.density).toInt(), // 2dp în px
            strokeColor = ContextCompat.getColor(this, R.color.md_theme_tertiaryContainer)
        )

        BackgroundStylerService.setRoundedBackground(
            view = binding.tvForgotPassword,
            backgroundColor = ContextCompat.getColor(this, R.color.md_theme_background),
            cornerRadius = 20f * resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(this, R.color.md_theme_surfaceVariant),
        )
    }
}