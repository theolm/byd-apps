# AGENTS.md

Guidelines for AI agents working on this Android/Kotlin WebView apps project for BYD vehicles.

## Project Overview

This is a Kotlin Android project using Jetpack Compose that creates WebView-based wrappers for web services (YouTube, Netflix, etc.) designed for BYD vehicle infotainment systems. The project uses a modular architecture with a shared `:core` module and individual app modules in `:apps/`.

## Build Commands

```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :apps:youtube:build
./gradlew :core:build

# Assemble APK
./gradlew :apps:youtube:assembleDebug
./gradlew :apps:youtube:assembleRelease

# Clean build
./gradlew clean

# Check (includes lint)
./gradlew check
```

## Test Commands

```bash
# Run all tests
./gradlew test

# Run unit tests for specific module
./gradlew :core:test
./gradlew :apps:youtube:test

# Run instrumented tests (requires emulator/device)
./gradlew :apps:youtube:connectedAndroidTest

# Run a specific test class
./gradlew :apps:youtube:test --tests "com.example.ClassName"

# Run a specific test method
./gradlew :apps:youtube:test --tests "com.example.ClassName.methodName"
```

## Code Style

- **Language**: Kotlin with official code style (set in `gradle.properties`)
- **Indentation**: 4 spaces
- **Line length**: Follow standard Kotlin conventions
- **No semicolons** at end of lines (unless required)
- **Trailing commas** encouraged in multi-line lists

## Naming Conventions

- **Classes/Objects**: PascalCase (e.g., `BaseWebView`, `YouTubeConfig`)
- **Functions**: camelCase (e.g., `onEnterFullscreen()`, `applySettings()`)
- **Properties**: camelCase for variables, UPPER_SNAKE_CASE for constants
- **Packages**: lowercase with dots (e.g., `dev.theolm.core.webview`)
- **Files**: PascalCase matching primary class name
- **Composables**: PascalCase, descriptive (e.g., `BaseWebView()`)

## Architecture Patterns

- **Modular architecture**: Shared `:core` module + app-specific modules
- **Interface-based configuration**: `WebViewConfiguration` interface for customization
- **Composable-first UI**: All UI built with Jetpack Compose
- **State management**: Use `remember`, `mutableStateOf`, `derivedStateOf` for Compose state
- **Callbacks**: Use interfaces for event handling (e.g., `FullscreenCallbacks`)
- **Extension functions**: Use for clean, reusable code (e.g., `WebView.applySettings()`)

## Import Style

- Group imports: Android framework, AndroidX, third-party, project
- Use wildcard imports only for Compose (`androidx.compose.runtime.*`, `androidx.compose.ui.*`)
- Prefer explicit imports for clarity
- Use `import androidx.activity.ComponentActivity` not `import android.app.Activity`

## Types & Null Safety

- Prefer **nullable types** when dealing with Android framework (WebView, Views)
- Use ** Elvis operator** (`?:`) for defaults
- Use **safe call operator** (`?.`) for nullable chains
- Use **`rememberUpdatedState`** for callbacks that need latest values
- Avoid `!!` unless absolutely necessary

## Error Handling

- **Toast for user errors**: Use `Toast.makeText()` for user-facing errors
- **Null checks**: Always check null before accessing framework objects
- **Graceful degradation**: Apps should work even if WebView features fail
- **No silent failures**: Log or display errors appropriately

## Android-Specific Guidelines

- **Target SDK**: 34, Min SDK: 29, Compile SDK: 36
- **Permissions**: Declare in `AndroidManifest.xml`
- **ProGuard**: Rules in `proguard-rules.pro` (currently empty)
- **Resources**: Use `res/values/` for colors, strings, themes
- **Themes**: Material3 with app-specific branding

## Composable Guidelines

- Accept `Modifier` as parameter with default `Modifier`
- Use `fillMaxSize()` as default modifier when appropriate
- Document parameters with KDoc comments
- Keep composables focused and reusable
- Use `DisposableEffect` for cleanup (e.g., WebView destruction)

## Testing

- **Unit tests**: JUnit 4 in `src/test/kotlin/`
- **UI tests**: Espresso + Compose testing in `src/androidTest/kotlin/`
- **Test runner**: `androidx.test.runner.AndroidJUnitRunner`
- **Naming**: Test class name = ClassUnderTest + "Test"
- **No tests currently exist** - follow patterns when adding

## File Organization

```
module/src/main/java/dev/theolm/
├── core/                    # Shared components
│   ├── webview/            # WebView-related classes
│   ├── constants/          # Constants
│   └── WebViewConfiguration.kt
└── apps/youtube/           # App-specific
    ├── config/             # App configuration
    ├── ui/theme/           # Theme & colors
    └── MainActivity.kt
```

## Dependency Management

- Use **Version Catalog** (`gradle/libs.versions.toml`)
- Reference dependencies: `implementation(libs.androidx.core.ktx)`
- Keep dependencies minimal - this is a lightweight wrapper project

## Comments

- **KDoc for public APIs**: Document interfaces, composables, and public functions
- **Inline comments**: Explain "why" not "what"
- **TODO comments**: Use `// TODO:` for future work

## Prohibited

- No Google Play Services dependencies
- No heavy dependencies (keep APKs small)
- No hardcoded strings in code (use `strings.xml`)
- No `!!` operator without justification
- No `System.out.println()` (use `Log.d()` if needed)
