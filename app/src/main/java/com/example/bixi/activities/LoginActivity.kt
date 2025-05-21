package com.example.bixi.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.bixi.R
import com.example.bixi.customViews.ValidatedTextInputLayout
import com.example.bixi.databinding.ActivityLoginBinding
import com.example.bixi.databinding.ActivityMainBinding
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.viewModels.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupInputValidations()
        setupClickListeners()
        setStyles()
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

            loginViewModel.login(binding.etUsername.text.toString(), binding.etPassword.text.toString())
        }
    }

    private fun setupViewModel(){
        loginViewModel.loginSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Login reușit!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Login eșuat!", Toast.LENGTH_SHORT).show()
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
    }
}