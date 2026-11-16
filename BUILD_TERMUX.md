# Building TUNDER LUCKY in Termux

> **Honesty note:** this project was generated in an offline sandbox with no Android SDK or Gradle.
> It has **not been compiled or run**. Expect to fix a few compile errors on first build.
> Building Jetpack Compose on a 3 GB phone is also **likely to be very slow or run out of memory** — see the cloud fallback below.

## Pinned versions
AGP 8.7.3 · Gradle 8.11.1 · Kotlin 2.0.21 (+ Compose compiler plugin 2.0.21) · JDK 17 (21 also OK) · compileSdk/targetSdk 34 · minSdk 30 · Compose BOM 2024.10.01

## 1. Termux
Install Termux from **F-Droid** (not the old Play Store build).
```
pkg update && pkg upgrade
pkg install openjdk-17 gradle unzip zip git wget
termux-setup-storage
```
Check ARM: `uname -m` → `aarch64` expected for the V2054 (it's a 64-bit ARM phone).

## 2. Extract the project
```
cd ~ && unzip /sdcard/Download/TUNDER_LUCKY_ANDROID_PROJECT.zip && cd TunderLucky
```

## 3. Android SDK (Java-based pieces work on ARM; native tools need ARM builds)
- Get Google's *command-line tools* (Java; works in Termux), unpack to `~/android-sdk/cmdline-tools/latest`.
- `export ANDROID_HOME=$HOME/android-sdk` (add to `~/.bashrc`), then
  `yes | sdkmanager --licenses` and `sdkmanager "platforms;android-34" "build-tools;34.0.0"`.
- **AAPT2 problem:** the aapt2 that Gradle downloads from Maven is an x86 *glibc* binary and will not run in Termux.
  Use a native one: try `pkg install aapt2`, or an aarch64 build-tools bundle from the community
  (e.g. GitHub "lzhiyong/android-sdk-tools" — I have not verified its current state). Put it on PATH or set `AAPT2_PATH=/path/to/aapt2`.
  `build-termux.sh` finds it, tests it, and passes `-Pandroid.aapt2FromMavenOverride=…` to Gradle.
- Same for `zipalign` / `apksigner` (only needed for manual release signing).

## 4. Build
```
./build-termux.sh          # uses --no-daemon, 2 workers, 1 GB heap
```
Output: `app/build/outputs/apk/debug/app-debug.apk`
Install: `cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/` then open it from Files (allow "install unknown apps").

The debug APK is automatically signed with the debug keystore (`~/.android/debug.keystore`), which is fine for personal use.

## 5. Signing a release build (optional)
```
keytool -genkeypair -v -keystore ~/tl-release.jks -alias tl -keyalg RSA -keysize 2048 -validity 10000
gradle assembleRelease --no-daemon -Pandroid.aapt2FromMavenOverride=$(command -v aapt2)
zipalign -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk tl-aligned.apk
apksigner sign --ks ~/tl-release.jks --out TunderLucky-release.apk tl-aligned.apk
apksigner verify TunderLucky-release.apk
```
Never commit the keystore.

## 6. Recommended fallback: GitHub Actions (free)
This is the recommended route for a phone with 3 GB RAM. The included workflow explicitly installs Android SDK platform 34 and build-tools 34.0.0, then builds the debug APK on GitHub's hosted runner.

1. Create a new GitHub repository.
2. Extract the ZIP. Upload the **contents of the `TunderLucky` folder** to the repository root. The root must contain `app/`, `build.gradle.kts`, `settings.gradle.kts`, and `.github/`.
3. Commit and push to the `main` branch (or `master`). Alternatively, open the repository's **Actions** tab and run **Build TUNDER LUCKY debug APK** using **Run workflow**.
4. Open the completed workflow run.
5. Under **Artifacts**, download `TUNDER-LUCKY-debug-apk`.
6. Extract the artifact ZIP. It contains `app-debug.apk`.
7. Transfer `app-debug.apk` to your phone and install it. Android may ask you to allow installs from the app used to open the APK.

The workflow uses Gradle 8.11.1 directly, so the missing `gradle-wrapper.jar` does not block the GitHub build. The ZIP still does not contain that binary; the Termux script tries to generate it using the installed `gradle` command.

## 7. Other fallbacks
- **AndroidIDE** (F-Droid/GitHub): open the project folder; its bundled ARM toolchain may help, but compatibility is not verified for this project.
- **Any PC with Android Studio:** open the project folder and use Build → Build APK(s).

## Build status
The project source has not been compiled or run by the ZIP generator. A successful GitHub Actions build will confirm compilation only; it will not prove that voice recognition, accessibility gestures, YouTube/Chrome automation, downloads, or the 3D interface work correctly on your Vivo device. Test those features individually after installation.
