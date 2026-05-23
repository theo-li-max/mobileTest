package com.accenture.booking.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.accenture.booking.databinding.ItemSegmentBinding
import com.accenture.booking.model.Segment

class BookingAdapter : RecyclerView.Adapter<BookingAdapter.VH>() {

    private var segments = listOf<Segment>()

    fun updateSegments(list: List<Segment>) {
        segments = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSegmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = segments[pos]
        val p = s.originAndDestinationPair
        h.b.tvSegmentId.text = "Segment ${s.id}"
        h.b.tvRoute.text = "${p.originCity} → ${p.destinationCity}"
        h.b.tvOrigin.text = p.origin.displayName
        h.b.tvDestination.text = p.destination.displayName
    }

    override fun getItemCount() = segments.size

    class VH(val b: ItemSegmentBinding) : RecyclerView.ViewHolder(b.root)
}