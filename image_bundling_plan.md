# Image Bundling Plan

## Findings

### Current Architecture
- `ExerciseRepository.syncExercises()` parses `exercises.json` from assets, creates `ExerciseEntity` objects with `imageUrl = null`, inserts them into Room, then downloads images from GitHub raw URLs one-by-one into `context.filesDir/exercise_images/{uuid}.jpg`, and updates `imageUrl` with the local file path after each download.
- `SYNC_PROGRESS_COUNT` DataStore key tracks resume-from position for image downloads.
- `SyncProgress(currentCount, totalCount)` data class emitted via callback during download loop.
- `ExerciseSyncViewModel` holds `_syncProgress` LiveData and passes callback to `syncExercises()`.
- `LibrarySyncFragment` observes `syncProgress` and renders a horizontal `ProgressBar` with count text.
- `ExerciseAdapter`, `ExerciseDetailFragment`, `ActiveSessionFragment` each load images via `Glide.with()` checking `File(imageSource).exists()` before loading as File vs. URL fallback.
- `AndroidManifest.xml` has NO `WRITE_EXTERNAL_STORAGE` permission — clean.
- `ic_exercise_placeholder.xml` already exists at `res/drawable/`.
- No `utils/` package exists yet.

### Image Path Format
- `exercises.json` entries have an `images` array with paths like `"3_4_Sit-Up/0.jpg"`, `"3_4_Sit-Up/1.jpg"`.
- Base URL: `https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/`
- In the free-exercise-db repo, images live at `exercises/<ExerciseName>/<number>.jpg`.
- Asset destination: `app/src/main/assets/exercise_images/<ExerciseName>/<number>.jpg`
- Stored asset path in Room: `exercise_images/3_4_Sit-Up/0.jpg` (relative, no scheme prefix)

### No wgerId Field
- The prompt references "wgerId" (WGER database numeric IDs) but free-exercise-db uses string IDs matching folder names (e.g., `"3_4_Sit-Up"`). We use `paths[0]` and `paths[1]` from the JSON images array directly to form asset paths.

## Implementation Plan

### Step 1 — download_images.sh
- Bash script at project root to clone free-exercise-db and copy `exercises/<FolderName>/` subdirectories to `app/src/main/assets/exercise_images/`.

### Step 2 — ExerciseRepository.kt changes
- Remove `imageBaseUrl`, `imageDir` field, image download loop (lines 116–148), SyncProgress data class, and `onProgress` callback parameter from `syncExercises`.
- Set `imageUrl = "exercise_images/${paths[0]}"` if paths non-empty, else null.
- Set `thumbnailUrl = "exercise_images/${paths[1]}"` if ≥2 images, else same as imageUrl.
- Remove imports: `java.io.File`, `java.io.FileOutputStream`, `java.net.HttpURLConnection`, `java.net.URL`.
- Remove `appPreferences.syncProgressCount` and `appPreferences.setSyncProgressCount` calls.

### Step 3 — AppPreferences.kt changes
- Remove `SYNC_PROGRESS_COUNT` key, `syncProgressCount: Flow<Int>`, `setSyncProgressCount()`.
- Update `resetSyncState()` to only reset `IS_LIBRARY_SYNCED`.

### Step 4 — ExerciseSyncViewModel.kt changes
- Remove `_syncProgress` and `syncProgress` LiveData.
- Remove `SyncProgress` import.
- Simplify `startSync()` to call `exerciseRepository.syncExercises()` without callback.

### Step 5 — LibrarySyncFragment.kt changes
- Remove `syncProgress.observe` block.
- Keep `syncState` and `errorMessage` observers.
- Update SYNCING case to set status text to "Loading exercise library...".

### Step 6 — fragment_library_sync.xml changes
- Replace horizontal `ProgressBar` with `CircularProgressIndicator` (indeterminate).
- Remove `textView_syncCount`.
- Update constraints accordingly.

### Step 7 — Create utils/AssetImageLoader.kt
- `object AssetImageLoader` with `load()` and `loadThumbnail()` methods using `file:///android_asset/` URI scheme.

### Step 8 — ExerciseAdapter.kt changes
- Replace File-based Glide block with `AssetImageLoader.load()`.
- Remove `import java.io.File`.

### Step 9 — ExerciseDetailFragment.kt changes
- Replace File-based Glide block with `AssetImageLoader.load()`.
- Remove `import java.io.File`.

### Step 10 — ActiveSessionFragment.kt changes
- Replace raw `Glide.with(this).load(exercise.imageUrl)` with `AssetImageLoader.load()` with proper placeholder/error.

### No Room Migration Needed
- `imageUrl` and `thumbnailUrl` column types are unchanged (nullable String). Values change from absolute file paths to relative asset paths, but schema is identical.

### No AndroidManifest Changes Needed
- No `WRITE_EXTERNAL_STORAGE` was added for image downloads.

### No Custom GlideModule Issues
- No AppGlideModule found in codebase; Glide default handles `file:///android_asset/` URIs natively.
