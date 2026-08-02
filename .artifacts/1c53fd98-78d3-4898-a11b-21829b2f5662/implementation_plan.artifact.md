# Implementation Plan - Privacy Hardening

This plan outlines steps to improve the "Personal Library" app's alignment with Android privacy best practices.

## User Review Required

> [!NOTE]
> I will be adding a **Permission Rationale** for the Camera. This will show an explanation to the user if they have previously denied the camera permission, helping them understand why it's needed for barcode scanning before requesting it again.

## Proposed Changes

### Permissions

#### [MODIFY] [BarcodeScannerActivity.kt](file:///C:/Users/semg6/StudioProjects/Personal_Library/app/src/main/java/com/personallibrary/app/BarcodeScannerActivity.kt)
- Implement `shouldShowRequestPermissionRationale`.
- Add an `AlertDialog` to explain the need for the camera if the user previously denied the request.

### Data Handling

#### [MODIFY] [MainActivity.kt](file:///C:/Users/semg6/StudioProjects/Personal_Library/app/src/main/java/com/personallibrary/app/MainActivity.kt)
- Review the `sendEmail` content for crash reports. Ensure that the stack trace and device info are presented transparently in the dialog before the user clicks "Send Report". (This is already mostly in place, but I will ensure the strings are clear).

## Verification Plan

### Automated Tests
- Build the app to ensure no compilation errors.

### Manual Verification
1.  **Permission Denial**:
    - Open the barcode scanner.
    - Deny the camera permission.
    - Re-open the scanner.
    - Verify that the rationale dialog appears explaining why the camera is needed.
2.  **Permission Grant**:
    - Grant the permission from the rationale or the system dialog.
    - Verify the camera starts correctly.
3.  **CSV Export**:
    - Verify that the exported CSV only contains book-related data (Title, ISBN, etc.) and no user-sensitive data like the reader's email or personal notes.
