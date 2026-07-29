# Implementation Plan - Fallback API for Book Description

This plan addresses the requirement to try alternative APIs if a book search succeeds but the description is missing ("N/A").

## User Review Required

> [!NOTE]
> The current logic returns the first successful API result. I will modify it to continue searching if the description is missing, while still returning the book details from the first API if no description is found anywhere.

## Proposed Changes

### Data Models

#### [MODIFY] [OpenLibraryResponse.kt](file:///C:/Users/semg6/StudioProjects/Personal_Library/app/src/main/java/com/personallibrary/app/OpenLibraryResponse.kt)
- Add `description: Any?` to support fetching descriptions from Open Library.

### Repository Logic

#### [MODIFY] [BookRepository.kt](file:///C:/Users/semg6/StudioProjects/Personal_Library/app/src/main/java/com/personallibrary/app/BookRepository.kt)
- **`tryOpenLibrary`**: Extract description from the `Any?` field (handling both String and Map types).
- **`searchBookByIsbn`**:
    - Iterate through APIs (Google, Open Library, IT Bookstore).
    - If an API returns a result:
        - If it has a description, return it immediately.
        - If it doesn't have a description, store it as `bestCandidate` if we don't have one yet, and continue to the next API.
    - After checking all APIs, if we have a `bestCandidate`, return it.

## Verification Plan

### Automated Tests
- I will attempt to build the project to ensure the `Any?` type in `OpenLibraryResponse` and its handling in `BookRepository` are correctly implemented.
- I'll add a log statement to verify when a fallback is triggered due to a missing description.

### Manual Verification
- Test with an ISBN that is known to have no description on Google Books but might have one on Open Library (or vice versa).
