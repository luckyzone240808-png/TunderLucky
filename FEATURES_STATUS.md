# Feature status (code written; **NOT yet compiled or tested on a device**)
Legend: ✅ implemented in code · 🔑 needs permission · ☁ needs API setup · ⚠ limited by Android/app layouts · ❌ not implemented

| Feature | Status | Notes |
|---|---|---|
| Cinematic splash (particles, GL orb, waveform, text) | ✅ | Orb is a fragment-shader (OpenGL ES 2.0) sphere; waveform animation is decorative idle motion on the splash |
| Dashboard: orb, mic, status, chat, quick actions, history | ✅ | Orb size reacts to mic level while listening; while speaking it pulses (visual, not real TTS audio level) |
| Low graphics mode | ✅ | Canvas orb, no OpenGL |
| Push-to-talk STT, TTS, states | ✅🔑 | Mic permission; recognizer quality/Hindi offline depends on device's speech service |
| Hindi / Hinglish / English | ⚠ | Recognition language: English(India) or Hindi; basic Hinglish keyword commands; TTS uses Hindi voice for Devanagari text |
| Local commands: open apps, YouTube, Chrome, settings pages, time, date, battery, torch | ✅ | Torch: devices with flash |
| Accessibility control: tap, scroll, swipe gesture, type, back, home, read screen | ✅🔑⚠ | Needs manual enable + Control switch; fails gracefully if labels not found |
| Safety: no password/OTP, confirmations, STOP | ✅ | Heuristic matching—may miss unusual labels |
| YouTube search/open/pause/resume/next/first result | ✅⚠ | Search via deep link; playback controls via accessibility label matching, may break with app updates |
| Chrome open/URL/search/scroll/back/tap | ✅⚠ | Same caveat; "copy selected text" ❌, "select visible links" only via "tap <link text>" |
| Download manager (progress, size, status, open) | ✅ | Saves to Downloads; custom folder via SAF ❌ |
| Cloud AI chat (OpenAI-compatible, streaming, errors, status) | ☁ | Untested against any real provider |
| Keystore-encrypted API key | ✅ | |
| Coding workspace (editor, highlight, ask/explain/debug, copy, save, open) | ✅☁ | AI part needs API; highlighting is regex-based; never executes code |
| System dashboard + settings shortcuts | ✅ | Opens settings pages only; cannot toggle Wi-Fi/Bluetooth directly |
| Local history + clear, activity log | ✅ | SharedPreferences (not Room) to keep dependencies minimal |
| Always-on wake word ("Hey TUNDER") | ❌ | Not implemented; push-to-talk only |
| Foreground microphone service | ❌ | Not needed |
| Unit/instrumented tests | ❌ | None written |
| Verified build / APK | ❌ | Not built in the authoring environment |
