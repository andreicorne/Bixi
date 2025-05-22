package com.example.bixi.helper

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun applyLocale(base: Context): Context {
        val prefs = PreferenceManager.getDefaultSharedPreferences(base)
        val lang = prefs.getString("language", "ro") ?: "ro"
        return setLocale(base, lang)
    }
}
