# TUNDER LUCKY — personal AI assistant for Android
Native Kotlin + Jetpack Compose + OpenGL ES 2.0. minSdk 30, targets Vivo V2054 (Android 11).

**Status: complete source, never compiled.** Read `FEATURES_STATUS.md` and `BUILD_TERMUX.md` first.

Local mode works with no key (open apps, YouTube/Chrome, settings, time/date/battery/torch, accessibility actions).
Cloud mode needs an OpenAI-compatible HTTPS endpoint, model name and your own API key (may cost money / have limits).

Layout: `app/src/main/java/dev/tunderlucky/{ui,ai,voice,accessibility,automation,youtube,browser,downloads,system,security,settings}`.
Docs: BUILD_TERMUX.md · PERMISSIONS_AND_PRIVACY.md · FEATURES_STATUS.md · TROUBLESHOOTING.md.
Build: `./build-termux.sh` → `app/build/outputs/apk/debug/app-debug.apk`. Cloud build: `.github/workflows/build.yml`.
Example commands: "open YouTube and search Python programming", "scroll down", "type hello world", "go back", "stop".
