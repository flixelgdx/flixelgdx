# Debugging and the Debugger App

Bugs in games are often about things you cannot see: an enemy one pixel inside a wall, a timer that never resets, a
save value written under the wrong key. This guide covers debug mode, the hotkeys that work in any debug build, the
FlixelGDX debugger app and every built-in panel, debugging on phones and in browsers, letting AI agents debug a
running game, and general bug-hunting techniques.

Adding your own values, commands, inspector sections, and trackers is covered in
[Extending the Debugger](debugger-extending.md), and building whole panels in [Custom Debugger Panels](debugger-panels.md).

## Table of contents

1. [The race car and the pit wall](#the-race-car-and-the-pit-wall)
2. [Turning on debug mode](#turning-on-debug-mode)
3. [Hotkeys that work without the app](#hotkeys-that-work-without-the-app)
4. [Reading hitboxes](#reading-hitboxes)
5. [Getting the debugger app](#getting-the-debugger-app)
6. [Connecting a game](#connecting-a-game)
7. [A tour of the app](#a-tour-of-the-app)
8. [The built-in panels](#the-built-in-panels)
9. [Finding objects: selection, ids, and paths](#finding-objects-selection-ids-and-paths)
10. [The built-in console commands](#the-built-in-console-commands)
11. [Debugging on Android](#debugging-on-android)
12. [Debugging in the browser](#debugging-in-the-browser)
13. [Letting an AI agent debug your game](#letting-an-ai-agent-debug-your-game)
14. [Debugger code in release builds](#debugger-code-in-release-builds)
15. [Other debugging tools](#other-debugging-tools)
16. [Bug-hunting recipes](#bug-hunting-recipes)
17. [Check your understanding](#check-your-understanding)

## The race car and the pit wall

Printing log lines helps, but you have to guess what to print *before* you run the game. A debugger turns that around:
it shows the whole game (its scene, its objects, its numbers, its textures, its save data) while it runs, and lets you
pause, step, and change things.

Think of a race car and its pit wall. The car (your game) is out on the track and carries sensors everywhere. A radio
link sends their readings to the pit wall (the debugger app, a separate desktop program), where the crew watches
screens and can radio instructions back. The car never needs a screen of its own, and the pit wall never needs to know
how the car is built. It only shows what the car sends.

That split has three nice consequences:

- **One debugger for every platform.** The same app debugs a desktop game, a browser tab, or a phone.
- **The game stays fast.** Nothing heavy is drawn inside the game; it only sends data, and only what you are looking at.
- **The game decides what the debugger shows.** Every panel is built by game code, so you can add, replace, or remove
  any of them (see [Custom Debugger Panels](debugger-panels.md)).

## Turning on debug mode

A game runs in release mode unless told otherwise, so nothing debug-related ever reaches players by accident.

| Platform | How to start in debug mode                                                                       |
|----------|--------------------------------------------------------------------------------------------------|
| Desktop  | Run `./gradlew :desktop:debug` in a generated project, or pass `-Dflixel.mode=debug` to the JVM. |
| Web      | Run `./gradlew :html5:debug`, or add `?flixel.mode=debug` (or just `?debug`) to the page address. |
| Android  | Any debuggable build (a normal debug build) runs in debug mode. Override with the `flixel.mode` intent extra. |
| Every platform | Select a profile with `app.mode = "debug"` in `project.toml`, such as `-Pflixel.profile=dev`. See [Profiles](building-and-shipping.md#profiles). |

A launch setting (the JVM flag, the page address, or the intent extra) always wins over a profile's `app.mode`, so one
build can still be switched for a single run. Your code can check with `Flixel.isDebugMode()`.

## Hotkeys that work without the app

These keys work in any debug build, even with no debugger app running:

| Key                      | Action                                                     |
|--------------------------|------------------------------------------------------------|
| F3                       | Pause or resume the game.                                  |
| F2                       | Show or hide every object's hitbox.                        |
| F4                       | Turn the free camera on or off.                            |
| Alt + left click         | Select the object under the mouse (outlined in yellow).    |
| W, A, S, D or arrows     | Move the free camera.                                      |
| Mouse wheel, Q, E        | Zoom the free camera.                                      |
| Shift                    | Move the free camera four times faster.                    |
| Right mouse drag         | Pan the free camera.                                       |

While paused, the game's update stops completely (tweens, timers, and audio too), but rendering and the debugger keep
running, so you can look around. **Step** (a button in the app, the `step` console command, or code) runs exactly one
more frame, then stays paused. Stepping frame by frame is the best way to understand fast events like collisions.

The **free camera** takes over the main camera so you can fly around the level, while the game ignores the keys and
mouse buttons it uses. Turn it off and the camera goes back to where the game had it.

All of this is also available from code through `Flixel.debug.controls`:

```java
Flixel.debug.controls.setPaused(true);
Flixel.debug.controls.stepFrame();
Flixel.debug.controls.setHitboxes(true);
Flixel.debug.controls.goTo(1200f, 300f); // Fly the free camera to a spot in the world.

Flixel.debug.controls.setPauseKey(FlixelKey.P); // Change a hotkey if it clashes with your game.
```

## Reading hitboxes

With hitboxes on (F2), every object draws its collision box. The colors tell you how it collides:

| Color | Meaning                                         |
|-------|-------------------------------------------------|
| Red   | Solid: it collides and can be pushed.           |
| Green | Immovable: it collides but never moves.         |
| Blue  | Does not collide (`setSolid(false)` or no allowed sides). |

A character whose red box is much bigger than its picture, or a wall that shows blue, explains most "why am I stuck?"
and "why did I fall through?" bugs at a glance.

## Getting the debugger app

The debugger app is a separate desktop program, developed in the
[flixelgdx-debugger](https://github.com/flixelgdx/flixelgdx-debugger) repository. Download a release for your system
(Windows, macOS on Apple Silicon, or Linux, on x64 or ARM64) from its Releases page, unzip it, and run it. It needs
nothing else installed. To build it yourself instead, install the .NET 10 SDK and run:

```bash
dotnet run --project src/FlixelDebugger.App
```

## Connecting a game

The app is a server and the game connects to it, so there is no address to type in most cases:

1. Start the debugger app. It listens on port 6510 on your computer.
2. Start the game in debug mode.
3. The game connects by itself. If the app is not running yet, the game keeps retrying every couple of seconds, so you
   can start them in either order.

To reach an app on **another computer**, give the game a `host[:port]` address:

| Platform | How                                                                     |
|----------|-------------------------------------------------------------------------|
| Desktop  | `-Dflixel.debugHost=192.168.1.20:6510`                                  |
| Web      | `?flixel.mode=debug&flixel.debugHost=192.168.1.20:6510` in the address  |
| Android  | `adb shell am start -n <package>/<activity> -e flixel.debugHost 192.168.1.20:6510` |

The app must also be set to listen on all network interfaces (in its settings) for other machines to reach it. Several
games can connect at once; the app lists all of them.

## A tour of the app

- **Game list** (top bar): every connected game, with its platform, device, and ping. The first game to connect is
  selected automatically. If it disconnects, its last data stays visible and it is selected again when it reconnects.
- **Connect Android device**: runs the `adb reverse` port forwarding for you.
- **Debug menu**: show or hide panels, reset the layout, and open the settings (port, network access, and the MCP
  server for AI agents).
- **Game menu**: hitboxes (F2), pause (F3), free camera (F4), step one frame, and clear the log. The shortcuts work
  anywhere in the app window, except while typing in a text box.
- **Panels**: dockable tabs you can drag, group, and resize. The layout is saved when you close the app.

The game only builds and sends a panel while its tab is visible, so a panel you are not looking at costs the game
nothing.

## The built-in panels

| Panel           | What it shows and does                                                                          |
|-----------------|--------------------------------------------------------------------------------------------------|
| **Stats**       | The newest frame rate, frame time, draw calls, memory, current state, and camera at a glance.    |
| **Performance** | Graphs of frame, render, memory, and network numbers over time. Keeps sampling while closed, so it has history when you open it. |
| **Logs**        | The latest log lines, with level filters, a text filter, and the full message and stack trace of the selected line. |
| **Scene**       | The tree of the current state, its substates, and their groups. Selecting a node selects its object. |
| **Viewport**    | A live map of what a camera draws. Click to select, drag to move objects, right-drag to pan, and scroll to zoom, even on a device without a mouse. |
| **Inspector**   | Every property of the selected object, one section per class, with editors for values that can change. |
| **Textures**    | Every loaded texture with its size, memory, users, a mark for duplicates, and a preview.         |
| **Save**        | A save editor: every value by its dotted path, unsaved changes highlighted, and set, remove, write, and erase. |
| **Watches**     | Every value you registered with `Flixel.debug.watches`.                                          |
| **Controls**    | Pause, step, hitboxes, the free camera, the sample rate, the log level, and the debugger keys.   |
| **Trackers**    | One section per tracker you registered with `Flixel.debug.trackers`.                             |
| **Console**     | A command line with Tab completion and history, for built-in and your own commands.             |

The panels start in a familiar layout: stats, performance, and logs on the left; scene, viewport, inspector, textures,
and save in the middle; watches, controls, and trackers on the right; and the console at the bottom. The textures panel
is great for finding memory waste: a texture marked as a duplicate was loaded twice under different paths.

## Finding objects: selection, ids, and paths

The app is a separate program, so it cannot point at a Java object directly. Instead, every object gets names it can
use:

- **An id**, like a coat-check ticket. The same object always gets the same number, and a number is never reused.
- **A scene path**, a readable address such as `state.3.0`, meaning "the first member of the fourth member of the
  current state." The `select state.3.0` console command uses it.
- **Picking**, which finds the object drawn at a point on the screen, topmost first.

There are four ways to select an object, and they all lead to the same inspector: Alt + click in the game, clicking in
the scene tree, clicking in the viewport map, or code:

```java
Flixel.debug.inspector.select(player);
```

When a tool such as an AI agent selects, moves, or edits an object, the scene panel marks it with an "agent" badge, so
you can always see what a tool did to your game.

## The built-in console commands

| Command                 | Does                                                                     |
|-------------------------|--------------------------------------------------------------------------|
| `help [filter]`         | Lists commands, or only those starting with the filter.                  |
| `pause [on\|off]`        | Pauses or resumes the game.                                              |
| `step`                  | Runs one frame while paused.                                             |
| `hitboxes [on\|off]`     | Shows or hides hitboxes.                                                 |
| `freecam [on\|off]`      | Turns the free camera on or off.                                         |
| `goto <x> <y>`          | Centers the main camera on a world position (turns on the free camera).  |
| `select [path]`         | Selects the object at a scene path, or the one under the mouse.          |
| `resetState`            | Restarts the current state.                                              |
| `loglevel [level]`      | Shows or sets the log level (`Debug`, `Info`, `Warn`, `Error`, `Default`). |
| `rate [hz]`             | Shows or sets how many times per second stats are sampled (1 to 30).     |
| `clear`                 | Clears the log.                                                          |
| `show <panel>`          | Brings a panel to the front, by id or title.                             |
| `close <panel>`         | Closes a panel.                                                          |

Double quotes keep spaces inside one argument: `spawn "big slime" 3`. Adding your own commands is covered in
[Extending the Debugger](debugger-extending.md).

## Debugging on Android

A debug build connects to `127.0.0.1:6510`, which on a phone means the phone itself. Forward the port over USB (the
app's **Connect Android device** button does this for you):

```bash
adb reverse tcp:6510 tcp:6510
```

Run it again after reconnecting the cable or restarting `adb`. For an emulator or a phone on the same network, name
your computer with the `flixel.debugHost` intent extra instead.

A phone has no keyboard for the hotkeys, so use the app: the **Viewport** panel lets you tap to select, drag to move,
and pinch to fly the camera, and the app can press keys, tap, scroll, type text, and take screenshots in the game. A
locked or dozing phone pauses the game, so keep the screen on while debugging.

For logcat, attaching Android Studio's debugger, trace profiling, and native crash stacks, see `ANDROID_DEBUGGING.md` in
the repository root.

## Debugging in the browser

Open the game with `?flixel.mode=debug` (or run `:html5:debug`) and it connects to the app on your computer like any
other platform. Log messages also appear in the browser's developer tools console, with correct file and line numbers,
and uncaught errors show up there too. The browser's own developer tools remain useful for network and performance
questions, such as checking that every asset downloaded.

## Letting an AI agent debug your game

The debugger app has a built-in [MCP](https://modelcontextprotocol.io/) server, so an AI coding agent can debug a
running game the way you do: read stats, logs, and watches; walk the scene; read and edit properties; move objects;
run console commands; press keys; and look at screenshots.

1. In the app, open **Debug > Settings...**, tick **Enable MCP server for AI agents**, and save. It listens only on
   your computer, at `http://127.0.0.1:6511/mcp`.
2. Start your game in debug mode.
3. Add the server to your agent. For example, with Claude Code:

```bash
claude mcp add --transport http flixelgdx http://127.0.0.1:6511/mcp
```

Other agents that speak MCP over Streamable HTTP use the same address; the app's settings dialog shows ready-to-copy
connection text. The tools read the game's **feeds** (a fixed data format meant for programs) and can drive any panel,
including your custom ones, by widget name. Everything an agent changes is marked with the "agent" badge in the scene
and inspector panels. See [Extending the Debugger](debugger-extending.md#feeds-the-debugger-for-programs) for how
feeds relate to panels.

## Debugger code in release builds

Lines such as `Flixel.debug.watches.addFloat(...)` and `Flixel.debug.console.register(...)` are safe to leave in your
code. In a release build the debugger never connects, never samples, and never sends anything; registering a watch or
command creates a few small objects once and then costs nothing. You do not need `if (Flixel.isDebugMode())` around
them, though you can add it if you prefer.

## Other debugging tools

- **Logging.** Still the quickest tool for "did this code run?" See [Logging](logging.md).
- **IDE breakpoints.** On desktop, run the launcher's `main` method in your IDE's debugger, or attach to the JVM. A
  breakpoint freezes the whole game, since everything runs on one thread.
- **Profiler sections.** `Flixel.profiler.begin("name")` and `end()` mark sections that appear in Android system
  traces. See [Optimization](optimization.md).
- **The asset report.** `Flixel.assets.getDiagnostics()` lists every cached asset with its reference count.

## Bug-hunting recipes

| Symptom                                    | Try this                                                                                     |
|--------------------------------------------|----------------------------------------------------------------------------------------------|
| "I fall through the floor."                | F2 for hitboxes. Is the floor green (immovable)? Pause and step through the landing frame.   |
| "This object is not where I think it is."  | Alt + click it, read its position in the inspector, then drag it in the viewport to test.    |
| "The game stutters every few seconds."     | Watch the memory graph in the performance panel. A sawtooth means garbage; see [Optimization](optimization.md). |
| "A value is wrong but I do not know when."   | Add a watch for it and step frame by frame.                                                  |
| "A level is hard to reach for testing."    | Register a console command that jumps there, or edit the save in the save panel.             |
| "Memory keeps growing."                    | Check the textures panel for duplicates and textures that never unload; check the scene panel for objects that pile up. |
| "It only happens on my phone."             | Connect the phone with `adb reverse` and use the viewport and inspector remotely.            |

## Check your understanding

1. Your game is running on a phone over USB, but the app never sees it. What command do you run?
2. You leave `Flixel.debug.watches.addFloat("Speed", player::getVelocityX)` in a release build. What does it cost?
3. A wall shows a blue hitbox and the player walks through it. What is wrong with the wall?

(Answers: `adb reverse tcp:6510 tcp:6510`, or use the app's Connect Android device button; almost nothing: a few
objects created once, and nothing runs per frame in release mode; it does not collide, because it is not solid or has
no allowed collision sides.)

**Next:** [Extending the Debugger](debugger-extending.md)
