# DiveMeter

Android app to measure the height of a dive (pool, lake, river) using multiple methods:

1. **Video calculation** (in progress) — time the free fall between take-off and water entry from a video, then derive height from `h = ½·g·t²`.
2. **Manual entry** (available) — type in a known height and save it with a location on the map.
3. **Barometer** (locked for now) — altitude-difference measurement using the phone's pressure sensor, for devices that have one.

All saved dives are stored locally (Room) and shown on a map on the home screen, with search by spot name.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Navigation Compose
- Room (local persistence)
- osmdroid (OpenStreetMap, no API key required)
- CameraX / Media3 (planned, for the video method)

## Requirements

- Android Studio (bundles the JDK used to build this project)
- Android SDK, `compileSdk`/`targetSdk` 36, `minSdk` 26
- A physical device is recommended over an emulator once camera/video features land (Phase 1+), since emulator cameras aren't representative

## Build

```bash
./gradlew assembleDebug
```

## Status

Home screen (map + search + add-dive flow) and manual entry are implemented. Video-based calculation and barometer support are the next milestones.
