package com.example.bixi.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bixi.AppSession
import com.example.bixi.R
import com.example.bixi.constants.StorageKeys
import com.example.bixi.customViews.ValidatedTextInputLayout
import com.example.bixi.databinding.ActivityLoginBinding
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.services.JsonConverterService
import com.example.bixi.services.SecureStorageService
import com.example.bixi.viewModels.LoginViewModel

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()

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
        setupClickListeners()
        setStyles()

//        binding.etUsername.setText("info@extravel.be")
//        binding.etPassword.setText("dcttest123")
    }

    private fun setupInputValidations(){
        binding.usernameLayout.setValidators(
            listOf(
                ValidatedTextInputLayout.Validator(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"), "Formatul nu este bun"),
                ValidatedTextInputLayout.Validator(Regex(".{8,}"), "Trebuie să aibă minim 8 caractere")
            )
        )
    }

    private fun setupClickListeners(){
        binding.btnLogin.setOnClickListener{
//            binding.usernameLayout.validate()

            showLoading(true)
            loginViewModel.login(binding.etUsername.text.toString(), binding.etPassword.text.toString())
        }

        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupViewModel(){
        loginViewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        loginViewModel.loginSuccess.observe(this) { success ->
            if (success) {
                showLoading(false, {
                    AppSession.user!!.user.password = binding.etPassword.text.toString()
                    SecureStorageService.putString(this, StorageKeys.USER_TOKEN, JsonConverterService.toJson(AppSession.user))
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                })
            } else {
                Toast.makeText(this, getString(R.string.server_error_generic_message), Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
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