# Wine Down Wednesday (WDW) Android Architecture and Features

This document provides a detailed overview of the architecture, features, and technical implementation of the `wdw-android` native mobile application.

## 1. Project Overview
`wdw-android` is a native Android application built using **Jetpack Compose** and modern Android development practices. It serves as the mobile presence for "Wine Down Wednesday," offering features like event management, membership details, podcast listings, wine catalogs, and real-time community chat. It shares backend infrastructure and design paradigms with the `wdw-web` client.

## 2. Architecture

### Technology Stack
- **Language**: Kotlin (JDK 11 / Kotlin 2.0+)
- **UI Framework**: Jetpack Compose (Material 3) with Adaptive Layouts (`ListDetailPaneScaffold`, `NavigationSuiteScaffold`)
- **Dependency Injection**: Dagger Hilt
- **Networking**: Ktor Client for API requests, Firebase SDKs for Auth/Cloud services
- **Local Database**: Room Database (Offline-first architecture)
- **Preferences/Storage**: Jetpack DataStore
- **Authentication**: Hybrid Model (Android Credential Manager for Passkeys/WebAuthn + Server-Side Password Validation via Firebase Custom Tokens)
- **Messaging**: Stream Chat Android SDK (Compose UI components, Offline support, Firebase Push Provider)
- **Image Loading**: Coil
- **Video Playback**: Android YouTube Player API
- **State Management**: MVVM (Model-View-ViewModel) using Kotlin Coroutines and `StateFlow`
- **Backend**: Firebase Cloud Functions (Node.js) & Firestore

### Project Structure
The project follows a standard Clean Architecture approach tailored for Android:

- **`app/src/main/java/net/winedownwednesday/android/`**
    - **`ui/`**: 
        - **`composables/`**: UI components and screen layouts (e.g., `Home`, `EventsScreens`, `MessagingScreens`, `ProfileScreens`, `SettingsScreen`).
        - **`viewmodels/`**: Business logic and UI state management using Hilt ViewModels.
        - **`theme/`**: Global Material 3 typography, colors, and theming.
    - **`data/`**:
        - **`models/`**: Data Transfer Objects (DTOs) and domain entities.
        - **`db/`**: Room Database configuration and DAOs (`EventDao`, `WineDao`, `MemberDao`, etc.).
        - **`network/`**: Ktor client configuration (`KtorClientInstance`) and API definitions (`ServerFunctions`).
        - **`repositories/`**: Main data access point (`AppRepository`) abstracting network and local database sources.
        - **`stream/`**: Encapsulated logic for Stream Chat integration (`ChatManager`) and moderation data models (`ModerationModels`).
    - **`di/`**: Dagger Hilt module definitions (`AppModule`).
    - **`FcmService.kt`**: Firebase Messaging Service for handling remote push payloads.
    - **`MainActivity.kt`**: Single-activity entry point hosting the Compose Navigation graph.

## 3. Key Features

### Authentication & Identity (Hybrid Strategy)
- **Passkeys (Credential Manager)**: Seamless biometric/hardware key login using the modern Android Credential Manager API.
- **Firebase Custom Tokens**: Validated credentials (Passkeys or standard Email/Password) hit the backend, which issues a Firebase Custom Token. The app uses this token to authenticate `Firebase.auth`.
- **Profile Synchronization**: The global user profile (`UserProfileData`) is fetched post-login and persisted across the session. Its `profilePictureUrl` is distributed globally to navigation TopBars across all screens.
  - **Save Semantics**: User-editable fields (`name`, `email`, `phone`, `aboutMe`, `birthDate`) have no kotlinx.serialization defaults, so they are **always serialized** (including as `null`) to signal explicit clears to the backend. Fields with defaults (`profileImageBitmap`, `profileImageUrl`, etc.) are omitted when equal to default, meaning "no change."
  - **Post-Save State**: After a successful save, the local `_profileData` StateFlow is immediately updated with the user's edited values (via `setProfileData()`). The server is **not** re-fetched immediately to avoid stale-data races; the `ON_RESUME` lifecycle observer handles eventual consistency.

### Offline-First Data Architecture
- **Room Database**: Core entities (Events, Wines, Members, Podcasts/Episodes) are aggressively cached using Room.
- **Repository Pattern**: `AppRepository` serves as the single source of truth, attempting to serve UI from local cache while silently refreshing data from the network in the background. 

### Content Modules
- **Event Management**: List view with RSVP capability. Events are cached locally and synced. 
- **Wine Catalog**: Browseable catalog of favorite and tried wines with local toggle capabilities synced back to the server. Wine cards display server-computed aggregate ratings (`averageRating`, `reviewCount`) inline.
  - **Wine Reviews & Ratings**: Full review lifecycle (submit, edit, delete, flag) backed by Cloud Function endpoints. `WineReviewViewModel` uses a per-wine in-memory **SWR (Stale-While-Revalidate) cache** (`Map<Long, WineCacheEntry>` with 5-minute TTL) to eliminate redundant network calls when switching between wines. Cache is invalidated on mutations (submit/delete). Pagination via cursor-based "Load More". Moderation banners display on flagged reviews with admin notes. The ViewModel is created outside `Dialog` boundaries and passed through to `WineReviewsSection` to avoid Dialog-scoped ViewModel re-instantiation.
