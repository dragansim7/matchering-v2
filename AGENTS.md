# Base44 Dev Environment

## Project Overview
This repo is **Matchering 2.0** — an open-source audio matching/mastering Python library plus a static landing page.

- `matchering/` — the Python library (numpy, scipy, soundfile, resampy, statsmodels)
- `index.html` + `page/` — static landing page (no JS framework, no backend in this repo)
- `examples/` — CLI usage examples for the library
- The full web application (file upload + audio processing server) lives in a separate prebuilt Docker image (`sergree/matchering-web`, port 8360) whose source is NOT in this repo.

## Running the Preview
- `docker-compose.base44.yml` serves the static `index.html` on port 3000 using `python -m http.server`.
- No database, no external services, no secrets required.
- Edits to `index.html` / `page/` are visible on browser refresh (no live-reload server; call `reload_preview` after changes if needed).

## Android App (`android/`)
- Native Kotlin app using Chaquopy to run the matchering Python library on-device.
- `android/app/src/main/python/mg_android.py` — bridge script that shims `soundfile` with Python's `wave` module (avoids native libsndfile), patches `scipy.ndimage.filters`, and provides a `statsmodels` LOWESS fallback.
- The `copyMatchering` Gradle task copies `matchering/` from repo root into `app/src/main/python/matchering/` before each build.
- Kotlin side (`AudioConverter.kt`) decodes any audio format to WAV via MediaExtractor/MediaCodec before passing to Python.
- **Cannot be built in this sandbox** (no Android SDK) — build with Android Studio. The Python bridge has been tested in the Docker container and works.
- See `android/README.md` for build instructions.

## Library Dependencies
- Python 3.8+, libsndfile1 (system), numpy, scipy, soundfile, resampy, statsmodels (see `requirements.txt`).
- The library is not currently installed in the preview container; it only serves static files.
