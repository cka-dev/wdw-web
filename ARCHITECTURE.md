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
- **Messaging**: Stream Chat SDK (with custom Cloud Function token generation)
- **Emoji Rendering**: Emoji.kt (Noto vector images for Wasm, where system emoji fonts are unavailable)
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
- **`StreamBridge.kt`**: Kotlin external declarations for Stream Chat JS SDK.

### JS Interop & Security
Since the project runs on Wasm, it interacts with JavaScript libraries for Firebase and WebAuthn.

#### Sensitive Configuration
To prevent leaking project identifiers, some JavaScript files are excluded from version control. Templates are provided for development:
- **`firebase-bridge.js`**: Bridges Kotlin to Firebase JS SDK. Template: `firebase-bridge.js.example`.
- **`firebase-messaging-sw.js`**: Firebase Messaging Service Worker. Template: `firebase-messaging-sw.js.example`.
- **`webauthn-bridge.js`**: Utility functions for WebAuthn cryptography. Template: `webauthn-bridge.js.example`.
- **`stream-bridge.js`**: Bridges Kotlin to Stream JS SDK. Template: `stream-bridge.js.example`.

## 3. Key Features

### Authentication & Identity (Unified Strategy)
- **Shared Backend Logic**: All authentication and credential management (except Passkey verification) happens server-side via Cloud Functions. Both the main web client and the Admin Dashboard share the same endpoints for `changePassword` and `linkPasswordToAccount`.
- **Custom Token Pattern**: Upon successful validation (Password or Passkey), the backend returns a Firebase Custom Token. The client uses `signInWithCustomToken` to establish the Firebase session.
- **Passkeys (WebAuthn)**: Biometric/Hardware key login scoped to the production domain. **Whitelisting Requirement**: For passkeys to function on the Admin Dashboard, the origin `https://admin.winedownwednesday.net` must be explicitly whitelisted in the backend's `expectedOrigins`.
- **Universal Credential Management**: Both Web and Admin clients offer a dynamic "Settings/Profile" UI that adapts based on user capabilities (e.g., "Add Passkey" only shows if missing; password change only shows if a password exists).
- **Password Support**: Standard email/password login, registration, and management via Ktor/React API calls.

### Firebase Cloud Messaging (Web Push)
- **Service Worker**: `firebase-messaging-sw.js` handles background notifications when the app tab is closed.
- **Foreground Notifications**: `onMessage` handler in `firebase-bridge.js` displays browser notifications with a two-tone audio chime when the tab is open.
- **Permission Flow**: The app requests notification permission upon successful login via the `FirebaseBridge`.
- **Token Registration**: FCM tokens are fetched and registered with both the backend (for admin/event notifications) and Stream Chat (for chat push notifications via `addDevice`).
- **Stream Push Integration**: Stream uses a Firebase push provider named `wdw` with service account credentials to send chat push notifications via FCM when users are offline.
- **Cleanup**: Tokens are automatically unregistered from the backend upon logout.

### Content Modules
- **Event Management**: List view with RSVP capability for authenticated users, featuring strict admin-side validation and chronological descending default sorts.
- **Member Directory & Spotlight**: Directory of community members, highlighting a deterministic rotating "Member Spotlight" driven by a robust round-robin queue, automated birthday prioritization, and secure manual override architecture.
- **Blog Engine**: A rich-text publishing system where content is served from Firebase as structured JSON `ContentBlock` arrays, rather than HTML or MD files. This permitting flawless native UI rendering (without WebViews) optimized for Compose reading modes across devices.
    - **Multi-Client Support Requirement**: While fully implemented on Web/Admin, the **Android client must be updated** to consume these structured payloads natively to maintain parity. See [android_blog_implementation_guide.md] for details.

### Messaging (Real-time Chat)
- **Stream Chat Integration**: Real-time communication for members via Stream Chat SDK.
- **Token Infrastructure**: Secure authentication using a dedicated `generateStreamToken` Cloud Function that verifies Firebase ID tokens.
- **Tiered Channel Access**: The `generateStreamToken` function looks up the user's `memberType` from the `members` Firestore collection and assigns channels accordingly:
  - **Community Lounge** (`wdw-community`): All authenticated users (GUEST, MEMBER, LEADER).
  - **Members Circle** (`wdw-members`): MEMBER and LEADER only.
  - **Leaders Room** (`wdw-leaders`): LEADER only.
