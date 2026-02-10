package com.personallibrary.app.v2

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.personallibrary.app.v2.databinding.ActivityBookDetailBinding
import com.google.android.material.snackbar.Snackbar

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding
    private lateinit var viewModel: BookViewModel
    private var bookId: Int = -1
    private var usersList: List<User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Custom Header Setup
        val buttonBack = findViewById<ImageButton>(R.id.button_back)
        val buttonDelete = findViewById<ImageButton>(R.id.button_delete_custom)
        val headerTitle = findViewById<TextView>(R.id.text_header_title)

        buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        buttonDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        bookId = intent.getIntExtra("BOOK_ID", -1)
        if (bookId == -1) {
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[BookViewModel::class.java]

        // Setup status spinner
        val statuses = arrayOf(
            getString(R.string.status_to_read),
            getString(R.string.status_reading),
            getString(R.string.status_completed)
        )
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter

        // Setup reader spinner
        viewModel.allUsers.observe(this) { users ->
            usersList = users
            val userNames = users.map { it.name }
            val readerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userNames)
            readerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerReader.adapter = readerAdapter
            
            // Re-apply selection if book is already loaded
            viewModel.currentBook.value?.userBook?.let { userBook ->
                val userIndex = usersList.indexOfFirst { it.userId == userBook.userId }
                if (userIndex != -1) {
                    binding.spinnerReader.setSelection(userIndex)
                }
            }
        }

        // Load book data
        viewModel.loadBook(bookId)

        viewModel.currentBook.observe(this) { bookWithDetails ->
            bookWithDetails?.let { book ->
                // Display book info in custom header
                headerTitle.text = book.book.title

                binding.textTitle.text = book.book.title
                binding.textSubtitle.text = book.book.subtitle ?: ""
                binding.textSubtitle.visibility = if (book.book.subtitle != null) View.VISIBLE else View.GONE

                val authors = book.authors.joinToString(", ") { it.name }
                binding.textAuthors.text = authors

                binding.textPublisher.text = getString(R.string.publisher_label, book.book.publisher ?: getString(R.string.unknown_publisher))
                binding.textPublishedDate.text = getString(R.string.published_date_label, book.book.publishedDate ?: getString(R.string.not_available))
                binding.textPages.text = getString(R.string.pages_label, book.book.pageCount?.toString() ?: getString(R.string.not_available))
                binding.textCategory.text = getString(R.string.category_label, book.category?.categoryName ?: getString(R.string.not_available))
                binding.textRating.text = getString(R.string.rating_label, book.book.averageRating?.toString() ?: getString(R.string.not_available))

                binding.textDescription.text = book.book.description ?: getString(R.string.description)

                book.book.thumbnail?.let { url ->
                    Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.ic_book_placeholder)
                        .into(binding.imageThumbnail)
                }

                // Load user book data if exists
                book.userBook?.let { userBook ->
                    when (userBook.status) {
                        "to_read" -> binding.spinnerStatus.setSelection(0)
                        "reading" -> binding.spinnerStatus.setSelection(1)
                        "read" -> binding.spinnerStatus.setSelection(2)
                    }

                    binding.ratingBar.rating = userBook.userRating ?: 0f
                    binding.editCurrentPage.setText(userBook.currentPage?.toString() ?: "")
                    binding.editNotes.setText(userBook.notes ?: "")
                    binding.editStartedDate.setText(userBook.startedDate ?: "")
                    binding.editReadDate.setText(userBook.readDate ?: "")

                    if (usersList.isNotEmpty()) {
                        val userIndex = usersList.indexOfFirst { it.userId == userBook.userId }
                        if (userIndex != -1) {
                            binding.spinnerReader.setSelection(userIndex)
                        }
                    }
                }
            }
        }

        // Save button
        binding.buttonSave.setOnClickListener {
            saveUserBook()
        }
    }

    private fun saveUserBook() {
        val selectedUserIndex = binding.spinnerReader.selectedItemPosition
        if (selectedUserIndex == -1 || usersList.isEmpty()) {
            Snackbar.make(binding.root, "Please select a reader", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val selectedUserId = usersList[selectedUserIndex].userId

        val status = when (binding.spinnerStatus.selectedItemPosition) {
            0 -> "to_read"
            1 -> "reading"
            2 -> "read"
            else -> "to_read"
        }

        val rating = binding.ratingBar.rating.takeIf { it > 0 }
        val currentPage = binding.editCurrentPage.text.toString().toIntOrNull()
        val notes = binding.editNotes.text.toString().takeIf { it.isNotBlank() }
        val startedDate = binding.editStartedDate.text.toString().takeIf { it.isNotBlank() }
        val readDate = binding.editReadDate.text.toString().takeIf { it.isNotBlank() }

        val userBook = UserBook(
            userId = selectedUserId,
            bookId = bookId,
            status = status,
            userRating = rating,
            currentPage = currentPage,
            notes = notes,
            startedDate = startedDate,
            readDate = readDate
        )

        viewModel.updateUserBook(userBook)

        Snackbar.make(binding.root, getString(R.string.save_success), Snackbar.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_book_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.currentBook.value?.book?.let { book ->
                    viewModel.deleteBook(book)
                    finish()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
