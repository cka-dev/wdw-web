# Wine Down Wednesday (WDW) Web Architecture and Features

This document provides a detailed overview of the architecture, features, and technical implementation of the `wdw-web` project.

## 1. Project Overview
`wdw-web` is a web application built using **Compose Multiplatform for Web (Kotlin/Wasm)**. It serves as the web presence for "Wine Down Wednesday," offering features like event management, membership details, podcast listings, and wine catalogs.

## 2. Architecture

### Technology Stack
- **Language**: Kotlin 2.1.0
- **UI Framework**: Compose Multiplatform (WasmJs target)
- **Dependency Injection**: Koin
- **Networking**: Ktor Client
- **Authentication**: Hybrid Model (Passkeys/WebAuthn + Server-Side Password Validation)
- **Backend**: Firebase Cloud Functions (Node.js)
- **State Management**: MVVM (Model-View-ViewModel) using Kotlin Coroutines and Flow

### Project Structure
The project follows a clean architecture approach, though currently heavily concentrated in the `wasmJsMain` source set.

- **`composeApp/src/wasmJsMain/kotlin`**:
    - **`composables/`**: UI components and screen layouts.
    - **`viewmodels/`**: Business logic and UI state management.
    - **`data/`**:
        - **`models/`**: Data Transfer Objects (DTOs) and domain models.
        - **`network/`**: Ktor client configuration and Remote Data Source.
        - **`repositories/`**: Main data access point (AppRepository).
    - **`di/`**: Koin module definitions.
    - **`FirebaseBridge.kt`**: Kotlin external declarations for JavaScript Firebase interop.
    - **`WebAuthn.kt`**: Kotlin wrapper for the WebAuthn JavaScript bridge.

### JS Interop & Security
Since the project runs on Wasm, it interacts with JavaScript libraries for Firebase and WebAuthn.

#### Sensitive Configuration
To prevent leaking project identifiers, some JavaScript files are excluded from version control. Templates are provided for development:
- **`firebase-bridge.js`**: Bridges Kotlin to Firebase JS SDK. Template: `firebase-bridge.js.example`.
- **`firebase-messaging-sw.js`**: Firebase Messaging Service Worker. Template: `firebase-messaging-sw.js.example`.
- **`webauthn-bridge.js`**: Utility functions for WebAuthn cryptography.

## 3. Key Features

### Authentication & Identity (Hybrid Model)
- **Unified Strategy**: All authentication (except Passkey verification) happens server-side via Cloud Functions.
- **Custom Token Pattern**: Upon successful validation (Password or Passkey), the backend returns a Firebase Custom Token. The client uses `signInWithCustomToken` to establish the Firebase session.
- **Passkeys (WebAuthn)**: Biometric/Hardware key login scoped to the production domain.
- **Password Support**: Standard email/password login, registration, and management (Change/Reset/Link) via Ktor calls.
- **Dynamic Profile UI**: The UI adapts based on user capabilities (e.g., "Add Passkey" only shows if missing).

### Firebase Cloud Messaging (Web Push)
- **Service Worker**: `firebase-messaging-sw.js` handles background notifications and notification display while the app is in the background.
- **Permission Flow**: The app requests notification permission upon successful login via the `FirebaseBridge`.
- **Token Registration**: FCM tokens are fetched and registered with the backend via `registerFcmInstanceId`, allowing targeted push notifications from the server.
- **Cleanup**: Tokens are automatically unregistered from the backend upon logout or when the ViewModel is cleared to ensure notification integrity.

### Content Modules
- **Event Management**: List view with RSVP capability for authenticated users, featuring strict admin-side validation and chronological descending default sorts.
- **Member Directory & Spotlight**: Directory of community members, highlighting a deterministic rotating "Member Spotlight" driven by a robust round-robin queue, automated birthday prioritization, and secure manual override architecture.
- **Podcasts & Wines**: Metadata-rich catalogs for media and wine inventory.

## 4. Technical Implementation Details

### Multi-Redundant API Requests
To ensure compatibility with various backend configurations, sensitive identifiers (like user email) are delivered via three redundant channels in a single request:
1.  **Request Body**: Standard JSON payload.
2.  **Query Parameter**: Appended to the URL.
3.  **Custom Header**: Sent via `x-user-email`.

### Responsive Design
Uses `Material3WindowSizeClass` to adapt layouts between mobile (Compact) and desktop screens.

### Navigation
Top-level navigation is managed via `androidx.navigation.compose.NavHost`, while sub-page switching within the main authenticated area is driven by an `AppBarState` enum for optimized performance.

## 5. Deployment Info
- **Hosting**: Firebase Hosting.
- **Build Command**: `./gradlew :composeApp:wasmJsBrowserDistribution`.
- **Headers**: `firebase.json` is configured to serve `.wasm` files with `application/wasm` MIME type and specific caching headers for performance.
