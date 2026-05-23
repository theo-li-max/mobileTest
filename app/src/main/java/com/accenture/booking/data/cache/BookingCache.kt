package com.accenture.booking.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.accenture.booking.model.BookingResponse
import com.google.gson.Gson

class BookingCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("booking_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(data: BookingResponse) {
        prefs.edit()
            .putString(KEY_DATA, gson.toJson(data))
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    fun get(): BookingResponse? {
        val json = prefs.getString(KEY_DATA, null) ?: return null
        return try {
            gson.fromJson(json, BookingResponse::class.java)
        } catch (e: Exception) {
            clear()
            null
        }
    }

    /** 基于expiryTime判断缓存是否过期 true代表没过期 false代表过期了或者第一次初始化*/
    fun isValid(): Boolean {
        val data = get() ?: return false
        val expiry = data.expiryTime.toLongOrNull() ?: return false
        return System.currentTimeMillis() < expiry * 1000L
    }

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_DATA = "booking_json"
        private const val KEY_TIME = "cache_time"
    }
}