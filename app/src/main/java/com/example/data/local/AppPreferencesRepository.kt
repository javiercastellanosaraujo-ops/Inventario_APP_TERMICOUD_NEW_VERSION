package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("termicoud_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_USER = "key_active_user"
        private const val KEY_EXCHANGE_RATE = "key_exchange_rate"
        private const val KEY_USER_SELECTED = "key_user_selected"
        private const val KEY_BACKEND_URL = "key_backend_url"
        private const val KEY_IS_DARK_MODE = "key_is_dark_mode"

        const val DEFAULT_USER = "Operador"
        const val DEFAULT_EXCHANGE_RATE = 36.50
        const val DEFAULT_BACKEND_URL = ""
        const val DEFAULT_DARK_MODE = false
    }

    private val _isDarkMode = MutableStateFlow(
        prefs.getBoolean(KEY_IS_DARK_MODE, DEFAULT_DARK_MODE)
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _activeUser = MutableStateFlow(
        prefs.getString(KEY_ACTIVE_USER, DEFAULT_USER) ?: DEFAULT_USER
    )
    val activeUser: StateFlow<String> = _activeUser.asStateFlow()

    private val _exchangeRate = MutableStateFlow(
        prefs.getFloat(KEY_EXCHANGE_RATE, DEFAULT_EXCHANGE_RATE.toFloat()).toDouble()
    )
    val exchangeRate: StateFlow<Double> = _exchangeRate.asStateFlow()

    private val _isUserSelected = MutableStateFlow(
        prefs.getBoolean(KEY_USER_SELECTED, false)
    )
    val isUserSelected: StateFlow<Boolean> = _isUserSelected.asStateFlow()

    private val _backendUrl = MutableStateFlow(
        prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
    )
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    fun setActiveUser(user: String) {
        prefs.edit()
            .putString(KEY_ACTIVE_USER, user)
            .putBoolean(KEY_USER_SELECTED, true)
            .apply()
        _activeUser.value = user
        _isUserSelected.value = true
    }

    fun setExchangeRate(rate: Double) {
        val validRate = if (rate <= 0.0) DEFAULT_EXCHANGE_RATE else rate
        prefs.edit().putFloat(KEY_EXCHANGE_RATE, validRate.toFloat()).apply()
        _exchangeRate.value = validRate
    }

    fun setBackendUrl(url: String) {
        val clean = url.trim()
        prefs.edit().putString(KEY_BACKEND_URL, clean).apply()
        _backendUrl.value = clean
    }
}