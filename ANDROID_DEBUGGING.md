# Debugging on Android

This guide shows how to find out what your FlixelGDX game is doing on an Android device or emulator:
reading logs, stepping through code, switching the runtime mode, profiling frames, and decoding native
crashes.

## Reading logs with logcat

On Android the framework sends every log line to logcat through `FlixelLogcatSink`, which the launcher
installs for you. The logcat tag is the tag of the logger that was called (for example
`Flixel.log.tagged("PlayState")`), or `FlixelGDX` when a logger has no tag.

```sh
# Everything your game logs, errors and above only.
adb logcat -s FlixelGDX:E

# Follow the log of your app's process only.
adb logcat --pid=$(adb shell pidof -s com.example.mygame)
```

GPU problems are logged too: uncaptured WebGPU errors, and the reason and message whenever the GPU
device is lost (logged as an error, or as a warning when the device was destroyed on purpose).

## Attaching the Android Studio debugger

1. Install a debug build (`./gradlew installDebug` in your game project).
2. In Android Studio, choose **Run > Attach Debugger to Android Process** and pick your app. Or just
   press **Debug** to launch and attach in one step.
3. Set breakpoints in your Java code as usual.

Pausing at a breakpoint blocks the render thread, so the app may look frozen until you resume.

## Choosing the runtime mode

`FlixelAndroidLauncher.launch(activity, game)` picks the runtime mode for you:

1. If the launch intent has a `flixel.mode` string extra (`debug`, `test` or `release`), that wins.
2. Otherwise a debuggable app (a normal debug build) starts in `DEBUG`, which enables the remote debugger
   and diagnostics.
3. Otherwise the app starts in `RELEASE`.

Because the default follows the build type, a published release build runs in release mode with no code
change. To override the mode from the command line:

```sh
adb shell am start -n <package>/<activity> -e flixel.mode debug
```

An unknown value logs a warning and falls back to the default described above. To force a mode no matter
what, call `FlixelAndroidLauncher.launch(activity, game, FlixelRuntimeMode.DEBUG)`.

## Using the remote debugger app

A game in `DEBUG` mode connects to the remote debugger app on `127.0.0.1:6510`, and every game gets the `INTERNET`
permission from the framework for this. On a phone, `127.0.0.1` is the phone itself, so forward the port over USB
first (run it again after you reconnect the cable or restart `adb`):

```sh
adb reverse tcp:6510 tcp:6510
```

The app on your PC then answers at the phone's `127.0.0.1:6510`. The game retries every two seconds, so the order in
which you start the app and the game does not matter. For an emulator or a phone on the same network, skip the forward
and name the PC instead:

```sh
adb shell am start -n <package>/<activity> -e flixel.debugHost 192.168.1.20:6510
```

The extra takes `host` or `host:port`. Release builds never open a connection.

## Profiling with trace sections

The framework marks its per-frame phases in the Android system trace, so they appear as labeled bars:

| Section                 | What it covers                                  |
|-------------------------|-------------------------------------------------|
| `flixel.update`         | One whole update step                           |
| `flixel.update.tweens`  | Global tweens and timers                        |
| `flixel.update.state`   | The active state and substate chain             |
| `flixel.update.cameras` | Camera updates                                  |
| `flixel.draw`           | One whole draw step                             |
| `flixel.switchState`    | Creating and entering a new state               |

You can add your own sections with `Flixel.profiler`. Always pair `begin` with `end` in a
`try`/`finally`, and use constant names of 127 characters or fewer:

```java
Flixel.profiler.begin("game.pathfinding");
try {
  findPath();
} finally {
  Flixel.profiler.end();
}
```

To look at a trace:

- **Perfetto:** record a trace with `adb shell perfetto` (or the Developer options system tracing
  tile), then open the file at <https://ui.perfetto.dev>. Enable app tracing (the `atrace` categories for
  your app) so your sections are recorded.
- **Android Studio profiler:** start a **System Trace** (or **Capture System Activities**) recording and
  look at your app's main and render threads for the sections above.

On desktop and web the profiler does nothing for now.

## Native crash stack traces with ndk-stack

A crash inside native code (the audio library, or the GPU driver) shows up in logcat as a tombstone with
raw addresses. Decode it with `ndk-stack` from the Android NDK, pointing it at the unstripped libraries:

```sh
adb logcat | $ANDROID_HOME/ndk/<version>/ndk-stack -sym <path-to-unstripped-libs>
```

The symbol directory contains one folder per ABI (for example `arm64-v8a`) holding the `.so` files built
with debug symbols. You can also decode a saved log:

```sh
$ANDROID_HOME/ndk/<version>/ndk-stack -sym <path-to-unstripped-libs> -dump crash.txt
```

Java and Kotlin crashes do not need this: the framework's crash handler logs their stack traces directly.
