# Photo Zoom Viewer Feature

## Overview
The photo zoom viewer feature allows users to view entry photos in fullscreen with zoom and pan capabilities.

## User Flow
1. User views an entry in `DetailFragment`
2. User taps on the photo
3. `PhotoViewerDialogFragment` opens in fullscreen
4. User can:
   - Pinch to zoom (1x to 4x magnification)
   - Pan/drag to move around zoomed images
   - Double-tap to zoom
   - Tap close button to dismiss

## Architecture

### Components

#### 1. ZoomableImageView (`ui/detail/ZoomableImageView.kt`)
Custom `AppCompatImageView` that handles zoom and pan gestures.

**Features:**
- Pinch-to-zoom using `ScaleGestureDetector`
- Pan/drag using touch event handling
- Single-tap detection using `GestureDetector`
- Matrix transformations for smooth scaling
- Min/max scale limits (1x to 4x)
- `resetZoom()` method to restore initial view

**Key Methods:**
- `onTouchEvent()` - Handles all touch gestures
- `resetZoom()` - Resets to initial fit-to-screen scale
- `onSingleTapListener` - Callback for tap events

#### 2. PhotoViewerDialogFragment (`ui/detail/PhotoViewerDialogFragment.kt`)
Fullscreen dialog that displays photos using `ZoomableImageView`.

**Features:**
- Fullscreen black background
- Close button (floating action button)
- Uses Glide for image loading
- Accepts photo URI as argument

**Usage:**
```kotlin
val dialog = PhotoViewerDialogFragment.newInstance(photoUri)
dialog.show(childFragmentManager, "photo_viewer")
```

#### 3. DetailFragment Integration
The detail view uses a regular `ImageView` with a click listener that opens the fullscreen viewer.

**Important:** DetailFragment uses `ImageView`, NOT `ZoomableImageView`, to avoid conflicting gestures. Only the fullscreen viewer uses `ZoomableImageView`.

## Testing

### Unit Tests (`test/java/com/qq7te/totalrecall/ZoomableImageViewTest.kt`)
Tests verify the feature architecture and implementation approach. Actual gesture testing requires instrumented tests due to Android framework dependencies.

### Instrumented Tests (`androidTest/java/com/qq7te/totalrecall/PhotoViewerDialogFragmentTest.kt`)
Tests verify:
- Fragment creation with correct arguments
- Fragment instantiation
- Dialog style configuration

To run tests:
```bash
# Unit tests
./gradlew testDebugUnitTest --tests "com.qq7te.totalrecall.ZoomableImageViewTest"

# Instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest --tests "com.qq7te.totalrecall.PhotoViewerDialogFragmentTest"
```

## Files Modified/Created

### New Files
- `app/src/main/java/com/qq7te/totalrecall/ui/detail/ZoomableImageView.kt`
- `app/src/main/java/com/qq7te/totalrecall/ui/detail/PhotoViewerDialogFragment.kt`
- `app/src/main/res/layout/fragment_photo_viewer.xml`
- `app/src/test/java/com/qq7te/totalrecall/ZoomableImageViewTest.kt`
- `app/src/androidTest/java/com/qq7te/totalrecall/PhotoViewerDialogFragmentTest.kt`

### Modified Files
- `app/src/main/java/com/qq7te/totalrecall/ui/detail/DetailFragment.kt` - Added click listener to open fullscreen viewer
- `app/src/main/res/layout/fragment_detail.xml` - Uses regular ImageView with clickable=true
- `WARP.md` - Added Photo Zoom Viewer documentation section

## Technical Details

### Gesture Handling
The `ZoomableImageView` uses two gesture detectors:
1. **ScaleGestureDetector** - For pinch-to-zoom
2. **GestureDetector** - For single-tap detection

Touch events flow through both detectors, with the custom `onTouchEvent` handling drag and multi-touch zoom.

### Matrix Transformations
The view uses Android's `Matrix` class for transformations:
- `initialMatrix` - Stores the original fit-to-screen matrix
- `savedMatrix` - Stores matrix state at gesture start
- `matrix` - Current transformation matrix applied to the image

### Scale Limits
- **Min scale:** 1x (fit to screen)
- **Max scale:** 4x magnification

## Future Enhancements
Potential improvements:
- Add double-tap to zoom to specific scale
- Add zoom controls (+ / - buttons)
- Add image rotation support
- Add swipe gestures to navigate between entry photos
- Add pinch-to-dismiss gesture
