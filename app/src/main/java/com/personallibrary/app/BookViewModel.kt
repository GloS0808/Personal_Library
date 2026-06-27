package com.personallibrary.app

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    DATE_ADDED, TITLE_ASC, TITLE_DESC, AUTHOR
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookViewModel @Inject constructor(
    private val repository: BookRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(sharedPreferences.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        sharedPreferences.edit().putBoolean("dark_mode", enabled).apply()
    }

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)

    val allBooksWithDetails: LiveData<List<BookWithDetails>> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getAllBooksWithDetails()
            } else {
                repository.searchBooksWithDetails(query)
            }
        },
        _sortOrder
    ) { books, sortOrder ->
        sortBooks(books, sortOrder)
    }.asLiveData(viewModelScope.coroutineContext)

    val allUsers: LiveData<List<User>> = repository.getAllUsers().asLiveData(viewModelScope.coroutineContext)

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    private fun sortBooks(books: List<BookWithDetails>, sortOrder: SortOrder): List<BookWithDetails> = when (sortOrder) {
        SortOrder.DATE_ADDED -> books.sortedByDescending { it.book.bookId }
        SortOrder.TITLE_ASC -> books.sortedBy { it.book.title.lowercase() }
        SortOrder.TITLE_DESC -> books.sortedByDescending { it.book.title.lowercase() }
        SortOrder.AUTHOR -> books.sortedBy { it.authors.firstOrNull()?.name?.lowercase() ?: "" }
    }

    private val _searchResult = MutableLiveData<Result<GoogleBooksResponse.VolumeInfo>?>()
    val searchResult: LiveData<Result<GoogleBooksResponse.VolumeInfo>?> = _searchResult

    private val _insertResult = MutableLiveData<Result<Long>?>()
    val insertResult: LiveData<Result<Long>?> = _insertResult

    private val _currentBook = MutableLiveData<BookWithDetails?>()
    val currentBook: LiveData<BookWithDetails?> = _currentBook

    // Search book by ISBN
    fun searchBookByIsbn(isbn: String) = viewModelScope.launch {
        val result = repository.searchBookByIsbn(isbn)
        _searchResult.postValue(result)
    }

    // Insert book from API result
    fun insertBookFromApi(volumeInfo: GoogleBooksResponse.VolumeInfo, userId: Int = 1) = viewModelScope.launch {
        val result = repository.insertBookFromApi(volumeInfo, userId)
        _insertResult.postValue(result)
    }

    // Load specific book
    fun loadBook(bookId: Int) = viewModelScope.launch {
        val book = repository.getBookWithDetails(bookId)
        _currentBook.postValue(book)
    }

    // Update user book info
    fun updateUserBook(userBook: UserBook) = viewModelScope.launch {
        repository.updateUserBook(userBook)
    }

    // Delete book
    fun deleteBook(book: Book) = viewModelScope.launch {
        repository.deleteBook(book)
    }

    // Search functionality for local library
    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    // User operations
    fun insertUser(name: String, email: String?) = viewModelScope.launch {
        repository.insertUser(User(name = name, email = email))
    }

    fun updateUser(user: User) = viewModelScope.launch {
        repository.updateUser(user)
    }

    fun deleteUser(user: User) = viewModelScope.launch {
        repository.deleteUser(user)
    }

    // Clear search result
    fun clearSearchResult() {
        _searchResult.value = null
    }

    // Clear insert result
    fun clearInsertResult() {
        _insertResult.value = null
    }
}
