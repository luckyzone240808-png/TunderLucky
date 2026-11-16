# TUNDER LUCKY repair notes

This repair updates the GitHub Actions workflow and the build instructions.

## Fixed
- GitHub Actions now installs Android SDK platform 34 and build-tools 34.0.0 explicitly.
- Workflow uses JDK 17 and Gradle 8.11.1, matching the project pins.
- Workflow verifies that the debug APK exists before uploading it.
- Artifact name is `TUNDER-LUCKY-debug-apk`.
- Build instructions now explain that the contents of `TunderLucky/` must be placed at the GitHub repository root.
- Cloud build instructions explain that the missing Gradle wrapper JAR does not block GitHub Actions because the workflow invokes the installed Gradle distribution directly.

## Not fixed / not verified
- No Kotlin/Gradle compilation was possible in the offline editing environment.
- The Android app has not been installed or tested on a Vivo V2054.
- A successful CI build would establish compilation, not runtime correctness.
- Termux native AAPT2 compatibility remains device/environment dependent.
