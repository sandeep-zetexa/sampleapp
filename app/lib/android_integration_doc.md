# ZetSIM Android SDK Integration Guide

## Overview

The ZetSIM Android SDK provides a ready-to-use Flutter-powered experience that can be embedded directly into any Android application.

The SDK is distributed as a local Maven repository and integrates seamlessly with native Android applications written in Kotlin.

> **Important**
>
> The SDK requires host-provided configuration through Pigeon APIs. A custom host activity must be implemented to provide branding, environment, checkout context, and navigation handling. Directly launching a default Flutter activity is not supported.

---

## Requirements

| Requirement | Version         |
| ----------- | --------------- |
| Kotlin      | 2.1.0 or higher |
| Minimum SDK | API 24+         |
| Compile SDK | API 36          |

---

# Installation

## Step 1: Add SDK Files

Copy the SDK package into your application's `libs` directory.

### Directory Structure

```text
app/
└── libs/
    └── repo/
        ├── com/
        ├── io/
        └── ...
```

> Ensure the complete repository structure is copied without modifications.

---

## Step 2: Configure Repositories

Add the local SDK repository and Flutter repository to your `settings.gradle.kts`.

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("${rootDir}/app/libs/repo")
        }

        maven {
            url = uri("https://storage.googleapis.com/download.flutter.io")
        }
    }
}
```

---

## Step 3: Add SDK Dependency

Add the ZetSIM SDK dependency to your app module's `build.gradle.kts`.

```kotlin
dependencies {
    implementation("com.zetexa.app_sdk:flutter_release:1.0")
}
```

---

## Step 4: Sync the Project

Sync Gradle and ensure the Flutter dependencies are resolved successfully.

The following imports should be available:

```kotlin
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
```

### Troubleshooting

If the imports are unresolved:

1. Verify all SDK files exist inside `app/libs/repo`.
2. Verify the repository path in `settings.gradle.kts`.
3. Re-sync Gradle.
4. Clean and rebuild the project.

---

# Android Configuration

## Register Flutter Activity & Configure Manifest

1. Add the custom `ZetSdkActivity` declaration inside the `<application>` tag of your `AndroidManifest.xml`.
2. Enable cleartext HTTP traffic inside the `<application>` tag to support APIs connecting to non-production environments.

```xml
<application
    ...
    android:usesCleartextTraffic="true">

    <activity
        android:name=".ZetSdkActivity"
        android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
        android:hardwareAccelerated="true"
        android:windowSoftInputMode="adjustResize" />

</application>
```

---

## Required Permissions

Add the required permissions to `AndroidManifest.xml` to enable internet access and network connectivity state monitoring (required by communication packages inside the SDK):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

---

# SDK Communication Architecture

The SDK uses **Pigeon** for type-safe communication between the Android host application and Flutter.

This provides:

* Compile-time safety
* Strongly typed models
* Reliable communication between Kotlin and Dart
* No manual JSON serialization
* Reduced runtime errors

---

## Communication Flow

```text
Host Android App (Kotlin)
          │
          ▼
      Pigeon APIs
          │
          ▼
     Flutter SDK
```

### Flow

1. Host launches the SDK activity.
2. Flutter requests branding and configuration.
3. Host returns branding data.
4. Flutter requests checkout/user context.
5. Host returns user information.
6. Flutter notifies the host when the SDK should close.

---

# Implementing the SDK Host Activity

To achieve instant startup speeds and avoid delays when opening the SDK, pre-warm the `FlutterEngine` once and reuse it across launches.

### 1. Pre-warming and Caching the Engine
Initialize the engine and configure static Pigeon configuration APIs (`NativeBrandConfigApi` and `HostToFlutterCheckoutApi`) once (e.g. in `MainActivity.onCreate` or a custom `Application` class):

```kotlin
// Pre-warm the FlutterEngine once and register Pigeon APIs
if (FlutterEngineCache.getInstance().get("zetsim_engine") == null) {
    val flutterEngine = FlutterEngine(applicationContext).apply {
        dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )
    }
    val messenger = flutterEngine.dartExecutor.binaryMessenger

    // Register configurations once
    NativeBrandConfigApi.setUp(messenger, object : NativeBrandConfigApi {
        override fun getInitialBrandTheme(): BrandConfig {
            return BrandConfig(
                resellerId = "1c4e1a36-c94c-45bb-8e63-402091a2d093",
                resellerEmail = "zetexa@zetexa.com",
                resellerName = "ZetSIM",
                preferredCurrency = "USD",
                environment = BrandEnvironment.STG,
                theme = BrandThemeConfig(
                    colors = BrandColorConfig(
                        primary = "#0000FF",
                        secondary = "#00FF00",
                        tertiary = "#FF0000",
                        success = "#00FF00",
                        error = "#FF0000",
                        warning = "#FFA500",
                        background = "#FFFFFF",
                        secondaryBackground = "#F0F0F0"
                    ),
                    typography = BrandTypographyConfig(
                        headingFontFamily = "Roboto",
                        bodyFontFamily = "Roboto"
                    )
                )
            )
        }
    })

    HostToFlutterCheckoutApi.setUp(messenger, object : HostToFlutterCheckoutApi {
        override fun getNativeHostCheckoutContext(): NativeHostCheckoutContextMessage {
            return NativeHostCheckoutContextMessage(
                nativeHostUserSignedIn = false,
                nativePrefillProfile = NativeCheckoutPrefillProfile(
                    nativePrefillFirstName = "John",
                    nativePrefillLastName = "Doe",
                    nativePrefillEmail = "john.doe@example.com",
                    nativePrefillPhone = "+1234567890",
                    nativePrefillNationalityIso2 = "US"
                )
            )
        }
    })

    FlutterEngineCache.getInstance().put("zetsim_engine", flutterEngine)
}
```

### 2. The Custom SDK Activity
Create `ZetSdkActivity.kt` extending `FlutterFragmentActivity` and implementing `NativeNavigationHostApi`. It dynamically binds the dismiss listener on creation and cleans it up on destruction:

```kotlin
class ZetSdkActivity : FlutterFragmentActivity(), NativeNavigationHostApi {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val engine = FlutterEngineCache.getInstance().get("zetsim_engine")
        if (engine != null) {
            val messenger = engine.dartExecutor.binaryMessenger
            NativeNavigationHostApi.setUp(messenger, this)
        }
    }

    override fun dismissFlutter() {
        finish()
    }

    override fun onDestroy() {
        val engine = FlutterEngineCache.getInstance().get("zetsim_engine")
        if (engine != null) {
            NativeNavigationHostApi.setUp(engine.dartExecutor.binaryMessenger, null)
        }
        super.onDestroy()
    }
}
```

---

# Brand Configuration

The SDK requests branding information through:

```kotlin
NativeBrandConfigApi
```

### Method

```kotlin
getInitialBrandTheme()
```

### Returns

```kotlin
BrandConfig
```

### Supported Configuration

* Brand Name
* Theme Colors
* Typography
* Currency
* Environment
* Authentication Email

### Supported Environments

```text
DEV
QA
STG
PROD
```

---

# Checkout Context

The SDK requests user and checkout information through:

```kotlin
HostToFlutterCheckoutApi
```

### Method

```kotlin
getNativeHostCheckoutContext()
```

### Returns

```kotlin
NativeHostCheckoutContextMessage
```

### Supported Data

* User Sign-In Status
* Customer Profile
* Checkout Prefill Data
* Addresses
* Session Reference

---

# Navigation Events

Flutter can communicate navigation events back to the Android host.

### Interface

```kotlin
NativeNavigationHostApi
```

### Method

```kotlin
dismissFlutter()
```

### Example

```kotlin
override fun dismissFlutter() {
    finish()
}
```

This closes the SDK and returns the user to the host application.

---

# Launching the SDK

To launch the SDK using the pre-warmed cached engine, use the `CachedEngineIntentBuilder` to create the intent:

## Launch from an Activity

```kotlin
val intent = FlutterFragmentActivity
    .CachedEngineIntentBuilder(ZetSdkActivity::class.java, "zetsim_engine")
    .destroyEngineWithActivity(false) // Keep it running in the background for reuse
    .build(this)
