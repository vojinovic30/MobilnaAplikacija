package com.example.mobilneprojekat

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NotificationViewModel(context: Context) : ViewModel() {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val _isNotificationEnabled = MutableLiveData<Boolean>().apply {
        value = sharedPreferences.getBoolean("is_notification_enabled", false)
    }
    val isNotificationEnabled: LiveData<Boolean> = _isNotificationEnabled


    fun setNotificationEnabled(enabled: Boolean) {
        _isNotificationEnabled.value = enabled
        sharedPreferences.edit().putBoolean("is_notification_enabled", enabled).apply()
    }
}

class NotificationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}