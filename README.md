# 🛡️ SafeWord — Voice-Triggered Emergency Location Alert (Android)

SafeWord is a personal safety Android application built in Kotlin. It runs as a foreground service, continuously listening for custom voice triggers fully offline. When a trigger is detected, the app retrieves the current high-accuracy GPS coordinates, dispatches an SMS message containing a live Google Maps link to pre-configured emergency contacts, begins continuous movement tracking, and records the incident history locally.

## Features
- **Offline On-Device Word Spotting**: Uses the local **Vosk** engine for battery-efficient keyword spotting without sending audio data to the cloud.
- **Dual location Fetch**: Combines last-known location coordinates with active GPS location requests to guarantee high-accuracy maps linking.
- **SMS Failover Channel**: Sends emergency alerts using standard SMS text messages, meaning it functions without cellular mobile data or active Wi-Fi.
- **Post-Trigger Tracking**: Periodic foreground location updates text contacts your location every X seconds for N minutes, enabling real-time movement monitoring.
- **Fallback Panic Widget**: Instant physical widget button for home screens and quick notification actions.
- **Room Database Storage**: Local-only SQLite records of emergency contacts and safety logs.

---

## 📁 Project Structure
```
safeword-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/safeword/app/
│   │   │   ├── MainActivity.kt              # Onboarding & settings navigation
│   │   │   ├── service/
│   │   │   │   ├── ListeningService.kt       # Background mic monitoring controller
│   │   │   │   ├── KeywordDetector.kt        # Vosk SDK recognizer wrapper
│   │   │   │   └── TrackingService.kt        # Periodic post-trigger SMS pinger
│   │   │   ├── location/
│   │   │   │   └── LocationHelper.kt         # FusedLocationProviderClient wrapper
│   │   │   ├── alert/
│   │   │   │   ├── SmsSender.kt              # SmsManager messaging formatter
│   │   │   │   └── AlertManager.kt           # Coordinates service-to-alert tasks
│   │   │   ├── data/
│   │   │   │   ├── Contact.kt                # Emergency contacts entity
│   │   │   │   ├── ContactDao.kt
│   │   │   │   ├── IncidentLog.kt            # Incident logs entity
│   │   │   │   ├── IncidentLogDao.kt
│   │   │   │   └── AppDatabase.kt            # Room DB coordinator
│   │   │   ├── ui/
│   │   │   │   ├── onboarding/               # Permissions request & disclosures
│   │   │   │   ├── settings/                 # Contact CRUD, triggers & settings UI
│   │   │   │   ├── history/                  # Incidents viewer screen
│   │   │   │   └── theme/                    # Color and theme configurations
│   │   │   └── widget/
│   │   │       └── PanicButtonWidget.kt      # Remote widget provider receiver
│   │   ├── res/                              # Layouts, XML widget metadata
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── models/
│   └── vosk-model-small-en/                  # Target folder for Vosk model assets
├── docs/
│   └── permissions.md                        # Plain-language explanation of permissions
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## ⚙️ Setup and Installation

### 1. Clone and Open
Open Android Studio, select **File -> Open** and choose the `safeword-app` folder.

### 2. Download Offline Voice Model
Vosk speech recognition relies on a pre-trained offline speech model.
1. Download the lightweight English model (`vosk-model-small-en-us-0.15`) from [Alpha Cephei Models](https://alphacephei.com/vosk/models).
2. Unzip the downloaded model archive.
3. Rename the directory to `vosk-model-small-en`.
4. Copy the directory content into your project's assets: `safeword-app/app/src/main/assets/vosk-model-small-en/` (create the `assets` folder under `src/main/` if it does not exist yet).
*Note: Make sure the model folder directly contains files like `am/`, `graph/`, `conf/` and `uuid`.*

### 3. Build & Run
Connect an Android device or emulator (API 26+) and run from Android Studio or command-line:

```bash
./gradlew installDebug
```

---

## 🛡️ Reliability & Customizations

### Battery Optimization Whitelist
Many Android devices (especially manufacturers like Xiaomi, Huawei, OnePlus, Samsung) employ strict battery-saver managers which kill foreground microphone services. 
SafeWord includes a settings button linking directly to **"Request Ignore Battery Optimization"**. Ensure this is checked to allow continuous background listening.

### SMS Format Link
Emergency SMS messages are formatted as follows:
```text
🚨 SafeWord Alert 🚨
[User's Name] may need help.
Location: https://maps.google.com/?q=[Latitude],[Longitude]
Time: [Formatted Timestamp]
This is an automated message.
```

---

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
