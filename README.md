# 🚗 Opel ELM327 Diagnostic (Torque-style)

Android OBD-II application for Opel vehicles using **ELM327 Bluetooth adapter**.  
Inspired by **Torque** and **CarPulse**, written in **Kotlin + Jetpack Compose**.

---

## ✨ Features

- 🔵 Bluetooth ELM327 (real device)
- 🔍 Auto ECU detect (CAN / ISO / Opel)
- 📊 Live gauges (RPM / Speed / Boost)
- 🧭 Torque-style dashboard (dark / night theme)
- 📡 CAN RAW frames (ID + DATA live)
- ❌ Read & Clear DTC
- 📝 CSV logging (Torque-compatible)
- 🔄 Auto reconnect
- 📈 Smoothed needle physics
- 🧮 Trip & fuel calculations

---

## 📁 Project Architecture

opel-elm/
├── README.md
├── gradlew
├── gradlew.bat
├── settings.gradle
├── build.gradle
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
└── app/
    ├── build.gradle
    ├── gradle.properties
    ├── proguard-rules.pro
    ├── opel-release.jks
    │
    └── src/
        └── main/
            ├── AndroidManifest.xml
            │
            ├── java/
            │   └── com/
            │       └── granitaistore/
            │           └── obddiagnostic/
            │
            │               ├── MainActivity.kt
            │               ├── MainNavHost.kt
            │
            │               ├── obd/
            │               │   ├── ElmConnection.kt
            │               │   ├── EcuDetector.kt
            │               │   ├── ObdPid.kt
            │               │   ├── ObdParser.kt
            │               │   └── CanRawReader.kt
            │
            │               ├── logging/
            │               │   ├── CsvLogger.kt
            │               │   └── TripComputer.kt
            │
            │               ├── ui/
            │               │   ├── screen/
            │               │   │   ├── ScanScreen.kt
            │               │   │   ├── DashboardScreen.kt
            │               │   │   ├── CanRawScreen.kt
            │               │   │   └── SettingsScreen.kt
            │               │   │
            │               │   ├── gauge/
            │               │   │   ├── RpmGauge.kt
            │               │   │   ├── SpeedGauge.kt
            │               │   │   └── BoostGauge.kt
            │               │   │
            │               │   └── theme/
            │               │       ├── Theme.kt
            │               │       ├── Color.kt
            │               │       └── Type.kt
            │
            │               └── util/
            │                   ├── PermissionUtil.kt
            │                   └── TimeUtil.kt
            │
            └── res/
                ├── drawable/
                │   ├── gauge_boost_ticks.xml
                │   ├── needle_boost.xml
                │   └── ic_launcher.xml
                │
                ├── values/
                │   ├── colors.xml
                │   ├── strings.xml
                │   └── themes.xml
                │
                └── mipmap/



---

## 🧠 ECU Detection

Supported:
- CAN 11bit / 500k
- CAN 11bit / 250k
- ISO 9141
- KWP2000
- Opel BCM / ABS / TCM headers

---

## 📡 CAN RAW

Displays live:
ID: 7E8 | DATA: 03 41 0C 1A F8


---

## 📝 CSV Log (Torque-compatible)

Example:


timestamp,rpm,speed,boost
2026-01-08 12:01:01,820,90,0.42


Can be opened in:
- Excel
- LibreOffice
- Torque Pro

---

## 🛠 Build

```bash
./gradlew assembleDebug

🔒 Permissions

BLUETOOTH

BLUETOOTH_CONNECT (Android 12+)

BLUETOOTH_SCAN

ACCESS_FINE_LOCATION

📌 Roadmap

 Graphs

 PID editor

 Opel proprietary PIDs

 ECU flashing (future)

 Cloud sync

📜 License

MIT (for now)

🚀 Made for Opel owners & car hackers