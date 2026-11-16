# Permissions & Privacy
| Permission | Why | When |
|---|---|---|
| `RECORD_AUDIO` | Push-to-talk speech recognition. Recording happens only while the mic button shows "Listening". | Asked on first mic tap |
| `INTERNET` | Cloud AI (your endpoint only) and downloads. | Install time |
| `ACCESS_NETWORK_STATE` | Show Wi-Fi/mobile/offline status. | Install time |
| Accessibility service (not a runtime permission) | Tap, scroll, type, read visible labels. **You** enable it in Android Settings, and the app additionally requires its own Control switch to be ON. | Manual |
No storage permission: downloads use Android DownloadManager (Downloads folder); code files use the system file picker. No camera/contacts/SMS/location permissions. The flashlight uses `CameraManager.setTorchMode`, which needs none.

## What the app does and doesn't do
- No hidden monitoring; no always-on listening; no foreground/background microphone service.
- Accessibility events are ignored (`onAccessibilityEvent` is empty). Screen content is read only when you say "read screen" and is shown locally.
- Password/OTP/PIN/CAPTCHA/CVV fields are never typed into or tapped; "send/post/buy/pay/delete/submit/confirm…" taps and all downloads require an explicit confirmation dialog.
- STOP button (top bar when control is on, and System tab) turns control off immediately. Saying "stop" also works.
- Cloud AI sends only the text of your conversation (last 20 messages) to the HTTPS endpoint you configure. Nothing else is uploaded.
- API key: encrypted with an Android Keystore AES-GCM key. A client-side key can never be fully hidden from someone controlling the device.
- Conversation history is stored locally (SharedPreferences, app-private) only if enabled; clear it any time. Backups are disabled.
- Activity history lists commands and results, locally, clearable.
