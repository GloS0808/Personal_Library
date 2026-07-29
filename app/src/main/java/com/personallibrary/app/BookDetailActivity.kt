package com.personallibrary.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.personallibrary.app.ui.PersonalLibraryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookDetailActivity : ComponentActivity() {

    private val viewModel: BookViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val bookId = intent.getIntExtra("BOOK_ID", -1)
        if (bookId == -1) {
            finish()
            return
        }

        viewModel.loadBook(bookId)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            PersonalLibraryTheme(darkTheme = isDarkMode) {
                BookDetailScreen(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    onBack = { finish() },
                    onDeleteSuccess = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: BookViewModel,
    windowSizeClass: WindowSizeClass,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val bookWithDetails by viewModel.currentBook.observeAsState()
    val users by viewModel.allUsers.observeAsState(emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val saveSuccessMessage = stringResource(R.string.save_success)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bookWithDetails?.book?.title ?: stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        bookWithDetails?.let { book ->
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    BookInfoSection(book)
                    Divider(modifier = Modifier.padding(vertical = 24.dp))
                    UserBookSection(book, users, viewModel, snackbarHostState, scope, saveSuccessMessage)
                }
            } else {
                Row(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        BookInfoSection(book)
                    }
                    Divider(modifier = Modifier.width(1.dp).fillMaxHeight().padding(horizontal = 24.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        UserBookSection(book, users, viewModel, snackbarHostState, scope, saveSuccessMessage)
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete)) },
                text = { Text(stringResource(R.string.delete_book_confirmation)) },
                confirmButton = {
                    TextButton(onClick = {
                        bookWithDetails?.book?.let {
                            viewModel.deleteBook(it)
                            onDeleteSuccess()
                        }
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun BookInfoSection(book: BookWithDetails) {
    Column {
        AsyncImage(
            model = book.book.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(150.dp, 225.dp)
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = book.book.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        book.book.subtitle?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium)
        }

        Text(
            text = book.authors.joinToString(", ") { it.name },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        DetailRow(stringResource(R.string.publisher), book.book.publisher ?: stringResource(R.string.not_available))
        DetailRow(stringResource(R.string.published_date), book.book.publishedDate ?: stringResource(R.string.not_available))
        DetailRow(stringResource(R.string.pages), book.book.pageCount?.toString() ?: stringResource(R.string.not_available))
        DetailRow(stringResource(R.string.category), book.category?.categoryName ?: stringResource(R.string.not_available))

        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.description), fontWeight = FontWeight.Bold)
        Text(book.book.description ?: stringResource(R.string.not_available))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserBookSection(
    book: BookWithDetails,
    users: List<User>,
    viewModel: BookViewModel,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    saveSuccessMessage: String
) {
    Column {
        Text(
            stringResource(R.string.my_reading_info),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        var statusExpanded by remember { mutableStateOf(false) }
        val statuses = listOf("to_read", "reading", "read")
        val statusLabels = listOf(
            stringResource(R.string.status_to_read),
            stringResource(R.string.status_reading),
            stringResource(R.string.status_completed)
        )
        
        var currentStatus by remember(book.userBook) { mutableStateOf(book.userBook?.status ?: "to_read") }
        var currentRating by remember(book.userBook) { mutableStateOf(book.userBook?.userRating ?: 0f) }
        var currentPage by remember(book.userBook) { mutableStateOf(book.userBook?.currentPage?.toString() ?: "") }
        var notes by remember(book.userBook) { mutableStateOf(book.userBook?.notes ?: "") }
        var startedDate by remember(book.userBook) { mutableStateOf(book.userBook?.startedDate ?: "") }
        var readDate by remember(book.userBook) { mutableStateOf(book.userBook?.readDate ?: "") }
        var selectedUserId by remember(book.userBook) { mutableStateOf(book.userBook?.userId ?: users.firstOrNull()?.userId ?: 1) }

        ExposedDropdownMenuBox(
            expanded = statusExpanded,
            onExpandedChange = { statusExpanded = it }
        ) {
            OutlinedTextField(
                value = statusLabels[statuses.indexOf(currentStatus).coerceAtLeast(0)],
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.status)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                statuses.forEachIndexed { index, status ->
                    DropdownMenuItem(
                        text = { Text(statusLabels[index]) },
                        onClick = {
                            currentStatus = status
                            statusExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        var userExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = userExpanded,
            onExpandedChange = { userExpanded = it }
        ) {
            OutlinedTextField(
                value = users.find { it.userId == selectedUserId }?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.reader_name)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = userExpanded,
                onDismissRequest = { userExpanded = false }
            ) {
                users.forEach { user ->
                    DropdownMenuItem(
                        text = { Text(user.name) },
                        onClick = {
                            selectedUserId = user.userId
                            userExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.my_rating))
        Slider(
            value = currentRating,
            onValueChange = { currentRating = it },
            valueRange = 0f..5f,
            steps = 9 // 0, 0.5, 1, ..., 5
        )
        Text(text = currentRating.toString(), modifier = Modifier.align(Alignment.End))

        OutlinedTextField(
            value = currentPage,
            onValueChange = { currentPage = it },
            label = { Text(stringResource(R.string.current_page)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = startedDate,
            onValueChange = { startedDate = it },
            label = { Text(stringResource(R.string.started_date_hint)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = readDate,
            onValueChange = { readDate = it },
            label = { Text(stringResource(R.string.finished_date_hint)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val updatedUserBook = UserBook(
                    userId = selectedUserId,
                    bookId = book.book.bookId,
                    status = currentStatus,
                    userRating = if (currentRating > 0) currentRating else null,
                    currentPage = currentPage.toIntOrNull(),
                    notes = notes.ifBlank { null },
                    startedDate = startedDate.ifBlank { null },
                    readDate = readDate.ifBlank { null }
                )
                viewModel.updateUserBook(updatedUserBook)
                scope.launch {
                    snackbarHostState.showSnackbar(saveSuccessMessage)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}
