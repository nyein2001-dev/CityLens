package com.geo.tracking.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreference(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "my_prefs"
        private const val RIDER_ID_KEY = "riderIdKey"
        private const val DEVICE_ID_KEY = "deviceIdKey"
        private const val DEVICE_NAME_KEY = "deviceNameKey"
    }

    private fun <T> setValue(key: String, value: T) {
        with(preferences.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Double -> putFloat(key, value.toFloat())
                is Boolean -> putBoolean(key, value)
                else -> throw IllegalArgumentException("Unsupported type")
            }
            apply()
        }
    }

    private inline fun <reified T> getValue(key: String): T? {
        return when (T::class) {
            String::class -> preferences.getString(key, null) as T?
            Float::class -> preferences.getFloat(key, 0.0f) as T?
            Boolean::class -> preferences.getBoolean(key, false) as T?
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }

    fun setRiderId(riderId: String) {
        setValue(RIDER_ID_KEY, riderId)
    }

    fun getRiderId(): String {
        return getValue(RIDER_ID_KEY) ?: ""
    }

    fun setDeviceId(deviceId: String) {
        setValue(DEVICE_ID_KEY, deviceId)
    }

    fun getDeviceId(): String {
        return getValue(DEVICE_ID_KEY) ?: ""
    }

    fun setDeviceName(deviceName: String) {
        setValue(DEVICE_NAME_KEY, deviceName)
    }

    fun getDeviceName(): String {
        return getValue(DEVICE_NAME_KEY) ?: ""
    }
}
