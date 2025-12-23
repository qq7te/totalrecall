# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Common Commands

### Build & Install
- Debug APK:
  ```bash
  ./gradlew assembleDebug
  ```
- Release APK (unsigned):
  ```bash
  ./gradlew assembleRelease
  ```
- Install on a connected device or emulator:
  ```bash
  ./gradlew installDebug
  ```

### Lint & Static Analysis
- Run the full Android/Kotlin linter suite:
  ```bash
  ./gradlew lint
  ```
  Lint reports are written to `app/build/reports/lint-results-debug.html`.

### Tests
- JVM unit tests (pure Kotlin/Java):
  ```bash
  ./gradlew testDebugUnitTest
  ```
- Instrumented UI tests on a device/emulator:
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```
- Run a single JVM unit test class or method:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.qq7te.totalrecall.EntryRepositoryUpdateTextTest"
  ```
  Replace the fully-qualified name with the desired test pattern.

### Dependency Updates (Version Catalog)
If you modify `gradle/libs.versions.toml`, refresh wrappers and IDE metadata with:
```bash
./gradlew buildHealth
```

## High-Level Architecture

The project targets Android 31–35 and is organised as a single Gradle module `:app` that follows a clean MVVM approach with Room persistence:

1. **Data layer**  
   • `entity/` – Room @Entity data classes representing a memory entry.  
   • `dao/` – Room @Dao interfaces for database access (suspend functions).  
   • `repository/` – Bridges DAO calls to the domain, exposed as Kotlin Flows.

2. **Domain/ViewModel layer**  
   • `viewmodel/` – AndroidX ViewModel classes that expose `LiveData` / `StateFlow` to the UI and encapsulate coroutine scopes.  
   • Business rules such as search and export live here.

3. **UI layer**  
   • The app uses a hybrid UI: traditional Fragments + XML layouts for navigation chrome, and Jetpack Compose for isolated screens (previewable components).  
   • Navigation is orchestrated by `androidx.navigation` with a `NavHostFragment` declared in `MainActivity`. Destinations are listed in `navigation/nav_graph.xml`.

4. **Camera & Media**  
   • `camera/` package wraps CameraX Preview + ImageCapture use cases.  
   • Captured images are stored in the app-private media directory; paths are persisted in the DB.

5. **Export & Import**  
   • `export/` contains utilities to serialise entries to JSON via Gson.  
   • Zipping helpers live in `utils/FileUtils.kt`.

6. **Third-party libs**  
   • CameraX, Glide, Room, Coroutines, Compose Material3.

### Data Flow Summary
```
UI (Fragment/Compose) ─▶ ViewModel ─▶ Repository ─▶ Room DAO ─▶ SQLite
                                              ▲
                                              │
                                   CameraX / File IO
```
User actions propagate down to Room; changes stream back up via `Flow`/`LiveData` to auto-update the UI.

### Editing Existing Entries

Entries can be re-edited from the detail screen:
- `BrowseFragment` opens `DetailFragment` for a specific entry.
- The **Edit Entry** button in `DetailFragment` navigates to `CaptureFragment` with an `entryId` argument.
- `CaptureFragment` switches to an "edit" mode (no camera preview) that pre-fills the existing photo and text.
- Saving in edit mode calls `EntryRepository.updateEntryText`, which delegates to `EntryDao.updateEntryText` to update only the `text` column.

## Project-Specific Rules for Future Agents

1. Use **suspend** DAO functions and favour `Flow` over `LiveData` in new code.  
2. Prefer Jetpack **Compose** for any new screens; legacy XML may remain until migrated.  
3. Avoid Google Play Services – location uses `LocationManager`, maps are out of scope.
4. Keep the app F-Droid-compatible: no proprietary binaries or closed-source SDKs.

