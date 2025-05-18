package com.example.bixi

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.bixi.databinding.ActivityMainBinding
import com.example.bixi.models.api.LoginRequest
import com.example.bixi.services.RetrofitClient
import com.example.bixi.viewModels.LoginViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        Glide.with(this)
            .load(R.drawable.ic_logo) // sau URL
            .apply(RequestOptions.bitmapTransform(RoundedCorners(32))) // 32 = raza colțurilor
            .into(binding.ivLogo)

        binding.usernameLayout.setRegex(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"), "Eroareee")

        binding.btnLogin.setOnClickListener{
//            binding.usernameLayout.validate()

            loginViewModel.login(binding.etUsername.text.toString(), binding.etPassword.text.toString())
        }

        loginViewModel.loginSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Login reușit!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Login eșuat!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}