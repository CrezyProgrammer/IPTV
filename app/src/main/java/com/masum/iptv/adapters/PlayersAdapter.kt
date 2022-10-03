package com.masum.iptv.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.masum.iptv.databinding.AdapterItemBinding
import com.masum.iptv.models.Channel

class PlayersAdapter(private val clicked: (String) -> Unit) :
    PagingDataAdapter<Channel, PlayersAdapter.PlayersViewHolder>(
        PlayersDiffCallback()
    ) {


    override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {

        val data = getItem(position)

        holder.bind(data)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {

        return PlayersViewHolder(
            AdapterItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    }

    inner class PlayersViewHolder(
        private val binding: AdapterItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: Channel?) {

            binding.let {

                val name = data?.title
                it.root.setOnClickListener {

                    data?.location?.let { it1 -> clicked.invoke(it1) }

                }
                it.title.text = name
                Glide.with(it.root).load(data?.logo).
                        diskCacheStrategy(DiskCacheStrategy.ALL).into(it.logo)

            }

        }
    }

    private class PlayersDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem == newItem
        }
    }

}