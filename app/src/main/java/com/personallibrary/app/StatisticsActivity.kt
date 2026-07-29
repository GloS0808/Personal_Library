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
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personallibrary.app.ui.PersonalLibraryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatisticsActivity : ComponentActivity() {

    private val viewModel: BookViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            PersonalLibraryTheme(darkTheme = isDarkMode) {
                StatisticsScreen(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: BookViewModel,
    windowSizeClass: WindowSizeClass,
    onBack: () -> Unit
) {
    val books by viewModel.allBooksWithDetails.observeAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_stats_available))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val totalBooks = books.size
                val readBooks = books.count { it.userBook?.status == "read" }
                val readingBooks = books.count { it.userBook?.status == "reading" }
                val toReadBooks = books.count { it.userBook?.status == "to_read" }

                Text(
                    text = stringResource(R.string.reading_progress),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                    StatCard(stringResource(R.string.books_read), readBooks, totalBooks, Color.Green)
                    StatCard(stringResource(R.string.currently_reading), readingBooks, totalBooks, Color.Blue)
                    StatCard(stringResource(R.string.to_read), toReadBooks, totalBooks, Color.Gray)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { StatCard(stringResource(R.string.books_read), readBooks, totalBooks, Color.Green) }
                        Box(Modifier.weight(1f)) { StatCard(stringResource(R.string.currently_reading), readingBooks, totalBooks, Color.Blue) }
                        Box(Modifier.weight(1f)) { StatCard(stringResource(R.string.to_read), toReadBooks, totalBooks, Color.Gray) }
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Pages Read (Current Books)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                val readingBooksList = books.filter { it.userBook?.status == "reading" }
                
                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                    readingBooksList.forEach { book ->
                        PageProgressItem(book)
                    }
                } else {
                    // Chunk into rows for tablet
                    readingBooksList.chunked(2).forEach { rowBooks ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowBooks.forEach { book ->
                                Box(Modifier.weight(1f)) {
                                    PageProgressItem(book)
                                }
                            }
                            if (rowBooks.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageProgressItem(book: BookWithDetails) {
    val currentPage = book.userBook?.currentPage ?: 0
    val totalPages = book.book.pageCount ?: 1
    val progress = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = book.book.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = "$currentPage / $totalPages pages (${(progress * 100).toInt()}%)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun StatCard(label: String, count: Int, total: Int, color: Color) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(text = "$count / $total")
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
            )
        }
    }
}
