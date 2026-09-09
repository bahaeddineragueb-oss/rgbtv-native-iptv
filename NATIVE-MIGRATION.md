# RGBTv Native Migration

## Current implementation

The project has been moved from a WebView shell to a native Android foundation using **Kotlin**, **Jetpack Compose Material 3**, **Media3/ExoPlayer**, and a ViewModel-driven state model. The visual direction is **Neon Cinema**: deep midnight surfaces, pink-to-violet gradients, cyan accents, large rounded cards, compact TV navigation, and high-contrast typography.

The native app now contains a real Media3 player surface, channel cards, category navigation, search, favorites, a native M3U parser, HTTP/HTTPS remote playlist loading, and encrypted credential storage infrastructure. HTTP cleartext traffic is enabled because many IPTV providers and Stalker portals expose only HTTP endpoints.

## Architecture

| Layer | Responsibility |
|---|---|
| Compose UI | Modern TV-oriented layout, navigation, search, cards, empty/player states |
| ViewModel | Screen state, filtering, selection, favorites, playlist loading |
| Data layer | M3U parsing, HTTP/HTTPS loading, encrypted credentials |
| Media3 | Native HLS playback, buffering, full-screen player controls |
| Future repositories | Xtream and Stalker API adapters using native networking |

## Product direction

This is intentionally not styled like Android 5. It uses a dark cinematic canvas, restrained gradients, rounded 16–22 dp surfaces, strong hierarchy, and a wide-screen layout that scales to TV and landscape tablets. The interface keeps decoration secondary to content: channels, categories, playback status, and search remain the primary visual elements.

## Remaining production work

The current migration establishes the native shell and player but still needs full production adapters for Xtream and Stalker, Room persistence for profiles/favorites/history, a real onboarding flow, EPG mapping, MediaSession/foreground playback, and device testing on Android TV remotes. These should be implemented before publishing. HTTP should be treated as a compatibility mode: it is vulnerable to interception, so HTTPS should be preferred whenever a provider supports it.
