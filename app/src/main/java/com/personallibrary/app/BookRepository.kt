package com.personallibrary.app

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.personallibrary.app.GoogleBooksResponse.VolumeInfo
import javax.inject.Inject

class BookRepository @Inject constructor(
    private val database: LibraryDatabase,
    private val googleBooksApi: GoogleBooksApi,
    private val openLibraryApi: OpenLibraryApi,
    private val itBookstoreApi: ITBookstoreApi
) {

    private val TAG = "BookRepository"
    private val bookDao = database.bookDao()
    private val authorDao = database.authorDao()
    private val categoryDao = database.categoryDao()
    private val userBookDao = database.userBookDao()
    private val bookAuthorDao = database.bookAuthorDao()
    private val userDao = database.userDao()

    // Get all books with details
    fun getAllBooksWithDetails(): Flow<List<BookWithDetails>> {
        return bookDao.getAllBooksWithDetails()
    }

    // Search books with details
    fun searchBooksWithDetails(query: String): Flow<List<BookWithDetails>> {
        return bookDao.searchBooksWithDetails(query)
    }

    // Get single book with details
    suspend fun getBookWithDetails(bookId: Int): BookWithDetails? {
        return withContext(Dispatchers.IO) {
            bookDao.getBookWithDetails(bookId)
        }
    }

    // Search for book by ISBN using multiple APIs
    suspend fun searchBookByIsbn(isbn: String): Result<VolumeInfo> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting book search for ISBN: $isbn")
            
            var bestCandidate: VolumeInfo? = null

            // Try Google Books API
            val googleResult = tryGoogleBooks(isbn)
            if (googleResult.isSuccess) {
                val info = googleResult.getOrNull()
                if (info?.description != null) {
                    Log.i(TAG, "Google Books search successful with description for $isbn")
                    return@withContext googleResult
                }
                bestCandidate = info
                Log.i(TAG, "Google Books search successful but NO description for $isbn")
            } else {
                Log.w(TAG, "Google Books failed for $isbn: ${googleResult.exceptionOrNull()?.message}")
            }

            // Try Open Library API
            val openLibraryResult = tryOpenLibrary(isbn)
            if (openLibraryResult.isSuccess) {
                val info = openLibraryResult.getOrNull()
                if (info?.description != null) {
                    Log.i(TAG, "Open Library search successful with description for $isbn")
                    return@withContext openLibraryResult
                }
                if (bestCandidate == null) bestCandidate = info
                Log.i(TAG, "Open Library search successful but NO description for $isbn")
            } else {
                Log.w(TAG, "Open Library failed for $isbn: ${openLibraryResult.exceptionOrNull()?.message}")
            }

            // Try IT Bookstore API
            val itBookstoreResult = tryITBookstore(isbn)
            if (itBookstoreResult.isSuccess) {
                val info = itBookstoreResult.getOrNull()
                if (info?.description != null) {
                    Log.i(TAG, "IT Bookstore search successful with description for $isbn")
                    return@withContext itBookstoreResult
                }
                if (bestCandidate == null) bestCandidate = info
                Log.i(TAG, "IT Bookstore search successful but NO description for $isbn")
            } else {
                Log.w(TAG, "IT Bookstore failed for $isbn: ${itBookstoreResult.exceptionOrNull()?.message}")
            }

            if (bestCandidate != null) {
                Log.i(TAG, "Returning best candidate (no description) for $isbn")
                return@withContext Result.success(bestCandidate)
            }

            Log.e(TAG, "All book lookup providers failed for ISBN: $isbn")
            Result.failure(Exception("Book not found in any supported database"))
        }
    }

    private suspend fun tryGoogleBooks(isbn: String): Result<VolumeInfo> {
        return try {
            val response = googleBooksApi.searchBookByIsbn("isbn:$isbn")
            if (response.isSuccessful) {
                val items = response.body()?.items
                if (!items.isNullOrEmpty()) {
                    Result.success(items[0].volumeInfo)
                } else {
                    Result.failure(Exception("Google Books returned empty items"))
                }
            } else {
                Result.failure(Exception("Google Books API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun tryOpenLibrary(isbn: String): Result<VolumeInfo> {
        return try {
            // Updated to match the interface signature: bibkeys, format, jscmd
            val bookKey = "ISBN:$isbn"
            val response = openLibraryApi.searchBookByIsbn(
                bibkeys = bookKey,
                format = "json",
                jscmd = "data"
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.containsKey(bookKey)) {
                    val olBook = body[bookKey]!!
                    
                    val desc = when (val d = olBook.description) {
                        is String -> d
                        is Map<*, *> -> d["value"] as? String
                        else -> null
                    }

                    val volumeInfo = VolumeInfo(
                        title = olBook.title,
                        subtitle = null,
                        authors = olBook.authors?.map { it.name },
                        publisher = olBook.publishers?.firstOrNull()?.name,
                        publishedDate = olBook.publishDate,
                        description = desc,
                        industryIdentifiers = olBook.identifiers?.let { ids ->
                            val identifiers = mutableListOf<GoogleBooksResponse.IndustryIdentifier>()
                            ids.isbn13?.forEach { identifiers.add(GoogleBooksResponse.IndustryIdentifier("ISBN_13", it)) }
                            ids.isbn10?.forEach { identifiers.add(GoogleBooksResponse.IndustryIdentifier("ISBN_10", it)) }
                            identifiers
                        },
                        pageCount = olBook.pageCount,
                        categories = null,
                        averageRating = null,
                        imageLinks = olBook.cover?.large?.let { GoogleBooksResponse.ImageLinks(it, it) }
                    )
                    Result.success(volumeInfo)
                } else {
                    Result.failure(Exception("Open Library returned no entry for key $bookKey"))
                }
            } else {
                Result.failure(Exception("Open Library API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun tryITBookstore(isbn: String): Result<VolumeInfo> {
        return try {
            val response = itBookstoreApi.getBookByIsbn(isbn)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.error == "0") {
                    val volumeInfo = VolumeInfo(
                        title = body.title,
                        subtitle = body.subtitle,
                        authors = body.authors?.split(",")?.map { it.trim() },
                        publisher = body.publisher,
                        publishedDate = body.year,
                        description = body.desc,
                        industryIdentifiers = listOfNotNull(
                            body.isbn13?.let { GoogleBooksResponse.IndustryIdentifier("ISBN_13", it) },
                            body.isbn10?.let { GoogleBooksResponse.IndustryIdentifier("ISBN_10", it) }
                        ),
                        pageCount = body.pages?.toIntOrNull(),
                        categories = null,
                        averageRating = body.rating?.toFloatOrNull(),
                        imageLinks = body.image?.let { GoogleBooksResponse.ImageLinks(it, it) }
                    )
                    Result.success(volumeInfo)
                } else {
                    Result.failure(Exception("IT Bookstore returned error: ${body?.error}"))
                }
            } else {
                Result.failure(Exception("IT Bookstore API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Insert book from API data
    suspend fun insertBookFromApi(volumeInfo: VolumeInfo, userId: Int = 1): Result<Long> {
        return try {
            database.withTransaction {
                // Check if book already exists
                val isbn13 = volumeInfo.industryIdentifiers?.find { it.type == "ISBN_13" }?.identifier
                val isbn10 = volumeInfo.industryIdentifiers?.find { it.type == "ISBN_10" }?.identifier

                val searchIsbn = isbn13 ?: isbn10
                if (searchIsbn != null) {
                    val existingBook = bookDao.getBookByIsbn(searchIsbn)
                    if (existingBook != null) {
                        throw Exception("Book already exists")
                    }
                }

                // Insert category if it exists
                var categoryId: Int? = null
                if (!volumeInfo.categories.isNullOrEmpty()) {
                    val categoryName = volumeInfo.categories[0]
                    val category = categoryDao.getCategoryByName(categoryName)
                    categoryId = category?.categoryId
                        ?: categoryDao.insertCategory(Category(categoryName = categoryName)).toInt()
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
                        val authorId = author?.authorId
                            ?: authorDao.insertAuthor(Author(name = authorName)).toInt()

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
            }
        } catch (e: Exception) {
            Result.failure(e)
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
    fun searchBooks(query: String): Flow<List<Book>> {
        return bookDao.searchBooks(query)
    }

    // User operations
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

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
        database.withTransaction {
            // First delete associated book-author links
            bookAuthorDao.deleteBookAuthors(book.bookId)
            // Then delete the book itself
            bookDao.deleteBook(book)
        }
    }
}
