# Troubleshooting
| Symptom | Fix |
|---|---|
| `gradle-wrapper.jar missing` | `gradle wrapper --gradle-version 8.11.1` (script does this if `gradle` is installed). |
| `aapt2 ... Syntax error: "(" unexpected` / cannot execute | Desktop aapt2 binary. Install a native ARM aapt2 and set `AAPT2_PATH` or rerun `build-termux.sh`. |
| `OutOfMemoryError` / `Killed` | Close apps, lower to `-Xmx768m` in `gradle.properties`, add swap if possible, or use the GitHub Actions / PC fallback. Android's low-memory killer may kill Termux: acquire the wakelock (Termux notification) and keep it in foreground. |
| `SDK location not found` | Set `ANDROID_HOME`; script writes `local.properties`. |
| `Failed to install platform android-34` / licenses | `yes | sdkmanager --licenses`. |
| Kotlin/Compose compiler mismatch | Keep Kotlin **2.0.21** with plugin `org.jetbrains.kotlin.plugin.compose` 2.0.21. |
| Compile error in a source file | The project was never compiled by its author; open an issue-style note with the error line and fix the reference/import. |
| App: speech does nothing | Needs Google speech service; check `Settings → Apps → Default apps → Voice input`. Hindi may need internet or an offline pack. |
| App: "accessibility service not enabled" | Settings → Accessibility → TUNDER LUCKY → On. On Android 11 sideloaded apps normally allow this; some Vivo/Funtouch builds also need battery "high background power" allowed or the service gets killed. |
| Tap/scroll does nothing in an app | Some apps (banking, secure screens) block accessibility. YouTube/Chrome layouts change; label matching may fail. |
| Orb is black / crashes | Settings → enable **Low graphics mode** (Canvas orb, no OpenGL). |
