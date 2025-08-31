package com.example.bixi.ui.fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.bixi.R
import com.example.bixi.helper.LocaleHelper

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val languagePref: ListPreference? = findPreference("language")
        languagePref?.setOnPreferenceChangeListener { _, newValue ->
            LocaleHelper.setLocale(requireContext(), newValue as String)
            requireActivity().recreate() // Reîncarcă UI-ul cu noua limbă
            true
        }

        val darkModePref: SwitchPreferenceCompat? = findPreference("dark_mode")
        darkModePref?.setOnPreferenceChangeListener { _, newValue ->
            val isDark = newValue as Boolean
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            true
        }

        // Setează versiunea aplicației dinamic
        val versionPref: Preference? = findPreference("app_version")
        versionPref?.summary = getAppVersion()
    }

    private fun getAppVersion(): String? {
        val context = requireContext()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName
    }
}
