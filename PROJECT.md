# Project Structure

FlixelGDX is organized into multiple Gradle modules to separate the core framework logic from the platform-specific backends.

## Modules

The project is split into several modules, each serving a specific purpose:

- **`flixelgdx-core`**: This is the heart of FlixelGDX. It contains the base framework classes (`FlixelGame`, `FlixelSprite`, `FlixelState`, etc.) and logic that is platform-independent.
- **`flixelgdx-jvm`**: Contains common logic for all JVM-based backends (Desktop, Android, etc.). This is primarily an internal module; most games will not depend on it directly.
- **`flixelgdx-lwjgl3`**: The primary desktop backend using the [Lightweight Java Game Library](https://www.lwjgl.org/). When you create a desktop launcher with FlixelGDX, this is the module that provides the actual `Lwjgl3Application`.
- **`flixelgdx-android`**: The backend for Android devices. This integrates FlixelGDX with libGDX's Android launcher and lifecycle.
- **`flixelgdx-ios`**: The backend for iOS using [MobiVM](https://github.com/MobiVM/robovm) (a maintained fork of RoboVM).
- **`flixelgdx-teavm`**: The backend for the web using TeaVM to transpile Java to JavaScript.

### Which module should my game depend on?

When you use FlixelGDX **as a library inside another libGDX project**, you almost always:

- add a dependency on **`me.stringdotjar.flixelgdx:flixelgdx-core`** to your own `core` module, and
- keep using your existing platform launchers (desktop, Android, etc.).

When you are building a project that is structured like FlixelGDX itself (multi-module with shared `core` and distinct backends), you can:

- depend on `flixelgdx-core` from your game logic,
- depend on the appropriate backend modules (`flixelgdx-lwjgl3`, `flixelgdx-android`, `flixelgdx-ios`, `flixelgdx-teavm`) in your platform-specific launchers.

For more info on how to wire FlixelGDX into a new libGDX project, see the [COMPILING.md](COMPILING.md) document.

## Build System

FlixelGDX uses **Gradle** as its build system. 

### Key Files

- **`build.gradle`**: The root build file that configures all projects, common repositories, and shared dependencies.
- **`settings.gradle`**: Defines all the modules included in the project.
- **`gradle.properties`**: Contains version numbers for libGDX and other dependencies, as well as JVM settings for the build process.

### Dependency Management

Dependencies are managed in the `build.gradle` file of each module. We use `api` and `implementation` configurations to control which dependencies are exposed to downstream projects. 

For example, the `flixelgdx-core` module uses `api` for libGDX, which means any project using FlixelGDX will also have access to the underlying libGDX classes.

When you publish FlixelGDX to your local Maven repository (see `TESTING.md`), other projects can simply add:

```gradle
dependencies {
    implementation "me.stringdotjar.flixelgdx:core:<version>"
}
```

to their own `core` module, and Gradle will transitively pull in the libGDX APIs used by FlixelGDX.

## Architecture

We aim to replicate the HaxeFlixel API as closely as possible while using libGDX idioms. 

- **Lifecycle Methods**: We use `update(float elapsed)`, `draw(Batch batch)`, and `destroy()` throughout the framework.
- **Strict Typing**: As this is a Java project, we ensure all methods and fields are strictly typed to provide a better developer experience.
- **Modularity**: The separation of backends allows the core logic to remain clean and portable across different platforms.

In practice this means:

- Game code lives in your `core` module alongside FlixelGDX's `flixelgdx-core` API.
- Platform launchers are thin wrappers that delegate to `FlixelGame` (or your subclass) while reusing libGDX's existing bootstrapping.

For concrete examples of how to wire FlixelGDX into a new libGDX project, see the launcher and `FlixelGame` samples in the main `README.md`, and the Maven/Gradle setup in `TESTING.md`.