- **Member Directory**: Directory of community members with profile views.
- **Blog Engine**: A rich-text publishing system that parses structured JSON `ContentBlock` payloads from the backend directly into native Compose UI elements (Text, AsyncImage, Markdown) rather than relying on WebViews, ensuring visual fidelity and performance.
- **Podcasts**: Built-in YouTube player integrations for viewing podcast episodes natively within a popup or detail pane.

### Messaging (Real-time Chat)
- **Stream Chat SDK**: Integrated via `stream-chat-android-compose` to provide robust, real-time messaging mirroring the web client.
- **Adaptive Two-Pane Layout**: Utilizes Compose Adaptive's `ListDetailPaneScaffold`. On tablets/foldables, the channel list and active chat render side-by-side. The active chat pane is distinctively styled as an elevated Material 3 `Card`.
- **Channel Scoped ViewModels**: Custom `ViewModelStoreOwner` logic (`ChannelScopedContent`) ensures that Stream's internal ViewModels are freshly instantiated whenever a new channel is selected, preventing state leakage between chats.
- **1:1 Direct Messages**: Includes a custom DM creation flow (`DmUserSearchDialog`) that queries the Stream backend for other users to initiate private chats. Clickable top-bar avatars dynamically open the peer's profile bottom sheet.
- **Typing Indicators**: Real-time typing status displayed natively at the bottom of the message list.
- **Native Theming**: The Stream Compose UI components are customized to inherit the app's global dark/light `MaterialTheme.colorScheme` for a seamless visual experience.
- **Lifecycle Management**: Stream connections are tied to the user's authentication lifecycle. `LoginViewModel` gracefully handles disconnecting and deregistering FCM devices upon logout.

### Moderation & Safety
- **Blocking / Unblocking**: Users can block or unblock peers from their profile definitions. Blocked channels are automatically sequestered into a separate collapsed section in the Messaging UI.
- **User / Message Flagging**: Users can flag/report abusive peers or explicit messages directly to the Ktor backend using a dedicated `ReportUserDialog` mirroring the web client.
- **Rate-Limit & Error Handling**: A structured `ReportResult` sealed class propagates 409 (Already Reported) and 429 (Rate Limited) errors gracefully via Toasts.
- **Account Deletion**: Full self-service account deletion capabilities utilizing a "Danger Zone" module in a dedicated Settings screen requiring phrase confirmation.

### Settings (Dedicated Screen)
- **`SettingsScreen.kt`**: A dedicated settings page accessible from Profile, containing four sections: Security, Privacy & Moderation, Preferences, and Danger Zone.
- **Adaptive Layouts**: Uses `WindowWidthSizeClass`:
  - **Compact (Phones)**: Single scrollable column of section cards.
  - **Expanded (Tablets/Foldables)**: Two-pane layout with a sidebar category selector and detail content pane.
- **Security Section**: Credential management (password link/change, passkey registration) with loading-aware dialogs (fields disabled, dismiss blocked during async operations).
- **Privacy & Moderation Section**: Expandable blocked users list. Resolves blocked user IDs to full profiles (name, avatar) via Stream Chat SDK's `queryUsers` API. Unblock flow includes confirmation alert, per-row spinner, and haptic feedback.
- **Preferences Section**: Haptic feedback intensity control (Off/Light/Normal/Strong) backed by DataStore.
- **Danger Zone**: Account deletion with phrase confirmation, styled with a danger-red card.
- **Loading State Patterns**:
  - **Pattern A (Full-Screen Scrim)**: Semi-transparent overlay with branded orange spinner on `MessagingScreen` during block/unblock operations via `moderationLoading` StateFlow.
  - **Pattern B (Inline Spinner)**: Per-dialog/button spinners on `ReportUserDialog`, RSVP submit, password dialogs. Fields and dismiss are blocked during async operations.

### Push Notifications
- **Firebase Cloud Messaging (FCM)**: Handles both general app notifications and Stream Chat offline alerts.
- **Routing Logic**: `FcmService.kt` intercepts incoming payloads. It uses `ChatClient.handlePushMessage` to determine if a payload belongs to Stream Chat. If Stream doesn't consume it, the app processes it as a standard notification.
- **SingleTop Launch Mode**: `AndroidManifest.xml` enforces `singleTop` to ensure tapping notifications smoothly routes the user into the existing Compose navigation graph (e.g., deep-linking directly into a specific chat channel).

## 4. Technical Implementation Details

