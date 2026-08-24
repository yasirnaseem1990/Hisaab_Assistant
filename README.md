# MyHissabSDK (Android)

Android Library SDK that embeds the MyHisaab webapp in a high-performance `WebView`. Pass a
`clientId`; the SDK opens:

`https://app.myhisaab.pk/login?clientId=<clientId>`

Supports **Jetpack Compose** (`MyHisaabView`) and **XML Views** (`MyHisaabActivity` /
`MyHisaabFragment`). Requires Android 7.0+ (API level 24+).

---

## 🚀 Installation via JitPack

### 1. Add the JitPack repository

In your root `settings.gradle.kts` (or `build.gradle`):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency

In your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.roamiebuddy-creator:MyHissabSDK:1.0.0")
}
```

---

## 💻 Usage

### Jetpack Compose

```kotlin
import pk.myhisaab.sdk.ui.MyHisaabView
import pk.myhisaab.sdk.model.MyHisaabEnvironment

@Composable
fun MainScreen() {
    MyHisaabView(
        clientId = "your-client-id",
        environment = MyHisaabEnvironment.PRODUCTION, // Optional (default: PRODUCTION)
        modifier = Modifier.fillMaxSize()
    )
}
```

#### Staging Environment:

```kotlin
MyHisaabView(
    clientId = "your-client-id",
    environment = MyHisaabEnvironment.STAGING,
    modifier = Modifier.fillMaxSize()
)
```

#### Passing Firebase FCM Token (Push Notifications):

```kotlin
MyHisaabView(
    clientId = "your-client-id",
    fcmToken = fcmToken,
    modifier = Modifier.fillMaxSize()
)
```

---

### XML / View-Based Applications

#### Launching via Activity:

```kotlin
import pk.myhisaab.sdk.ui.MyHisaabActivity
import pk.myhisaab.sdk.model.MyHisaabEnvironment

MyHisaabActivity.launch(
    context = this,
    clientId = "your-client-id",
    environment = MyHisaabEnvironment.PRODUCTION
)
```

#### Embedding via Fragment:

```kotlin
import pk.myhisaab.sdk.ui.MyHisaabFragment

val fragment = MyHisaabFragment.newInstance(
    clientId = "your-client-id"
)

supportFragmentManager.beginTransaction()
    .replace(R.id.fragment_container, fragment)
    .commit()
```

---

## 🔐 Permissions (AndroidManifest.xml)

The SDK automatically merges required permissions. If your host app uses camera, microphone, or
image pickers, ensure your app's `AndroidManifest.xml` includes:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

---

## 🛠 Features Included

- **Edge-to-Edge WebView**: Optimized viewport rendering with full JavaScript and DOM storage
  support.
- **Hardware Permission Handling**: Automatic handling of camera & audio permissions requested by
  web applications.
- **Native Camera & Gallery Picker**: Intercepts file inputs to allow photo capture via native
  CameraX overlay or gallery picker.
- **Push Notification Token Injection**: Safely evaluates and injects FCM tokens into
  `window.receiveFcmToken`.
- **Automatic Back Navigation**: Connects system back-button to WebView history navigation.
