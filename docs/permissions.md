# SafeWord App Permissions Disclosure

SafeWord is built to prioritize user privacy and safety. The app performs all computations, database storage, and voice recognition directly on your device. It does not stream or upload any data to cloud servers.

To perform its emergency functions, Android requires the following permissions. Below is an explanation of why each is needed:

## 1. Microphone (`RECORD_AUDIO`)
*   **Why we need it**: SafeWord continuous monitoring relies on access to your microphone to capture audio buffers for offline voice trigger detection.
*   **Privacy Guarantee**: Audio processing occurs entirely in-memory using a local offline Vosk model. The app never records audio files to storage, nor does it stream any voice data over the internet.

## 2. Precise Location (`ACCESS_FINE_LOCATION`)
*   **Why we need it**: When you say the trigger word or tap the panic button, SafeWord queries the GPS to get your coordinates, formatting it into a Google Maps URL.
*   **Privacy Guarantee**: Your location is only read at the moment of an alert (or during active tracking mode) and is only shared with the contacts you pre-configure. It is saved in a local Room database on your device and is never sent to us.

## 3. Background Location (`ACCESS_BACKGROUND_LOCATION`)
*   **Why we need it**: Critical so the app can request location fixes when the phone screen is off or when the app is minimized. Without background location, the maps link sent during an emergency could be empty or inaccurate if the app isn't active in the foreground.

## 4. Send SMS Messages (`SEND_SMS`)
*   **Why we need it**: SMS is the most reliable fallback channel in emergencies because it does not depend on Wi-Fi or cellular mobile data. SafeWord uses `SmsManager` to directly text contacts with the Maps link.
*   **Security Guarantee**: SafeWord only sends SMS to the phone numbers you explicitly add inside the app Settings.

## 5. Notifications (`POST_NOTIFICATIONS`)
*   **Why we need it**: On Android 13 (API 33) and above, apps running foreground services must display a persistent notification to prevent the system from killing the background worker. This notification also serves as a visible indicator that SafeWord is actively protecting you.