- **Admin Role Syncing**: Firebase `isAdmin` flag from the `users` Firestore collection is synced to Stream's role system. Admin users receive the Stream `admin` role, granting moderation powers (ban, remove members, delete messages).
- **JS Bridge (Web)**: Leverages the established JS interop pattern to maintain a lightweight Wasm bundle while utilizing the full Stream Browser SDK.
- **Inline Replies**: Users can drag/swipe a message right (or tap Reply) to reply to specific messages; the reply bubble shows a quoted preview of the parent message with an accent border. A stylized, elevated reply banner appears above the input when composing.
- **Emoji Reactions**: Tap-accessible emoji picker (👍❤️😂🔥🎉) on each message. WhatsApp-style reaction chips with counts are anchored overlapping the bottom edge of the message bubbles. Reactions use a 500ms consistency delay before refreshing to ensure Stream's backend has processed the update.
- **Expanded Emoji Picker**: Full emoji suite with 270+ emojis across 7 categories (Smileys, Gestures, Animals, Food, Activities, Hearts, Symbols) with category search.
- **Message Editing**: Users can edit their own messages via the ✏️ button. Input switches to edit mode with pre-filled text, a blue "Editing message" banner, and a ✓ Save action. Uses Stream's `updateMessage()` API.
- **Scroll-to-Bottom FAB**: A floating circular button with ↓ arrow and orange unread badge appears when scrolled up. Smart auto-scroll only scrolls on new messages when already at the bottom; otherwise increments the unread counter.
- **Date Separators**: Centered pill-shaped date labels ("Today", "Yesterday", or "Mar 12, 2026") appear between messages from different dates.
- **Online Presence Indicators**: Green dots on message avatars and DM channel list avatars show when users are currently online. Uses Stream's `presence: true` subscription.
- **Thread View**: Clicking the reply count on a message opens a right-side panel showing the parent message preview and all threaded replies. Users can send new replies within the panel. Uses Stream's `getReplies()` and `sendMessage()` with `parent_id`.
- **File Attachments**: Users can attach and send any file type (PDF, docs, spreadsheets, etc.) via the file picker. Files are uploaded using Stream's `channel.sendFile()` and rendered as downloadable cards with file-type icons (📄/📊/📎), filename, and human-readable file size. Clicking a file card opens it in a new tab.
- **Pinned Messages**: Any message can be pinned/unpinned via a 📌 pushpin button in the action row. Pinned messages display a "📌 Pinned" indicator in orange next to the sender name. Uses Stream's `client.pinMessage()`/`client.unpinMessage()` API.
- **GIF Search**: A "GIF" button in the message input opens a 3-column grid search panel powered by the Tenor GIF API. Users search for GIFs and click to send them as image attachments via `sendGiphyMessage()`. GIFs display with a "GIF ▶" badge and open in an in-app lightbox on click.
- **Image Lightbox**: Clicking any image or GIF attachment opens a full-screen dark overlay (`ImageLightboxDialog`) with the image displayed at full resolution. Includes a close button (X) and click-outside-to-dismiss.
- **Link Preview Polish**: URL attachments are rendered as styled OG cards with a blue accent bar on the left, extracted domain name with 🔗 icon, 140dp OG image, bold title, muted description, and a max width of 300dp.
- **File Attachment Cards**: File attachments render as compact cards with a color-coded accent bar (red=PDF, blue=DOC, green=XLS, orange=PPT, purple=ZIP), file extension badge, file name, size, file type label, and download icon.
- **Drag & Drop File Upload**: Files can be dropped directly into the chat area. JS event listeners (`dragover`/`dragleave`/`drop`) handle browser drag events; Kotlin polls for dropped files via `getDroppedFile()`. A "📎 Drop file here" overlay with an orange dashed border appears during drag.
- **User Profile Popover**: Clicking a user's avatar in the message list opens a `UserProfilePopover` dialog. If the user is a registered member, it shows their profile picture, role badge (LEADER/MEMBER/GUEST), profession/company, interests, and favorite wines via `LocalMembers` CompositionLocal. Falls back to basic chat info for non-members.
- **Channel Info Panel**: An ℹ️ button in the chat header toggles a panel showing the channel name, member count, and a scrollable list of members with avatars, online status indicators, and role badges (admin/owner).
- **Message Forwarding**: A ↪️ button in the message action row opens a dialog with a searchable list of channels. Selecting a channel forwards the message text with a "↪️ Forwarded:" prefix.
- **Emoji Skin Tone Selector**: A row of 6 skin tone circles (default yellow + 5 Fitzpatrick modifiers) appears at the top of the emoji picker. Selecting a skin tone applies it to all hand/gesture emojis in the "Gestures & Body" category.
- **Message Caching**: Messages are cached per channel in memory. Switching back to a previously viewed channel loads instantly from cache while a background refresh runs.
- **Draggable Split Pane**: The channel list / chat area split is user-resizable via a draggable divider (20%-50% range).
- **Mobile Responsive**: On compact screens, the channel sidebar and chat area toggle between full-width views with a back button. Padding and elevation are reduced on mobile for maximum usable space.
- **UI & Interaction Polish**:
    - **Enter-to-send**: Users can press Enter to send messages, and Shift+Enter to insert newlines.
    - **Conversation List**: Elevated channel cards with subtle gradients and active-state accent bars provide a modern two-pane chatting experience.
