package com.v2ray.ang.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.v2ray.ang.extension.defaultDPreference
import com.v2ray.ang.ui.SettingsActivity

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val perAppProxyEnabled = MutableLiveData<Boolean>()
    val speedEnabled = MutableLiveData<Boolean>()
    val sniffingEnabled = MutableLiveData<Boolean>()
    val proxySharingEnabled = MutableLiveData<Boolean>()
    val localDnsEnabled = MutableLiveData<Boolean>()
    val forwardIpv6Enabled = MutableLiveData<Boolean>()
    val routingDomainStrategy = MutableLiveData<String>()
    val routingMode = MutableLiveData<String>()
    val remoteDns = MutableLiveData<String>()
    val domesticDns = MutableLiveData<String>()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val prefs = getApplication<Application>().defaultDPreference
        perAppProxyEnabled.value = prefs.getPrefBoolean(SettingsActivity.PREF_PER_APP_PROXY, false)
        speedEnabled.value = prefs.getPrefBoolean(SettingsActivity.PREF_SPEED_ENABLED, false)
        sniffingEnabled.value = prefs.getPrefBoolean(SettingsActivity.PREF_SNIFFING_ENABLED, false)
        proxySharingEnabled.value = prefs.getPrefBoolean(SettingsActivity.PREF_PROXY_SHARING, false)
        localDnsEnabled.value = prefs.getPrefBoolean(SettingsActivity.PREF_LOCAL_DNS_ENABLED, false)
        forwardIpv6Enabled.value = prefs.getPrefBoolean(SettingsActivity.PREF_FORWARD_IPV6, false)
        routingDomainStrategy.value = prefs.getPrefString(SettingsActivity.PREF_ROUTING_DOMAIN_STRATEGY, "IPIfNonMatch")
        routingMode.value = prefs.getPrefString(SettingsActivity.PREF_ROUTING_MODE, "0")
        remoteDns.value = prefs.getPrefString(SettingsActivity.PREF_REMOTE_DNS, "")
        domesticDns.value = prefs.getPrefString(SettingsActivity.PREF_DOMESTIC_DNS, "")
    }

    fun saveSettings() {
        val prefs = getApplication<Application>().defaultDPreference
        perAppProxyEnabled.value?.let { prefs.setPrefBoolean(SettingsActivity.PREF_PER_APP_PROXY, it) }
        speedEnabled.value?.let { prefs.setPrefBoolean(SettingsActivity.PREF_SPEED_ENABLED, it) }
        sniffingEnabled.value?.let { prefs.setPrefBoolean(SettingsActivity.PREF_SNIFFING_ENABLED, it) }
        proxySharingEnabled.value?.let { prefs.setPrefBoolean(SettingsActivity.PREF_PROXY_SHARING, it) }
        localDnsEnabled.value?.let { prefs.setPrefBoolean(SettingsActivity.PREF_LOCAL_DNS_ENABLED, it) }
        forwardIpv6Enabled.value?.let { prefs.setPrefBoolean(SettingsActivity.PREF_FORWARD_IPV6, it) }
        routingDomainStrategy.value?.let { prefs.setPrefString(SettingsActivity.PREF_ROUTING_DOMAIN_STRATEGY, it) }
        routingMode.value?.let { prefs.setPrefString(SettingsActivity.PREF_ROUTING_MODE, it) }
        remoteDns.value?.let { prefs.setPrefString(SettingsActivity.PREF_REMOTE_DNS, it) }
        domesticDns.value?.let { prefs.setPrefString(SettingsActivity.PREF_DOMESTIC_DNS, it) }
    }
}