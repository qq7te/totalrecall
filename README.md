# Total Recall

A privacy-focused, open-source memory crutch app for Android that lets you capture memories with photos, text, and location data.

## Features

- 📸 **Photo Capture**: Take photos with CameraX integration
- 📝 **Text Entries**: Add descriptions and notes to your photos
- 📍 **Location Tracking**: Optional GPS location for entries (using native Android LocationManager)
- 🔍 **Search**: Find entries by text content
- 🗂️ **Browse**: View all entries sorted by timestamp
- ✏️ **Edit Entries**: Open an entry from Browse, tweak its text from the detail screen while keeping photo and location intact
- 🗑️ **Delete**: Remove entries with confirmation dialog
- 🔒 **Privacy**: All data stored locally, FOSS-compatible

## Screenshots

*Coming soon*

## Building

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 26+ (target 33)
- Kotlin 1.8+

### Build Instructions

1. Clone the repository:
```bash
git clone https://github.com/qq7te/TotalRecall.git
cd TotalRecall
```

2. Build the app:
```bash
./gradlew assembleDebug
```

3. Install on device:
```bash
./gradlew installDebug
```

### Release Build

```bash
./gradlew assembleRelease
```

## Permissions

- **CAMERA**: Required for taking photos
- **ACCESS_FINE_LOCATION**: Optional for GPS coordinates
- **ACCESS_COARSE_LOCATION**: Optional for approximate location
- **POST_NOTIFICATIONS**: For Android 13+ compatibility

## Architecture

- **MVVM Pattern**: ViewModel + LiveData
- **Room Database**: Local SQLite storage
- **Navigation Component**: Fragment-based navigation
- **CameraX**: Modern camera implementation
- **Data Binding**: UI binding
- **Coroutines**: Asynchronous operations

## Dependencies

All dependencies are FOSS-compatible:
- AndroidX libraries (Apache 2.0)
- Room database (Apache 2.0)
- CameraX (Apache 2.0)
- Navigation Component (Apache 2.0)
- Glide image loading (BSD-2-Clause)

## F-Droid

This app is fully compatible with F-Droid:
- ✅ No proprietary dependencies
- ✅ Native Android LocationManager (no Google Play Services)
- ✅ Reproducible builds
- ✅ Open source license

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Privacy

- All data is stored locally on your device
- No data is transmitted to external servers
- Location data is optional and can be disabled
- No analytics or tracking

## Support

This is an open-source project maintained by volunteers. Please report issues via GitHub issues.
