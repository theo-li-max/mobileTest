package com.accenture.booking.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.accenture.booking.data.DataState
import com.accenture.booking.databinding.ActivityMainBinding
import com.accenture.booking.view.adapter.BookingAdapter
import com.accenture.booking.viewmodel.BookingViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vm: BookingViewModel
    private val adapter = BookingAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener { vm.refresh() }

        // 上拉加载：滚动到底部时触发
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (lastVisible >= total - 2 && vm.hasMore.value == true) {
                    vm.loadMore()
                }
            }
        })

        vm = ViewModelProvider(this)[BookingViewModel::class.java]
        vm.data.observe(this) { state ->
            when (state) {
                is DataState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                }
                is DataState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    adapter.updateSegments(state.data.segments)
                    // 控制台打印数据
                    Log.d(TAG, "Booking Data: $state.data")
                    state.data.segments.forEach { seg ->
                        Log.d(TAG, "  Segment ${seg.id}: ${seg.originAndDestinationPair.originCity} -> ${seg.originAndDestinationPair.destinationCity}")
                    }
                }
                is DataState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 上拉加载进度条
        vm.loadingMore.observe(this) { isLoading ->
            binding.progressLoadMore.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume -> load")
        vm.load()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}