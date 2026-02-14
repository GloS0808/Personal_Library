package com.personallibrary.app.v2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personallibrary.app.v2.databinding.ItemReaderBinding

class ReaderAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : ListAdapter<User, ReaderAdapter.ReaderViewHolder>(ReaderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderViewHolder {
        val binding = ItemReaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReaderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReaderViewHolder(private val binding: ItemReaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textReaderName.text = user.name
            
            if (user.email.isNullOrEmpty()) {
                binding.textReaderEmail.visibility = View.GONE
            } else {
                binding.textReaderEmail.visibility = View.VISIBLE
                binding.textReaderEmail.text = user.email
            }

            binding.root.setOnClickListener { onEditClick(user) }
            binding.buttonDelete.setOnClickListener { onDeleteClick(user) }
        }
    }

    class ReaderDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
