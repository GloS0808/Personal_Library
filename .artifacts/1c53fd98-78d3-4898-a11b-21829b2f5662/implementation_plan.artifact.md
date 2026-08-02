# Implementation Plan - Stability & GMS Compatibility

This plan addresses the "Something went wrong" error related to Google Play Services (GMS) and improves overall app stability for test environments.

## User Review Required

> [!IMPORTANT]
> The error message "Check that Google Play is enabled on your device..." is typically triggered by **Play Integrity** or **Google Play Services** availability checks.
>
> If you have enabled **"App Integrity"** or **"Integrity check on launch"** in the Google Play Console, it will block any app that is not installed directly from the Play Store. For testing sideloaded APKs, you should disable these checks in the console or use **Internal App Sharing**.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/semg6/StudioProjects/Personal_Library/app/build.gradle.kts)
- Update `androidx.security:security-crypto` from `1.1.0-alpha06` to `1.1.0` (Stable) to improve security module stability.
- Explicitly add `com.google.android.gms:play-services-base` to allow manual GMS availability checks.

### Stability Improvements

#### [MODIFY] [MainActivity.kt](file:///C:/Users/semg6/StudioProjects/Personal_Library/app/src/main/java/com/personallibrary/app/MainActivity.kt)
- Add a runtime check for **Google Play Services** at app startup.
- If GMS is missing or outdated, show a friendly warning instead of letting the platform throw a generic "Something went wrong" dialog.
- This will allow the app to function (except for barcode scanning) even on devices without full Google Play support.

#### [MODIFY] [BarcodeScannerActivity.kt](file:///C:/Users/semg6/StudioProjects/Personal_Library/app/src/main/java/com/personallibrary/app/BarcodeScannerActivity.kt)
- Add a guard clause to check GMS availability before initializing ML Kit.

## Verification Plan

### Automated Tests
- Run `:app:assembleDebug` to ensure no dependency conflicts.

### Manual Verification
1.  **Device with GMS**: Verify the app opens and barcode scanning works as expected.
2.  **Device without GMS (or disabled)**: Verify the app shows a clear message explaining that some features (scanning) might be unavailable, but still allows manual entry and library browsing.
