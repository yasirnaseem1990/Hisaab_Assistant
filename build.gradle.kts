// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // AGP 8.5.1+ zip-aligns uncompressed .so files to 16 KB boundaries in the
    // Play-generated APK (Android 16 KB page-size requirement 1). 8.2.2 did not.
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
