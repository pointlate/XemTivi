package com.ltnstore.titvi.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ltnstore.titvi.R
import com.ltnstore.titvi.model.Channel

class ChannelAdapter(
    private var items: List<Channel> = emptyList(),
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private var currentNumber: Int = -1

    fun submit(newItems: List<Channel>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setCurrent(number: Int) {
        currentNumber = number
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = items[position]
        holder.bind(ch, ch.number == currentNumber)
    }

    override fun getItemCount() = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_number)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)

        fun bind(channel: Channel, isCurrent: Boolean) {
            tvNumber.text = channel.number.toString()
            tvName.text = channel.name
            itemView.isSelected = isCurrent
            itemView.alpha = if (isCurrent) 1f else 0.85f
            itemView.setOnClickListener { onClick(channel) }
        }
    }
}