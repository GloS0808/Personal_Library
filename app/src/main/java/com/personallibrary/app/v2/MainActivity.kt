package com.personallibrary.app.v2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.personallibrary.app.v2.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: BookViewModel
    private lateinit var adapter: BookAdapter
    private var currentSortOrder = SortOrder.DATE_ADDED

    enum class SortOrder {
        DATE_ADDED, TITLE_ASC, TITLE_DESC, AUTHOR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!isSetupComplete()) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Crash Catcher for deployment
        setupCrashCatcher()
        checkForPreviousCrash()

        viewModel = ViewModelProvider(this)[BookViewModel::class.java]

        findViewById<ImageButton>(R.id.button_hamburger).setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        adapter = BookAdapter { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("BOOK_ID", book.book.bookId)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        observeBooks("")

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                observeBooks(newText ?: "")
                return true
            }
        })

        binding.buttonSort.setOnClickListener { showSortDialog() }
        binding.buttonAddBookTop.setOnClickListener { navigateToAddBook() }

        setupNavigationDrawer()
    }

    private fun setupNavigationDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_readers -> startActivity(Intent(this, ManageReadersActivity::class.java))
                R.id.nav_feedback -> showFeedbackDialog()
                R.id.nav_share_data -> {
                    val actionView = menuItem.actionView as? SwitchCompat
                    val newState = !(actionView?.isChecked ?: false)
                    actionView?.isChecked = newState
                    // Using EncryptedSharedPreferences for data sharing preference
                    SecurityUtils.setShareDataEnabled(this, newState)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val shareItem = binding.navigationView.menu.findItem(R.id.nav_share_data)
        // Check EncryptedSharedPreferences
        (shareItem.actionView as? SwitchCompat)?.isChecked = SecurityUtils.isShareDataEnabled(this)
    }

    private fun showFeedbackDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_feedback, null)
        val feedbackInput = dialogView.findViewById<TextInputEditText>(R.id.edit_feedback)
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val feedbackText = feedbackInput.text.toString()
                if (feedbackText.isNotBlank()) {
                    sendEmail("App Feedback", feedbackText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendEmail(subject: String, content: String) {
        val deviceDoc = """
            
            --- Device Info ---
            App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            -------------------
        """.trimIndent()

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("personallibraryappfeedback@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "$subject - Personal Library")
            putExtra(Intent.EXTRA_TEXT, "$content\n\n$deviceDoc")
        }
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Select Email App"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCrashCatcher() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // We'll keep the logic but it's now always enabled or could be handled differently
            // Since the toggle is gone, we'll just log it if a crash occurs.
            val stackTrace = throwable.stackTraceToString()
            getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_crash_log", stackTrace)
                .apply()
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun checkForPreviousCrash() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val crashLog = prefs.getString("last_crash_log", null)
        if (crashLog != null) {
            prefs.edit().remove("last_crash_log").apply()
            AlertDialog.Builder(this)
                .setTitle("Application Crash Detected")
                .setMessage("The app closed unexpectedly last time. Would you like to send a crash report to help us fix the issue?")
                .setPositiveButton("Send Report") { _, _ ->
                    sendEmail("Crash Report", "The app crashed with the following stack trace:\n\n$crashLog")
                }
                .setNegativeButton("No Thanks", null)
                .show()
        }
    }

    private fun savePreference(key: String, value: Boolean) {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean(key, value).apply()
    }

    private fun isSetupComplete(): Boolean = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getBoolean("setup_complete", false)

    private fun navigateToAddBook() = startActivity(Intent(this, AddBookActivity::class.java))

    private fun observeBooks(query: String) {
        viewModel.allBooksWithDetails.removeObservers(this)
        val booksLiveData = if (query.isEmpty()) viewModel.allBooksWithDetails else viewModel.searchLibrary(query)
        booksLiveData.observe(this) { books ->
            adapter.submitList(sortBooks(books))
            binding.emptyView.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (books.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun sortBooks(books: List<BookWithDetails>): List<BookWithDetails> = when (currentSortOrder) {
        SortOrder.DATE_ADDED -> books.sortedByDescending { it.book.bookId }
        SortOrder.TITLE_ASC -> books.sortedBy { it.book.title.lowercase() }
        SortOrder.TITLE_DESC -> books.sortedByDescending { it.book.title.lowercase() }
        SortOrder.AUTHOR -> books.sortedBy { it.authors.firstOrNull()?.name?.lowercase() ?: "" }
    }

    private fun showSortDialog() {
        val options = arrayOf(getString(R.string.sort_date_added), getString(R.string.sort_title_asc), getString(R.string.sort_title_desc), getString(R.string.sort_author))
        AlertDialog.Builder(this).setTitle(R.string.sort_by).setItems(options) { _, which ->
            currentSortOrder = SortOrder.values()[which]
            observeBooks(binding.searchView.query.toString())
        }.show()
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
