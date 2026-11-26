# The Flixel Class: Your Toolbox of Global Services

`Flixel` is a static class you never create. It is the stage crew from the theater analogy: always backstage, ready
whenever you need something. This guide lists every service it holds, explains when they become available, and
covers its helper methods and global signals.

## Table of contents

1. [Why one global toolbox?](#why-one-global-toolbox)
2. [The services at a glance](#the-services-at-a-glance)
3. [The game, the config, and the current state](#the-game-the-config-and-the-current-state)
4. [Screen size helpers](#screen-size-helpers)
5. [Time helpers](#time-helpers)
6. [Collisions and world bounds](#collisions-and-world-bounds)
7. [Antialiasing](#antialiasing)
8. [Logging shortcuts](#logging-shortcuts)
9. [Global signals](#global-signals)
10. [Three rules for using services](#three-rules-for-using-services)
11. [Why services are interfaces](#why-services-are-interfaces)
12. [Check your understanding](#check-your-understanding)

## Why one global toolbox?

Almost every part of a game needs the same few things: input, sound, files, logging. If each object had to be handed
a keyboard object, a sound object, and a file object through its constructor, every class would grow long parameter
lists that only exist to pass things along. HaxeFlixel solved this with a global `FlxG` class, and FlixelGDX keeps
the idea: one well-known place, `Flixel`, that any code can reach.

Think of it like the front desk of a hotel. You do not carry a toolbox around the building; you call the front desk
and ask for what you need. The front desk always knows where everything is.

## The services at a glance

| Field               | Type                         | What it is for                                                         | Guide |
|---------------------|------------------------------|------------------------------------------------------------------------|-------------------------------------|
| `Flixel.keys`       | `FlixelKeyInputManager`      | The keyboard.                                                          | [Input](input.md) |
| `Flixel.mouse`      | `FlixelMouseInputManager`    | The mouse: buttons, position, scroll wheel, cursor icons.              | [Input](input.md) |
| `Flixel.touches`    | `FlixelTouchManager`         | Touch screens, up to ten fingers.                                      | [Input](input.md) |
| `Flixel.gamepads`   | `FlixelGamepadInputManager`  | Controllers, sticks, triggers, and rumble.                             | [Input](input.md) |
| `Flixel.input`      | `FlixelInputDevice`          | The raw platform input layer, for listeners and low-level polling.     | [Input](input.md) |
| `Flixel.sound`      | `FlixelSoundManager`         | Sound effects, music, master volume.                                   | [Audio](audio.md) |
| `Flixel.files`      | `FlixelFiles`                | Open files from the assets folder and other locations.                 | [Files and Assets](files-and-assets.md) |
| `Flixel.assets`     | `FlixelAssetManager`         | Preload and cache images, sounds, and data.                            | [Files and Assets](files-and-assets.md) |
| `Flixel.save`       | `FlixelSave`                 | A ready-to-use save file.                                              | [Saving](saving.md) |
| `Flixel.locale`     | `FlixelLocalization`         | Translated text for the player's language.                             | [Localization](localization.md) |
| `Flixel.cameras`    | `FlixelArray<FlixelCamera>`  | The list of active cameras. `Flixel.cameras.first()` is the main one.  | [Cameras](cameras.md) |
| `Flixel.graphics`   | `FlixelGraphicsManager`      | The GPU: textures, shaders, render resolution, display modes, FPS.     | [Graphics and Shaders](graphics-and-shaders.md) |
| `Flixel.window`     | `FlixelWindow`               | The window: title, size, fullscreen, position, opacity.                | [Window and Platform Services](window-and-platform-services.md) |
| `Flixel.host`       | `FlixelHostIntegration`      | The operating system: clipboard, notifications, file pickers, monitors. | [Window and Platform Services](window-and-platform-services.md) |
| `Flixel.haptics`    | `FlixelHaptics`              | Phone vibration.                                                       | [Window and Platform Services](window-and-platform-services.md) |
| `Flixel.alert`      | `FlixelAlerter`              | Native message boxes (info, warning, error).                           | [Window and Platform Services](window-and-platform-services.md) |
| `Flixel.runtime`    | `FlixelRuntimeDevice`        | Facts about the running program: memory, mode, environment.            | [Window and Platform Services](window-and-platform-services.md) |
| `Flixel.random`     | `FlixelRandom`               | Random numbers, booleans, and picks.                                   | [Math and Utilities](math-and-utilities.md) |
| `Flixel.log`        | `FlixelLogger`               | The logger and its settings.                                           | [Logging](logging.md) |
| `Flixel.debug`      | `FlixelDebugManager`         | Watches, console commands, the inspector, and debugger panels.         | [Debugging](debugging.md) |
| `Flixel.profiler`   | `FlixelProfiler`             | Named sections that show up in platform profilers.                     | [Optimization](optimization.md) |
| `Flixel.boot`       | `FlixelBootManager`          | Callbacks that run before and after the framework starts.              | [The Boot Sequence](boot-sequence.md) |

Two plain settings live here too:

- `Flixel.timeScale` (default `1f`): multiplies `elapsed` for the whole game. `0.5f` is half speed.
- `Flixel.autoPause` (default `true`): pause updates and audio when the window loses focus.

See [The Game Loop](game-loop.md) for both.

## The game, the config, and the current state

| Member                         | What it holds                                                                      |
|--------------------------------|-----------------------------------------------------------------------------------------|
| `Flixel.game`                  | Your `FlixelGame` instance. Cast it to reach your own fields: `((MyGame) Flixel.game)`. |
| `Flixel.config`                | The `FlixelConfig` you built: title, company, version, size, frame rate, and more. |
| `Flixel.state`                 | The active state.                                                                  |
| `Flixel.switchState(recipe)`   | Move to another state. See [States and Substates](states-and-substates.md).        |
| `Flixel.resetState()`          | Build a fresh copy of the current state.                                           |
| `Flixel.quit()`                | Closes the game. Works always on desktop and Android, browser can vary. |
| `Flixel.isDebugMode()`         | `true` when the game runs in debug mode.                                           |

```java
Flixel.info("Running {} version {}", Flixel.config.getTitle(), Flixel.config.getVersion());

FlixelState state = Flixel.state;
state.add(new FlixelSprite().loadGraphic(Flixel.files.internal("images/player.png")));

if (Flixel.isDebugMode()) {
  Flixel.switchState(DebugEditor::new);
}
```

## Screen size helpers

There are two kinds of "screen size," and choosing the right one avoids layout bugs on unusual windows.

| Method                                         | Returns                                                                                       |
|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Flixel.getDesignWidth()` / `getDesignHeight()`   | The size you asked for in `FlixelConfig`. Never changes. Use it for layout in game pixels.  |
| `Flixel.getVisibleWidth()` / `getVisibleHeight()` | The part of the world the first camera actually shows. It equals the design size with the default letterboxing viewport, but can be bigger with an "extend" viewport, and can change when the window is resized. |

Use the design size to place things in your fixed play area, and the visible size when something must hug the real
edges of the screen.

## Time helpers

```java
float elapsed = Flixel.getElapsed();     // This frame's seconds, after timeScale.
float real = Flixel.getRawElapsed();     // This frame's seconds, before timeScale.
```

Both are clamped between `Flixel.MIN_ELAPSED` and `Flixel.MAX_ELAPSED` (0.1 seconds), so a long freeze never makes
the next frame jump too far. See [The Game Loop](game-loop.md).

## Collisions and world bounds

`Flixel.overlap(...)` and `Flixel.collide(...)` check objects or whole groups against each other. Overlap only asks
whether they touch; collide also pushes them apart. Both are covered in
[Physics and Collisions](physics-and-collisions.md).

```java
Flixel.collide(player, walls);
Flixel.overlap(player, coins, onCoin, null);
```

The framework also keeps a **world bounds** rectangle, which defaults to a very large square around the origin
(20,000 by 20,000 pixels starting at -10,000). Ask any object whether it is inside with `object.inWorldBounds()`,
which is handy for killing things that fell out of the level. `overlap(...)` and `collide(...)` also skip any object
that is completely outside it, so a level larger than the default needs bigger bounds. Set it to match your level:

```java
Flixel.setWorldBounds(0f, 0f, levelWidth, levelHeight);

// In a bullet's update(...):
if (!inWorldBounds()) {
  kill();
}
```

## Antialiasing

Antialiasing smooths the edges of scaled and rotated images. It is off by default, which keeps pixel art crisp.

```java
Flixel.setAntialiasing(true);                // The global default for new sprites.
boolean on = Flixel.isAntialiasing();
Flixel.applyAntialiasingOnStateAdd = true;   // Force the global value on everything added to a state.
```

Individual sprites can still choose with `sprite.setAntialiasing(...)`, unless `applyAntialiasingOnStateAdd` is on.

## Logging shortcuts

`Flixel` has static shortcuts for the four log levels, so you do not have to write `Flixel.log.info(...)`:

```java
Flixel.info("Level {} started.", levelNumber);
Flixel.warn("Player health is low: {}", health);
Flixel.error("Could not load the level file!", exception);
Flixel.debug("Jump pressed.");
```

Each `{}` is filled in with the next argument. See [Logging](logging.md).

## Global signals

`Flixel.Signals` holds events the framework fires at important moments. Add a listener to react to them from
anywhere:

| Signal                                  | Fires                                                        | Data                     |
|-----------------------------------------|-------------------------------------------------------------------|----------------------------------------------------------|
| `preUpdate` / `postUpdate`              | Before and after each update pass.                           | `UpdateSignalData` (reused, read `elapsed()` right away) |
| `preDraw` / `postDraw`                  | Before and after each draw pass.                             | none                     |
| `preStateSwitch` / `postStateSwitch`    | Around every state switch.                                   | `StateSwitchSignalData` with the old or new `state()` |
| `windowFocused` / `windowUnfocused`     | When the window gains or loses focus.                        | none                     |
| `windowMinimized`                       | When the desktop window is minimized.                        | none                     |
| `gameSuspended`                         | When the platform may stop the game soon. Save here.         | none                     |
| `gamePaused`                            | When the game loop is paused or resumed, such as by the debugger. | `GamePausedSignalData` with `paused()` |
| `preGameClose` / `postGameClose`        | At the start and end of a clean shutdown.                    | none                     |

```java
Flixel.Signals.postStateSwitch.add(data -> Flixel.info("Now in {}", data.state()));
Flixel.Signals.gameSuspended.add(data -> Flixel.save.flush());
```

Listeners on global signals stay registered across state switches until you remove them. See [Signals](signals.md).

## Three rules for using services

1. **They are ready once your first state is created.** Use them from `create()`, `update(...)`, and later. Do not use
   them in a static initializer or in your `FlixelGame` constructor, because the platform has not installed them yet.
   Before then, most services are safe "no-op" stand-ins that do nothing, so early calls are silently lost rather
   than crashing.
2. **Use them from the main thread.** Unless a method's documentation says otherwise, call framework APIs from your
   game code (your state's methods), not from your own background threads. If you must hand work back to the game
   thread, queue it with `Flixel.graphics.queueMainThread(() -> ...)`.
3. **Do not cache what can change.** `Flixel.state` changes on every switch, and `Flixel.cameras.first()` is a new
   camera after every switch. Read them when you need them instead of storing them in a long-lived field.

## Why services are interfaces

Every service is an interface (`FlixelFiles`, `FlixelWindow`, `FlixelSaveHandler`, `FlixelLogger`, and so on) with a
platform implementation behind it. The desktop launcher installs SDL3 and miniaudio versions; the web launcher
installs browser versions; Android installs its own. Your game only talks to the interface, which is why the same
code runs everywhere.

It also means you can swap a service for your own. A custom save handler that syncs to the cloud, a log sink that
sends errors to a server, or an asset loader for your own file format all plug in without changing the framework.
See [Extending the Framework](extending-the-framework.md) and [Architecture](architecture.md).

## Check your understanding

1. Why does `Flixel.keys.pressed(...)` in your `FlixelGame` constructor not work?
2. You want a label pinned to the real right edge of a widescreen window. Design width or visible width?
3. A network thread receives a chat message. How do you show it in the game safely?

(Answers: the platform installs the input managers after the constructor runs, during startup; visible width;
queue the update with `Flixel.graphics.queueMainThread(...)` and change the text inside that callback.)

**Next:** [Groups and Recycling](groups-and-recycling.md)
