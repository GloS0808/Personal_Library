package com.personallibrary.app.v2

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.personallibrary.app.v2.GoogleBooksResponse.VolumeInfo

class BookRepository(database: LibraryDatabase) {

    private val bookDao = database.bookDao()
    private val authorDao = database.authorDao()
    private val categoryDao = database.categoryDao()
    private val userBookDao = database.userBookDao()
    private val bookAuthorDao = database.bookAuthorDao()
    private val userDao = database.userDao()

    // Get all books with details
    fun getAllBooksWithDetails(): LiveData<List<BookWithDetails>> {
        return bookDao.getAllBooksWithDetails()
    }

    // Search books with details
    fun searchBooksWithDetails(query: String): LiveData<List<BookWithDetails>> {
        return bookDao.searchBooksWithDetails(query)
    }

    // Get single book with details
    suspend fun getBookWithDetails(bookId: Int): BookWithDetails? {
        return withContext(Dispatchers.IO) {
            bookDao.getBookWithDetails(bookId)
        }
    }

    // Search for book by ISBN using Google Books API
    suspend fun searchBookByIsbn(isbn: String): Result<VolumeInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.searchBookByIsbn("isbn:$isbn")
                val items = response.body()?.items
                if (response.isSuccessful && !items.isNullOrEmpty()) {
                    Result.success(items[0].volumeInfo)
                } else {
                    Result.failure(Exception("Book not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Insert book from API data
    suspend fun insertBookFromApi(volumeInfo: VolumeInfo, userId: Int = 1): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if book already exists
                val isbn13 = volumeInfo.industryIdentifiers?.find { it.type == "ISBN_13" }?.identifier
                val isbn10 = volumeInfo.industryIdentifiers?.find { it.type == "ISBN_10" }?.identifier

                val searchIsbn = isbn13 ?: isbn10
                if (searchIsbn != null) {
                    val existingBook = bookDao.getBookByIsbn(searchIsbn)
                    if (existingBook != null) {
                        return@withContext Result.failure<Long>(Exception("Book already exists"))
                    }
                }

                // Insert category if it exists
                var categoryId: Int? = null
                if (!volumeInfo.categories.isNullOrEmpty()) {
                    val categoryName = volumeInfo.categories[0]
                    val category = categoryDao.getCategoryByName(categoryName)
                    categoryId = category?.categoryId ?: categoryDao.insertCategory(Category(categoryName = categoryName)).toInt()
                }

                // Insert book
                val book = Book(
                    isbn13 = isbn13,
                    isbn10 = isbn10,
                    title = volumeInfo.title ?: "Unknown Title",
                    subtitle = volumeInfo.subtitle,
                    publisher = volumeInfo.publisher,
                    publishedDate = volumeInfo.publishedDate,
                    description = volumeInfo.description,
                    pageCount = volumeInfo.pageCount,
                    averageRating = volumeInfo.averageRating,
                    thumbnail = volumeInfo.imageLinks?.thumbnail,
                    categoryId = categoryId
                )

                val bookId = bookDao.insertBook(book)

                // Insert authors
                if (!volumeInfo.authors.isNullOrEmpty()) {
                    for (authorName in volumeInfo.authors) {
                        val author = authorDao.getAuthorByName(authorName)
                        val authorId = author?.authorId ?: authorDao.insertAuthor(Author(name = authorName)).toInt()

                        bookAuthorDao.insertBookAuthor(BookAuthor(bookId.toInt(), authorId))
                    }
                }

                // Create link between user and book
                val userBook = UserBook(
                    userId = userId,
                    bookId = bookId.toInt(),
                    status = "to_read",
                    userRating = null,
                    currentPage = 0,
                    notes = null,
                    startedDate = null,
                    readDate = null
                )
                userBookDao.insertUserBook(userBook)

                Result.success(bookId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Update user book info (status, rating, etc.)
    suspend fun updateUserBook(userBook: UserBook) {
        withContext(Dispatchers.IO) {
            userBookDao.updateUserBook(userBook)
        }
    }

    // Get user book info
    suspend fun getUserBook(bookId: Int, userId: Int): UserBook? {
        return withContext(Dispatchers.IO) {
            userBookDao.getUserBook(bookId, userId)
        }
    }

    // Search books
    fun searchBooks(query: String): LiveData<List<Book>> {
        return bookDao.searchBooks(query)
    }

    // User operations
    fun getAllUsers(): LiveData<List<User>> = userDao.getAllUsers()

    suspend fun insertUser(user: User) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) = withContext(Dispatchers.IO) {
        userDao.deleteUser(user)
    }

    // Delete book
    suspend fun deleteBook(book: Book) {
        withContext(Dispatchers.IO) {
            // First delete associated book-author links
            bookAuthorDao.deleteBookAuthors(book.bookId)
            // Then delete the book itself
            bookDao.deleteBook(book)
        }
    }
}
