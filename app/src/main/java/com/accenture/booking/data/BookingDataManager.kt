package com.accenture.booking.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.accenture.booking.data.cache.BookingCache
import com.accenture.booking.data.service.BookingService
import com.accenture.booking.model.*
import kotlinx.coroutines.*

sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Error(val message: String) : DataState<Nothing>()
}

class BookingDataManager(context: Context) {

    private val service = BookingService(context)
    private val cache = BookingCache(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _data = MutableLiveData<DataState<BookingResponse>>()
    val data: LiveData<DataState<BookingResponse>> = _data

    private val _loadingMore = MutableLiveData(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    private var loading = false
    private var page = 0
    private var currentData: BookingResponse? = null

    /** 缓存优先：先展示有效缓存，再后台拉取新数据 */
    fun load() {
        page = 0
        _hasMore.postValue(true)
        val cached = if (cache.isValid()) cache.get() else null
        if (cached != null) {
            currentData = cached
            _data.postValue(DataState.Success(cached))
        } else {
            _data.postValue(DataState.Loading)
        }
        fetch()
    }

    /** 下拉刷新，重置分页 */
    fun refresh() {
        page = 0
        _hasMore.postValue(true)
        if (cache.get() == null) _data.postValue(DataState.Loading)
        fetch()
    }

    /** 上拉加载下一页 */
    fun loadMore() {
        if (_loadingMore.value == true || _hasMore.value == false) return
        _loadingMore.postValue(true)
        scope.launch {
            delay(800)
            val existing = currentData ?: return@launch
            page++
            if (page > MAX_PAGES) {
                _hasMore.postValue(false)
                _loadingMore.postValue(false)
                return@launch
            }
            val pageData = generatePage(page)
            val updated = existing.copy(segments = existing.segments + pageData)
            currentData = updated
            _data.postValue(DataState.Success(updated))
            _loadingMore.postValue(false)
        }
    }

    /** 生成一页mock数据 */
    private fun generatePage(p: Int): List<Segment> {
        return (1..PAGE_SIZE).map { i ->
            val id = p * PAGE_SIZE + i
            Segment(
                id = id,
                originAndDestinationPair = OriginAndDestinationPair(
                    destination = Location("D$id", "Dest $id", ""),
                    destinationCity = "City D$id",
                    origin = Location("O$id", "Origin $id", ""),
                    originCity = "City O$id"
                )
            )
        }
    }

    private fun fetch() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { service.fetchBookingData() }
                // 将booking.json的真实数据作为第1页，补齐到PAGE_SIZE条
                val padded = padToPageSize(result, 1)
                cache.save(padded)
                currentData = padded
                _data.postValue(DataState.Success(padded))
            } catch (e: Exception) {
                Log.e(TAG, "Fetch failed", e)
                val fallback = cache.get()
                if (fallback != null) {
                    currentData = fallback
                    _data.postValue(DataState.Success(fallback))
                } else {
                    _data.postValue(DataState.Error(e.message ?: "Unknown error"))
                }
            } finally {
                loading = false
            }
        }
    }

    /** 将真实数据补齐到PAGE_SIZE条，剩余用mock填充 */
    private fun padToPageSize(data: BookingResponse, page: Int): BookingResponse {
        if (data.segments.size >= PAGE_SIZE) return data
        val mock = (data.segments.size + 1..PAGE_SIZE).map { i ->
            Segment(
                id = i,
                originAndDestinationPair = OriginAndDestinationPair(
                    destination = Location("D$i", "Dest $i", ""),
                    destinationCity = "City D$i",
                    origin = Location("O$i", "Origin $i", ""),
                    originCity = "City O$i"
                )
            )
        }
        return data.copy(segments = data.segments + mock)
    }

    companion object {
        private const val TAG = "BookingDataManager"
        private const val PAGE_SIZE = 10
        private const val MAX_PAGES = 3 // 总共3页=30条

        @Volatile
        private var instance: BookingDataManager? = null

        fun getInstance(ctx: Context): BookingDataManager {
            return instance ?: synchronized(this) {
                instance ?: BookingDataManager(ctx.applicationContext).also { instance = it }
            }
        }
    }
}