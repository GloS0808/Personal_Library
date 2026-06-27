package com.personallibrary.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var repository: BookRepository

    private lateinit var viewModel: BookViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock default behaviors
        `when`(repository.getAllUsers()).thenReturn(flowOf(emptyList()))
        
        viewModel = BookViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `insertUser calls repository`() = runTest {
        val name = "Test User"
        val email = "test@example.com"
        
        viewModel.insertUser(name, email)
        
        verify(repository).insertUser(any())
    }

    @Test
    fun `setQuery updates allBooksWithDetails`() = runTest {
        val query = "Kotlin"
        `when`(repository.searchBooksWithDetails(query)).thenReturn(flowOf(emptyList()))
        
        viewModel.setQuery(query)
        
        // Observe to trigger the flatMapLatest
        val observer = mock(Observer::class.java) as Observer<List<BookWithDetails>>
        viewModel.allBooksWithDetails.observeForever(observer)
        
        verify(repository).searchBooksWithDetails(query)
        viewModel.allBooksWithDetails.removeObserver(observer)
    }
}
