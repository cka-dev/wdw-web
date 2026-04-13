# Wine Down Wednesday (WDW) Web Architecture and Features

This document provides a detailed overview of the architecture, features, and technical implementation of the `wdw-web` project.

## 1. Project Overview
`wdw-web` is a web application built using **Compose Multiplatform for Web (Kotlin/Wasm)**. It serves as the web presence for "Wine Down Wednesday," offering features like event management, membership details, podcast listings, and wine catalogs.

## 2. Architecture

### Technology Stack
- **Language**: Kotlin 2.2.20
- **UI Framework**: Compose Multiplatform 1.10.2 (WasmJs target)
- **Navigation**: Jetpack Navigation 3 (`org.jetbrains.androidx.navigation3:navigation3-ui:1.0.0-alpha06`) — Alpha multiplatform support via CMP 1.10.2
- **Dependency Injection**: Koin (with KSP 2.2.20-2.0.4)
- **Networking**: Ktor Client
- **Authentication**: Hybrid Model (Passkeys/WebAuthn + Server-Side Password Validation)
- **Messaging**: Stream Chat SDK (with custom Cloud Function token generation)
- **Emoji Rendering**: Emoji.kt (Noto vector images for Wasm, where system emoji fonts are unavailable)
- **Image Loading**: Coil 3 (`setSingletonImageLoaderFactory` configured with 25% memory cache + crossfade)
- **Image Serialization**: `ImageBitmapSerializer` encodes bitmaps as JPEG (quality 80) for upload payloads
- **Backend**: Firebase Cloud Functions (Node.js) with `sharp` for server-side image optimisation (resize, WebP conversion, immutable cache headers). Core read endpoints use `minInstances: 1` for warm starts.
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
    - **`AiBridge.kt`**: Kotlin external declarations for the AI bridge (on-device + cloud inference).
- **`StreamBridge.kt`**: Kotlin external declarations for Stream Chat JS SDK.

### JS Interop & Security
Since the project runs on Wasm, it interacts with JavaScript libraries for Firebase and WebAuthn.

#### Sensitive Configuration
To prevent leaking project identifiers, some JavaScript files are excluded from version control. Templates are provided for development:
- **`firebase-bridge.js`**: Bridges Kotlin to Firebase JS SDK. Template: `firebase-bridge.js.example`.
- **`firebase-messaging-sw.js`**: Firebase Messaging Service Worker. Template: `firebase-messaging-sw.js.example`.
- **`webauthn-bridge.js`**: Utility functions for WebAuthn cryptography. Template: `webauthn-bridge.js.example`.
- **`stream-bridge.js`**: Bridges Kotlin to Stream JS SDK. Template: `stream-bridge.js.example`.
- **`ai-bridge.js`**: AI inference bridge implementing three-tier hybrid architecture (on-device Chrome Prompt API → server-side Gemini 3.1 Flash Lite fallback → dedicated bot/summarization endpoints). Powers the Vino AI bot, smart replies, message drafting, translation, and thread summarization.

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
    - **Blog TL;DR Summarizer**: When a blog post is opened, the client silently extracts all text blocks (Paragraph, Heading, Quote, ListBlock) and calls `aiInfer` with `task: "summarize"`. If the post has ≥50 chars of content, a collapsed "✨ TL;DR" pill appears below the author line. Expanding it reveals a 3-bullet summary with WdwOrange left-accent card. Requires authentication. Results cached per post ID for the session.
- **Wine Catalog & Ratings**: A catalog of featured wines with aggregated ratings and user reviews.
    - **Review System**: Authenticated users can leave 1-5 star ratings and written reviews (max 500 chars). The system pins the logged-in user's own review to the top, allowing for edits and deletions.
    - **Soft-Delete Moderation**: Admins can remove inappropriate reviews. Instead of hard deletion, removed reviews are flagged (`flagged: true`) and hidden from the public feed. The original author still sees their review but with editing disabled and a red warning banner containing a `moderationNote` from the admin.
    - **User Flagging**: Community members can flag inappropriate reviews for admin attention. The system guards against duplicate reports and utilizes the shared `moderation_flags` collection to feed the admin dashboard.
    - **Vino Wine Recommendations** (`recommendWines` Cloud Function): On The Cellar page, a collapsed "✨ Vino's Picks" pill is silently auto-fetched on login via `LaunchedEffect`. Expands to compact `surfaceVariant` cards with WdwOrange left accent, capped at `widthIn(max=500dp)`. **Zero-signal guard**: returns empty if the user has no wine reviews *and* no attended events — prevents hallucinated preferences for new members.
    - **Multi-Client Support Requirement**: The Android client must implement the review API endpoints (`submitWineReview`, `flagWineReview`, etc.) and the corresponding UI components to reach parity. See `~/Documents/wdw-feature-docs/wdw-web/wine-reviews/mobile-client-prompt.md`.

