# Matchering Android App

A native Android app that performs **on-device audio matching and mastering** using the Matchering 2.0 library.

## How It Works

1. **Select** a TARGET track (the song you want to master) and a REFERENCE track (the sound you want to match).
2. The app decodes both files to WAV using Android's native `MediaExtractor` + `MediaCodec`.
3. The Matchering Python library runs **on-device** via [Chaquopy](https://chaquo.com/chaquopy/), processing the audio entirely offline — no server, no cloud.
4. The mastered result is saved as a WAV file you can play or share.

## Architecture

| Layer | Technology |
|---|---|
| UI | Kotlin + XML layouts (Material Components) |
| Audio decoding | Android `MediaExtractor` + `MediaCodec` → 16-bit PCM WAV |
| Audio processing | Matchering 2.0 Python library via Chaquopy |
| Audio I/O shim | Python `wave` module replaces `soundfile` (no native libsndfile needed) |

### Python Bridge (`app/src/main/python/mg_android.py`)

The matchering library imports `soundfile` (which requires the native `libsndfile` library, unavailable on Android). The bridge script installs a lightweight shim into `sys.modules` **before** importing matchering, replacing `soundfile.read()` / `soundfile.write()` with Python's built-in `wave` module. This means:

- No native C library needed on Android
- The matchering library code is **unchanged** — it's copied as-is from the repo root
- The shim also patches `scipy.ndimage.filters` (deprecated path) and provides a fallback `statsmodels` LOWESS implementation if the package can't be installed

## Building

### Prerequisites

- **Android Studio** (Hedgehog 2023.1.1 or newer)
- **JDK 17**
- An Android device or emulator with **API 24+** (Android 7.0+)
- **4 GB+ RAM** on the device (audio processing is memory-intensive)

### Steps

1. Open the `android/` folder in Android Studio.
2. Let Gradle sync — Chaquopy will download the Python interpreter and pip packages (numpy, scipy, resampy, statsmodels).
3. The `copyMatchering` Gradle task automatically copies the `matchering/` package from the repo root into `app/src/main/python/matchering/` before each build.
4. Connect an Android device (USB debugging enabled) or start an emulator.
5. Click **Run** ▶.

### Notes

- **First build** may take several minutes as Chaquopy downloads and compiles Python packages for the target ABI.
- **Processing time** depends on the track length and device CPU — a 3-minute track may take 30–60 seconds on a modern phone.
- If `statsmodels` fails to install via Chaquopy, the app still works — the bridge script falls back to a simplified LOWESS implementation (slightly different smoothing quality).
- The app processes audio at 44100 Hz internally (matchering's default). Files at other sample rates are resampled automatically.

## Project Structure

```
android/
├── build.gradle.kts              # Project-level Gradle config
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
├── app/
│   ├── build.gradle.kts          # App module + Chaquopy config + copyMatchering task
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/matchering/app/
│       │   ├── MainActivity.kt       # UI: file pickers, process, play, share
│       │   ├── MatcheringProcessor.kt # Orchestrates conversion + Python call
│       │   └── AudioConverter.kt     # MediaExtractor → WAV conversion
│       ├── python/
│       │   ├── mg_android.py         # Bridge: soundfile shim + matchering.process()
│       │   └── requirements.txt
│       └── res/                      # Layouts, strings, colors, themes
```
