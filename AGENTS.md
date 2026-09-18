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

## Library Dependencies
- Python 3.8+, libsndfile1 (system), numpy, scipy, soundfile, resampy, statsmodels (see `requirements.txt`).
- The library is not currently installed in the preview container; it only serves static files.
