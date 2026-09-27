# Alphabet Launcher

A minimalist, high-performance Android custom launcher built with **Kotlin** and **Jetpack Compose**. It features an interactive curved A–Z alphabet scrubber along the right edge with real-time app filtering, 60 fps touch tracking, zero allocations on the touch path, and a live updating resting state.

---

## 📸 Overview & Features

* **Resting State:**
  * Prominent live-updating digital clock and formatted date.
  * Curated list of favorite apps (prioritizing popular apps like WhatsApp, Chrome, Camera, Gmail, etc. with automatic fallback).
  * Straight vertical A–Z column along the right edge with a star (`★`) at the top and a dot (`•`) at the bottom.
* **Interactive Curved Scrubber:**
  * Single-canvas rendering ensuring steady 60 fps tracking with zero GC allocations during touches.
  * Enlarged circular letter bubble adjacent to the finger.
  * Dynamic app list displaying all installed apps starting with the selected letter, updated instantly during scrubbing.
  * Clear empty state (`"No apps"`) for letters with no installed applications.
* **Release Animation:**
  * Spring-physics return animation that snaps the column smoothly back to a straight line.
* **App Launching:**
  * Tap any app from either the resting favorites list or the filtered alphabet list to launch it immediately.
* **Package Visibility:**
  * Full Android 11+ (API 30+) compatibility using `QUERY_ALL_PACKAGES` and launcher intent categories (`CATEGORY_HOME`, `CATEGORY_DEFAULT`, `CATEGORY_LAUNCHER`).
* **Universal SDP Sizing:**
  * Strict single-unit convention across all layouts and typography using `sdp` (no raw `dp`, `sp`, or `ssp`).

---

## 🌟 Bonus Features Implemented

* **🔍 Swipe-Up Search:**
  * Swiping up on the home screen smoothly slides up an animated full-screen search view with soft keyboard auto-focused.
  * **Initial State:** Displays a `"Favorites"` section heading followed by all user favorite apps.
  * **Active Typing:** Real-time case-insensitive filtering across all installed apps with a `"Results"` heading.
  * **Empty State:** Shows a clear `"No apps found"` message when no apps match the search query.
  * **Dismissal:** Dismissable by swiping down, tapping "Cancel", or pressing the system back button.

* **⭐ Custom Favorites with Jetpack DataStore:**
  * **Add to Favorites:** Long-press any app in the search results or A–Z drawer to open an "Add to Favorites" confirmation dialog.
  * **Remove from Favorites:** Long-press any favorite app on the resting home screen or search overlay to open a "Remove from Favorites" confirmation dialog.
  * **Persistence:** All custom favorite selections are asynchronously persisted using **Jetpack DataStore Preferences**, surviving app kills and device reboots.
  * **Home Screen Cap:** Resting home screen dynamically displays up to 7 favorites to preserve the clean clock + list aesthetic, while the swipe-up overlay displays all favorites.

* **🌀 Spring Physics Animation:**
  * Bar release animation utilizes Compose `Animatable` configured with `Spring.DampingRatioMediumBouncy` and `Spring.StiffnessMedium`, providing a natural overshoot and settling effect.

* **🌓 Adaptive Light & Dark Theme:**
  * Fully supports system theme switching, rendering a deep black minimalist canvas in dark mode and clean high-contrast surfaces in light mode.

---

## 📐 Curve Animation Math & Architecture

The signature bending alphabet column is implemented from scratch inside [`AlphabetScrubberCanvas.kt`](app/src/main/java/com/example/alphabetlauncher/ui/components/AlphabetScrubberCanvas.kt) without external animation or curve libraries:

1. **Geometry & Slots:**
   * The 28 column items (`★`, `A` through `Z`, `•`) are evenly spaced vertically within the available height.
   * Baseline $X$-coordinate is pinned near the right edge ($X_{\text{base}}$).