- **User Moderation (Block & Flag)**:
    - **Block/Unblock**: Accessible from the `UserProfilePopover` (click any avatar). Blocks are persisted in Firestore (`blockedEmails` + `blockedUserIds` arrays on `userProfiles`) and synced to Stream Chat server-side. Blocked users' messages are filtered client-side via a derived `messages` flow that combines the raw message list with the blocked user IDs.
    - **Blocked Conversations**: DM channels with blocked users appear in a collapsed "BLOCKED (N)" section at the bottom of the channel list. Clicking a blocked conversation shows an unblock confirmation dialog instead of loading the chat.
    - **Blocked Users Management**: A settings gear icon (⚙️) in the chat header opens a `ChatSettingsDialog` showing blocked users with avatars and unblock buttons. Profiles are fetched via `queryUsersByIds` on the Stream JS bridge.
    - **Report**: Unified moderation system — both user reports and message flags are routed through backend Cloud Functions (`flagUser`, `flagMessage`) and stored in the `moderation_flags` Firestore collection with `type` field (`USER`, `MESSAGE`, `AUTO_ESCALATED`). Category-based reporting (Spam, Harassment, Inappropriate, Other) with optional text reason. Both types also forward to Stream's moderation dashboard. **Auto-escalation**: When a user accumulates 3+ pending message flags, a `type: AUTO_ESCALATED` user-level flag is automatically created. **Admin Notifications**: A Firestore trigger (`onModerationFlagCreated`) sends both email (via `mail` collection / Firebase Trigger Email extension) and FCM push notifications to all admin users (`isAdmin === true`) whenever a new flag is created. Auto-escalated flags receive higher visual emphasis (🚨 vs 🛡️).
    - **Admin Moderation Queue**: Admin dashboard includes a dedicated Moderation page (`/moderation`) with status/type filters, color-coded flag cards, expandable review panel with admin notes, dismiss/review actions, a pending-count stat card on the Dashboard, and a quick "Block User" button on pending flags. Backend endpoints: `adminGetModerationFlags` (GET with optional `?status=&type=` filters) and `adminUpdateModerationFlag` (POST, updates status/notes/reviewedBy). The Users page shows a "⚠️ N reports" badge next to users with pending flags.
    - **Backend Guards**: `flagUser` and `flagMessage` enforce duplicate report prevention (409 Conflict if a PENDING flag already exists from the same reporter) and rate limiting (429 Too Many Requests if more than 5 reports per hour). Required Firestore composite indexes are declared in `firestore.indexes.json`.
    - **Moderation Feedback**: All moderation actions (block, unblock, flag user, flag message) show a toast notification at the bottom of the chat area — green for success, red for errors. Auto-dismisses after 3 seconds.
    - **Report Dialog State**: The `ReportUserDialog` state is managed via `MessagingViewModel.reportDialogTarget` (not local `remember`) so it survives layout changes (e.g., window resize between desktop and compact modes).
    - **Self-protection**: Users cannot block or report themselves; the buttons are hidden on the user's own profile popover.
    - **Cross-platform**: Blocked list syncs via Firestore; blocking on web also takes effect on Android and vice versa.
