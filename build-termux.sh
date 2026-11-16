#!/data/data/com.termux/files/usr/bin/bash
# TUNDER LUCKY — Termux build helper. Usage: ./build-termux.sh [clean]
# NOTE: written without being run on a real device. Read BUILD_TERMUX.md if anything fails.
set -u
cd "$(dirname "$0")"
red(){ printf '\033[31m%s\033[0m\n' "$*"; }; grn(){ printf '\033[32m%s\033[0m\n' "$*"; }; ylw(){ printf '\033[33m%s\033[0m\n' "$*"; }
die(){ red "ERROR: $*"; exit 1; }

ARCH=$(uname -m); echo "CPU architecture: $ARCH"
case "$ARCH" in aarch64|armv8*|arm64) ;; armv7l|armv8l) ylw "32-bit ARM userland: Gradle + Compose may not build here. Use the cloud fallback.";; x86_64) ;; *) ylw "Unknown arch $ARCH";; esac
MEM_KB=$(awk '/MemTotal/{print $2}' /proc/meminfo 2>/dev/null || echo 0)
echo "RAM: $((MEM_KB/1024)) MB"; [ "$MEM_KB" -lt 3500000 ] && ylw "Low RAM. Close other apps; consider adding swap or using the cloud fallback (see BUILD_TERMUX.md)."

command -v java >/dev/null || die "java not found. Run: pkg install openjdk-17"
JV=$(java -version 2>&1 | head -1); echo "Java: $JV"
echo "$JV" | grep -Eq '"(17|21)\.' || ylw "JDK 17 or 21 recommended."

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
[ -d "$SDK" ] || die "Android SDK not found at $SDK. Set ANDROID_HOME (see BUILD_TERMUX.md step 3)."
[ -f "$SDK/platforms/android-34/android.jar" ] || die "Missing $SDK/platforms/android-34/android.jar. Install: sdkmanager \"platforms;android-34\""
BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)
echo "SDK: $SDK  build-tools: ${BT:-none}"

# --- find a NATIVE (bionic/ARM) aapt2: desktop-Linux aapt2 from Maven will not run in Termux
AAPT2=""
for c in "${AAPT2_PATH:-}" "$PREFIX/bin/aapt2" "${BT:-/nonexistent}/aapt2"; do
  [ -n "$c" ] && [ -x "$c" ] && "$c" version >/dev/null 2>&1 && { AAPT2="$c"; break; }
done
[ -z "$AAPT2" ] && die "No working aapt2. Try: pkg install aapt2   (or install aarch64 build-tools; set AAPT2_PATH=/path/to/aapt2)"
grn "aapt2: $AAPT2 ($("$AAPT2" version 2>&1 | head -1))"
for t in zipalign apksigner; do
  p=$(command -v $t || ls "${BT:-/nonexistent}/$t" 2>/dev/null | head -1); [ -n "$p" ] && echo "$t: $p" || ylw "$t not found (only needed for manual release signing)"
done

echo "sdk.dir=$SDK" > local.properties

if [ -f gradle/wrapper/gradle-wrapper.jar ]; then GR=./gradlew
elif command -v gradle >/dev/null; then
  ylw "Generating Gradle wrapper (gradle-wrapper.jar)…"; gradle wrapper --gradle-version 8.11.1 --no-daemon && GR=./gradlew || GR=gradle
else die "Neither gradle-wrapper.jar nor 'gradle' found. Run: pkg install gradle"; fi

[ "${1:-}" = "clean" ] && $GR clean --no-daemon
echo "Building (this can take 10-40 min on a phone)…"
$GR assembleDebug --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1024m" \
  -Pandroid.aapt2FromMavenOverride="$AAPT2" -Pkotlin.daemon.jvmargs="-Xmx1024m" --stacktrace
RC=$?
APK=app/build/outputs/apk/debug/app-debug.apk
if [ $RC -eq 0 ] && [ -f "$APK" ]; then
  grn "BUILD OK → $(pwd)/$APK"
  echo "Copy to phone storage:  cp $APK ~/storage/downloads/TunderLucky.apk   (after: termux-setup-storage)"
else
  red "BUILD FAILED (exit $RC). See TROUBLESHOOTING.md. If it is an out-of-memory error, use the cloud fallback."
  exit $RC
fi
