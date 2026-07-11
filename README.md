# LiteCut Android

LiteCut is an Android-first lossless video editor prototype for long videos.

The core idea is deliberately pragmatic: for many long-video workflows, frame-perfect
cut boundaries are less important than export speed. Starting or ending one or two
seconds away from the exact finger position is usually acceptable, but waiting minutes
or hours for a re-encode is not.

LiteCut therefore plans edits around FFmpeg stream-copy operations:

- import videos through Android's system picker,
- persist read permission when Android allows it,
- copy media into an app-private workspace for native tooling,
- probe duration/keyframes,
- align export ranges to practical keyframe boundaries,
- export with `-c copy` instead of re-encoding.

## Current Features

- Android single-track timeline editor.
- Immediate preview playback through Media3/ExoPlayer with a TextureView-backed preview.
- Chinese UI for import, playback, split, duplicate, delete, reorder, zoom, and export.
- Scrollable and zoomable timeline.
- Sparse timeline thumbnails generated in the background.
- Timeline-to-source time mapping for split, duplicated, and reordered clips.
- Lossless export planning with FFmpegKit-compatible Android execution.

## Non-Goals

LiteCut is not trying to be a frame-accurate professional NLE.

It intentionally avoids:

- re-encoding by default,
- filters/effects/transitions,
- multi-track compositing,
- dense frame-by-frame thumbnails,
- waveform rendering.

The MVP optimizes for fast, good-enough editing of long source videos.

## Build

Required local toolchain:

- JDK 17
- Android SDK with API 36
- Gradle compatible with Android Gradle Plugin 9.2.0

Build debug APK:

```powershell
gradle :app:assembleDebug
```

Run unit tests:

```powershell
gradle :app:testDebugUnitTest
```

Run connected Android tests:

```powershell
gradle :app:connectedDebugAndroidTest
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Android Media Flow

```text
System picker URI
  -> persist read permission when Android allows it
  -> immediate URI preview
  -> copy to app-private workspace in background
  -> ffprobe / thumbnail generation against local file paths
  -> stream-copy export
  -> publish completed output
```

## Notes

This repository contains only the Android project code and project-facing docs.
Planning workflow artifacts from the development workspace are intentionally not
included.

