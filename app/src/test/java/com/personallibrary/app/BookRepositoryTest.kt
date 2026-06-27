package com.personallibrary.app

import com.personallibrary.app.GoogleBooksResponse.VolumeInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BookRepositoryTest {

    @Mock
    private lateinit var database: LibraryDatabase
    @Mock
    private lateinit var googleBooksApi: GoogleBooksApi
    @Mock
    private lateinit var openLibraryApi: OpenLibraryApi
    @Mock
    private lateinit var itBookstoreApi: ITBookstoreApi

    private lateinit var repository: BookRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock DAOs to avoid NullPointerException in constructor
        `when`(database.bookDao()).thenReturn(mock(BookDao::class.java))
        `when`(database.authorDao()).thenReturn(mock(AuthorDao::class.java))
        `when`(database.categoryDao()).thenReturn(mock(CategoryDao::class.java))
        `when`(database.userBookDao()).thenReturn(mock(UserBookDao::class.java))
        `when`(database.bookAuthorDao()).thenReturn(mock(BookAuthorDao::class.java))
        `when`(database.userDao()).thenReturn(mock(UserDao::class.java))

        repository = BookRepository(database, googleBooksApi, openLibraryApi, itBookstoreApi)
    }

    @Test
    fun `searchBookByIsbn falls back to IT Bookstore when others fail`() = runTest {
        val isbn = "1234567890"
        
        // Google Books fails
        `when`(googleBooksApi.searchBookByIsbn(anyString())).thenReturn(Response.success(null))
        
        // Open Library fails
        `when`(openLibraryApi.searchBookByIsbn(anyString(), anyString(), anyString())).thenReturn(Response.success(emptyMap()))
        
        // IT Bookstore succeeds
        val mockResponse = ITBookstoreResponse(
            error = "0",
            title = "IT Book Title",
            subtitle = "IT Subtitle",
            authors = "Author One, Author Two",
            publisher = "IT Publisher",
            isbn10 = "1234567890",
            isbn13 = "9781234567890",
            pages = "300",
            year = "2024",
            rating = "4",
            desc = "IT Description",
            image = "https://example.com/image.png"
        )
        `when`(itBookstoreApi.getBookByIsbn(isbn)).thenReturn(Response.success(mockResponse))

        val result = repository.searchBookByIsbn(isbn)

        assertTrue(result.isSuccess)
        val volumeInfo = result.getOrNull()
        assertEquals("IT Book Title", volumeInfo?.title)
        assertEquals(listOf("Author One", "Author Two"), volumeInfo?.authors)
        
        verify(googleBooksApi).searchBookByIsbn(anyString())
        verify(openLibraryApi).searchBookByIsbn(anyString(), anyString(), anyString())
        verify(itBookstoreApi).getBookByIsbn(isbn)
    }

    @Test
    fun `searchBookByIsbn returns failure when all providers fail`() = runTest {
        val isbn = "1234567890"
        
        `when`(googleBooksApi.searchBookByIsbn(anyString())).thenReturn(Response.success(null))
        `when`(openLibraryApi.searchBookByIsbn(anyString(), anyString(), anyString())).thenReturn(Response.success(emptyMap()))
        `when`(itBookstoreApi.getBookByIsbn(isbn)).thenReturn(Response.success(ITBookstoreResponse("error", null, null, null, null, null, null, null, null, null, null, null)))

        val result = repository.searchBookByIsbn(isbn)

        assertTrue(result.isFailure)
        assertEquals("Book not found in any supported database", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchBookByIsbn returns success from Google Books and does not call others`() = runTest {
        val isbn = "1234567890"
        
        val mockVolumeInfo = VolumeInfo(
            title = "Google Title",
            subtitle = null,
            authors = listOf("Google Author"),
            publisher = null,
            publishedDate = null,
            description = null,
            industryIdentifiers = null,
            pageCount = null,
            categories = null,
            averageRating = null,
            imageLinks = null
        )
        val mockResponse = GoogleBooksResponse(items = listOf(GoogleBooksResponse.BookItem("id", mockVolumeInfo)))
        
        `when`(googleBooksApi.searchBookByIsbn("isbn:$isbn")).thenReturn(Response.success(mockResponse))

        val result = repository.searchBookByIsbn(isbn)

        assertTrue(result.isSuccess)
        assertEquals("Google Title", result.getOrNull()?.title)
        
        verify(googleBooksApi).searchBookByIsbn("isbn:$isbn")
        verifyNoInteractions(openLibraryApi)
        verifyNoInteractions(itBookstoreApi)
    }
}
