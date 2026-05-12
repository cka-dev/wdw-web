- Do NOT make any changes until you make a plan and ASK for approval before proceeding.
- Update the ARCHITECTURE.md if we make any changes

## Project Context

This is the Android client for WDW. It shares backend infrastructure with:
- `wdw-firebase` — Firebase Cloud Functions backend (TypeScript)
- `wdw-web` — Compose Multiplatform web client (Kotlin)
- `wdw-ios` — Native iOS client (Swift/SwiftUI)

## Documentation Maintenance
- **ARCHITECTURE.md**: Update whenever making structural, UI, or infrastructure changes (new patterns, new dependencies, new screens, new SDKs).
- **PENDING.md** (`~/Documents/wdw-feature-docs/PENDING.md`): The central cross-platform feature tracker. Update it when:
  - A feature is **shipped** on this platform (mark Android column ✅; if all platforms done, move to "Recently Completed")
  - A feature is **researched/planned** but not yet implemented (add to "Researched / Not Yet Implemented")
  - Always check PENDING.md at the **start of a new conversation** to see what's outstanding
- **API_CONTRACTS.md** (in `wdw-firebase`): Reference this when consuming server endpoints to verify response shapes match expectations.

## Feature Flags
The server exposes per-platform feature flags. Mobile clients should:
1. **Consume**: Call `GET /getFeatureFlags?platform=android` at startup
2. **Parse**: Into a `FeatureFlags` data class with category-aware defaults:
   - Stable features (e.g. `deleteDmConversations`) default `true` — survive outages
   - New/experimental features default `false` — hidden until ready
3. **Gate**: Wrap feature UI with `if (flags.flagName) { ... }`
4. **Lifecycle**: Flags are temporary. When admin dashboard shows 🧹 (30+ days all-enabled), remove the flag check and keep the feature always-on.
- See [mobile migration plan](~/Documents/wdw-feature-docs/wdw-server/feature-flags/mobile-migration-plan.md) for full implementation guide.

## Cross-Repo Impact Check
- When consuming a new or modified server endpoint, verify the response shape matches `API_CONTRACTS.md` in `wdw-firebase`.
- When adding a new data model, check if the server returns all required fields (non-nullable fields without defaults will crash the deserializer).
- When a server-side change adds a new feature, check PENDING.md for mobile requirements.

## API Key & Secret Security
- **NEVER hardcode API keys, secrets, or tokens in source files.** GitHub secret scanning will flag these.
- Client-side secrets belong in `local.properties` (gitignored), `BuildConfig` fields sourced from CI env vars, or Firebase remote config.
- Before committing, scan the diff: `git diff --cached | grep -iE '(api.?key|secret|token|password).*=.*[A-Za-z0-9]{20}'`

## Git Branch Policy
- Never implement a new feature directly on `main`. Before making any code changes:
  1. Check the current branch with `git branch --show-current`
  2. If on `main`, or the branch name does not match the feature being worked on, create a new branch: `git checkout -b <feature-name>`
  3. Notify the user of the branch that was created or is being used before proceeding

## Feature Documentation
- Whenever you begin planning a new feature (i.e. when creating an implementation plan), automatically create the directory `~/Documents/wdw-feature-docs/wdw-android/<feature-name>/` if it does not already exist. Use a short kebab-case name derived from the feature (e.g. `haptic-feedback`, `ui-animations`, `messaging-ui`).
- Inside that directory, maintain three files throughout the entire conversation:
  - `implementation_plan.md` — the technical plan (update as design evolves)
  - `task.md` — the live checklist of tasks and their completion status
  - `walkthrough.md` — written after verification is complete. Documents what was **actually built**, not just what was planned. Must include:
    - Summary of changes shipped
    - **Deviations from the implementation plan** (features added, removed, or changed mid-stream, and why)
    - Key design decisions made during execution
    - What was tested and how
- These files must be kept up to date at all times, not just created once.
- Do NOT put these in the conversation brain directory or the repo. The path `~/Documents/wdw-feature-docs/wdw-android/` is the single source of truth across all feature conversations for this project.

## Versioning & Release Policy

### Version Format
- Use **Semantic Versioning**: `MAJOR.MINOR.PATCH` (e.g. `1.4.0`)
  - **MAJOR**: Breaking changes, major redesigns, or significant milestones
  - **MINOR**: New features, substantial UI additions
  - **PATCH**: Bug fixes, performance improvements, small tweaks
- Both platforms (Android & iOS) share the same marketing version for feature releases
- Platform-specific hotfixes may diverge at the PATCH level (e.g. Android `1.4.1` while iOS stays `1.4.0`)
- Build numbers (`versionCode`) are monotonically increasing integers, independent of the marketing version and independent of iOS

### Version Bump Locations
- `versionCode` and `versionName` in `app/build.gradle.kts`

### Git Tags
- Tag every release: `v{VERSION}-android` (e.g. `v1.4.0-android`)
- Tags are created **after** the version bump commit is on `main`
- Once a version is tagged, that version number is **never reused**. If a build is rejected by Google Play, fix the issue and bump the build number (`versionCode`), not the marketing version.

### Release Branches
- Cut `release/vX.Y.Z` from `main` when all features for the release are merged
- Only bug fixes on release branches; merge back to `main` when done
- For urgent production fixes, cut `hotfix/vX.Y.Z` from the release tag
- **No dependency version bumps** on `release/*` or `hotfix/*` branches

### Release Checklist
When preparing a release:
1. Confirm target version with the user
2. Create `release/vX.Y.Z` branch from `main`
3. Run `./gradlew assembleRelease` and verify a clean build (no errors, no unresolved TODOs in release paths)
4. Bump `versionCode` (+1) and set `versionName` to the target version in `app/build.gradle.kts`
5. Update `CHANGELOG.md` with release notes
6. Write store "What's New" copy in the changelog or a dedicated `WHATSNEW.md`
7. Commit with message: `release: vX.Y.Z`
8. Merge to `main` and create git tag `vX.Y.Z-android`
9. Notify the user the release is tagged and ready for Play Store submission

### Changelog
- Maintain `CHANGELOG.md` in the repo root
- Follow [Keep a Changelog](https://keepachangelog.com/) format
- Group entries under: Added, Changed, Fixed, Removed
- Every version bump commit must include a changelog update

### Minimum OS Version Policy
- `minSdk` may only be bumped with a `MAJOR` or `MINOR` release, never a `PATCH`
- Bumping `minSdk` always requires explicit user approval since it drops device support
