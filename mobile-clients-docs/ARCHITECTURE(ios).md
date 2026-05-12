# Wine Down Wednesday (WDW) iOS Architecture and Features

This document provides a detailed overview of the architecture, features, and technical implementation of the `wdw-ios` native mobile application.

## 1. Project Overview
`wdw-ios` is a native iOS application built using **SwiftUI** targeting iOS 17.6+. It serves as the iOS mobile presence for "Wine Down Wednesday," offering features like event management, membership details, podcast listings, wine catalogs, and real-time community chat. It shares backend infrastructure and design paradigms with the `wdw-android` and `wdw-web` clients.

## 2. Architecture

### Technology Stack
- **Language**: Swift 5.9+
- **UI Framework**: SwiftUI with iOS 17+ APIs
- **Dependency Injection**: `@EnvironmentObject` / `@StateObject` pattern
- **Networking**: Custom `APIClient` (URLSession-based) for REST API requests, Firebase SDKs for Auth/Cloud services
- **Authentication**: Firebase Auth with custom token flow
- **Messaging**: Stream Chat iOS SDK (SwiftUI components, Push support)
- **Image Loading**: SwiftUI `AsyncImage`
- **State Management**: MVVM using `@Observable` / `ObservableObject` with `@Published` properties
- **Backend**: Firebase Cloud Functions (Node.js) & Firestore

### Project Structure
The project uses a flat SwiftUI file structure under `ios/`:

- **`WDWApp.swift`**: App entry point (`@main`), Firebase configuration, `AppDelegate` for push notifications (FCM + APNs)
- **`ContentView.swift`**: Root navigation controller with custom tab bar, hamburger menu, search state, and `NavigationStack`
- **`AuthManager.swift`**: `ObservableObject` managing authentication state, provided as `@EnvironmentObject`
- **`APIClient.swift`**: Centralized networking layer handling all REST API calls to the backend
- **`Theme.swift`**: Brand color palette (`darkNavy`, `accentOrange`, `wineRed`, `cardBackground`, `surfaceLight`) and `WDWCardStyle` modifier (`.wdwCard()`)
- **Feature Views**:
    - `HomeView.swift` — About section carousel + content cards
    - `EventsView.swift` — Event listing, detail sheets, RSVP form
    - `MembersView.swift` — Member grid directory with detail sheets
    - `WinesView.swift` — Wine catalog (favorites + tried)
    - `PodcastsView.swift` — Episode listing with detail sheets
    - `BlogViews.swift` — Blog content rendering
    - `ChatView.swift` — Real-time messaging via Stream Chat SDK
    - `LoginView.swift` — Authentication flows
    - `ProfileView.swift` — User profile management, settings, account deletion
      - **Save Serialization**: `UserProfileDTO.encode(to:)` uses a custom encoder. User-editable fields (`name`, `email`, `phone`, `aboutMe`, `birthDate`) are **always encoded** (even as `null`) to signal explicit clears to the backend. Server-managed fields (`profileImageBitmap`, `profileImageUrl`, `isVerified`, etc.) use `encodeIfPresent` — omission means "no change."
      - **Post-Save Merge**: `ProfileViewModel.mergeProfile()` uses user-intent-preserving logic: user-editable fields always use the **base** (what we sent), while server-only fields (`isVerified`, `isMember`, credentials) prefer the server response. This prevents the server from overriding intentional field clears.
      - **Profile Picture Pipeline**: Images are downscaled (1024px pre-crop, 500px post-crop) → JPEG-compressed once (0.7 quality) → base64-encoded on a background thread (`Task.detached`). A `cachedBitmapImage` (`@State UIImage?`) avoids re-decoding the base64 string in the view body. After save, `profileImageBitmap` is cleared from the in-memory profile once a `profileImageUrl` is available.
- **Utilities**:
    - `HapticUtils.swift` — Haptic feedback engine
    - `AnimationUtils.swift` — Reusable animation primitives
    - `EventDateParser.swift` — Date parsing utilities
    - `KoinHelper.swift` — Kotlin Multiplatform interop helper
    - `FlowCollector.swift` — Kotlin Flow collection bridge

## 3. Key Features

### Authentication
- **Firebase Auth**: Login via email/password with Firebase Custom Token validation through the backend.
- **Session Management**: `AuthManager` tracks authenticated state as an `@EnvironmentObject` available throughout the view hierarchy.

### Content Modules
- **Event Management**: Scrollable event list with RSVP capability, grouped by year with expandable sections.
- **Wine Catalog**: Dual-section layout (Favorites + Tried) with rich detail sheets. Includes community reviews, aggregate star rating, submit/edit/delete/flag flows, expandable text, pagination, and toast feedback. New `WineReviewsViewModel` manages all review state.
- **Member Directory**: Grid-based member cards with detail sheets showing interests and favorite wines.
- **Blog Engine**: Structured content rendering from backend payloads.
- **Podcasts**: Episode listing with guest profiles and detail sheets.

