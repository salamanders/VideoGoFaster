# VideoGoFaster

VideoGoFaster is a specialized Android application designed to speed up videos (2x, 4x, 8x) while maintaining exceptional visual quality. Unlike standard tools that simply drop frames to achieve higher speeds, VideoGoFaster utilizes a **frame blending** technique to create a smooth, cinematic motion blur effect.

## What makes this cool? (The Technical Focus)

Most video speed-up tools operate by decimating the stream: to get 2x speed, they drop every other frame. This results in "choppy" motion, especially noticeable in panning shots or high-action scenes.

**VideoGoFaster takes a different approach:**
1.  **Frame Blending (Accumulation):** Instead of discarding frames, it averages them using the `tmix` filter. For 4x speed, it blends 4 consecutive frames into one. This preserves the "light energy" of the original scene, resulting in natural motion blur.
2.  **10-bit Precision Pipeline:** Blending frames (e.g., averaging pixel values) in standard 8-bit color space often leads to rounding errors, visible as "banding" or "posterization" in smooth gradients. This project solves that by promoting the video stream to **10-bit color (`yuv420p10le`)** *before* any processing occurs. This provides enough mathematical headroom to blend frames cleanly before encoding back to the efficient HEVC format.

## Tech Stack

This project is built with a modern, "clean architecture" approach on the Android platform.

*   **Language:** [Kotlin 2.3.0](https://kotlinlang.org/) (targeting Java 21)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Media Engine:** [FFmpegKit](https://github.com/arthenica/ffmpeg-kit) (Full GPL variant)
*   **Codec:** `libx265` (High Efficiency Video Coding)
*   **Concurrency:** Kotlin Coroutines (`Dispatchers.IO`)
*   **Build System:** Gradle 9.x with Version Catalogs (`libs.versions.toml`)

## Under the Hood

The core logic resides in `VideoProcessor.kt`, which constructs a complex FFmpeg filter chain dynamically.

A typical 4x speed pipeline looks like this:

```
format=pix_fmts=yuv420p10le,  <-- Promote to 10-bit immediately
tmix=frames=4,                <-- Blend 4 frames together
select='not(mod(n\,4))',      <-- Keep only the blended frames
setpts=0.25*PTS               <-- Retime the remaining frames
```

All processing happens locally on the device.

## Documentation Links

*   [GEMINI.md](GEMINI.md) - Full Product Specification & Implementation Details.
*   [AGENTS.md](AGENTS.md) - Coding Standards & Contribution Guidelines.