### Messaging (Real-time Chat)
- **Stream Chat Integration**: Real-time communication for members via Stream Chat SDK.
- **Token Infrastructure**: Secure authentication using a dedicated `generateStreamToken` Cloud Function that verifies Firebase ID tokens. Profile image URLs are sanitized before being passed to Stream to filter out CORS-unfriendly sources (e.g., Google's default avatar `google.com/images/branding/`).
- **Tiered Channel Access**: The `generateStreamToken` function looks up the user's `memberType` from the `members` Firestore collection and assigns channels accordingly:
  - **Community Lounge** (`wdw-community`): All authenticated users (GUEST, MEMBER, LEADER).
  - **Members Circle** (`wdw-members`): MEMBER and LEADER only.
  - **Leaders Room** (`wdw-leaders`): LEADER only.
- **Admin Role Syncing**: Firebase `isAdmin` flag from the `users` Firestore collection is synced to Stream's role system. Admin users receive the Stream `admin` role, granting moderation powers (ban, remove members, delete messages).
- **JS Bridge (Web)**: Leverages the established JS interop pattern to maintain a lightweight Wasm bundle while utilizing the full Stream Browser SDK.
- **Bot-Aware DM Detection**: The `_mapChannelData` function in `stream-bridge.js` uses a bot-filtered member count (`_botUserIds: ['vino-bot']`) to correctly identify DMs. A channel is classified as a DM when it has no explicit name AND either `allMembers.length === 2` (user+human or user+bot) OR `humanMembers.length === 2` (human DM where a bot was added later). For the "other user" display (name, avatar, online), it prefers a human member and falls back to the bot.
- **Image URL Sanitization**: `_sanitizeImageUrl()` in `stream-bridge.js` filters out CORS-unfriendly URLs (Google default avatars) before they reach the UI. Applied to all image touchpoints: messages, channel data, user search results, and channel members.
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
    - Users can permanently delete their account from the **Settings page** "Danger Zone" section.
    - Requires typing "DELETE MY ACCOUNT" as a confirmation phrase.
    - Backend `deleteAccount` Cloud Function chains: hard-delete from Stream Chat → delete Firestore profile → delete Firebase Auth account.
    - On success, the client signs out via `FirebaseBridge.signOut()`.
- **Vino AI Bot** (`vino-bot`):
    - **Identity**: Vino is the WDW community AI sommelier — professional but approachable personality, wine-knowledgeable with a warm tone.
    - **Three-Tier Hybrid AI Architecture**:
        - **Tier 1 (On-Device)**: Chrome Prompt API (Gemini Nano) for smart replies, message drafting, and translation. Zero cost, zero latency, private.
        - **Tier 2 (Server Lightweight)**: `aiInfer` Cloud Function using Gemini 3.1 Flash Lite for browsers without Prompt API (Safari, Firefox, iOS). Same prompts as on-device, served from server.
        - **Tier 3 (Server Heavy)**: `chatWithBot` and `summarizeThread` Cloud Functions for Vino Q&A and thread catch-up features, grounded in Firestore data.
    - **Bot Q&A**: Users mention `@Vino` or `@bot` in any channel (DM or community). The message is sent normally, then `chatWithBot` Cloud Function processes it via Gemini 3.1 Flash Lite with WDW context (wines, events, members, spotlight), and Vino responds in-thread. Rate limited to 20 queries/hour per user (server) + 3-second client-side cooldown.
    - **Vino Auto-Response Rules**:
        - **Vino DM**: Every message auto-triggers Vino — no @mention needed. Responses are flat (no threading), `parentMessageId = null`.
        - **Community channels**: Requires explicit `@Vino` mention. Vino replies as a top-level message.
        - **Thread continuation**: Once Vino is engaged in a thread, subsequent replies auto-trigger Vino if: (a) the thread parent was a Vino message, OR (b) Vino was the last to reply. If another human replies and breaks the chain, Vino goes silent until explicitly @mentioned again.
        - **GIFs in threads**: GIF messages in Vino-active threads also trigger Vino (using the GIF title as message text).
    - **Two-Layer Contextual Intelligence**: `buildWdwContext` in `ai_bot.ts` uses two detection layers:
        1. **Layer 1 (current query keywords)**: Absolute precedence — explicit domain terms ("wine", "event", "gathering", "upcoming", etc.) determine context regardless of history.
        2. **Layer 2 (history fallback)**: Only fires when the current query is ambiguous (no strong keywords). Scans the last 3 messages for domain signals. Prevents context drift on short follow-ups like "tell me more".
        - Returns `detectedTopic`: `"wines"` | `"events"` | `"both"` | `"general"`.
    - **Topic Anchor**: After context detection, a hard domain lock is injected into the Gemini prompt: `[Topic: WINES. Answer ONLY about wines — do NOT mention events.]`. Prevents Gemini from drifting to a different topic on ambiguous phrasing.
    - **Vino Cards**: `chatWithBot` returns a `cards` array alongside the text reply:
        - `EventCard`: `{ type, name, date, time, location, description }`
        - `WineCard`: `{ type, name, year, wine_type, region }`
        - Cards are only included when domain-relevant data exists. The client renders them as inline rich cards below the Vino message bubble.
        - **Per-message persistence**: Cards are stored in `_vinoCardsByMessageId: Map<String, List<VinoCard>>` keyed by Vino message ID. Each Vino reply permanently owns its cards for the session — they don’t disappear when new messages arrive.
        - **View Details**: Each card has a "View Details →" pill button that navigates to the Wine Cellar or Gatherings page.
        - **Type-specific accents**: Wine cards use a terracotta accent (`#E07B5F`); event cards use purple (`#9C6ADE`).
    - **Domain-Aware Smart Replies**: `chatWithBot` returns `detectedTopic` in the response. The client stores it in `_lastVinoTopic` and injects it into the smart reply context so the AI generates on-topic suggestions (wine-focused after wine Qs, event-focused after event Qs).
    - **Rate Limiting**:
        - **Server**: In-memory `rateLimitMap` — 20 queries/hour per user. Returns `{ rateLimited: true, retryAfter: 3600 }` on 429.
        - **Client**: 3-second cooldown between @Vino messages. Rapid-fire sends are blocked with a toast: "Give Vino a moment to think... 🍷".
        - **Gemini quota (503)**: If Google’s Gemini API itself returns a 429/quota error, `chatWithBot` catches it and returns 503 with a Vino-voiced message: "I’m getting a lot of questions right now — give me a moment! 🍷"
    - **Clear Chat**: A 🗑️ trash icon in the Vino DM header lets users wipe the conversation. Triggers Stream’s `truncateChannel()` server-side and clears `_messages`, `_vinoCardsByMessageId`, and `_pendingVinoCards` client-side. Guarded by a confirmation `AlertDialog`.
    - **Community Lounge**: `@Vino` mentions in any channel (not just DM) trigger `chatWithBot`. In community/team channels, Vino’s reply is a top-level message (not threaded) so all members see it.
    - **Vino Avatar**: Stored in Firebase Storage at `vino_avatar.png`, served via public GCS URL (`storage.googleapis.com/wdw-app-52a3c.firebasestorage.app/vino_avatar.png`). Set to `publicRead` ACL via `gsutil acl ch`. Firebase Storage security rules (`allow read: if false`) block the REST API URL; the public GCS URL bypasses these rules.
    - **Birthday & Wineversary Bot**: Scheduled Cloud Function (`checkBirthdaysAndWineversaries`) runs daily at 9 AM ET. Scans `members` collection for today’s birthdays and membership anniversaries (“Wineversary”). Posts Gemini-generated personalized greetings to Community Lounge only. Duplicate prevention via `bot_greetings` Firestore collection.
    - **JS Bridge**: `ai-bridge.js` (`window.wdwAiBridge`) with `AiBridge.kt` Kotlin external declarations.
    - **Cloud Model**: Gemini 3.1 Flash Lite (`gemini-3.1-flash-lite-preview`) — $0.25/1M input tokens, $1.50/1M output tokens.
    - **Beyond-Chat AI Functions** (all on `beyond-chat-ai` branch, deployed):
        - **`recommendWines`**: Personalized wine recommendations. Takes user's review history and attended events from Firestore; feeds to Gemini for top-3 picks with rationale. Zero-signal guard returns `[]` if user has no reviews and no attendance history.
        - **`recommendEvents`**: Event suggestions based on past attendance patterns. Infrastructure deployed; UI intentionally removed from web client (club is small enough that all-events attendance is the norm). Can be re-enabled in a single PR.
        - **`aiInfer` (summarize task)**: Called for Blog TL;DR. Context body `{ text, maxBullets }`. Returns `{ result: "• Bullet1\n• Bullet2\n• Bullet3" }`.
    - **Data Requirements**: `memberSince` field on `members` collection for Wineversary tracking; `birthday` field parsed from mixed formats ("March 14", "03/14").
    - **UI Treatment**: Vino bot messages are visually distinct from regular messages:
        - Purple gradient background with gradient border instead of the standard `surfaceVariant`.
        - "🤖 Vino `[AI]`" label with purple accent instead of plain username.
        - Purple-ringed avatar with gradient border (2dp `linearGradient`).
        - Profile popover disabled on bot avatar (non-clickable).
        - "Vino is thinking..." indicator with purple spinner appears in all channel types while waiting for bot response.
        - `@Vino` autocomplete: typing `@` or `@v` shows a suggestion chip ("🤖 @Vino — Ask the AI sommelier") above the input; clicking completes to `@Vino `.
- **Admin Tooling** (`wdw-firebase/admin-scripts/`):
    - **`manage-channels.js`**: Node.js script using Stream Chat Admin API and GCP Secret Manager for secure credential access.
    - **`wdw-admin.sh`**: Shell wrapper for developer convenience. Commands:
        - `clear-community` — truncates `wdw-community` and `wdw-community-test` history.
        - `create-test-channel` — creates `wdw-community-test` for safe feature testing.
        - `list-channels` — lists all active channels.
        - `fix-stream-images` — scans all Stream users and clears CORS-unfriendly image URLs (e.g., Google default avatars). One-time migration utility.
    - Credentials are fetched from GCP Secret Manager at runtime; not stored in the repo.

### Settings Page (`SettingsPage.kt`)
A dedicated **Settings** page accessible via a button on the Profile page (navigates to `Route.Settings`).

- **Layout**: Sidebar + detail pane on wide screens (GitHub/Google settings pattern), stacked cards on compact. Categories: Security, Privacy & Moderation, Danger Zone.
- **Security Section**: Shows password/passkey status with ✓/✗ indicators. Actions: Change Password, Link Password (passkey-only users), Add Passkey.
- **Privacy & Moderation Section**: Displays blocked users with count badge, initial-letter avatar fallback, and per-user Unblock button with confirmation dialog. Loading states during async operations.
- **Danger Zone Section**: Account deletion with typed confirmation (moved from Profile page).
- **Email Verification**: Uses Firebase client-side `sendEmailVerification()` SDK directly (via `FirebaseBridge`) instead of the broken `/sendEmailVerification` server endpoint.

### Profile Picture UX
Enhanced profile picture management in `ProfilePage.kt`:

- **Action Sheet**: When editing and a photo exists, clicking the avatar shows a menu: View Photo, Re-crop Photo, Choose New Photo, Remove Photo. When no photo exists, tap goes directly to file picker.
- **Image Cropping** (`ImageCropperDialog.kt`): Pure-Compose cropper with circle mask overlay, drag-to-pan, scroll-to-zoom. Produces a 512×512px cropped bitmap.
- **Full-Screen Viewer**: Dialog preview of current profile photo at full resolution.
- **Remove Photo**: Clears both `profileImageBitmap` and `profileImageUrl`, allowing users to revert to the placeholder.
- **Upload Optimisation**: Profile images are serialized as JPEG (quality 80) on the client, then server-side `sharp` resizes to 600px max width, converts to WebP, and sets `cacheControl: public, max-age=31536000, immutable`. The `fetchUserProfile` response no longer returns `profileImageBitmap` (always null; the field is client-local only).

### Profile Data Integrity
- `ProfileEditSection` now carries `eventRsvps`, `blockedEmails`, and `profileImageUrl` through when constructing `updatedProfile`, preventing silent field drops on save.
- Empty `phone` and `aboutMe` fields send `null` instead of `""` to avoid server-side fallback issues.

## 4. Technical Implementation Details

### Multi-Redundant API Requests
To ensure compatibility with various backend configurations, sensitive identifiers (like user email) are delivered via three redundant channels in a single request:
1.  **Request Body**: Standard JSON payload.
2.  **Query Parameter**: Appended to the URL.
3.  **Custom Header**: Sent via `x-user-email`.

### Cloud Functions v2 Routing
For Firebase Functions v2 (e.g., Passkey Auth, Messaging), the application uses explicit Cloud Run URLs (e.g., `https://function-name-iktff5ztia-uc.a.run.app`) instead of the legacy `cloudfunctions.net` proxy. This ensures consistent routing for functions with project secrets and mitigates 404 errors during proxy propagation.

### API Performance Optimisations
- **Batch Initial Data** (`getInitialData`): A single endpoint returns `members`, `events`, `episodes`, `wines`, `aboutItems`, `memberSpotlight`, `featuredWines`, and `blogPosts` in one gzip-compressed response. Uses `Promise.all` for parallel Firestore reads. The web client calls this at startup (via `AppRepository.init`), falling back to individual endpoints on failure. Mobile clients still use individual endpoints until their next App Store release.
- **Cold-Start Mitigation**: All core read endpoints (`getInitialData`, `fetchUserProfile`, `getMembers`, `getEvents`, `getWines`, `getEpisodes`, `getAboutItems`, `getMemberSpotlight`, `getFeaturedWines`) use `minInstances: 1` to keep one container warm.
- **Gzip Compression**: A shared `sendCompressed()` helper gzips JSON payloads > 1KB when the client sends `Accept-Encoding: gzip`. Ktor auto-decompresses.
- **Parallelised Reads**: `fetchUserProfile` reads `userProfiles`, `members`, and `users` collections in parallel via `Promise.all` (previously 3 sequential awaits).
- **Extracted Mapping Helpers**: `mapMemberDoc`, `mapEventDoc`, `mapWineDoc`, `mapEpisodeDoc`, `mapAboutItemDoc`, `sortEventsDesc` — shared by both individual and batch endpoints.

### Haptic Feedback (Vibration API)
Mobile compact layouts use the browser [Vibration API](https://developer.mozilla.org/en-US/docs/Web/API/Vibration_API) (`navigator.vibrate()`) for subtle haptic feedback on interactive elements.

- **Bridge**: `VibrationBridge.kt` uses inline `js()` calls with built-in feature detection — all calls no-op silently when unsupported. Supports single-pulse and pattern vibrations, plus localStorage-backed preference storage.
- **Utilities**: `HapticFeedback.kt` provides:
  - `HapticDuration` constants (`TICK` 10ms, `LIGHT` 25ms, `MEDIUM` 50ms, `HEAVY` 100ms)
  - `HapticPattern` for multi-pulse patterns (ERROR: buzz-pause-buzz, WARNING, SUCCESS)
  - `HapticIntensity` enum (OFF, LIGHT, NORMAL, STRONG) with configurable multiplier
  - `hapticVibrate()` / `hapticVibratePattern()` — intensity-aware wrappers that scale durations
  - `rememberHapticIntensity()` composable for settings UI binding
- **Browser Support**: Android browsers (Chrome, Firefox, Samsung Internet) ✅ via Vibration API — iOS 17.4+ (Safari, Chrome, all WebKit browsers) ✅ via hidden `<input switch>` Taptic Engine fallback — older iOS ❌ (progressive enhancement, graceful no-op).
- **User Preferences**: Intensity toggle (Off / Light / Normal / Strong) + per-category on/off switches (Navigation, Interactions, Reactions, Dialogs, Alerts) in the NavDrawer, accessible to all users without login. Persisted via localStorage.
- **Integration Points**:
  - Navigation: `MobileBottomNavBar` taps, `NavDrawerContent` item taps, hamburger menu button
  - Events: card tap to detail, RSVP button, RSVP success/error
  - Messaging: send button + Enter key, reaction chips, reaction picker, emoji reactions, thread replies
  - Contact: form submit, validation error, network error, success
  - Login: success (MEDIUM), error (ERROR pattern)
  - Detail dialogs: member, wine, event card opens
- **Security**: Requires user gesture (tap) and secure context (HTTPS) — both already satisfied.
- **Native App Guide**: See `~/Documents/wdw-feature-docs/wdw-web/native-haptics-guide/` for detailed iOS and Android native integration recommendations beyond the web implementation.

### Responsive Design
Uses `Material3WindowSizeClass` to adapt layouts between mobile (Compact) and desktop screens. The top navigation bar shows the user's profile picture (from `UserProfileData.profileImageUrl`) as a circular avatar instead of the generic account icon when logged in.

### Navigation
Top-level navigation uses **Jetpack Navigation 3** (`NavDisplay` + `rememberNavBackStack`). Routes are defined as a `@Serializable sealed interface Route : NavKey`. A polymorphic `SerializersModule` registered in `SavedStateConfiguration` enables Nav3's saved-state serialization for all route subtypes on Wasm.

**Browser history integration** (custom JS interop via `kotlinx.browser`):
- `LaunchedEffect(currentRoute)` pushes `window.history.pushState()` whenever the active route changes, keeping the URL hash (e.g. `#about`) in sync.
- `LaunchedEffect(Unit)` reads the initial hash on first load to support bookmarked / deep-linked URLs.
- `DisposableEffect(Unit)` attaches a `popstate` listener so the browser Back/Forward buttons update the back stack.

> **Note**: Full browser address-bar integration (path-based URLs rather than hash fragments) is postponed to a future CMP release per JetBrains roadmap.

On desktop, a horizontal `TopNavBar` shows all tabs. On compact/mobile screens, a hybrid pattern (matching the Android app) is used:
- **Bottom bar** (`MobileBottomNavBar`): 5 primary tabs — Home, Podcasts, Gatherings, The Cellar, Bubbly Chat
- **Hamburger drawer** (`NavDrawerContent`): secondary tabs — Members, Tasting Notes (labeled "More")
- **Top bar**: ☰ hamburger + logo (40dp height) + profile avatar/login icon
- Footer is hidden on compact screens (bottom bar replaces it)

**Wine-Themed Nav Labels** (desktop top nav):
- About → **Our Story** | Blog → **Tasting Notes** | Events → **Gatherings**
- Our Wine → **The Cellar** | Messages → **Bubbly Chat** | Podcasts → **Uncorked Conversations**
- Mobile uses shorter labels where space is constrained (e.g. "Podcasts" instead of "Uncorked Conversations")

**Become a Member CTA** (`HomePage`): When the user is not authenticated, a pill-shaped orange "Become a Member >>" button and an "Already a member? Sign in" text link are shown below the welcome headline. Both navigate to `Route.Login`. The CTA is hidden once the user is logged in.

### Footer (`Footer.kt`)
A slim single-row 4-column dark-themed footer rendered below all desktop page content:
- **Connect**: IG/YT social icons
- **Contact**: clickable email (opens `ContactFormDialog`), phone number (`tel:` deep link)
- **Get our Apps**: Google Play + Apple App Store badges side by side, linking to store listings
- **Legal**: copyright notice + Privacy Policy link

A `CompactFooter` strip is shown on mobile above the `MobileBottomNavBar`.

### Typography
- **Welcome Headline** (`HomePage`): Rendered in **Cormorant Garamond** (Bold, 30sp) via `displayFontFamily()` in `Typography.kt`. Font files (`CormorantGaramond_Regular.ttf`, `CormorantGaramond_Bold.ttf`) are bundled in `commonMain/composeResources/font/`. Playfair Display files are also present as an alternative.
- **Subtitle + body**: System default sans-serif (intentional serif/sans pairing for premium feel).

### Contact Form (`ContactFormDialog.kt`)
A `BasicAlertDialog` modal triggered by clicking the email address in the footer.
- Fields: Name, reply-to email, message (multiline)
- Inline field validation before submit
- HTTP POST to `sendContactEmail` Cloud Function
- **WASM JS interop pattern**: Uses `@JsFun` with callbacks (`onSuccess`/`onError`) + `suspendCancellableCoroutine` to bridge async JS fetch to Kotlin coroutines. `Promise<T>` cannot be used as a `@JsFun` return type in Kotlin/WASM 2.1; the callback pattern is the correct approach.
- Loading spinner during submit; success screen with `Icons.Default.CheckCircle` on completion

### Logo
The app logo is stored as `wdw_new_logo.png` in `composeApp/src/commonMain/composeResources/drawable/` and referenced in both `SharedScreens.kt` (TopNavBar) and `Footer.kt` (brand column) via `painterResource(Res.drawable.wdw_new_logo)`.

## 5. Animation System

All animation primitives live in `composables/AnimationUtils.kt` for consistent, maintainable reuse.

| Composable / Modifier | Description |
|---|---|
| `Modifier.hoverScale(scale)` | Scales on mouse hover. Scale is tunable: `1.04f` cards, `1.12f` nav items. |
| `shimmerBrush()` | Animated diagonal shimmer `Brush` for loading skeleton placeholders. |
| `SlideInCard(delayMs)` | Right-to-left slide entrance (60dp offset → 0). Staggered delays on HomePage cards. |
| `FadeInPage()` | Page-level alpha fade via `graphicsLayer`. Used on all 10 nav routes. |
| `GridItemReveal(index)` | Bottom-up 40dp entrance for grid items with 60ms/index stagger (cap 360ms, 500ms EaseOut). Used on EventsPage. |

**Page-level highlights:**
- **HomePage**: Typewriter hero text, post-title subtitle fade, 3-card staggered slide-in, inner carousel delayed 1800ms
- **EventsPage**: Bottom-up staggered grid reveal, toggle color animation, gallery dot size animation
- **WinePage / PodcastsPage**: Selection highlight `animateColorAsState`
- **TopNavItem**: Active tab underline pulse + glow bloom (`InfiniteTransition`)
- **All routes**: `FadeInPage` transition wrapper

> **Note on dialog animations**: Compose Multiplatform's `Dialog` uses a browser-level popup whose backdrop cannot be animated via Compose. `DialogReveal` (available in `AnimationUtils.kt`) was evaluated but removed — the system animation and Compose animation conflict caused jarring results.

---

## 6. Adaptive Layout System

Responsive layout decisions are driven by a single `WindowSizeInfo` object, replacing the previous binary `isCompactScreen: Boolean` flag.

### Breakpoints — `WindowSizeInfo.kt`

| Class | Width | Principal devices |
|---|---|---|
| `WidthClass.Compact` | < 600 dp | Phones (portrait) |
| `WidthClass.Medium` | 600 – 839 dp | Portrait tablets, large foldables |
| `WidthClass.Expanded` | 840 – 1199 dp | Landscape tablets |
| `WidthClass.Large` | 1200 – 1599 dp | Large tablet / small desktop |
| `WidthClass.XLarge` | ≥ 1600 dp | Desktop / TV |

| Height class | Height |
|---|---|
| `HeightClass.Compact` | < 480 dp (landscape phones) |
| `HeightClass.Medium` | 480 – 899 dp |
| `HeightClass.Expanded` | ≥ 900 dp (portrait tablets) |

### Key Properties

| Property | Description |
|---|---|
| `useCompactNav` | `true` for Compact + Medium → bottom nav + hamburger drawer |
| `useWideNav` | `true` for Expanded+ → full top nav bar |
| `horizontalPadding` | 16 / 24 / 48 / 80 / 120 dp by width class |
| `maxContentWidth` | Content column cap: unset / 720 / 1100 / 1400 / 1600 dp |

### `rememberWindowSizeInfo()`

Top-level `@Composable` that uses `LocalWindowInfo.containerSize` + `LocalDensity` for precise pixel→dp mapping (more accurate than Material3's `calculateWindowSizeClass()` alone). Called once in `AppNavigation` and the result passed down.

### Per-Page Adaptations

| Page | Compact | Medium | Expanded | Large / XLarge |
|---|---|---|---|---|
| **HomePage** | Full-width cards | 80% cards, 420 dp | 30% cards, 500 dp | Same |
| **AboutPage** | Single-column list | 2-col grid | 2-col grid | 2-col grid |
| **MembersPage** | 2-col grid | 3-col grid | Side-by-side sections | Same |
| **EventsPage** | 280 dp min col | 300 dp | 340 dp | 380 / 420 dp |
| **WinePage** | Stacked list | Stacked list | 30 / 70 split | 25 / 75 split |
| **PodcastsPage** | Stacked cards | Stacked cards | 1:2 list/video | 1:3 list/video |
| **BlogPage** | 16 dp padding | 24 dp | 48 dp | 80 / 120 dp |
| **Carousel** | 320 dp tall | 380 dp | 450 dp | 480 / 520 dp |

---

## 7. Deployment Info
- **Hosting**: Firebase Hosting.
- **Build Command**: `./gradlew :composeApp:wasmJsBrowserDistribution`
- **Memory**: Production WASM compilation requires at least 6 GB heap. Set in `gradle.properties`: `kotlin.daemon.jvmargs=-Xmx6G` and `org.gradle.jvmargs=-Xmx6G`.
- **Headers**: `firebase.json` is configured to serve `.wasm` files with `application/wasm` MIME type and specific caching headers for performance.
- **Hosting Cache Headers**: JS/CSS/Wasm files are served with `Cache-Control: public, max-age=604800, stale-while-revalidate=86400` (7-day cache), and images with `max-age=2592000` (30-day cache), on both the admin and default hosting targets.
- **Storage Image Optimisation**: All uploaded images are processed by `sharp` (resize + WebP conversion) and served with `cacheControl: public, max-age=31536000, immutable` metadata. Event gallery uploads generate separate thumbnail (400px) and full-resolution (1200px) URLs.
- **Versioning**: Semantic Versioning (`MAJOR.MINOR.PATCH`).
    - **Web Client**: Source of truth is `gradle.properties` → `appVersion`. A Gradle task (`generateBuildConfig`) auto-generates `BuildConfig.kt` with a `VERSION` constant at compile time. Displayed in the footer (desktop: Legal column, mobile: compact footer).
    - **Admin Dashboard**: Source of truth is `admin/package.json` → `"version"`. Injected at build time via Vite `define` → `__APP_VERSION__`. Displayed in sidebar footer, logged to console on mount.
    - **Bump Policy**: Versions are only bumped at deploy time, not on every commit. Git tags use `wdw-web@X.Y.Z` / `wdw-admin@X.Y.Z` prefixes.

---

## 8. Adaptive Theming System

The app supports **light and dark modes**, persisted via `localStorage` and toggled by the user.

### `WdwTheme.kt`
A custom Material 3 theme wrapper in `composables/WdwTheme.kt` defines two complete color palettes:

| Token | Dark | Light |
|---|---|---|
| `background` | `#000000` | `#FEFDF9` (warm white) |
| `surface` | `#1A1A1A` | `#EFEFEF` |
| `surfaceVariant` | `#333333` | `#D9D9D9` |
| `primary` | `#1E1E1E` | `#F5EDD8` (champagne) |
| Brand accent | `WdwOrange = #FF7F33` | same |

The `WdwTheme` composable wraps the entire app via `AppNavigation.kt` and provides both a `MaterialTheme` color scheme and a `LocalIsDarkTheme: CompositionLocal<Boolean>` for imperative per-composable theme checks.

### Theme Toggle
- **Wide screens (non-compact)**: A `ThemeTogglePill` `FloatingActionButton` is overlaid at `Alignment.BottomEnd` on all pages **except** the Messaging screen (where it would overlap the send button).
- **Compact/mobile**: A `DarkMode`/`LightMode` icon row in the `NavDrawerContent` drawer.
- Preference saved to `localStorage` via `saveThemePreference()` / `loadThemePreference()` in `ThemePreference.kt`.

### Design Rules
- **Semantic tokens first**: All surfaces, text, and icons use `MaterialTheme.colorScheme.*` tokens.
- **`LocalIsDarkTheme` for exceptions**: Used sparingly when a color must diverge from its semantic token (e.g., birthday badge text: gold in dark, `WdwOrange` in light for contrast).
- **Brand colors are always WdwOrange** regardless of mode (message bubbles, avatars, CTA buttons).
- **Non-negotiable dark elements** (video player backgrounds, image overlay gradients) intentionally use hardcoded `Color.Black` — they are not mode-dependent.
