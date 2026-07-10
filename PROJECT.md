# Project Structure

FlixelGDX is organized into multiple Gradle modules to separate the core framework logic from the platform-specific backends.

## Modules

The project is split into several modules, each serving a specific purpose:

- **`flixelgdx-core`**: The heart of FlixelGDX. It contains the base framework classes (`FlixelGame`, `FlixelSprite`, `FlixelState`, etc.) and logic that is platform-independent. Every module in the entire framework depends on this one.
- **`flixelgdx-common`**: Shared code that multiple JVM-family backends can use without pulling in LWJGL3 or TeaVM. This includes the MiniAudio-based sound handler used by desktop, Android, and iOS launchers.
- **`flixelgdx-lwjgl3`**: The primary desktop backend using the third release of the **[Lightweight Java Game Library](https://www.lwjgl.org/)**. When you create a desktop launcher with FlixelGDX, this is the module that provides the actual `Lwjgl3Application`.
- **`flixelgdx-android`**: The backend for Android devices. This integrates FlixelGDX with libGDX's Android launcher and lifecycle.
- **`flixelgdx-ios`**: The backend for iOS using [MobiVM](https://github.com/MobiVM/robovm) (a maintained fork of RoboVM). Not supported yet.
- **`flixelgdx-teavm`**: The backend for the web using TeaVM to transpile Java bytecode to JavaScript, allowing games to run seamlessly in a browser.
- **`flixelgdx-basisu-plugin`**: Compression plugin that automatically downloads a Basis Universal binary for the current OS and applies `.ktx2` compression for every `.png` asset.
- **`flixelgdx-teavm-plugin`**: Plugin that automates the workflow for web games. This includes copying assets, creating the HTML index file, extracting native scripts, and more.
- **`flixelgdx-logging-plugin`**: Plugin that runs after `compileJava` and rewrites `FlixelLogger` and **`Flixel`** static `info(...)` / `warn(...)` / `error(...)` calls to injected hooks / `*WithSite` overloads so logs show accurate file and line without relying on stack walking (essential on TeaVM and helpful on the JVM).
- **`flixelgdx-jvm`**: JVM-only helpers that are not suitable for TeaVM or other non-JVM targets (stack traces, optional log files, etc.). It depends on **`flixelgdx-common`** for shared native-friendly pieces such as MiniAudio.
- **`flixelgdx-video`**: Optional video playback extension (i.e., for cutscenes), split into per-platform modules so games only ship the decoder they need.
  - **`flixelgdx-video-core`**: The platform-agnostic API (`FlixelVideo`, `FlixelVideos`, `FlixelBaseVideo`, `FlixelVideoBackend`). Depends only on `flixelgdx-core`.
  - **`flixelgdx-video-lwjgl3`**: Desktop backend powered by [libvlc](https://www.videolan.org/vlc/libvlc.html).
  - **`flixelgdx-video-teavm`**: Web backend built on a hidden HTML video element, which the browser decodes and each frame is transferred GPU-to-GPU with `texImage2D`.
  - **`flixelgdx-video-android`**: Android backend that decodes with the platform `MediaPlayer` into a `SurfaceTexture` bound to a `GL_TEXTURE_EXTERNAL_OES` texture, then blits each frame into a normal framebuffer texture so videos draw through the regular batch and follow state draw order (added first draws under, added last draws over).
- **`flixelgdx-test`**: **Test-only** module. Holds JUnit tests for `flixelgdx-core` (tweens, utilities, signals, etc.). It is not published to Maven; run `./gradlew :flixelgdx-test:test` locally and in CI.

## Build System

FlixelGDX uses **Gradle** as its build system. 

### Key Files

- **`build.gradle`**: The root aggregator where the build system enters. It triggers other task-specific scripts inside of [`gradle/`](./gradle).
- **`settings.gradle`**: Defines all the modules included in the project.
- **`gradle.properties`**: Contains version numbers for libGDX and other dependencies, as well as JVM settings for the build process.

### Dependency Management

Dependencies are managed in the `build.gradle` file of each module. We use `api` and `implementation` configurations to control which dependencies are exposed to downstream projects.
For example, the `flixelgdx-core` module uses `api` for libGDX, which means any project using FlixelGDX will also have access to the underlying libGDX classes.

## GitHub Integration

FlixelGDX's codebase has multiple GitHub configurations and templates, which can be found inside of [`.github/`](./.github).
It holds the issue and pull request templates, Dependabot configurations, workflows, and more.