startActivity(intent)
```

## Launch with Page Routing (Deep Linking)

If you wish to route the SDK directly to a specific sub-page:

```kotlin
val intent = FlutterFragmentActivity
    .CachedEngineIntentBuilder(ZetSdkActivity::class.java, "zetsim_engine")
    .destroyEngineWithActivity(false)
    .build(this)
    .apply {
        putExtra("destination", SdkNavigationDestination.BROWSE_PLANS.name)
    }
startActivity(intent)
```

### Available Destinations
- `CLAIM_FREE_SIM`: Route to the free SIM claim page.
- `BROWSE_PLANS`: Route to the plans browsing page.
- `MY_SIMS`: Route to the user's active SIMs page.
- `ORDER_HISTORY`: Route to purchase order history.
- `FAQS`: Route to help and FAQs.

---

# ProGuard / R8 Configuration

If code shrinking or obfuscation is enabled, add the following rules:

```proguard
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }
-dontwarn io.flutter.**
```

---

# Troubleshooting

## Theme Compatibility Issues

If you encounter theme-related crashes such as:

```text
You need to use a Theme.AppCompat theme (or descendant) with this activity
```

ensure your application or SDK host activity uses an AppCompat-based theme.

### Example Theme

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style
        name="Theme.SampleApp"
        parent="Theme.AppCompat.Light.NoActionBar" />

</resources>
```

### Apply the Theme

```xml
<application
    android:theme="@style/Theme.SampleApp"
    ...>

    <activity
        android:name=".ZetSdkActivity" />

</application>
```

After updating the theme, clean and rebuild the project before launching the SDK again.



## Flutter Imports Not Found

Verify:

* SDK files are copied correctly.
* Repository path is correct.
* Gradle sync completed successfully.

---

## SDK Does Not Launch

Verify:

* `ZetSdkActivity` is registered in the manifest.
* SDK dependency is added.
* All SDK artifacts are present in `app/libs/repo`.
* Required Pigeon APIs are implemented and registered.

---

## Pigeon Communication Issues

Verify:

* Generated Pigeon files are included in the project.
* All APIs are registered in `configureFlutterEngine()`.
* The activity extends `FlutterFragmentActivity`.

---

# Integration Checklist

Before releasing, verify:

* [ ] SDK repository copied to `app/libs/repo`
* [ ] Repository configuration added
* [ ] SDK dependency added
* [ ] Pigeon generated files directory added to `sourceSets` inside `build.gradle.kts`
* [ ] Gradle sync successful
* [ ] `ZetSdkActivity` registered in manifest
* [ ] Required permissions (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`) added
* [ ] Cleartext traffic (`android:usesCleartextTraffic="true"`) enabled in manifest
* [ ] Application theme inherits from an AppCompat/MaterialComponents parent theme
* [ ] ProGuard rules configured
* [ ] Pigeon APIs implemented and cached engine setup
* [ ] SDK launches successfully

---

# Quick Start

```kotlin
val intent = FlutterFragmentActivity
    .CachedEngineIntentBuilder(ZetSdkActivity::class.java, "zetsim_engine")
    .destroyEngineWithActivity(false)
    .build(this)
startActivity(intent)
```

Your application is now ready to launch the ZetSIM SDK.
