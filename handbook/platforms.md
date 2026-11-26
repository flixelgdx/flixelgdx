# Platform Guide: Desktop, Web, and Android

FlixelGDX's promise is "write once, run everywhere," and most of the time you never think about the platform. This
guide collects what is different on each platform anyway: requirements, how the game is launched and run, platform
limits, and the gotchas that only show up on one of them.

## Table of contents

1. [Platform overview](#platform-overview)
2. [Feature comparison](#feature-comparison)
3. [Desktop](#desktop)
4. [Web](#web)
5. [Android](#android)
6. [iOS](#ios)
7. [Writing code that works everywhere](#writing-code-that-works-everywhere)
8. [Testing on every platform](#testing-on-every-platform)
9. [Check your understanding](#check-your-understanding)

## Platform overview

| Platform | Module              | Runs on                                     | Requirements                                     |
|----------|---------------------|---------------------------------------------|--------------------------------------------------|
| Desktop  | `flixelgdx-desktop` | Windows, macOS, Linux (x64 and ARM64)       | Java 25, a GPU with Vulkan, Metal, or Direct3D 12 |
| Web      | `flixelgdx-html5`   | Browsers, as JavaScript or WebAssembly      | A browser with WebGPU                            |
| Android  | `flixelgdx-android` | Phones and tablets                          | Android 8.0 (API 26), Vulkan 1.1, 64-bit (`arm64-v8a` or `x86_64`) |
| iOS      | `flixelgdx-ios`     | Planned                                     | Not supported yet                                |

## Feature comparison

| Feature                          | Desktop        | Web                                  | Android                    |
|----------------------------------|----------------|--------------------------------------|----------------------------|
| Keyboard                         | Yes            | Yes                                  | Hardware keyboards         |
| Mouse                            | Yes            | Yes                                  | Rarely                     |
| Touch                            | Off by default | Taps arrive as clicks                | On by default              |
| Gamepads                         | Yes            | Yes                                  | Yes                        |
| Moving the pointer               | Yes            | No                                   | No                         |
| Window position, opacity, borders | Yes           | No                                   | No                         |
| Fullscreen                       | Yes            | After a click or key press           | Always                     |
| Quitting                         | `Flixel.quit()` | Up to the browser                   | Up to the system           |
| Saves                            | App data folder | `localStorage`                      | Private app storage        |
| Log files                        | Yes            | No                                   | Yes                        |
| Profiler sections                | Not yet        | Not yet                              | System trace               |
| Default camera scaling           | Letterbox (`FIT`) | Letterbox (`FIT`)                 | Fill the screen (`EXTEND`) |
| Compressed textures              | No             | No                                   | Yes, with the Basis Universal plugin |

## Desktop

### Running

```bash
./gradlew :desktop:run
```

```bash
./gradlew :desktop:debug
```

The second starts the game in debug mode (it passes `-Dflixel.mode=debug`).

### JVM settings

- **Native access.** The renderer and SDL use Java's foreign function API, so the JVM must start with
  `--enable-native-access=ALL-UNNAMED`. Generated run tasks, the game jar's manifest, and packaged games set it for
  you; if you launch the game another way and see a warning about restricted methods, add the flag.
- **Memory.** A run task may limit the heap with `-Xmx`, and that limit only applies to that task. Give packaged games
  their own limit in the packaging plugin (`jvmArg('-Xmx1G')`).
- **Graphics API.** WebGPU picks Vulkan, Metal, or Direct3D 12 by itself. For troubleshooting a driver problem, force
  one with `-Dflixel.render.backend=vulkan` (also `metal`, `direct3d11`, `direct3d12`, `opengl`, `opengles`, `noop`).
  Games can also force one from code with `Flixel.graphics.setApi(...)` in a boot callback (see
  [Graphics and Shaders](graphics-and-shaders.md#which-gpu-api-is-running)); the system property wins over it.

### Window icons

Window icons come from your **resources** folder (the classpath), not from `assets/`, listed from smallest to largest:

```java
FlixelDesktopLauncher.launch(new MyGame(), "icons/icon16.png", "icons/icon32.png", "icons/icon256.png");
```

A missing icon throws at startup, so you notice immediately.

### Shipping

Use the packagr Gradle plugin to build a self-contained folder per operating system with a native launcher and a
trimmed Java runtime, so players do not need Java installed. See [Building and Shipping](building-and-shipping.md).

## Web

### Running

```bash
./gradlew :html5:run
```

```bash
./gradlew :html5:debug
```

Both build the game, start a local server at `http://localhost:8080`, and open the browser. The second adds
`?flixel.mode=debug` to the address.

### WebGPU is required

The web backend draws with WebGPU, with no WebGL fallback. When a browser lacks WebGPU, the page shows a message that
names the browsers that support it, and the game does not start. Test in the browsers your players use.

### JavaScript and WebAssembly

TeaVM compiles your Java code to JavaScript, WebAssembly (with garbage collection support), or both. The generated page
prefers the WebAssembly build, which is faster, and falls back to JavaScript in browsers without WebAssembly GC. The
framework is tested with TeaVM 0.13.0, the version its Gradle files use; newer TeaVM versions have broken readable
stack traces in the WebAssembly build before, so stay on the tested version unless the framework moves.

### What Java can and cannot do in the browser

TeaVM translates your bytecode ahead of time, and the browser is a different world from a JVM:

- **One thread.** No background threads. Do slow work in small slices across frames.
- **No reflection.** That is one reason FlixelGDX uses generated code for JSON (`@JsonSerializable`).
- **No `java.net`, no file system.** Use `Flixel.files` (preloaded assets) and `Flixel.save` (`localStorage`).
- **A partial Java class library.** TeaVM implements most of `java.lang` and `java.util`, but not everything. If a web
  build fails with a missing class or method, use a FlixelGDX equivalent or simpler code.

### Assets are downloaded first

The HTML5 plugin writes a list of every asset, and the browser downloads all of them before the game starts. Large
assets make the initial load longer. Use the plugin's `excludes` option to leave out files the web build does not need.

### Browser rules that affect games

Browsers protect players from pages that act on their own, so some features only work in response to input:

- **Audio** starts after the first click, tap, or key press. The framework resumes audio automatically at that moment.
  A "click to start" title screen hides this nicely.
- **Fullscreen** and **file dialogs** need a click or key press. The framework waits for the next one if needed.
- **Pointer moving** and **window position** are not possible.

### Other web notes

- **Case-sensitive paths.** `Player.png` and `player.png` are different files. Keep names lowercase.
- **Saves live in `localStorage`**, which the player can clear and which private browsing may discard.
- **Touch** is not handled directly; taps reach the game as mouse clicks. Design touch UI to work with clicks.
- **Audio effects** that some desktop backends support are not available on the web.
- **Deploy** with `./gradlew :html5:package`, which builds a zip ready for any static web host.

## Android

### Running

```bash
./gradlew :android:installDebug
```

Debug builds start in debug mode automatically. To override, launch with an intent extra:

```bash
adb shell am start -n <package>/<activity> -e flixel.mode release
```

### Requirements

- Android 8.0 (API 26) or newer, with Vulkan 1.1.
- 64-bit ABIs only: `arm64-v8a` and `x86_64`.
- The framework adds the `INTERNET` permission, which debug builds use to reach the debugger app.

### How the game runs

The game runs on a dedicated render thread called `flixel-render`, which owns the GPU. Android's UI thread only
forwards surface and activity events to it. Your game code still runs on one thread (the render thread), exactly like
on desktop.

When the player switches away, the game gets `onFocusLost()` and `onSuspended()`; save progress there (see
[Saving Progress](saving.md)). Android may then stop the process without warning.

### Screens of every shape

Phones come in many aspect ratios, so Android uses the `EXTEND` viewport by default: instead of black bars, the
camera shows more of the world on wider screens. Place HUD elements relative to `Flixel.getVisibleWidth()` and
`getVisibleHeight()` rather than the design size. Use `Flixel.host.getDensity()` to size touch buttons for the
screen's physical size.

### Touch and the back button

Touch input is on by default. The system back button arrives as `FlixelKey.BACK`:

```java
if (Flixel.keys.justPressed(FlixelKey.BACK)) {
  openSubState(new PauseSubState());
}
```

### Performance and textures

Phones are slower and throttle when hot, so leave headroom. The Basis Universal plugin compresses PNGs at build time
and keeps textures compressed in GPU memory, which can cut texture memory several times over; your code keeps asking
for `.png` files. Lower `Flixel.graphics.setRenderScale(...)` for weaker devices. See [Optimization](optimization.md).

### Java APIs on Android

Android's runtime (ART) supports Java 17's library, with newer *syntax* (records, pattern matching, `_`) translated
for it. Code that runs on Android must not use library APIs newer than that, such as virtual threads,
`java.lang.foreign`, `ScopedValue`, `StructuredTaskScope`, `getFirst()`/`getLast()`/`reversed()` on collections,
`Math.clamp`, `StringBuilder.repeat`, `HexFormat`, `Thread.threadId()`, or `Locale.of`. They compile fine and then crash
on the device. Prefer FlixelGDX helpers such as `FlixelMath.clamp(...)`.

### Debugging

Forward the debugger port with `adb reverse tcp:6510 tcp:6510` (or the debugger app's **Connect Android device**
button), read logs with `adb logcat`, and see `ANDROID_DEBUGGING.md` in the repository root for Android Studio,
profiling, and native crash stacks. See also [Debugging](debugging.md#debugging-on-android).

## iOS

An iOS backend is planned but not supported yet. The module currently fails fast with a clear error if used.

## Writing code that works everywhere

1. **Use the framework's services**, never platform APIs, in shared code.
2. **Ask before using optional features:** `Flixel.window.supportsFullscreen()`, `Flixel.host.supportsClipboard()`,
   `Flixel.haptics.isSupported()`. See [Window and Platform Services](window-and-platform-services.md).
3. **Save early and often**, because the web and phones rarely shut down cleanly.
4. **Keep asset names lowercase**, with forward slashes.
5. **Avoid reflection, threads, and `java.net`** in shared code, so the web build works.
6. **Avoid Java library APIs newer than Java 17**, so the Android build works.
7. **Design for different screen shapes:** use the visible size for edge-anchored UI.
8. **Offer every input method your platforms have**: keyboard, gamepad, and touch. [Input Actions](input-actions.md)
   make that easy.

## Testing on every platform

A feature that works on desktop is not done until it is tried on the other platforms you ship. A good rhythm is
developing on desktop (fastest to iterate), checking the web build regularly (it catches reflection, threading, and
case-sensitivity mistakes early), and testing on a real mid-range phone before each release.

## Check your understanding

1. An image works on desktop but is missing in the browser. Name two likely causes.
2. Why might `list.getLast()` crash on Android even though it compiles?
3. Why does the Android game show more of the level on a wide phone than on a narrow one?

(Answers: different capitalization in the file name, or the file being excluded from the web build; ART does not have
that newer library method, even though the compiler accepts it; Android uses the `EXTEND` viewport, which shows more
world instead of adding bars.)

**Next:** [Building and Shipping](building-and-shipping.md)
