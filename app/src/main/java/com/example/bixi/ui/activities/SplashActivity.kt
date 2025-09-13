package com.example.bixi.ui.activities

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.bixi.AppSession
import com.example.bixi.R
import com.example.bixi.constants.StorageKeys
import com.example.bixi.models.api.LoginRequest
import com.example.bixi.services.AuthRepository
import com.example.bixi.services.JsonConverterService
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.SecureStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class SplashActivity : Activity() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_splash)

        applyDefaultsIfFirstLaunch()
        navigateToFirstScreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun navigateToFirstScreen(){
        val userDataJson = SecureStorageService.getString(this, StorageKeys.USER_TOKEN)
        if(userDataJson == null){
            navigateToLogin()
        }
        else{
            AppSession.user = JsonConverterService.fromJson(userDataJson)
            performAutoLogin()
        }
    }

    private fun performAutoLogin() {
        scope.launch {
            val username = AppSession.user?.user?.username
            val password = AppSession.user?.user?.password

            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                navigateToLogin()
                return@launch
            }

            val result = AuthRepository.login(username, password)
            if (result.isSuccess) {
                AppSession.user!!.user.password = password
                navigateToHome()
            } else {
                navigateToLogin()
            }
        }
    }

    private fun navigateToHome(){
        SecureStorageService.putString(this, StorageKeys.USER_TOKEN, JsonConverterService.toJson(AppSession.user))
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToLogin(){
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun applyDefaultsIfFirstLaunch() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstLaunch = prefs.getBoolean("first_launch_done", false)

        if (!isFirstLaunch) {
            // 1. Setează limba telefonului
            val localeLang = Locale.getDefault().language
            prefs.edit().putString("language", localeLang).apply()

            // 2. Setează dark mode după tema telefonului
            val currentNightMode = this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isDark = (currentNightMode == Configuration.UI_MODE_NIGHT_YES)
            prefs.edit().putBoolean("dark_mode", isDark).apply()

            // Marchează că setările au fost aplicate
            prefs.edit().putBoolean("first_launch_done", true).apply()
        }

        // Aplică aceste setări la pornire
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}