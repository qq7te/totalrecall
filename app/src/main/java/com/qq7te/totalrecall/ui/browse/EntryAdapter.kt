package com.qq7te.totalrecall.ui.browse

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.databinding.ItemEntryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EntryAdapter(private val onItemClick: (Long) -> Unit) :
    ListAdapter<Entry, EntryAdapter.EntryViewHolder>(EntryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class EntryViewHolder(private val binding: ItemEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position).id)
                }
            }
        }
        
        fun bind(entry: Entry) {
            binding.textPreview.text = entry.text
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.date.text = dateFormat.format(entry.timestamp)
            
            Glide.with(binding.thumbnail)
                .load(Uri.parse(entry.photoPath))
                .centerCrop()
                .into(binding.thumbnail)
        }
    }
    
    class EntryDiffCallback : DiffUtil.ItemCallback<Entry>() {
        override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem == newItem
        }
    }
} 