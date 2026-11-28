# The Boot Sequence: How a Game Starts Up

Your launcher is one line, but a lot happens behind it before your first state's `create()` runs. This guide walks
through the whole startup in order, from `main` to the first frame, explains why each step happens where it does,
shows how startup differs on the web and Android, describes the shutdown, and shows how to hook your own code into
startup with boot callbacks.

## Table of contents

1. [Why the order matters](#why-the-order-matters)
2. [The whole sequence at a glance](#the-whole-sequence-at-a-glance)
3. [Step 1: constructing your game](#step-1-constructing-your-game)
4. [Step 2: the launcher installs the platform](#step-2-the-launcher-installs-the-platform)
5. [Step 3: Flixel.start wires the core](#step-3-flixelstart-wires-the-core)
6. [Step 4: the runner opens the window and the GPU](#step-4-the-runner-opens-the-window-and-the-gpu)
7. [Step 5: FlixelGame.create prepares the game](#step-5-flixelgamecreate-prepares-the-game)
8. [Step 6: the first state](#step-6-the-first-state)
9. [Step 7: the loop](#step-7-the-loop)
10. [Shutting down](#shutting-down)
11. [How the web starts differently](#how-the-web-starts-differently)
12. [How Android starts differently](#how-android-starts-differently)
13. [Hooking into startup: boot callbacks](#hooking-into-startup-boot-callbacks)
14. [The crash handler](#the-crash-handler)
15. [Common startup mistakes](#common-startup-mistakes)
16. [Check your understanding](#check-your-understanding)

## Why the order matters

Starting a game is like opening a restaurant for the night. The building must be unlocked before the kitchen can be
switched on; the kitchen must be on before the cooks start; the cooks must be ready before the doors open to guests.
Doing things out of order means a cook arriving to a dark kitchen.

In the same way, the input managers cannot listen to a window that does not exist yet, and textures cannot be loaded
before the GPU is ready. The boot sequence guarantees that by the time your state's `create()` runs, every service is
installed and ready.

## The whole sequence at a glance

```
main(...)
 |- new MyGame()                          1. Store the config and the first state's recipe.
 '- FlixelDesktopLauncher.launch(game)    2. Install platform services into the seams.
     '- Flixel.start(game, runner)         3. Crash handler, boot callbacks, core managers.
         '- runner.run(game)               4. Open the window, create the GPU device.
             |- game.create()              5. Cameras, render settings, input listeners, debugger.
             |   '- switchState(first)     6. Your first state's constructor, then create().
             '- loop until closed:         7. advanceTime, beginFrame, update, draw, endFrame.
             '- game.destroy()                Shutdown.
```

## Step 1: constructing your game

```java
public static void main(String[] args) {
  FlixelDesktopLauncher.launch(new MyGame());
}
```

`new MyGame()` runs first. The `FlixelGame` constructor only stores two things: your `FlixelConfig` (in
`Flixel.config`) and the recipe for your first state. **Nothing else is ready yet:** there is no window, no input, no
audio. That is why you must not use `Flixel.keys`, `Flixel.sound`, or similar services in your game's constructor or in
static initializers.

## Step 2: the launcher installs the platform

The launcher's job is to fill every seam (see [Architecture](architecture.md)) with this platform's implementation. On
desktop, `FlixelDesktopLauncher.launch(game)`:

1. **Reads the runtime mode** from `-Dflixel.mode` (release when absent).
2. **Installs logging first**, so every message from here on is formatted and routed correctly: the call-site
   resolver, the colored console sink, and the log file sink.
3. **Checks the environment:** warns if native access is not enabled, and checks that window icon files exist.
4. **Installs the WebGPU backend** (jwebgpu's FFM backend, which loads Dawn).
5. **Creates the platform pieces:** the runtime device, the SDL window, host integration, the input device, the
   WebGPU graphics manager (with an image decoder, needed early for window icons), the gamepad provider, and the
   cursor manager.
6. **Assigns them to the seams:** `Flixel.alert`, `window`, `host`, `files`, `input`, `graphics`, `assets`,
   `FlixelSave.defaultHandler`, `FlixelSoundManager.defaultFactory`, and the font rasterizer.
7. **Registers an after-start callback** that hands the gamepad provider and cursor manager to the input managers,
   because those managers do not exist until the next step.
8. **Sets the runtime mode**, and in debug mode installs the debugger connection.
9. **Calls `Flixel.start(game, runner)`.**

## Step 3: Flixel.start wires the core

`Flixel.start(...)` is the single entry point every launcher calls. It:

1. **Ignores repeat calls**, with a warning, so a game can only start once.
2. **Installs the crash handler**, so any uncaught error from here on is reported (see below).
3. **Runs the `beforeStart` boot callbacks.**
4. **Stores the game** in `Flixel.game`.
5. **Creates the core managers:** `Flixel.keys`, `Flixel.debug`, `Flixel.save`, `Flixel.mouse`, `Flixel.touches`,
   `Flixel.gamepads`, and `Flixel.sound` (built from the platform's sound factory).
6. **Registers every tween type** with the tween pools.
7. **Marks the framework as initialized.** From now on, boot callbacks can no longer be added.
8. **Runs the `afterStart` boot callbacks.** This is the place to replace a service before anything uses it.
9. **Hands the game to the runner** with `runner.run(game)`. From here on, the platform owns the loop.

## Step 4: the runner opens the window and the GPU

The runner is the platform's main loop. On desktop, `FlixelDesktopRunner`:

1. Initializes SDL3 and creates the window at the render resolution (centered, with the right flags for the platform
   and for fullscreen or transparency).
2. Binds the window to the window and input services, and applies the window icons.
3. Creates a WebGPU surface from the window's native handle, asks for an adapter and a device, and starts the
   renderer.
4. Opens any gamepads that are already plugged in, and reads the monitors.
5. Calls `game.create()`.

## Step 5: FlixelGame.create prepares the game

`FlixelGame.create()` runs once the window and GPU exist. It creates every manager in `Flixel.managers`, from the
lowest order to the highest, then switches to your first state. Creating the managers:

1. Turns on compressed texture lookup (used on Android), in `Flixel.assets`.
2. Applies the configured render smoothing, render scale, and render resolution, and prepares the one-pixel white
   texture used for backgrounds and effects, in `Flixel.render`.
3. Creates the default camera at the design size, plus the overlay camera, in `Flixel.cameraManager`.
4. Registers the keyboard, mouse, and touch managers as listeners on `Flixel.input`, so raw events now reach them.
5. Starts the debugger, in debug mode.
6. Creates your own managers, if you registered any.

You can override `create()` in your game class to do global setup, but call `super.create()` first; the services and
cameras do not exist until it has run.

## Step 6: the first state

`Flixel.switchState(recipe)` now runs for the first time:

1. Your recipe runs (`PlayState::new`), so your state's **constructor** runs.
2. `Flixel.Signals.preStateSwitch` fires.
3. Tweens and timers are cleared and the cameras are reset.
4. Your state's **`create()`** runs. Everything is ready: input, audio, files, assets, the camera.
5. `Flixel.Signals.postStateSwitch` fires.

## Step 7: the loop

Every frame, the runner:

1. Handles platform events: window focus, minimize, resize, close requests, and input. Focus and minimize reach the
   game through `onFocusLost()`, `onFocusGained()`, and `onMinimized()`.
2. Measures the time since the last frame and calls `game.advanceTime(...)` (clamped, then scaled by
   `Flixel.timeScale`).
3. Calls `game.beginFrame()`, which runs every manager's `beginFrame()`.
4. Calls `game.update(elapsed)`.
5. Calls `game.draw(batch)`.
6. Calls `game.endFrame()` to finish the input state.
7. Waits as needed to keep the target frame rate.

The exact contents of update and draw are listed in [The Game Loop](game-loop.md).

## Shutting down

When the player closes the window (or you call `Flixel.quit()`), the loop ends and `game.destroy()` runs:

1. `onSuspended()` fires `Flixel.Signals.gameSuspended`, so save listeners run.
2. `Flixel.Signals.preGameClose` fires.
3. The debugger disconnects, gamepads reset, and tweens and timers are canceled.
4. The current state is destroyed, global shaders are released, and the asset manager unloads everything.
5. Audio, cameras, the overlay, and fonts are released.
6. `Flixel.Signals.postGameClose` fires.
7. File logging stops.

The runner then releases the GPU and the window. On the web and on phones, the platform often stops the game without
this clean shutdown, which is why important data should be saved earlier (see [Saving Progress](saving.md)).

## How the web starts differently

A browser has one thread, and several startup steps are asynchronous (the browser answers "later" instead of right
away). The web runner therefore takes one step per browser turn:

1. The mode comes from the page address (`?flixel.mode=debug`) instead of a JVM flag, or from a default the build
   plugin baked into the page.
2. The runner finds or creates the `<canvas>` and sizes it.
3. It checks for WebGPU. Without it, the page shows a message naming the browsers that support it, and the game does
   not start. There is no WebGL fallback.
4. It turns the canvas into a WebGPU surface and asks for an adapter and a device, waiting for the callbacks.
5. It downloads every asset listed in the build's asset manifest, so later file reads are instant.
6. It calls `game.create()` and starts a `requestAnimationFrame` loop: each browser animation frame runs one game frame.

Hidden tabs and page unloads call `onSuspended()`, and the game pauses audio and updates while the tab is hidden.

## How Android starts differently

On Android, your `Activity` calls `FlixelAndroidLauncher.launch(this, new MyGame())` from `onCreate`:

1. The mode comes from the `flixel.mode` intent extra, or from whether the build is debuggable.
2. The launcher installs the JNI WebGPU backend, the Android services (files, assets, saves, host, haptics, logcat
   logging, the Basis Universal texture transcoder, the font rasterizer), and an `EXTEND` viewport so games fill phone
   screens of any shape.
3. It creates a surface view for the game and registers the runner for the surface's lifecycle.
4. When the surface is ready, the runner starts a dedicated render thread, `flixel-render`, which owns the GPU and runs
   `game.create()` and the loop. The UI thread only forwards surface and activity events to it.
5. When the activity pauses, the game gets `onFocusLost()` and `onSuspended()`; when it resumes, `onFocusGained()`.

## Hooking into startup: boot callbacks

`Flixel.boot` lets advanced code run at two moments inside `Flixel.start(...)`:

| Callback          | Runs                                                        | Good for                                      |
|-------------------|-------------------------------------------------------------|-----------------------------------------------|
| `beforeStart(...)` | Before the core managers exist.                            | Replacing a platform service (`Flixel.files`, `FlixelSave.defaultHandler`) before anything uses it, or forcing a graphics API with `Flixel.graphics.setApi(...)`. |
| `afterStart(...)`  | After the core managers exist, before the window opens.    | Configuring managers, registering debugger commands, adding log sinks, registering asset loaders. |

Register them **before** the game starts. Since your game's constructor runs before the launcher, it is a convenient
place:

```java
public class MyGame extends FlixelGame {

  public MyGame() {
    super(new FlixelConfig.Builder("My Game").company("My Studio").size(640, 360).build(), TitleState::new);

    Flixel.boot.afterStart(() -> {
      Flixel.log.addSink(new MyCrashReporterSink());
      Flixel.debug.console.register("unlockAll", args -> Progress.unlockAll());
      Flixel.gamepads.globalDeadZone = 0.15f;
    });
  }
}
```

Registering a callback after the framework has started throws an exception, because the moment has passed. Remember
that the window and GPU do not exist yet during either callback, so do not load textures there; do that in
`create()` or a loading state.

## The crash handler

`Flixel.start(...)` installs a handler for uncaught errors as its very first act. When something throws and nothing
catches it, the handler:

1. Logs the error with its full stack trace.
2. Shows a native error message box (`Flixel.alert.error(...)`) so players see what happened instead of a silent close.
3. Tries to destroy the game cleanly, so resources are released (and reports, without crashing again, if that fails).
4. On desktop, closes the game.

On the web and mobile, it does not force the program closed, to respect those platforms' rules.

## Common startup mistakes

| Mistake                                                         | Result and fix                                                        |
|-----------------------------------------------------------------|-----------------------------------------------------------------------|
| Using `Flixel.keys` or `Flixel.sound` in the game constructor   | They are not installed yet. Move the code to a state's `create()`.     |
| Loading textures in a static initializer or boot callback       | There is no GPU yet. Load in `create()` or a loading state.            |
| Registering a boot callback from `create()`                     | Throws, because startup is over. Register before `launch(...)`.        |
| Overriding `FlixelGame.create()` without calling `super.create()` | No cameras, no input listeners, and no first state.                  |
| Expecting the web game to start without WebGPU                  | The page shows a message instead. Test in a browser with WebGPU.       |

## Check your understanding

1. In which step do `Flixel.keys` and `Flixel.sound` come into existence?
2. You want every state to have the console command `unlockAll`. Where do you register it?
3. Why does the web runner do its startup in several separate steps instead of one?

(Answers: step 3, inside `Flixel.start(...)`; in an `afterStart` boot callback registered in your game's constructor;
creating the GPU device and downloading assets are asynchronous in the browser, so the runner must wait for each
answer before continuing.)

**Next:** [Extending the Framework](extending-the-framework.md)
