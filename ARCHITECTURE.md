# Project Architecture and Structure

FlixelGDX is organized into multiple Gradle modules to separate the core framework logic from the platform-specific backends.

## Modules

The project is split into several modules, each serving a specific purpose.

### Base (Core + Platform Backends)

- **`flixelgdx-core`**: The heart of FlixelGDX. It contains the base framework classes (`FlixelGame`, `FlixelSprite`, `FlixelState`, etc.) and logic that is platform-independent. Every platform in the entire framework depends on this.
- **`flixelgdx-gpu`**: The one WebGPU renderer every platform shares. It implements core's graphics abstractions (sprite batch, textures, render targets, shaders, the global shader chain) on top of `jwebgpu-api` only, so it contains no desktop, Android or web types. Each platform supplies a jwebgpu backend, a surface for its window and a device, and hands them to `FlixelGpuGraphics`.
- **`flixelgdx-desktop`**: The desktop backend using the third release of the **[Lightweight Java Game Library](https://www.lwjgl.org/)** (SDL3 for windows and input, miniaudio for sound) and `flixelgdx-gpu` for drawing. It installs jwebgpu's FFM backend, which loads the Dawn library of the current platform, and creates the WebGPU surface from the SDL3 window's native handles (X11, Wayland, Win32 or a Metal layer).
- **`flixelgdx-html5`**: The backend for the web using [TeaVM](https://teavm.org) to transpile Java bytecode to JavaScript or WebAssembly, allowing games to run seamlessly in a browser.
- **`flixelgdx-android`**: The backend for Android mobile devices.
- **`flixelgdx-ios`**: Planned backend for iOS mobile devices. Not supported yet. Currently only fail-fasts when attempted to be used.

### Plugins (inside [`flixelgdx-plugins`](flixelgdx-plugins/))

- **`flixelgdx-basisu-plugin`**: Bundles Basis Universal binaries for each OS and applies `.ktx2` compression for every `.png` asset on mobile.
- **`flixelgdx-html5-plugin`**: Automates the workflow for web games. This includes copying assets and generating the HTML index file that boots the WebAssembly or JavaScript bundle, and more.
- **`flixelgdx-packagr-plugin`**: Automates deployment and packaging for desktop games, with default options for Windows, macOS and Linux on all architectures.
- **`flixelgdx-shader-plugin`**: Build-time shader tooling. Games load WGSL shaders directly at run time, so the plugin is no longer needed to make a shader run on the desktop.

### Rendering and shaders

Everything is drawn with [WebGPU](https://www.w3.org/TR/webgpu/) through [jwebgpu](https://github.com/stringdotjar/jwebgpu), the project's Java WebGPU bindings. jwebgpu is consumed as a Gradle composite build until it is published, so a checkout of it must sit next to FlixelGDX (see [COMPILING.md](COMPILING.md)).

Shaders are written in **WGSL** only. A shader is one `.wgsl` file, and the framework puts a *preamble* (`flixelgdx-core/src/main/resources/org/flixelgdx/shaders/flixel_preamble.wgsl`, the single source of truth for the contract) in front of it:

| What | Where | Notes |
|------|-------|-------|
| `FlixelGlobals` uniform `flixel` | `@group(0) @binding(0)` | `proj` (`mat4x4f`, world pixels to clip space), `texSize` and `texelSize` (`vec2f`). Bound with a dynamic offset. |
| Texture `flixelTexture` | `@group(0) @binding(1)` | `texture_2d<f32>` of the sprite or render target being drawn. |
| Sampler `flixelSampler` | `@group(0) @binding(2)` | Nearest or linear, following the texture's smooth setting. |
| The shader's own uniform struct | `@group(1) @binding(0)` | Optional. Members are set from Java by name with `FlixelShader.setUniform(...)`. |
| Vertex structs | `FlixelVertexInput`, `FlixelVertexOutput` | A vertex is a `vec2f` position (world pixels), a `vec2f` uv and a `vec4f` color. |
| Entry points | `fs_main` (required), `vs_main` (optional) | Without `vs_main` the preamble's `flixel_vs` is used. |

The renderer lays out the shader's uniform struct once, when the shader is created, with a small WGSL layout parser (`FlixelWgslLayout`), so `setUniform("name", value)` writes into a CPU copy at the right byte offset and the copy is uploaded at most once per change.

### Misc.

- **`flixelgdx-miniaudio`**: Holds the Java API and C source code for interacting with miniaudio. It doesn't hold any natives; other backends provide their own.
- **`flixelgdx-json-processor`**: Annotation processor for the framework's JSON annotation `@JsonSerializable`.
- **`flixelgdx-test`**: **Test-only** module. Holds JUnit tests for `flixelgdx-core` (tweens, utilities, signals, etc.). It is not published to Maven; run `./gradlew :flixelgdx-test:test` locally and in CI.

## Build System

FlixelGDX uses **Gradle** with **Kotlin DSL** as its build system. 

### Key Files and Folders

- **`build.gradle.kts`**: The root aggregator where the build system enters. It only registers the `javadocsAll` task, nothing else.
- **`settings.gradle.kts`**: Defines all the modules included in the project and includes the **jwebgpu** checkout as a composite build (`../jwebgpu` by default; override it with `-Pjwebgpu.dir=...`, `jwebgpu.dir` in `local.properties`, or the `JWEBGPU_DIR` environment variable). Note that `flixelgdx-android` is excluded by default unless
  `includeAndroid` is set to true in a `local.properties` file or passed as a command argument in the terminal via `PincludeAndroid=true`.
  Refer to the [COMPILING.md](COMPILING.md) file for more information.
- **`gradle.properties`**: Contains JVM settings for the build process, Maven publishing details and various other properties.
- **`build-logic/`**: Where the build system's main logic lies. It contains various build scripts for systems like Maven Central publishing and more.

### Dependency Management

Dependencies are managed in the `build.gradle.kts` file of each module. We use `api` and `implementation` configurations to control which dependencies are exposed to downstream projects.

## GitHub Integration

FlixelGDX's codebase has multiple GitHub configurations and templates, which can be found inside of [`.github/`](.github).
It holds the issue and pull request templates, Dependabot configurations, workflows, and more.
