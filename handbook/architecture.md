# Architecture: Core, Backends, and the Seams Between Them

This guide explains how FlixelGDX is put together: the modules, the line between platform-neutral core code and
platform backends, the interfaces (seams) that connect them, the threading model, the renderer, and the rules that
keep one codebase running on desktop, the web, and Android. Read it when you want to understand why things are
shaped the way they are, or before extending the framework.

## Table of contents

1. [The big picture](#the-big-picture)
2. [The modules](#the-modules)
3. [Core never names a platform](#core-never-names-a-platform)
4. [Seams: the interfaces backends fill in](#seams-the-interfaces-backends-fill-in)
5. [Safe defaults: the no-op implementations](#safe-defaults-the-no-op-implementations)
6. [What each backend installs](#what-each-backend-installs)
7. [The renderer: one WebGPU implementation](#the-renderer-one-webgpu-implementation)
8. [The game loop: one state and many managers](#the-game-loop-one-state-and-many-managers)
9. [Threads](#threads)
10. [Inside core: the package map](#inside-core-the-package-map)
11. [The object model: small capability interfaces](#the-object-model-small-capability-interfaces)
12. [Rules that keep core portable](#rules-that-keep-core-portable)
13. [Check your understanding](#check-your-understanding)

## The big picture

Think of a universal power adapter. The device you plug in (your game) has one plug shape. The adapter (a backend)
converts it to whatever socket the country uses (Windows, a browser, Android). The device never needs to know which
country it is in.

```
            Your game (core module)
                     |
                     v
              flixelgdx-core            <- the framework API you program against
                     |
   +-----------------+------------------+
   |                 |                  |
flixelgdx-desktop  flixelgdx-html5   flixelgdx-android    <- backends: one per platform
   |                 |                  |
   +--------+--------+------------------+
            |
       flixelgdx-gpu                    <- the one WebGPU renderer they share
            |
         jwebgpu                        <- Java WebGPU bindings (FFM, browser, JNI)
```

Your game depends only on `flixelgdx-core`. Each platform launcher module depends on one backend. The backend fills in
every platform-specific piece through interfaces that core defines, then starts the game.

## The modules

| Module                     | Role                                                                                         |
|----------------------------|----------------------------------------------------------------------------------------------|
| `flixelgdx-core`           | The framework: game loop, states, sprites, input managers, audio manager, tweens, saving, the debugger, and every interface backends implement. Platform-neutral. |
| `flixelgdx-gpu`            | The WebGPU renderer shared by every platform: sprite batch, textures, render targets, shaders, global shader chain. Written only against the jwebgpu API. |
| `flixelgdx-desktop`        | Windows, macOS, and Linux: SDL3 (window, input, gamepads) through LWJGL, jwebgpu's FFM backend (Dawn), and miniaudio. |
| `flixelgdx-html5`          | The browser, compiled by TeaVM to JavaScript or WebAssembly: canvas, DOM input, the Gamepad API, Web Audio, `localStorage`, browser WebGPU. |
| `flixelgdx-android`        | Android 8.0+ with Vulkan 1.1, 64-bit: jwebgpu's JNI backend (Dawn on Vulkan), miniaudio, Android input and storage. |
| `flixelgdx-ios`            | Planned. Currently fails fast if used.                                                       |
| `flixelgdx-jvm`            | Code shared by desktop and Android that core cannot hold, such as the debugger's WebSocket connection (core never uses `java.net`). |
| `flixelgdx-miniaudio`      | The Java API and C source for miniaudio. Each backend supplies its own native library.       |
| `flixelgdx-json-processor` | The compile-time annotation processor for `@JsonSerializable`.                               |
| `flixelgdx-plugins`        | Gradle plugins: HTML5 builds, desktop packaging (packagr), shader validation, and Android texture compression (Basis Universal). |
| `flixelgdx-test`           | The framework's unit tests. Not published.                                                   |

## Core never names a platform

Core code never imports SDL, Android, browser, or LWJGL types. Anything that differs per platform is an interface in
core, and each backend provides an implementation. This is why:

- Your game code runs unchanged everywhere, with no `if (android)` checks.
- Core compiles for the browser, where many Java classes (threads, sockets, reflection) do not exist or behave
  differently.
- You can replace any service with your own implementation (see [Extending the Framework](extending-the-framework.md)).

## Seams: the interfaces backends fill in

A **seam** is a place where you can swap one implementation for another without changing the code on either side,
like the standard socket in a wall. These are the main seams:

| Seam (in core)                          | Installed into                              | What it abstracts                                    |
|-----------------------------------------|---------------------------------------------|------------------------------------------------------|
| `FlixelGameRunner`                      | passed to `Flixel.start(...)`               | The main loop: window creation, frame timing, events. |
| `FlixelWindow`                          | `Flixel.window`                             | The window or canvas.                                |
| `FlixelGraphicsManager`                 | `Flixel.graphics`                           | The GPU: batch, textures, render targets, shaders.   |
| `FlixelInputDevice`                     | `Flixel.input`                              | Raw keyboard, mouse, and touch events.               |
| `FlixelGamepadProvider`                 | `Flixel.gamepads.setGamepadProvider(...)`   | Controller hardware.                                 |
| `FlixelMouseIconManager`                | `Flixel.mouse.setMouseIconManager(...)`     | The system cursor.                                   |
| `FlixelSoundFactory`                    | `FlixelSoundManager.defaultFactory`         | The audio engine.                                    |
| `FlixelFiles`                           | `Flixel.files`                              | File roots and reading.                              |
| `FlixelAssetManager`                    | `Flixel.assets`                             | Loading and caching (threaded where possible).       |
| `FlixelSaveHandler`                     | `FlixelSave.defaultHandler`                 | Where saves are stored.                              |
| `FlixelHostIntegration`                 | `Flixel.host`                               | Clipboard, notifications, file dialogs, monitors.    |
| `FlixelHaptics`                         | `Flixel.haptics`                            | Device vibration.                                    |
| `FlixelAlerter`                         | `Flixel.alert`                              | Native message boxes.                                |
| `FlixelRuntimeDevice`                   | `Flixel.runtime`                            | Memory, runtime mode, crash handler, environment, main-thread task queue. |
| `FlixelProfiler`                        | `Flixel.profiler`                           | Trace sections for platform profilers.               |
| `FlixelFontRasterizer`                  | `FlixelFontRegistry.setRasterizer(...)`     | Turning `.ttf`/`.otf` files into glyph images.       |
| `FlixelLogSink`, `FlixelLogSiteResolver`, `FlixelLogFileSink` | `Flixel.log`          | Where logs go and how call sites are found.          |
| `FlixelDebugTransport`                  | `FlixelDebugManager.defaultTransport`       | The connection to the debugger app.                  |

## Safe defaults: the no-op implementations

Before a backend runs, every seam already holds a **no-op** implementation (`FlixelNoopWindow`,
`FlixelNoopGraphicsManager`, `FlixelNoopFiles`, `FlixelNoopSaveHandler`, and so on) that quietly does nothing. That has
two benefits:

- `Flixel.graphics`, `Flixel.window`, and friends are never `null`, so early code and missing platform features never
  crash. A backend that does not support haptics simply leaves the no-op in place.
- Unit tests can run the framework headless (`FlixelGameRunner.NOOP`), with no window or GPU.

## What each backend installs

Every launcher does the same job in its own way: install a logger, create the platform pieces, assign them to the
seams, choose the runtime mode, connect the debugger in debug mode, and call `Flixel.start(game, runner)`. The full
step-by-step order is in [The Boot Sequence](boot-sequence.md).

| Seam            | Desktop                         | Web                               | Android                          |
|-----------------|---------------------------------|-----------------------------------|----------------------------------|
| Window          | SDL3 window                     | `<canvas>` element                | Surface view                     |
| Graphics        | WebGPU (Dawn, FFM) on Vulkan, Metal, or Direct3D 12 | Browser WebGPU    | WebGPU (Dawn, JNI) on Vulkan     |
| Input           | SDL3 events                     | DOM events                        | Android touch, key, and motion events |
| Gamepads        | SDL3 controller database        | Web Gamepad API                   | Android input devices            |
| Audio           | miniaudio                       | Web Audio API                     | miniaudio                        |
| Files           | Folder or jar, plus classpath   | Preloaded in-memory cache         | APK assets and app storage       |
| Saves           | App data folder                 | `localStorage`                    | Private app storage              |
| Fonts           | stb_truetype                    | stb_truetype compiled to WebAssembly | Android font rendering        |
| Logs            | Colored terminal, optional files | Browser console                  | Logcat, optional files           |
| Debugger link   | WebSocket (`flixelgdx-jvm`)     | Browser WebSocket                 | WebSocket (`flixelgdx-jvm`)      |
| Default viewport | Letterbox (`FIT`)              | Letterbox (`FIT`)                 | Fill the screen (`EXTEND`)       |

## The renderer: one WebGPU implementation

All drawing goes through `flixelgdx-gpu`, written once against the jwebgpu API. Each platform only supplies three
things: a jwebgpu backend, a surface for its window, and a device. This means a shader, a blend mode, or a texture
format behaves identically on every platform.

Shaders are WGSL only. The framework prepends a fixed preamble that declares the globals (projection, texture size),
the texture and sampler, and the vertex structs, so a shader file only contains its own logic. Textures hold straight
(not premultiplied) color, and the framework premultiplies a shader's output itself before blending, so transparency
behaves the same in every blend mode. See [Graphics and Shaders](graphics-and-shaders.md).

## The game loop: one state and many managers

Seams decide *what* a platform provides. Managers decide *when* each part of the framework runs. Every frame, the
runner calls the same few methods on your game (`beginFrame()`, `update(...)`, `draw(...)`, `endFrame()`), and the game turns each
call into a fixed series of steps.

Think of a theater. The **state** is the play on stage, and it is the only thing `FlixelGame` runs itself: it updates
the state chain, draws it when asked, switches it with `Flixel.switchState(...)`, and pauses it. Everything else is
the **crew**: input, sound, the cameras, the renderer, the debugger. Each crew member is a **manager**, an object that
implements `FlixelManagers.Manager` and sits in the registry `Flixel.managers`. At every step of the lifecycle, the game
calls the matching hook on every manager, next to the matching [signal](signals.md):

```
FlixelGame.beginFrame()
  managers.beginFrame()       every manager, even while paused, before anything updates

FlixelGame.update(elapsed)
  managers.preUpdate(...)     every manager, lowest order first
  managers.update(...)        input, debugger, tweens, timers, sound, your managers
  state chain update          skipped while paused
  managers.postUpdate(...)    cameras, overlay, debugger sampling, your managers

FlixelGame.draw(batch)
  managers.preDraw(...)
  managers.draw(...)          the renderer draws every camera; inside it:
                                drawCamera(...) once per camera, after the state drew on it
                                drawOverlay(...) on top of everything
  managers.postDraw(...)      debugger hitboxes, straight onto the window

FlixelGame.endFrame()
  managers.endFrame(...)      input finishes the frame; the renderer makes the window opaque if needed
```

The other hooks fire on events: `create()`, `resize(...)`, the focus, minimize, and suspend hooks, `onPaused(...)`, the
three state switch hooks, and `destroy()`.

### The built-in managers

Each built-in manager has a fixed **order**. Hooks run from the lowest order to the highest, and `destroy()` runs in
reverse, so a manager is always destroyed before the managers it depends on.

| Order constant   | Value | Manager                                   | Its job in the loop                                      |
|------------------|-------|-------------------------------------------|----------------------------------------------------------|
| `ORDER_ASSETS`   | -2000 | `Flixel.assets`                           | Created first and destroyed last, since everything loads through it. |
| `ORDER_FONTS`    | -1900 | `FlixelFontRegistry.MANAGER`              | Disposes baked fonts before the assets go away.          |
| `ORDER_RENDER`   | -1800 | `Flixel.render` (`FlixelRenderManager`)   | Render settings, the background color, and drawing every frame. |
| `ORDER_CAMERAS`  | -1700 | `Flixel.cameraManager` (`FlixelCamera.Manager`) | Creates, moves, refits, and resets the cameras.    |
| `ORDER_OVERLAY`  | -1600 | `Flixel.overlay` (`FlixelOverlayManager`) | The global overlay drawn on top of every camera.         |
| `ORDER_KEYS` to `ORDER_GAMEPADS` | -1000 to -700 | `Flixel.keys`, `mouse`, `touches`, `gamepads` | Read the hardware first, finish the frame last. |
| `ORDER_ACTIONS`  | -600  | `FlixelActionSets.MANAGER`                | Input actions, right after the devices.                  |
| `ORDER_DEBUG`    | -500  | `Flixel.debug`                            | Takes the input it owns, then samples the frame.         |
| `ORDER_TWEENS`   | -400  | The global tween manager                  | Advances tweens; clears them on state switches.          |
| `ORDER_TIMERS`   | -300  | The global timer manager                  | Advances timers; cancels them on state switches.         |
| `ORDER_SOUND`    | -200  | `Flixel.sound`                            | Advances sounds; pauses them with the game and the window. |
| `ORDER_DEFAULT`  | 0     | Your managers, unless they choose an order | Run after every built-in manager.                       |

The orders are spaced apart so a manager can slip in between two built-in ones (see
[Extending the Framework](extending-the-framework.md#writing-your-own-manager)).

### Pausing

While the game is paused (the debugger's pause, or auto-pause when the window loses focus), the game skips the state
update, and every manager whose `isPausable()` returns `true` skips its three update hooks. Input and the debugger are
not pausable, so a pause menu and the debugger keep working; tweens, timers, sound, cameras, and the overlay are, so
they freeze with the state. Draw and event hooks always run.

### Why the registry looks fields up every time

The built-in managers are registered as **slots** that read their `Flixel` field each time a hook runs (for example,
the sound slot reads `Flixel.sound` at that moment), rather than holding the object. Like a contact in your phone
instead of a printed phone number, the slot always reaches whatever object the field holds now. That is why replacing
a service such as `Flixel.sound` is safe at any time, and why a field that is `null` (in a headless test) is simply
skipped.

### Profiling for free

Every manager call is wrapped in a `Flixel.profiler` section named by the manager's `getName()` (`flixel.render`,
`flixel.keys`, `flixel.sound`, and so on), and the state update has its own section, `flixel.update.state`. A platform
profiler such as the Android system trace therefore shows every system by name with no extra code. See
[Optimization](optimization.md).

## Threads

FlixelGDX is single-threaded from your point of view: **all game code runs on one thread**, the game's main thread.
States, sprites, input, tweens, signals, saving, and the debugger are all used from there, and none of them are
thread-safe. That is deliberate. One thread means no locks in game code and no surprising timing.

| Platform | The game thread                                                                              |
|----------|----------------------------------------------------------------------------------------------|
| Desktop  | The thread that called `main`.                                                               |
| Web      | The browser's only thread, driven by `requestAnimationFrame`.                                |
| Android  | A dedicated render thread named `flixel-render` that owns the GPU. The UI thread only forwards surface and lifecycle events to it. |

A few systems use helper threads internally, and always hand results back to the game thread:

- The asset manager reads and decodes files on worker threads where the platform allows, then finishes (uploads to the
  GPU) on the game thread.
- The debugger's network connection runs on its own thread and passes messages through queues.

If your own code uses threads (networking, heavy generation), hand results back with
`Flixel.runtime.queueMainThread(...)`.

## Inside core: the package map

| Package                       | Contents                                                                  |
|-------------------------------|---------------------------------------------------------------------------|
| `org.flixelgdx`               | `Flixel`, `FlixelGame`, `FlixelConfig`, `FlixelState`, `FlixelSubState`, `FlixelBasic`, `FlixelObject`, `FlixelSprite`, `FlixelCamera`. |
| `animation`                   | Animation controller, state machine, Sparrow and Adobe Animate support.   |
| `asset`                       | The asset manager, handles, and loaders.                                  |
| `audio`                       | Sound manager, sounds, groups, sources.                                   |
| `backend`                     | Seams for the window, host, runtime, haptics, alerts, profiler, boot, and the game runner. |
| `collections`                 | Garbage-free lists, maps, sets, and pools.                                |
| `debug`, `debug.*`            | The debugger: manager, feeds, panels, widget toolkit, inspector, remote protocol. |
| `file`                        | File roots and handles.                                                   |
| `functional`, `functional.*`  | Capability interfaces and primitive functional interfaces.                |
| `graphics`                    | The graphics manager seam, textures, images, batch, render targets, shaders, viewports. |
| `group`                       | Group types.                                                              |
| `input`, `input.*`            | Keyboard, mouse, touch, gamepads, and actions.                            |
| `json`, `xml`                 | Data parsers and writers.                                                 |
| `logging`                     | Loggers, sinks, levels, and call sites.                                   |
| `math`                        | Vectors, rectangles, matrices, random, bounds, and ranges.                |
| `particle`                    | Emitters and particles.                                                   |
| `save`                        | Save data and handlers.                                                   |
| `signal`                      | Signals and their data types.                                             |
| `text`                        | Text objects, fonts, and layout.                                          |
| `timer`, `tween`, `tween.*`   | Timers, tweens, eases, and motion paths.                                  |
| `util`                        | Colors, strings, blend modes, flags, bars, and helpers.                   |

Each package has its own guide in its `package-info.java` Javadoc, and this handbook has a guide for each area.

## The object model: small capability interfaces

Instead of one huge base class, the framework splits "what an object can do" into small interfaces in
`org.flixelgdx.functional`:

| Interface             | Capability                                     |
|-----------------------|------------------------------------------------|
| `FlixelUpdatable`     | `update(elapsed)`                              |
| `FlixelDrawable`      | `draw(batch)`                                  |
| `FlixelDestroyable`   | `destroy()`                                    |
| `FlixelKillable`      | `kill()` and `revive()`                        |
| `FlixelExistable`     | The `exists` flag                              |
| `FlixelPositional`    | A position                                     |
| `FlixelPhysical`      | Velocity and acceleration                      |
| `FlixelAngleable`     | An angle                                       |
| `FlixelColorable`     | A tint color                                   |
| `FlixelVisible`       | Visibility                                     |
| `FlixelShaderable`    | A custom shader                                |
| `FlixelAntialiasable` | Antialiasing                                   |
| `FlixelShakeable`     | Shaking                                        |

Systems ask only for what they need: a camera follows any `FlixelPositional`, a color tween works on any
`FlixelColorable`, and a shake works on any `FlixelShakeable` (including the window). `IFlixelBasic` combines the
lifecycle interfaces into the full contract a state or group needs, so a class from your own hierarchy can implement it
and still be added to a state.

## Rules that keep core portable

These rules apply to the framework's own code, and they are good rules for your game's shared code too:

- **No reflection.** It breaks ahead-of-time compilation (the web, native images). Use code generation instead.
- **No platform types in core.** Abstract with an interface and implement it per backend.
- **No `java.net` in core.** It does not exist in the browser build. Networking lives in backends or `flixelgdx-jvm`.
- **Only Java APIs Android has.** Code that runs on Android cannot use newer library APIs such as virtual threads or
  `java.lang.foreign`, even though Java 25 *syntax* is fine (records, pattern matching, `_`).
- **No garbage in the frame loop.** See [Optimization](optimization.md).

## Check your understanding

1. Why does `Flixel.graphics` never return `null`, even in a unit test with no GPU?
2. Your game uses `java.net.Socket` in its core module. On which platform will that fail, and why?
3. Can a class that does not extend `FlixelBasic` be added to a state?
4. The debugger pauses the game. Why do your tweens stop while the keyboard still works?

(Answers: a no-op graphics manager is installed before any backend runs; the web, because the browser build has no
`java.net`; yes, if it implements `IFlixelBasic`; the tween manager is pausable, so it skips its update hooks during a
pause, but the input managers are not.)

**Next:** [The Boot Sequence](boot-sequence.md)
