package com.personallibrary.app.v2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BookRepository
    val allBooksWithDetails: LiveData<List<BookWithDetails>>
    val allUsers: LiveData<List<User>>

    private val _searchResult = MutableLiveData<Result<GoogleBooksResponse.VolumeInfo>?>()
    val searchResult: LiveData<Result<GoogleBooksResponse.VolumeInfo>?> = _searchResult

    private val _insertResult = MutableLiveData<Result<Long>?>()
    val insertResult: LiveData<Result<Long>?> = _insertResult

    private val _currentBook = MutableLiveData<BookWithDetails?>()
    val currentBook: LiveData<BookWithDetails?> = _currentBook

    init {
        val database = LibraryDatabase.getDatabase(application)
        repository = BookRepository(database)
        allBooksWithDetails = repository.getAllBooksWithDetails()
        allUsers = repository.getAllUsers()
    }

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
    fun searchLibrary(query: String): LiveData<List<BookWithDetails>> {
        return repository.searchBooksWithDetails(query)
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
