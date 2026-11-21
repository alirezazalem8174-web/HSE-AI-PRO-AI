# HSE AI PRO v4.6.2 — Architecture Step 7

## Data Layer Hardening

- Added `HseDataRepository` as the application data boundary.
- Extracted feature classes from direct `LibraryDb` access to repository access.
- Kept `LibraryDb` as the single SQLite implementation to avoid a risky database migration.
- MainActivity now owns the database lifecycle and repository composition.
- Repository is recreated after restore so features never retain a stale database boundary.
- Existing BackupManager/PdfImporter contracts intentionally remain database-aware in this incremental step.
- Added a generic read transaction hook for future repository-backed query composition.

## Scope decision

This step deliberately does **not** introduce Room, Hilt, Compose, or a large database rewrite. The goal is to establish a stable dependency boundary first and keep regression risk low.

## QA

- Data layer contract QA: PASS
- Existing source, persistence, backup/restore, concurrency, reminder, adversarial and environment QA: PASS
- Real Android Gradle build: not executed in this environment because Android SDK/Gradle are unavailable locally.
