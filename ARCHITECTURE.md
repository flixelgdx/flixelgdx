# Project Architecture and Structure

FlixelGDX is organized into multiple Gradle modules to separate the core framework logic from the platform-specific backends.

## Modules

The project is split into several modules, each serving a specific purpose:

- **`flixelgdx-core`**: The heart of FlixelGDX. It contains the base framework classes (`FlixelGame`, `FlixelSprite`, `FlixelState`, etc.) and logic that is platform-independent. Every platform in the entire framework depends on this.
- **`flixelgdx-desktop`**: The primary desktop backend using the third release of the **[Lightweight Java Game Library](https://www.lwjgl.org/)**.
- **`flixelgdx-html5`**: The backend for the web using [TeaVM](https://teavm.org) to transpile Java bytecode to JavaScript or WebAssembly, allowing games to run seamlessly in a browser.
- **`flixelgdx-android`**: The backend for Android mobile devices.
- **`flixelgdx-ios`**: Planned backend for iOS mobile devices. Not supported yet. Currently only fail-fasts when attempted to be used.
- **`flixelgdx-jvm`**: JVM-only helpers that are not suitable for the browser or other non-JVM targets (stack traces, optional log files, etc.).
- **`flixelgdx-basisu-plugin`**: Compression plugin that automatically downloads a Basis Universal binary for the current OS and applies `.ktx2` compression for every `.png` asset.
- **`flixelgdx-teavm-plugin`**: Plugin that automates the workflow for web games. This includes copying assets, creating the HTML index file, extracting native scripts, and more.
- **`flixelgdx-logging-plugin`**: Plugin that runs after `compile*` and rewrites `FlixelLogger` and **`Flixel`** static `info(...)` / `warn(...)` / `error(...)` / `debug(...)` calls to injected hooks / `*WithSite` overloads so logs show accurate file and line without relying on stack walking (essential on the web and helpful on the JVM).
- **`flixelgdx-json-processor`**: Annotation processor for the framework's JSON annotation `@JsonSeralizable`.
- **`flixelgdx-test`**: **Test-only** module. Holds JUnit tests for `flixelgdx-core` (tweens, utilities, signals, etc.). It is not published to Maven; run `./gradlew :flixelgdx-test:test` locally and in CI.

## Build System

FlixelGDX uses **Gradle** with **Kotlin DSL** as its build system. 

### Key Files and Folders

- **`build.gradle.kts`**: The root aggregator where the build system enters. It only registers the `javadocsAll` task, nothing else.
- **`settings.gradle.kts`**: Defines all the modules included in the project. Note that `flixelgdx-android` is excluded by default unless
  `includeAndroid` is set to true in a `local.properties` file or passed as a command argument in the terminal via `PincludeAndroid=true`.
  Refer to the [COMPILING.md](COMPILING.md) file for more information.
- **`gradle.properties`**: Contains JVM settings for the build process, Maven publishing details and various other properties.
- **`build-logic/`**: Where the build system's main logic lies. It contains various build scripts for systems like Maven Central publishing and more.

### Dependency Management

Dependencies are managed in the `build.gradle.kts` file of each module. We use `api` and `implementation` configurations to control which dependencies are exposed to downstream projects.

## GitHub Integration

FlixelGDX's codebase has multiple GitHub configurations and templates, which can be found inside of [`.github/`](./.github/).
It holds the issue and pull request templates, Dependabot configurations, workflows, and more.
