package com.accenture.booking.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.accenture.booking.data.BookingDataManager

class BookingViewModel(app: Application) : AndroidViewModel(app) {

    private val dm = BookingDataManager.getInstance(app)
    val data = dm.data
    val loadingMore: LiveData<Boolean> = dm.loadingMore
    val hasMore: LiveData<Boolean> = dm.hasMore

    fun load() {
        Log.d(TAG, "load")
        dm.load()
    }

    fun refresh() {
        Log.d(TAG, "refresh")
        dm.refresh()
    }

    fun loadMore() {
        Log.d(TAG, "loadMore")
        dm.loadMore()
    }

    companion object {
        private const val TAG = "BookingVM"
    }
}