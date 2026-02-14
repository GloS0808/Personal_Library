package com.personallibrary.app.v2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.personallibrary.app.v2.databinding.ActivityAddBookBinding
import com.google.android.material.snackbar.Snackbar

class AddBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBookBinding
    private lateinit var viewModel: BookViewModel
    private var customImageUri: Uri? = null
    private var currentVolumeInfo: GoogleBooksResponse.VolumeInfo? = null

    private val scanBarcodeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCAN_RESULT")
            barcode?.let {
                binding.editIsbn.setText(it)
                performSearch(it)
            }
        }
    }

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customImageUri = it
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_book_placeholder)
                .into(binding.imageThumbnail)
            binding.buttonAddPhoto.text = getString(R.string.change_photo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add_book)

        viewModel = ViewModelProvider(this)[BookViewModel::class.java]

        // Setup end icon click (Camera icon)
        binding.layoutIsbn.setEndIconOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            scanBarcodeLauncher.launch(intent)
        }

        // Search button click
        binding.buttonSearch.setOnClickListener {
            val isbn = binding.editIsbn.text.toString().trim()
            if (isbn.isEmpty()) {
                binding.editIsbn.error = getString(R.string.error_empty_isbn)
                return@setOnClickListener
            }
            performSearch(isbn)
        }

        // Add photo button click
        binding.buttonAddPhoto.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        // Observe search results
        viewModel.searchResult.observe(this) { result ->
            if (result == null) return@observe
            binding.progressBar.visibility = View.GONE

            if (result.isSuccess) {
                val volumeInfo = result.getOrNull()
                if (volumeInfo != null) {
                    currentVolumeInfo = volumeInfo
                    // Show book preview
                    binding.bookPreviewCard.visibility = View.VISIBLE
                    binding.textTitle.text = volumeInfo.title ?: getString(R.string.unknown_title)
                    binding.textAuthors.text = volumeInfo.authors?.joinToString(", ") ?: getString(R.string.unknown_author)
                    binding.textPublisher.text = getString(R.string.publisher_label, volumeInfo.publisher ?: getString(R.string.unknown_publisher))
                    binding.textPages.text = getString(R.string.pages_label, volumeInfo.pageCount?.toString() ?: getString(R.string.not_available))

                    if (customImageUri == null) {
                        volumeInfo.imageLinks?.thumbnail?.let { url ->
                            Glide.with(this)
                                .load(url)
                                .placeholder(R.drawable.ic_book_placeholder)
                                .into(binding.imageThumbnail)
                        }
                    }

                    // Add book button
                    binding.buttonAdd.setOnClickListener {
                        binding.progressBar.visibility = View.VISIBLE
                        val updatedVolumeInfo = if (customImageUri != null) {
                            volumeInfo.copy(imageLinks = GoogleBooksResponse.ImageLinks(customImageUri.toString(), customImageUri.toString()))
                        } else {
                            volumeInfo
                        }
                        viewModel.insertBookFromApi(updatedVolumeInfo)
                    }
                }
            } else {
                val exception = result.exceptionOrNull()
                Snackbar.make(
                    binding.root,
                    getString(R.string.error_book_not_found, exception?.message ?: ""),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        // Observe insert results
        viewModel.insertResult.observe(this) { result ->
            if (result == null) return@observe
            binding.progressBar.visibility = View.GONE

            if (result.isSuccess) {
                Snackbar.make(binding.root, getString(R.string.book_added_successfully), Snackbar.LENGTH_SHORT).show()
                finish()
            } else {
                val exception = result.exceptionOrNull()
                Snackbar.make(
                    binding.root,
                    getString(R.string.error_adding_book, exception?.message ?: ""),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun performSearch(isbn: String) {
        hideKeyboard()
        binding.progressBar.visibility = View.VISIBLE
        binding.bookPreviewCard.visibility = View.GONE
        customImageUri = null
        binding.buttonAddPhoto.text = getString(R.string.add_photo)
        viewModel.searchBookByIsbn(isbn)
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearSearchResult()
        viewModel.clearInsertResult()
    }
}
