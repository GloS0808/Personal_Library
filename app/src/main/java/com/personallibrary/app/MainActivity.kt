package com.personallibrary.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.personallibrary.app.ui.PersonalLibraryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: BookViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!isSetupComplete()) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        setupCrashCatcher()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            PersonalLibraryTheme(darkTheme = isDarkMode) {
                var showFeedbackDialog by remember { mutableStateOf(false) }
                var crashLogState by remember { mutableStateOf<String?>(null) }
                
                LaunchedEffect(Unit) {
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val log = prefs.getString("last_crash_log", null)
                    if (log != null) {
                        crashLogState = log
                        prefs.edit().remove("last_crash_log").apply()
                    }
                }

                MainScreen(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    onBookClick = { book ->
                        val intent = Intent(this, BookDetailActivity::class.java)
                        intent.putExtra("BOOK_ID", book.book.bookId)
                        startActivity(intent)
                    },
                    onAddBookClick = {
                        startActivity(Intent(this, AddBookActivity::class.java))
                    },
                    onReadersClick = {
                        startActivity(Intent(this, ManageReadersActivity::class.java))
                    },
                    onFeedbackClick = { showFeedbackDialog = true },
                    onPrivacyClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)))
                        startActivity(intent)
                    },
                    onStatisticsClick = {
                        startActivity(Intent(this, StatisticsActivity::class.java))
                    },
                    onExportClick = { exportLibraryToCsv() }
                )

                if (showFeedbackDialog) {
                    FeedbackDialog(
                        onDismiss = { showFeedbackDialog = false },
                        onSend = { feedback ->
                            sendEmail(getString(R.string.feedback_subject), feedback)
                            showFeedbackDialog = false
                        }
                    )
                }

                crashLogState?.let { log ->
                    AlertDialog(
                        onDismissRequest = { crashLogState = null },
                        title = { Text(stringResource(R.string.crash_detected_title)) },
                        text = { Text(stringResource(R.string.crash_detected_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                sendEmail(getString(R.string.crash_report_subject), "The app crashed with the following stack trace:\n\n$log")
                                crashLogState = null
                            }) {
                                Text(stringResource(R.string.send_report))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { crashLogState = null }) {
                                Text(stringResource(R.string.no_thanks))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun isSetupComplete(): Boolean = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getBoolean("setup_complete", false)

    private fun setupCrashCatcher() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = throwable.stackTraceToString()
            getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_crash_log", stackTrace)
                .apply()
            defaultHandler?.uncaughtException(thread, throwable)
        }
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
            putExtra(Intent.EXTRA_SUBJECT, "$subject - ${getString(R.string.app_name)}")
            putExtra(Intent.EXTRA_TEXT, "$content\n\n$deviceDoc")
        }
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Select Email App"))
        } catch (e: Exception) {
        }
    }

    private fun exportLibraryToCsv() {
        val books = viewModel.allBooksWithDetails.value ?: return
        if (books.isEmpty()) return

        try {
            val csvFile = File(cacheDir, "my_library_export.csv")
            FileOutputStream(csvFile).use { fos ->
                fos.write("Title,Author,ISBN13,Category,Status,Rating\n".toByteArray())
                books.forEach { bookWithDetails ->
                    val title = bookWithDetails.book.title.replace(",", " ")
                    val author = bookWithDetails.authors.joinToString(" & ") { it.name }.replace(",", " ")
                    val isbn = bookWithDetails.book.isbn13 ?: bookWithDetails.book.isbn10 ?: ""
                    val category = bookWithDetails.category?.categoryName ?: ""
                    val status = bookWithDetails.userBook?.status ?: ""
                    val rating = bookWithDetails.userBook?.userRating?.toString() ?: ""

                    fos.write("$title,$author,$isbn,$category,$status,$rating\n".toByteArray())
                }
            }

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", csvFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "My Personal Library Export")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_library_chooser)))
        } catch (e: Exception) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: BookViewModel,
    windowSizeClass: WindowSizeClass,
    onBookClick: (BookWithDetails) -> Unit,
    onAddBookClick: () -> Unit,
    onReadersClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val books by viewModel.allBooksWithDetails.observeAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val usePermanentDrawer = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val drawerContent = @Composable {
        ModalDrawerSheet {
            Spacer(Modifier.height(12.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                label = { Text(stringResource(R.string.manage_readers)) },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onReadersClick()
                }
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Email, contentDescription = null) },
                label = { Text(stringResource(R.string.leave_feedback)) },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onFeedbackClick()
                }
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                label = { Text(stringResource(R.string.privacy_policy)) },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onPrivacyClick()
                }
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                label = { Text(stringResource(R.string.statistics)) },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onStatisticsClick()
                }
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Share, contentDescription = null) },
                label = { Text(stringResource(R.string.share_book_data)) },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onExportClick()
                }
            )
            Spacer(Modifier.weight(1f))
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dark Mode")
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
            }
        }
    }

    if (usePermanentDrawer) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(Modifier.width(240.dp)) {
                    drawerContent()
                }
            }
        ) {
            MainScaffold(
                viewModel = viewModel,
                windowSizeClass = windowSizeClass,
                drawerState = drawerState,
                scope = scope,
                books = books,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchActiveChange = { isSearchActive = it },
                onSearchQueryChange = { searchQuery = it },
                onBookClick = onBookClick,
                onAddBookClick = onAddBookClick,
                showMenuIcon = false
            )
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = drawerContent
        ) {
            MainScaffold(
                viewModel = viewModel,
                windowSizeClass = windowSizeClass,
                drawerState = drawerState,
                scope = scope,
                books = books,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchActiveChange = { isSearchActive = it },
                onSearchQueryChange = { searchQuery = it },
                onBookClick = onBookClick,
                onAddBookClick = onAddBookClick,
                showMenuIcon = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    viewModel: BookViewModel,
    windowSizeClass: WindowSizeClass,
    drawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    books: List<BookWithDetails>,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBookClick: (BookWithDetails) -> Unit,
    onAddBookClick: () -> Unit,
    showMenuIcon: Boolean
) {
    Scaffold(
        topBar = {
            if (isSearchActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        onSearchQueryChange(it)
                        viewModel.setQuery(it)
                    },
                    onSearch = { onSearchActiveChange(false) },
                    active = true,
                    onActiveChange = { onSearchActiveChange(it) },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        IconButton(onClick = { 
                            onSearchActiveChange(false)
                            onSearchQueryChange("")
                            viewModel.setQuery("")
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                onSearchQueryChange("")
                                viewModel.setQuery("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { }
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        if (showMenuIcon) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onSearchActiveChange(true) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        var showSortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_date_added)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.DATE_ADDED)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_title_asc)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.TITLE_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_title_desc)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.TITLE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_author)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.AUTHOR)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBookClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_book))
            }
        }
    ) { padding ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.no_books),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        stringResource(R.string.empty_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(books) { book ->
                        BookItem(book = book, onClick = { onBookClick(book) })
                    }
                }
            } else {
                val columns = when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Medium -> 2
                    else -> 3
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(books) { book ->
                        BookItem(book = book, onClick = { onBookClick(book) })
                    }
                }
            }
        }
    }
}

@Composable
fun BookItem(book: BookWithDetails, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.book.thumbnail,
                contentDescription = stringResource(R.string.book_cover),
                modifier = Modifier
                    .size(width = 60.dp, height = 90.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_book_placeholder),
                error = painterResource(R.drawable.ic_book_placeholder)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.authors.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val statusText = when (book.userBook?.status) {
                    "to_read" -> stringResource(R.string.status_to_read)
                    "reading" -> stringResource(R.string.status_reading)
                    "read" -> stringResource(R.string.status_completed)
                    else -> stringResource(R.string.status)
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var feedback by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.leave_feedback)) },
        text = {
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text(stringResource(R.string.feedback_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(feedback) },
                enabled = feedback.isNotBlank()
            ) {
                Text(stringResource(R.string.send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
