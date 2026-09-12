# AI-ASSISTANT

Personal AI assistant (optimized HTML wrapped in a native Android WebView).

- `index.html` — the assistant web UI (source of truth).
- `app/src/main/assets/index.html` — copy bundled into the APK (`file:///android_asset/index.html`).
- `app/src/main/java/com/chetan/assistant/MainActivity.java` — WebView host (JS + DOM storage + mic permission).

## Build APK (GitHub Actions)

1. Push to `main` or open the repo → **Actions** tab → **Build APK** → **Run workflow**.
2. After the run finishes, download the `app-debug` artifact → `app-debug.apk`.
3. Install on phone (enable Install unknown apps if asked).

To update the app: edit `index.html`, copy it to `app/src/main/assets/index.html`, commit, push — a new APK builds automatically.

## API keys (important)

Keys are NOT committed (GitHub push protection blocks them). The code contains
`__CEREBRAS_API_KEY__` / `__GEMINI_API_KEY__` placeholders.

For a working APK, add repo secrets once:
repo → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**:
- `CEREBRAS_API_KEY` = your Cerebras key
- `GEMINI_API_KEY` = your Gemini key

The workflow injects them into `app/src/main/assets/index.html` at build time.
For local browser testing, replace the placeholders in your local copy only (never commit).
