package com.v2ray.ang.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.AngConfigManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application), 
    SharedPreferences.OnSharedPreferenceChangeListener {

    fun startListenPreferenceChange() {
        PreferenceManager.getDefaultSharedPreferences(getApplication())
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        try {
            PreferenceManager.getDefaultSharedPreferences(getApplication())
                .unregisterOnSharedPreferenceChangeListener(this)
        } catch (e: Exception) {
            // Ignore
        }
        Log.i(AppConfig.ANG_PACKAGE, "Settings ViewModel is cleared")
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.d(AppConfig.ANG_PACKAGE, "Observe settings changed: $key")

        when (key) {
            SettingsActivity.PREF_SNIFFING_ENABLED,
            SettingsActivity.PREF_PROXY_SHARING,
            SettingsActivity.PREF_LOCAL_DNS_ENABLED,
            SettingsActivity.PREF_REMOTE_DNS,
            SettingsActivity.PREF_DOMESTIC_DNS,
            SettingsActivity.PREF_ROUTING_DOMAIN_STRATEGY,
            SettingsActivity.PREF_ROUTING_MODE,
            AppConfig.PREF_V2RAY_ROUTING_AGENT,
            AppConfig.PREF_V2RAY_ROUTING_BLOCKED,
            AppConfig.PREF_V2RAY_ROUTING_DIRECT -> {
                GlobalScope.launch {
                    if (!AngConfigManager.genStoreV2rayConfig()) {
                        Log.d(AppConfig.ANG_PACKAGE, "$key changed but generate full configuration failed!")
                    }
                }
            }
        }
    }
}
