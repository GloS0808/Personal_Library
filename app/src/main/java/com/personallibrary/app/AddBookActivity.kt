package com.personallibrary.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.personallibrary.app.ui.PersonalLibraryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddBookActivity : ComponentActivity() {

    private val viewModel: BookViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            PersonalLibraryTheme(darkTheme = isDarkMode) {
                AddBookScreen(
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
fun AddBookScreen(
    viewModel: BookViewModel,
    windowSizeClass: WindowSizeClass,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isbn by remember { mutableStateOf("") }
    val searchResult by viewModel.searchResult.observeAsState()
    val insertResult by viewModel.insertResult.observeAsState()
    var customImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCAN_RESULT")
            barcode?.let {
                isbn = it
                viewModel.searchBookByIsbn(it)
                isSearching = true
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        customImageUri = uri
    }

    LaunchedEffect(searchResult) {
        if (searchResult != null) {
            isSearching = false
        }
    }

    LaunchedEffect(insertResult) {
        if (insertResult?.isSuccess == true) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_book)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SearchSection(isbn, onIsbnChange = { isbn = it }, onScan = {
                    if (GoogleServicesUtils.isGooglePlayServicesAvailable(context)) {
                        val intent = Intent(context, BarcodeScannerActivity::class.java)
                        scanLauncher.launch(intent)
                    } else {
                        Toast.makeText(context, "Barcode scanning is not available on this device", Toast.LENGTH_SHORT).show()
                    }
                }, onSearch = {
                    if (isbn.isNotBlank()) {
                        isSearching = true
                        viewModel.searchBookByIsbn(isbn)
                    }
                }, isSearching = isSearching)

                searchResult?.let { result ->
                    if (result.isSuccess) {
                        result.getOrNull()?.let { volumeInfo ->
                            BookPreviewCard(volumeInfo, customImageUri, onAddPhoto = { imageLauncher.launch("image/*") }, onAddBook = {
                                val updatedVolumeInfo = if (customImageUri != null) {
                                    val newImageLinks = GoogleBooksResponse.ImageLinks(
                                        smallThumbnail = customImageUri.toString(),
                                        thumbnail = customImageUri.toString()
                                    )
                                    volumeInfo.copy(imageLinks = newImageLinks)
                                } else {
                                    volumeInfo
                                }
                                viewModel.insertBookFromApi(updatedVolumeInfo)
                            })
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.error_book_not_found, result.exceptionOrNull()?.message ?: ""),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
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
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SearchSection(isbn, onIsbnChange = { isbn = it }, onScan = {
                        if (GoogleServicesUtils.isGooglePlayServicesAvailable(context)) {
                            val intent = Intent(context, BarcodeScannerActivity::class.java)
                            scanLauncher.launch(intent)
                        } else {
                            Toast.makeText(context, "Barcode scanning is not available on this device", Toast.LENGTH_SHORT).show()
                        }
                    }, onSearch = {
                        if (isbn.isNotBlank()) {
                            isSearching = true
                            viewModel.searchBookByIsbn(isbn)
                        }
                    }, isSearching = isSearching)

                    searchResult?.let { result ->
                        if (result.isFailure) {
                            Text(
                                text = stringResource(R.string.error_book_not_found, result.exceptionOrNull()?.message ?: ""),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                
                Divider(modifier = Modifier.width(1.dp).fillMaxHeight().padding(horizontal = 24.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    searchResult?.let { result ->
                        if (result.isSuccess) {
                            result.getOrNull()?.let { volumeInfo ->
                                BookPreviewCard(volumeInfo, customImageUri, onAddPhoto = { imageLauncher.launch("image/*") }, onAddBook = {
                                    val updatedVolumeInfo = if (customImageUri != null) {
                                        val newImageLinks = GoogleBooksResponse.ImageLinks(
                                            smallThumbnail = customImageUri.toString(),
                                            thumbnail = customImageUri.toString()
                                        )
                                        volumeInfo.copy(imageLinks = newImageLinks)
                                    } else {
                                        volumeInfo
                                    }
                                    viewModel.insertBookFromApi(updatedVolumeInfo)
                                })
                            }
                        } else if (!isSearching) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Search results will appear here")
                            }
                        }
                    } ?: run {
                        if (!isSearching) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Search results will appear here")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSection(
    isbn: String,
    onIsbnChange: (String) -> Unit,
    onScan: () -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean
) {
    OutlinedTextField(
        value = isbn,
        onValueChange = onIsbnChange,
        label = { Text(stringResource(R.string.isbn)) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = onScan) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Scan")
            }
        }
    )

    Button(
        onClick = onSearch,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.search))
    }

    if (isSearching) {
        CircularProgressIndicator()
    }
}

@Composable
fun BookPreviewCard(
    volumeInfo: GoogleBooksResponse.VolumeInfo,
    customImageUri: Uri?,
    onAddPhoto: () -> Unit,
    onAddBook: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = customImageUri ?: volumeInfo.imageLinks?.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp, 225.dp)
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )
            
            Button(
                onClick = onAddPhoto,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(if (customImageUri == null) R.string.add_photo else R.string.change_photo))
            }

            Text(
                text = volumeInfo.title ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = volumeInfo.authors?.joinToString(", ") ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onAddBook,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_to_library))
            }
        }
    }
}
