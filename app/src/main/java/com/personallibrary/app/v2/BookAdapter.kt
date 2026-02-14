package com.personallibrary.app.v2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.personallibrary.app.v2.databinding.ItemBookBinding

class BookAdapter(private val onBookClick: (BookWithDetails) -> Unit) :
    ListAdapter<BookWithDetails, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bookWithDetails: BookWithDetails) {
            val book = bookWithDetails.book
            binding.textTitle.text = book.title
            binding.textAuthors.text = bookWithDetails.authors.joinToString(", ") { it.name }
            
            Glide.with(binding.imageThumbnail.context)
                .load(book.thumbnail)
                .placeholder(R.drawable.ic_book_placeholder)
                .into(binding.imageThumbnail)

            binding.root.setOnClickListener { onBookClick(bookWithDetails) }
        }
    }

    private class BookDiffCallback : DiffUtil.ItemCallback<BookWithDetails>() {
        override fun areItemsTheSame(oldItem: BookWithDetails, newItem: BookWithDetails): Boolean {
            return oldItem.book.bookId == newItem.book.bookId
        }

        override fun areContentsTheSame(oldItem: BookWithDetails, newItem: BookWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}
