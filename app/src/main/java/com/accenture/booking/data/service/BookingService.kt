package com.accenture.booking.data.service

import android.content.Context
import com.accenture.booking.model.BookingResponse
import com.google.gson.Gson
import kotlinx.coroutines.delay

class BookingService(private val context: Context) {

    private val gson = Gson()

    suspend fun fetchBookingData(): BookingResponse {
        delay(1500)
        val json = context.assets.open("booking.json").bufferedReader().use { it.readText() }
        return gson.fromJson(json, BookingResponse::class.java)
    }
}