# Tasks - Target Android 16 (API 36)

- [x] Update `targetSdk` to 36 in `app/build.gradle.kts`
- [x] Add `enableEdgeToEdge()` to Activities:
    - [x] `MainActivity.kt`
    - [x] `AddBookActivity.kt`
    - [x] `WelcomeActivity.kt`
    - [x] `BookDetailActivity.kt`
    - [x] `ManageReadersActivity.kt`
    - [x] `StatisticsActivity.kt`
    - [x] `BarcodeScannerActivity.kt`
- [x] Migrate legacy `onBackPressed` to `OnBackPressedDispatcher`
- [x] Verify build and UI
- [x] Implement Fallback API for Book Description
    - [x] Update `OpenLibraryResponse.kt` to include description field
    - [x] Update `BookRepository.kt` to parse description and retry search across APIs if description is missing