### Responsive Design & Adaptive Layouts
The app is built to scale gracefully from phones to foldables to tablets:
- **`currentWindowAdaptiveInfo()`**: Window size classes dynamically dictate the navigation paradigm.
- **Compact (Phones)**: Employs a Bottom Navigation Bar for primary tabs and a hidden Navigation Drawer (Hamburger menu) for overflow tabs (Members, Blog). Uses full-screen transitions.
- **Medium/Expanded (Tablets/Foldables)**: Employs a side Navigation Rail and side-by-side List/Detail panes for content (Events, Messaging, Podcasts, Wines).

### AI Architecture (Vino)
The platform follows a hybrid AI strategy:
- **Tier 3 (Server-Side)**: `vino-bot` logic and conversational intelligence live entirely in the Firebase Cloud Functions layer (`chatWithBot`). 
- **Client Integration**: 
  - The client intercepts `@Vino` and `@bot` mentions in the Stream SDK composer and triggers an authenticated Ktor POST request.
  - Responses can contain inline cards (Event/Wine) and actions (RSVP) which are buffered in the `MessagingViewModel` and flushed atomically into a `messageId -> Card/Action` map when the Stream `message.new` event arrives.
  - UI components hook into the Stream SDK `itemContent` slot to render `VinoCardsRow` and `VinoRsvpConfirmCard` beneath standard message bubbles.

### Background Tasks & Coroutines
- Extensive use of Kotlin Coroutines (`viewModelScope`, `rememberCoroutineScope`) and `StateFlow` for unidirectional data flow. 
- UI state is reactively observed via `collectAsState` / `collectAsStateWithLifecycle`.

### Haptic Feedback
A shared haptic utility layer (`ui/utils/HapticUtils.kt`) provides platform-appropriate tactile feedback across all interactive surfaces:

- **`HapticType` Enum**: Semantic types (`TICK`, `LIGHT`, `MEDIUM`, `HEAVY`, `ERROR`, `SUCCESS`) mapped to Android's `HapticFeedbackConstants` (`CLOCK_TICK`, `CONTEXT_CLICK`, `CONFIRM`, `REJECT`) with fallbacks to `VibrationEffect` on older API levels.
- **`HapticIntensity`**: User-configurable intensity (OFF / LIGHT / NORMAL / STRONG) stored in DataStore via `HAPTIC_INTENSITY_KEY`, provided app-wide through `LocalHapticIntensity` CompositionLocal.
- **`Modifier.hapticClickable()`**: Drop-in replacement for `.clickable()` that fires haptic feedback on tap while preserving Material3 ripple indication.
- **`rememberHapticFeedback()`**: Composable function returning a `(HapticType) -> Unit` lambda for imperative use in callbacks (e.g., RSVP success/error).

| Haptic Point | Type | Screens |
|---|---|---|
| Card / list item tap | `TICK` | About, Members, Wine, Podcasts, Events, Blog, Messaging |
| Navigation tab / drawer tap | `TICK` | Home |
| RSVP button tap | `MEDIUM` | Events |
| RSVP success | `SUCCESS` | Events |
| RSVP error | `ERROR` | Events |

### Animations & Motion
A shared animation utility layer (`ui/utils/AnimationUtils.kt`) provides reusable motion primitives applied across all screens:

#### Core Primitives
| Primitive | Description |
|---|---|
| `Modifier.pressScale()` | Touch-down scale-to-0.96 with spring bounce-back + haptic tick feedback. Applied to all card composables app-wide. |
| `Modifier.staggeredEntrance(index)` | Per-item fade + slide-from-right with staggered delay. Used on `AboutList` items. |
| `Modifier.scrollReveal()` | Fade + slide-up triggered on first appearance in `LazyColumn`/`LazyRow`. |
| `shimmerBrush()` / `ShimmerCard` | Animated diagonal gradient skeleton for loading states. |

#### Page Transitions
`Home.kt` uses `AnimatedContent` with `slideInHorizontally + fadeIn` / `slideOutHorizontally + fadeOut` for page-level tab switching, matching the web client's slide transitions.

#### Dialog Entrances
All 5 detail popup dialogs use `AnimatedVisibility` with `scaleIn(0.85f, spring(LowBouncy)) + fadeIn(tween(200))`:
- `AboutDetailPopup`, `MemberDetailPopup`, `EventDetailPopup`, `WineDetailPopup`, `EpisodeVideoPopup`

#### Theming
- **Orange Ripple**: `WdwTheme` provides `LocalRippleConfiguration` with brand orange `#FF7F33`, applied globally to all interactive surfaces.
- **Carousel Dots**: `DotsIndicator` uses `animateDpAsState(spring)` for smooth dot size transitions on page change.

## 5. Deployment Info
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) using Version Catalogs (`libs.versions.toml`).
- **Target SDK**: Android 35 (VanillaIceCream)
- **Min SDK**: Android 26
- **Architecture**: Enforces Java 11/Kotlin JVM 11 compatibility.
