# PromptVault Android

A local-first Android app for storing, organizing, templating, searching, and reusing AI prompts.

## Version 1 features

- Prompt name, category, tags, notes, and full prompt text
- Search and category filters
- Favorites
- Template variables such as `{customer}` and `{serial_number}`
- One-tap copy and Android sharing
- JSON backup and restore
- No account, server, tracking, or special permissions

## Build

The GitHub Actions workflow builds a debug APK on every push to `main`. Open **Actions**, select the latest successful **Build Android APK** run, and download the `PromptVault-v1.0-debug` artifact.

Local build: `gradle assembleDebug`
