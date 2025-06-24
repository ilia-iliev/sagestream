package com.example.SageStream.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.SageStream.R
import com.example.SageStream.databinding.ItemQuoteBinding
import com.example.SageStream.model.MotivationalQuote

class QuotesAdapter(
    private val onEditClick: (MotivationalQuote) -> Unit,
    private val onDeleteClick: (MotivationalQuote) -> Unit
) : ListAdapter<MotivationalQuote, QuotesAdapter.QuoteViewHolder>(QuoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val binding = ItemQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuoteViewHolder(private val binding: ItemQuoteBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val quote = getItem(position)
                    showPopupMenu(it, quote)
                }
                true
            }
        }
        
        fun bind(quote: MotivationalQuote) {
            binding.quoteTextView.text = quote.quote
        }
        
        private fun showPopupMenu(view: android.view.View, quote: MotivationalQuote) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.item_menu, popupMenu.menu)
            
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEditClick(quote)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(quote)
                        true
                    }
                    else -> false
                }
            }
            
            popupMenu.show()
        }
    }

    private class QuoteDiffCallback : DiffUtil.ItemCallback<MotivationalQuote>() {
        override fun areItemsTheSame(oldItem: MotivationalQuote, newItem: MotivationalQuote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MotivationalQuote, newItem: MotivationalQuote): Boolean {
            return oldItem == newItem
        }
    }
} 