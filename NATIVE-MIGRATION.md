# RGBTv Native Migration

## Current implementation

The project has been moved from a WebView shell to a native Android foundation using **Kotlin**,
**Jetpack Compose Material 3**, **Media3/ExoPlayer**, and a ViewModel-driven state model. The visual
direction is **Neon Cinema**: deep midnight surfaces, pink-to-violet gradients, cyan accents, large
rounded cards, compact TV navigation, and high-contrast typography.

The native app contains a real Media3 player surface with themed Compose controls, channel cards,
category navigation, search, favorites, a native M3U parser, HTTP/HTTPS remote playlist loading, and
localized English/Arabic (RTL) resources. HTTP cleartext traffic is enabled because many IPTV
providers and Stalker portals expose only HTTP endpoints.

## Architecture

| Layer | Responsibility |
|---|---|
| Compose UI | Adaptive layout (rail on tablets/TV, chips + grid on phones), D-pad focus, localized strings, loading/empty/error states |
| ViewModel | Screen state, filtering, selection, favorites, playlist loading, `UiMessage` errors backed by string resources |
| Data layer | M3U parsing, Xtream/Stalker HTTP adapters, encrypted credentials infrastructure |
| Media3 | Native HLS playback, buffering, pause/resume with the lifecycle, error surfacing |
| Future repositories | Room persistence for profiles, favorites and history |

## Product direction

This is intentionally not styled like Android 5. It uses a dark cinematic canvas, restrained
gradients, rounded 12–28 dp surfaces, strong hierarchy, and a layout that scales from a phone in
portrait to a TV at ten feet. Decoration stays secondary to content: channels, categories, playback
status, and search remain the primary visual elements.

Two pinks are used on purpose: a vivid pink (`#FF3D71`) for text and icons on the dark canvas
(5.8:1 contrast) and a deeper pink (`#D81B60`) for filled surfaces that carry white labels (4.95:1),
because white on the vivid pink would only reach 3.41:1.

## Remaining production work

- Room persistence for profiles, favorites and history, plus wiring `SecureCredentials` to it.
- Full production adapters and error taxonomy for Xtream and Stalker (pagination, retries, EPG mapping).
- MediaSession, foreground playback and Picture-in-Picture (the manifest flag is already set).
- Device testing on real Android TV remotes and low-end phones.
- Release signing key managed outside the repository.
- HTTP should be treated as a compatibility mode: it is vulnerable to interception, so HTTPS should
  be preferred whenever a provider supports it.
