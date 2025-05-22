package com.example.bixi.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.example.bixi.AppSession
import com.example.bixi.R
import com.example.bixi.constants.StorageKeys
import com.example.bixi.services.JsonConverterService
import com.example.bixi.services.SecureStorageService
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        applyDefaultsIfFirstLaunch()
        navigateToFirstScreen()
    }

    private fun navigateToFirstScreen(){
        val userDataJson = SecureStorageService.getString(this, StorageKeys.USER_TOKEN)
        if(userDataJson == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        else{
            AppSession.user = JsonConverterService.fromJson(userDataJson)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
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