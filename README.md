# RGBTv Native IPTV

Modern native Android IPTV player built with **Kotlin**, **Jetpack Compose Material 3**, and **Media3/ExoPlayer**.

The visual identity is **Neon Cinema**: a dark cinematic canvas, pink-to-violet gradients, cyan accents, rounded cards, wide-screen TV navigation, and high-contrast typography. This is not a WebView application.

## Features

- Native HLS playback with Media3/ExoPlayer.
- Xtream Codes account login.
- Stalker Portal login with MAC address and handshake.
- M3U playlist URL loading.
- HTTP and HTTPS source support for IPTV providers that expose legacy HTTP endpoints.
- Fast live search while typing.
- Automatic category extraction from M3U `group-title` and Xtream live categories.
- Favorites and channel cards.
- TV-oriented landscape interface.
- Secure credential storage infrastructure using AndroidX Security Crypto.

## Requirements

- Android Studio Ladybug or newer.
- JDK 17 or newer.
- Android SDK 35.
- Minimum Android version: Android 8.0 / API 26.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If the Gradle wrapper is not present in a fresh checkout, open the project in Android Studio or install Gradle 8.10.2 and run the Gradle task from the project root.

## Adding a source

At startup, the app opens **Add IPTV source**. Choose one of the following tabs:

### Xtream

Enter the provider server URL, username, and password. Both `http://` and `https://` are accepted.

### Stalker

Enter the portal URL and MAC address, for example:

```text
00:1A:79:XX:XX:XX
```

### M3U

Enter an HTTP or HTTPS playlist URL. The playlist parser reads channel names, logos, groups, and stream URLs.

## HTTP compatibility warning

Cleartext HTTP is intentionally enabled because many IPTV providers and Stalker portals do not provide HTTPS. HTTP is vulnerable to interception. Use HTTPS whenever the provider supports it, and avoid entering credentials on untrusted networks.

## Project structure

```text
app/src/main/java/com/rgbtv/app/
├── MainActivity.kt                 # Compose UI, onboarding, account dialog, player surface
├── data/M3uParser.kt               # Native M3U parser
├── data/SecureCredentials.kt       # Encrypted credentials infrastructure
├── model/Models.kt                 # Channels, profiles, and player state
└── ui/
    ├── MainViewModel.kt            # M3U, Xtream, Stalker, search state, favorites
    └── Theme.kt                    # Neon Cinema Material 3 theme
```

## Notes

This repository contains a working native foundation and debug APK. Before production distribution, add Room persistence for profiles and history, MediaSession/foreground playback, EPG integration, stronger provider-specific error handling, Android TV remote testing, and a release signing key managed outside the repository.

Do not commit provider usernames, passwords, MAC addresses, tokens, or private playlists.