- **Account Deletion (Play Store Compliance)**:
    - Users can permanently delete their account from the **Profile page** "Danger Zone" section.
    - Requires typing "DELETE MY ACCOUNT" as a confirmation phrase.
    - Backend `deleteAccount` Cloud Function chains: hard-delete from Stream Chat → delete Firestore profile → delete Firebase Auth account.
    - On success, the client signs out via `FirebaseBridge.signOut()`.

## 4. Technical Implementation Details

### Multi-Redundant API Requests
To ensure compatibility with various backend configurations, sensitive identifiers (like user email) are delivered via three redundant channels in a single request:
1.  **Request Body**: Standard JSON payload.
2.  **Query Parameter**: Appended to the URL.
3.  **Custom Header**: Sent via `x-user-email`.

### Cloud Functions v2 Routing
For Firebase Functions v2 (e.g., Passkey Auth, Messaging), the application uses explicit Cloud Run URLs (e.g., `https://function-name-iktff5ztia-uc.a.run.app`) instead of the legacy `cloudfunctions.net` proxy. This ensures consistent routing for functions with project secrets and mitigates 404 errors during proxy propagation.

### Responsive Design
Uses `Material3WindowSizeClass` to adapt layouts between mobile (Compact) and desktop screens. The top navigation bar shows the user's profile picture (from `UserProfileData.profileImageUrl`) as a circular avatar instead of the generic account icon when logged in.

### Navigation
Top-level navigation is managed via `AppBarState` enum. On desktop, a horizontal `TopNavBar` shows all tabs. On compact/mobile screens, a hybrid pattern (matching the Android app) is used:
- **Bottom bar** (`MobileBottomNavBar`): 5 primary tabs — About, Podcasts, Events, Our Wine, Chat
- **Hamburger drawer** (`NavDrawerContent`): secondary tabs — Members, Blog (labeled "More")
- **Top bar**: ☰ hamburger + logo + profile avatar/login icon
- Footer is hidden on compact screens (bottom bar replaces it)

### Footer (`Footer.kt`)
A full-width 4-column dark-themed footer rendered below all desktop page content:
- **Brand**: Logo (`wdw_new_logo`), tagline
- **Navigate**: 7-link site map split into 2 sub-columns; links use `onPointerEvent(Enter/Exit)` for true hover detection (required in Kotlin/WASM — `clickable` alone causes hover state to stick)
- **Connect**: IG/YT icons (white-tinted), clickable email (opens `ContactFormDialog`), clickable phone number (`tel:` deep link — opens dialer on mobile, FaceTime/Skype on desktop)
- **Get our Apps**: Google Play badge + Apple App Store badge (official SVGs/PNGs from `/drawable`), linking to store listings; deep-linked to app stores on mobile

A `CompactFooter` strip is shown on mobile above the `MobileBottomNavBar`.

### Contact Form (`ContactFormDialog.kt`)
A `BasicAlertDialog` modal triggered by clicking the email address in the footer.
- Fields: Name, reply-to email, message (multiline)
- Inline field validation before submit
- HTTP POST to `sendContactEmail` Cloud Function
- **WASM JS interop pattern**: Uses `@JsFun` with callbacks (`onSuccess`/`onError`) + `suspendCancellableCoroutine` to bridge async JS fetch to Kotlin coroutines. `Promise<T>` cannot be used as a `@JsFun` return type in Kotlin/WASM 2.1; the callback pattern is the correct approach.
- Loading spinner during submit; success screen with `Icons.Default.CheckCircle` on completion

### Logo
The app logo is stored as `wdw_new_logo.png` in `composeApp/src/commonMain/composeResources/drawable/` and referenced in both `SharedScreens.kt` (TopNavBar) and `Footer.kt` (brand column) via `painterResource(Res.drawable.wdw_new_logo)`.

## 5. Deployment Info
- **Hosting**: Firebase Hosting.
- **Build Command**: `./gradlew :composeApp:wasmJsBrowserDistribution`
- **Memory**: Production WASM compilation requires at least 6 GB heap. Set in `gradle.properties`: `kotlin.daemon.jvmargs=-Xmx6G` and `org.gradle.jvmargs=-Xmx6G`.
- **Headers**: `firebase.json` is configured to serve `.wasm` files with `application/wasm` MIME type and specific caching headers for performance.
