# AnimationSwipeMemo

AnimationSwipeMemo is an Android MVP built with Kotlin and Jetpack Compose for short expressive memo creation.
It lets you type a short phrase on a paper-like card, preview text animation in real time, save or discard with gestures, and export the animation as a generated GIF instead of screen recording.

## Directory structure

```text
AnimationSwipeMemo/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/kazumaproject/animationswipememo/
│       │   ├── MainActivity.kt
│       │   ├── MemoSwipeApp.kt
│       │   ├── MemoSwipeApplication.kt
│       │   ├── data/
│       │   │   ├── export/
│       │   │   ├── local/
│       │   │   ├── preferences/
│       │   │   └── repository/
│       │   ├── di/
│       │   ├── domain/
│       │   │   ├── animation/
│       │   │   ├── export/
│       │   │   └── model/
│       │   └── ui/
│       │       ├── components/
│       │       ├── editor/
│       │       ├── list/
│       │       ├── navigation/
│       │       ├── settings/
│       │       └── theme/
│       └── res/
│           ├── values/
│           └── xml/
├── build.gradle.kts
└── gradle/libs.versions.toml
```

## Implementation policy

- Architecture: MVVM with `ui / domain / data` separation.
- Persistence: Room for saved memos, DataStore Preferences for app settings.
- Navigation: Compose Navigation with Editor, Memo List, and Settings.
- Animation model: one shared animation engine drives both Compose preview and GIF frame rendering.
- Export: GIF frames are generated from the animation definition and saved with MediaStore on Android 10+.

## Implemented features

- Paper memo editor with real-time preview
- Animation presets: Fade, Typewriter, Float, Shake, Bounce, Glow
- Gesture actions: save, discard, export GIF, none
- Configurable gesture mapping
- Saved memo list with re-edit flow
- Settings for default animation and GIF quality
- GIF export built from rendered frames, not screen capture

## Setup

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an Android device or emulator with API 24+.

## GIF library choice

The app uses `com.squareup:gifencoder` because it is lightweight and fits the MVP need well:

- no video recording pipeline is needed
- frames can be fed directly from generated bitmaps
- the export implementation stays isolated behind `AnimationExporter`, which keeps future MP4 support easy to add

## Main user flow

1. Create or edit a short memo on the Editor screen.
2. Preview the selected text animation in real time.
3. Use gestures:
   - swipe right: save by default
   - swipe left: export GIF by default
   - pull down: discard by default
4. Browse saved memos in the list screen and tap one to re-edit it.
5. Update gesture mapping, default animation, and GIF quality from settings.

## Future extension ideas

- more paper themes and typography presets
- per-character animation composition
- MP4 / WebP export via another exporter implementation
- share sheet integration after export
- richer text style editing
- tags or collections for saved animated memos