### Messaging (Real-time Chat)
- **Stream Chat SDK**: Integrated for real-time messaging mirroring the Android and web clients.
- **Push Notifications**: APNs token forwarded to both Firebase and Stream for offline message delivery.

### Push Notifications
- **Firebase Cloud Messaging (FCM)**: `AppDelegate` handles APNs token registration and FCM token refresh.
- **Foreground Display**: Notifications shown as banners with badge and sound even when app is active.

## 4. Technical Implementation Details

### AI Architecture (Vino)
The platform follows a hybrid AI strategy:
- **Tier 3 (Server-Side)**: `vino-bot` logic and conversational intelligence live entirely in the Firebase Cloud Functions layer (`chatWithBot`). 
- **Client Integration**: 
  - The client intercepts `@Vino` and `@bot` mentions in the Stream SDK composer (`ChatView.swift` / `sendMessage()`) and triggers an authenticated HTTP POST request via `APIClient`.
  - Responses can contain inline cards (Event/Wine) and actions (RSVP) which are buffered in `ChatViewModel` and flushed atomically into a `messageId -> Card/Action` map when the `vino-bot` message arrives in the Stream delegate.
  - Custom SwiftUI message rendering loops (`ForEach` over `ListChange<ChatMessage>`) insert `VinoCardsRow` and `VinoRsvpConfirmCard` beneath bot message bubbles.

### Navigation & Layout
- **Custom Tab Bar**: `ContentView` renders a hand-built tab bar with 4 primary tabs (Home, Events, Podcasts, Chat) using `matchedGeometryEffect` for an animated underline indicator that glides between tabs.
- **Hamburger Menu**: Overflow screens (Members, Wines, Blog, Profile) accessible via a side menu sheet with `PressableButtonStyle` for touch feedback.
- **Dark Mode**: App enforces `.preferredColorScheme(.dark)` globally.

### Haptic Feedback
A shared haptic utility layer (`HapticUtils.swift`) provides tactile feedback across all interactive surfaces:

- **`HapticType` Enum**: Semantic types (`tick`, `light`, `medium`, `heavy`, `error`, `success`) mapped to `UIImpactFeedbackGenerator` styles and `UINotificationFeedbackGenerator` types.
- **`HapticIntensity`**: User-configurable intensity (off / light / normal / strong) stored in `UserDefaults`, provided app-wide.
- **`fireHaptic(_:)`**: Global function for imperative haptic triggers.
- **`ShakeEffect`**: `GeometryEffect`-based shake animation + haptic for form validation errors.

| Haptic Point | Type | Views |
|---|---|---|
| Card / list item tap | `tick` | Home, Members, Wine, Podcasts, Events, Blog |
| Tab switch | `tick` | ContentView |
| RSVP success | `success` | EventsView |
| RSVP error | `error` | EventsView |
| Form validation shake | `error` | EventsView (RSVP form) |

### Animations & Motion
A shared animation utility layer (`AnimationUtils.swift`) provides reusable motion primitives:

#### Core Primitives
| Primitive | Description |
|---|---|
| `PressableButtonStyle` | `ButtonStyle` that scales to 0.96 on press. Applied to all card views app-wide. |
| `.staggerEntrance(index:)` | Per-item opacity + offset animation with staggered delay. Used on HomeView cards. |
| `.scrollReveal()` | Fade + slide-up triggered by `.onAppear` in scroll views. |
| `ShimmerModifier` / `ShimmerCard` | Animated gradient overlay for loading skeleton states. Replaces `ProgressView` on Podcasts, Wines, Home. |
| `.pressable()` | Non-Button view scale feedback modifier. |

#### Native iOS Enhancements (iOS 17+)
| Feature | API | Usage |
|---|---|---|
| Tab icon bounce | `.symbolEffect(.bounce)` | Tab bar SF Symbols bounce on selection |
| Numeric text roll | `.contentTransition(.numericText())` | RSVP guest count digits animate on +/- |
| Scroll parallax | `.scrollTransition { content, phase in }` | Wine and Podcast cards fade + scale at viewport edges |
| Animated tab indicator | `matchedGeometryEffect` | Orange underline glides between tab bar items |
| Carousel dots | `animateDpAsState` + `spring` | Dot sizes animate on page change |

## 5. Deployment Info
- **Platform**: iOS 17.6+
- **Build System**: Xcode (Swift Package Manager for dependencies)
- **Architecture**: SwiftUI App lifecycle (`@main struct WDWApp: App`)
- **Push**: Dual provider — Firebase Cloud Messaging + Apple Push Notification service (APNs)
