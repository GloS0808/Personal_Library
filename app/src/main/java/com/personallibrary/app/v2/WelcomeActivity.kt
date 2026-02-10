package com.personallibrary.app.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.personallibrary.app.v2.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var viewModel: BookViewModel
    private lateinit var adapter: ReaderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BookViewModel::class.java]
        
        setupRecyclerView()

        viewModel.allUsers.observe(this) { users ->
            adapter.submitList(users)
            binding.buttonFinishSetup.isEnabled = users.isNotEmpty()
        }

        binding.buttonAddInitialReader.setOnClickListener {
            showAddReaderDialog()
        }

        binding.buttonFinishSetup.setOnClickListener {
            markSetupComplete()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ReaderAdapter(
            onEditClick = { user ->
                showEditReaderDialog(user)
            },
            onDeleteClick = { user ->
                viewModel.deleteUser(user)
            }
        )
        binding.recyclerInitialReaders.layoutManager = LinearLayoutManager(this)
        binding.recyclerInitialReaders.adapter = adapter
    }

    private fun showAddReaderDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reader, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.edit_reader_name)
        val emailInput = dialogView.findViewById<EditText>(R.id.edit_reader_email)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_new_reader_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val name = nameInput.text.toString().trim()
                val email = emailInput.text.toString().trim().ifEmpty { null }
                if (name.isNotEmpty()) {
                    viewModel.insertUser(name, email)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showEditReaderDialog(user: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reader, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.edit_reader_name)
        val emailInput = dialogView.findViewById<EditText>(R.id.edit_reader_email)

        nameInput.setText(user.name)
        emailInput.setText(user.email)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_reader_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = nameInput.text.toString().trim()
                val email = emailInput.text.toString().trim().ifEmpty { null }
                if (name.isNotEmpty()) {
                    val updatedUser = user.copy(name = name, email = email)
                    viewModel.updateUser(updatedUser)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun markSetupComplete() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("setup_complete", true).apply()
    }
}
