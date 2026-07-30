# DiveMeter

Android app for measuring and sharing the height of a dive (pool, lake, river). Spots are shared: everyone using the app sees the same map and can add their own dives.

## Features

- **Home map** — Google Maps view centered on your current position, showing every saved dive spot (from all users) with satellite/normal toggle.
- **Manual entry** — type a known height, pick the spot on the map (or use your current GPS position), add a description and a 1–5 star rating.
- **Video calculation** — pick a video of the dive, scrub to mark the apex of the trajectory and the moment of water entry, and the height is derived from free-fall physics: `h = ½·g·t²`. Videos stay on the device that recorded them and aren't uploaded.
- **Barometer** — locked, planned for a future update.
- **Dive detail** — tap any marker to see its data (height, method, description, rating, who added it and when). If you added it yourself, a delete button lets you remove it.
- **Nicknames** — on first launch you pick a nickname (editable later from Settings). Nicknames are globally unique and tied to a server-verified anonymous identity, not just a display string.

## Tech stack

- Kotlin + Jetpack Compose (Material 3), Navigation Compose
- Google Maps SDK for Android + Play Services Location (current-position GPS)
- Media3 ExoPlayer (video scrubbing for the height calculation)
- **Supabase** (Postgrest + anonymous Auth) as the shared backend — see `supabase/schema.sql` for the table/RLS setup
- kotlinx.serialization + Ktor (used internally by the Supabase client)

## Requirements

- Android Studio (bundles the JDK used to build this project)
- Android SDK, `compileSdk`/`targetSdk` 35, `minSdk` 26
- A Google Maps API key and a Supabase project (see below)

## Setup

Two things need to be configured locally before building, both kept out of git in `secrets.properties` (create this file at the repo root; `local.defaults.properties` shows the expected keys with placeholder values):

```properties
MAPS_API_KEY=your-google-maps-api-key
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-public-key
```

- **Maps API key**: enable "Maps SDK for Android" for the key in Google Cloud Console.
- **Supabase**: create a free project, run `supabase/schema.sql` in the SQL Editor (creates the `profiles` and `dives` tables with row-level security), and enable **Anonymous sign-ins** under Authentication → Sign In / Providers. Use the project's Settings → API page for the URL and anon/public key — never use the `service_role` key here.

## Build

```bash
./gradlew assembleDebug
```

## Install without building

Grab the latest APK from the [Releases page](https://github.com/nmorrione/DiveMeter/releases) and sideload it — no Supabase/Maps setup needed, it's already configured to talk to the shared backend.

## Status

Home map, manual entry, video-based height calculation, dive details with per-owner delete, nicknames, and the shared Supabase backend are all implemented. Barometer support is the next milestone.