2. **Raised Cosine Falloff (Hann Window):**
   * For each letter at vertical center $Y_i$ and touch coordinate $Y_{\text{touch}}$, the vertical distance is $\Delta Y = |Y_i - Y_{\text{touch}}|$.
   * Within an influence radius $R$ ($\approx 5.5$ letter slots):
     $$\text{displacement}(Y_i) = X_{\max} \cdot \left( \frac{1 + \cos\left(\frac{\pi \cdot \Delta Y}{R}\right)}{2} \right) \cdot \alpha_{\text{curve}}$$
   * Letters further than $R$ have zero displacement.
   * This produces a smooth bell curve with $C^1$ continuity (zero derivative at boundaries), avoiding any visual kinks or sharp corners.
3. **Release Spring Physics:**
   * When released, the curve progress $\alpha_{\text{curve}}$ transitions from $1 \to 0$ using Compose's `Animatable.animateTo` with `Spring.DampingRatioMediumBouncy` and `Spring.StiffnessMedium`, giving a natural organic spring return.
4. **Performance Guarantee:**
   * Pre-allocated `Paint` instances and bounds rectangles.
   * Zero memory allocations inside `onDraw` or touch event loops.
   * The app list is indexed once in-memory into `Map<Char, List<AppModel>>`; scrubbing performs instant $O(1)$ lookups with zero `PackageManager` calls.

---

## 🏗 Architecture & Design Patterns

The project strictly follows **MVVM** and the **Repository Pattern** with clean separation of concerns:

```
[MainActivity]
      │
      ▼
[HomeScreen] (Jetpack Compose UI)
      │
      ▼
[LauncherViewModel] (StateFlow & Sealed States)
      │
      ▼
[AppRepository] (In-memory Cache & Mutex)
   ┌──┴──────────────────────────────┐
   ▼                                 ▼
[PackageManagerAppDataSource]  [FavoritesPreferencesDataSource]
(Package Discovery)            (DataStore Persistence)
```

* **UI Layer:** Composable functions (`HomeScreen`, `AlphabetScrubberCanvas`, `ClockDateView`, `FavoritesList`, `FilteredAppList`, `SearchOverlay`, `FavoriteActionDialog`, `AppItemRow`) observing immutable `StateFlow` state.
* **ViewModel:** `LauncherViewModel` exposes `LauncherUiState` (`Loading`, `Success`, `Error`), live `ClockState`, search state, and dialog state.
* **Repository:** `AppRepositoryImpl` caches apps, handles sorting and letter grouping, and provides thread-safe access via coroutines mutex.
* **Dependency Injection:** Configured with **Koin** (`io.insert-koin:koin-android`, `koin-androidx-compose`).

---

## 📦 Third-Party Libraries

| Library | Version | Rationale |
| :--- | :--- | :--- |
| `io.insert-koin:koin-android` | `4.0.2` | Lightweight, idiomatic Kotlin dependency injection for Android. |
| `io.insert-koin:koin-androidx-compose` | `4.0.2` | Integration between Koin and Jetpack Compose (`koinViewModel()`). |
| `androidx.compose.material3:material3` | BOM `2026.02.01` | Material 3 design foundation, color schemes, and surface components. |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.11.0` | Lifecycle-aware StateFlow collection (`collectAsStateWithLifecycle`). |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.11.0` | ViewModel lifecycle integration in Compose. |
| `androidx.datastore:datastore-preferences` | `1.1.3` | Asynchronous preference storage for persistent custom favorites. |

*Note: The curved alphabet bar, physics falloff, and letter bubble are implemented entirely with native Compose `Canvas` with no external animation libraries.*

---

## 🛠 Setup & Build Instructions

### Prerequisites
* Android Studio Ladybug / Meerkat or newer
* JDK 17+ (Java 23 supported)
* Android SDK 35/37 (Android 10+ / API 29+ minimum)

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/dev-aman-rathore/Alphabet-Launcher-.git
   cd AlphabetLauncher
   ```
2. Build debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install on device:
   ```bash
   ./gradlew installDebug
   ```

---

## 🤖 AI Disclosure

AI coding assistant (Google Antigravity) was utilized during development for:
* Assisting in structuring the Koin dependency injection module and Gradle version catalog entries.
* Formulating the raised cosine Hann window displacement equation for the vertical Canvas curve falloff.
