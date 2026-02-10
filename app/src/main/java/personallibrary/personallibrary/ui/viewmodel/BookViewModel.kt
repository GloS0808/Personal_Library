package com.personallibrary.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BookRepository
    
    private val _searchQuery = MutableLiveData<String>("")
    val allBooksWithDetails: LiveData<List<BookWithDetails>> = _searchQuery.switchMap { query ->
        if (query.isNullOrBlank()) {
            repository.getAllBooksWithDetails()
        } else {
            repository.searchBooksWithDetails(query)
        }
    }

    val allUsers: LiveData<List<User>>

    private val _searchResult = MutableLiveData<Result<VolumeInfo>?>()
    val searchResult: LiveData<Result<VolumeInfo>?> = _searchResult

    private val _insertResult = MutableLiveData<Result<Long>?>()
    val insertResult: LiveData<Result<Long>?> = _insertResult

    private val _currentBook = MutableLiveData<BookWithDetails?>()
    val currentBook: LiveData<BookWithDetails?> = _currentBook

    init {
        val database = LibraryDatabase.getDatabase(application)
        repository = BookRepository(database)
        allUsers = repository.getAllUsers()
    }

    // Filter local library
    fun filterLibrary(query: String) {
        _searchQuery.value = query
    }

    // Search book by ISBN
    fun searchBookByIsbn(isbn: String) = viewModelScope.launch {
        val result = repository.searchBookByIsbn(isbn)
        _searchResult.postValue(result)
    }

    // Insert book from API result
    fun insertBookFromApi(volumeInfo: VolumeInfo, userId: Int = 1) = viewModelScope.launch {
        val result = repository.insertBookFromApi(volumeInfo, userId)
        _insertResult.postValue(result)
    }

    // Load specific book
    fun loadBook(bookId: Int) = viewModelScope.launch {
        val book = repository.getBookWithDetails(bookId)
        _currentBook.postValue(book)
    }

    // User operations
    fun insertUser(name: String, email: String?) = viewModelScope.launch {
        repository.insertUser(User(name = name, email = email))
    }

    fun deleteUser(user: User) = viewModelScope.launch {
        repository.deleteUser(user)
    }

    // Update user book info
    fun updateUserBook(userBook: UserBook) = viewModelScope.launch {
        repository.updateUserBook(userBook)
    }

    // Delete book
    fun deleteBook(book: Book) = viewModelScope.launch {
        repository.deleteBook(book)
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
