# Android Standby

A one-to-one take on **iOS StandBy** for Android. When your phone is charging, locked and resting on its side, it turns into a glanceable bedside or desk display: widgets, a photo frame, or a big clock, with Night Mode in the dark.

Built with Kotlin and Jetpack Compose. No Google Play Services, no API keys.

## What's in it

| iOS StandBy | Android Standby |
| --- | --- |
| Opens when the iPhone is charging, locked and in landscape | Same. A small background service watches the charger and accelerometer and opens StandBy over the lock screen |
| Leaves when you unplug or turn the phone upright | Same |
| Three views, swiped horizontally: **Widgets**, **Photos**, **Clock** | Same, and it remembers the last view |
| Two **Smart Stacks**, swiped vertically, with page dots | Same |
| Smart Rotate | Brings Music forward while something plays and Up Next before an event |
| Widgets | Clock, Digital Clock, Calendar, Up Next, Weather, Batteries, Alarm, World Clock, Music (with playback controls), Photos |
| Touch and hold to edit (Face ID first) | Touch and hold to edit; asks for your Android unlock first. Jiggle, remove (−), add (+) from a widget gallery, Smart Rotate toggle per stack |
| Photos view with the time in the corner; swipe through albums; hide albums | Same, with a slow Ken Burns drift and a tap to skip |
| Clock styles **Analog, Digital, World, Solar, Float** with per-style colors | Same five, each with its own palette and the bottom-right color button. World uses a dotted map lit by the real day/night line |
| Night Mode: red tint in low light | Same, driven by the ambient light sensor (falls back to 10 pm–6 am without one), and the screen dims |
| Always On | Keeps the screen on while charging |
| Notifications full screen; "Show Preview on Tap Only" | Same |
| Settings › StandBy | The app opens to an iOS-style Settings › StandBy page with a live preview |

It also works as the Android **screen saver** (Settings › Display › Screen saver › Android Standby), which lays itself out for portrait too.

Weather comes from [Open-Meteo](https://open-meteo.com) (free, no key). Without location access it uses the main city of your time zone.

## Install

Download `AndroidStandby-<version>.apk` from the [latest release](../../releases/latest) and open it on your phone (allow installing from your browser or file manager when asked). Android 8.0 or newer.

## Setup

Open **Android Standby** once. Then, to have StandBy appear on its own like on iPhone:

1. **Display Over Other Apps → On.** This is what lets Android open StandBy over the lock screen when you put the phone on its side to charge. (Android 14+ refuses full-screen notifications from apps that aren't alarms or calls, so this is the dependable route.)
2. **Unrestricted Battery Use → On**, so the system doesn't stop the background watcher.
3. Optional, for richer widgets: **Notifications & Music** (full-screen notifications and the Music widget), **Photos**, **Calendars**, **Location for Weather**.

Tap the preview or **Open StandBy Now** to try it any time.

### Using it

- Swipe left and right between Widgets, Photos and Clock.
- Swipe up and down on a stack, an album or a clock to change it.
- Touch and hold to customize. Tap **Done** when finished.
- Swipe up from the bottom edge, press Back, unplug, or turn the phone upright to leave.

## Build

Requires JDK 17+ and the Android SDK (compile SDK 37).

```bash
./gradlew assembleRelease
```

The release APK lands in `app/build/outputs/apk/release/`. It's signed with your local debug key so it installs straight away; set up a release keystore before publishing to a store.

## Project layout

```
app/src/main/java/com/gaales/standby/
├── StandByActivity.kt          Full-screen StandBy over the lock screen; exits on unplug/upright
├── MainActivity.kt             Settings › StandBy and permission setup
├── service/
│   ├── ChargingMonitorService  Charger + orientation watcher that opens StandBy
│   ├── StandByDreamService     StandBy as the Android screen saver
│   ├── StandByNotificationListener  Full-screen notifications and media sessions
│   └── BootReceiver
├── data/                       Settings, weather, calendar, media, photos, sun math, world map
└── ui/
    ├── StandByRoot.kt          The three views, Night Mode, burn-in shift, exit gesture
    ├── widgets/                Smart Stacks, widgets, gallery, edit mode
    ├── photos/                 Photos view
    ├── clocks/                 Analog, Digital, World, Solar, Float
    ├── overlays/               Notifications, charging badge
    └── settings/               iOS-style settings components
```

## Credits

- [Nunito](https://fonts.google.com/specimen/Nunito) by Vernon Adams et al., SIL Open Font License 1.1 (`licenses/Nunito-OFL.txt`), stands in for SF Pro Rounded.
- World map dots rasterized from [Natural Earth](https://www.naturalearthdata.com) 1:110m land polygons (public domain).
- Weather data by [Open-Meteo](https://open-meteo.com) (CC BY 4.0).

StandBy, iPhone and iOS are trademarks of Apple Inc. This project is not affiliated with Apple.
