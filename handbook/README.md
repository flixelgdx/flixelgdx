# The FlixelGDX Handbook

Welcome! This handbook is the main guide to using FlixelGDX, a 2D game framework for Java that runs the same game on
desktop, in the browser, and on Android. It explains every part of the framework in plain language, with analogies,
working code examples, and short quizzes, ordered from what you need on day one to what is good to know later.

## Table of contents

1. [How to use this handbook](#how-to-use-this-handbook)
2. [Suggested reading paths](#suggested-reading-paths)
3. [Part 1: Foundations](#part-1-foundations)
4. [Part 2: Building gameplay](#part-2-building-gameplay)
5. [Part 3: Data and resources](#part-3-data-and-resources)
6. [Part 4: Graphics and the platform](#part-4-graphics-and-the-platform)
7. [Part 5: Debugging and performance](#part-5-debugging-and-performance)
8. [Part 6: Shipping and under the hood](#part-6-shipping-and-under-the-hood)
9. [Appendices](#appendices)
10. [Other documentation](#other-documentation)

## How to use this handbook

- **Every guide stands on its own.** Each one starts with its own table of contents and links to related guides when
  it touches their topics, so you can jump straight to what you need.
- **Guides follow the same shape:** why the feature exists, the mental model (usually an analogy), how to use it with
  real code, common mistakes, and a short "Check your understanding" quiz with answers.
- **Code examples use real FlixelGDX APIs.** Copy them into your project and adapt them. Imports are shown in longer
  examples; shorter snippets assume the usual `org.flixelgdx` imports.
- **The [Cookbook](cookbook.md)** answers quick "How do I...?" questions in one or two lines each, and the
  [Glossary](glossary.md) defines every term.
- **The Javadoc goes deeper.** Every class and package in the framework is documented in detail. This handbook is the
  map; the Javadoc is the street-level view.

## Suggested reading paths

**Brand new to FlixelGDX (or to game development):** read Part 1 in order, then [Groups and Recycling](groups-and-recycling.md),
[Physics and Collisions](physics-and-collisions.md), and [Input](input.md). You can then build a complete small game.
Dip into everything else as you need it.

**Coming from HaxeFlixel:** skim the [Introduction](introduction.md) and [The Flixel Class](the-flixel-class.md), then
read [Optimization](optimization.md) and [Collections and Pooling](collections-and-pooling.md), which cover the biggest
differences: FlixelGDX is designed to create no garbage per frame and uses no reflection.

**Experienced and in a hurry:** the [Cookbook](cookbook.md), then [Architecture](architecture.md) and
[The Boot Sequence](boot-sequence.md) for how the pieces fit together.

**Preparing a release:** [Platform Guide](platforms.md), [Building and Shipping](building-and-shipping.md), and
[Optimization](optimization.md).

## Part 1: Foundations

The core ideas everything else builds on. Read these first, in order.

| # | Guide | What you will learn |
|---|-------|---------------------|
| 1 | [What FlixelGDX Is: The Mental Model](introduction.md) | The theater analogy, the five big ideas, the rules of the house, and the modules. |
| 2 | [Getting Started: Your First Project and Your First Game](getting-started.md) | Tools, creating a project, the project layout, launchers, and a first game in two classes. |
| 3 | [The Game Loop: Create, Update, Draw, and Destroy](game-loop.md) | What `elapsed` is, the exact order of a frame, time scale, pausing, and platform events. |
| 4 | [States and Substates: Organizing Your Game into Screens](states-and-substates.md) | Switching and restarting states, passing data, pause menus, and transitions. |
| 5 | [Basics, Objects, and Sprites: The Building Blocks](sprites-and-objects.md) | Lifecycle flags, kill versus destroy, graphics, scaling, tinting, hitboxes, and draw order. |
| 6 | [The Flixel Class: Your Toolbox of Global Services](the-flixel-class.md) | Every global service, screen-size and time helpers, and global signals. |

## Part 2: Building gameplay

Everything that turns a scene into a game.

| # | Guide | What you will learn |
|---|-------|---------------------|
| 7 | [Groups and Recycling: Managing Many Objects at Once](groups-and-recycling.md) | Group types, garbage-free iteration, recycling, sprite groups, and sorting. |
| 8 | [Movement, Physics, and Collisions](physics-and-collisions.md) | Velocity, acceleration, drag, platformer and top-down movement, overlap and collide. |
| 9 | [Input: Keyboard, Mouse, Touch, and Gamepads](input.md) | Polling, the three questions, screen versus world space, gamepads, and typed text. |
| 10 | [Input Actions: Rebindable Controls for Every Device](input-actions.md) | Digital and analog actions, bindings, menu repeat, rebinding screens, and Steam Input. |
| 11 | [Animation: Spritesheets, Atlases, Rigs, and State Machines](animation.md) | Grid sheets, Sparrow atlases, offsets, frame events, Adobe Animate rigs, and state machines. |
| 12 | [Cameras: Following, Effects, HUDs, and Split Screens](cameras.md) | Following, bounds, zoom, shake, flash, fade, parallax, HUD cameras, and viewports. |
| 13 | [Text, HUDs, and Bars](text-and-ui.md) | Text, fonts, borders, wrapping, garbage-free text, bars, and simple buttons. |
| 14 | [Audio: Sound Effects, Music, and Volume](audio.md) | Sounds, music, fades, groups, persistence across states, and platform notes. |
| 15 | [Tweens and Timers: Smooth Changes and Delayed Actions](tweens-and-timers.md) | Goals, eases, tween types, chaining, motion paths, and global and per-state timers. |
| 16 | [Signals: Events Without Tangled Code](signals.md) | Creating and dispatching signals, global signals, leaks, and garbage. |
| 17 | [Particles: Sparks, Smoke, Rain, and Explosions](particles.md) | Emitters, bursts and streams, ranges, shapes, custom particles, and recipes. |

## Part 3: Data and resources

Loading files, saving progress, and working with data.

| # | Guide | What you will learn |
|---|-------|---------------------|
| 18 | [Files and Assets: Loading, Caching, and Loading Screens](files-and-assets.md) | File roots, reading and writing, the asset manager, loading screens, and reference counting. |
| 19 | [Saving Progress: Persistent Game Data](saving.md) | Bind, write, flush, slots, defaults, saving safely on phones and browsers, and custom storage. |
| 20 | [JSON and XML: Load, Parse, and Handle Data Formats](data-serialization.md) | Reading and writing JSON, `@JsonSerializable` code generation, XML, and data recipes. |
| 21 | [Collections and Pooling: Garbage-Free Lists, Maps, and Pools](collections-and-pooling.md) | `FlixelArray`, `FlixelMap`, primitive collections, snapshot iteration, and object pools. |
| 22 | [Math, Randomness, Colors, and Utilities](math-and-utilities.md) | `FlixelMath`, random numbers and seeds, vectors, bounds and ranges, and colors. |

## Part 4: Graphics and the platform

How things are drawn, and how the game talks to the world around it.

| # | Guide | What you will learn |
|---|-------|---------------------|
| 23 | [Graphics, Rendering, and Shaders](graphics-and-shaders.md) | Batching, design size versus render resolution and scale, blend modes, render targets, and WGSL shaders. |
| 24 | [Window and Platform Services](window-and-platform-services.md) | The window, fullscreen, clipboard, notifications, file dialogs, vibration, and device facts. |

## Part 5: Debugging and performance

Finding out what your game is doing, and making it fast.

| # | Guide | What you will learn |
|---|-------|---------------------|
| 25 | [Logging: Messages, Levels, Tags, and Sinks](logging.md) | Log levels, placeholders, tagged loggers, log files, and custom sinks. |
| 26 | [Debugging and the Debugger App](debugging.md) | Debug mode, hotkeys, the debugger app and its panels, phones, browsers, and AI agents. |
| 27 | [Extending the Debugger: Watches, Commands, Inspectors, and Trackers](debugger-extending.md) | Live values, console commands, inspector sections, trackers, and feeds. |
| 28 | [Custom Debugger Panels: The Widget Toolkit](debugger-panels.md) | Building panels, widgets, tables, graphs, events, and replacing built-in panels. |
| 29 | [Optimization: Writing Fast, Garbage-Free Games](optimization.md) | Why garbage stutters, measuring, the seven habits, drawing, collisions, and memory. |

## Part 6: Shipping and under the hood

Getting the game to players, and how the framework works inside.

| # | Guide | What you will learn |
|---|-------|---------------------|
| 30 | [Platform Guide: Desktop, Web, and Android](platforms.md) | Requirements, feature differences, and the gotchas of each platform. |
| 31 | [Building and Shipping: Packaging for Desktop, Web, and Android](building-and-shipping.md) | The packagr, HTML5, shader, and Basis Universal plugins, and a release checklist. |
| 32 | [Architecture: Core, Backends, and the Seams Between Them](architecture.md) | Modules, seams, no-op defaults, the shared WebGPU renderer, threads, and the package map. |
| 33 | [The Boot Sequence: How a Game Starts Up](boot-sequence.md) | Every step from `main` to the first frame, shutdown, web and Android startup, and boot callbacks. |
| 34 | [Extending the Framework: Custom Services, Loaders, and Backends](extending-the-framework.md) | Custom asset types, mod folders, save storage, log sinks, replacing services, and new backends. |

## Appendices

| Guide | What it is |
|-------|------------|
| ["How Do I...?" Cookbook](cookbook.md) | Quick answers to common questions, grouped by topic. |
| [Glossary](glossary.md) | Plain-language definitions of every term in the handbook. |

## Other documentation

- **The Javadoc.** Every package has a detailed guide in its `package-info.java`, starting with the root
  `org.flixelgdx` package, which is a complete beginner's tutorial of its own.
- **[`README.md`](../README.md)**: the project overview and feature tour.
- **[`ARCHITECTURE.md`](../ARCHITECTURE.md)**: the module layout and the rendering contract, for contributors.
- **[`ANDROID_DEBUGGING.md`](../ANDROID_DEBUGGING.md)**: logcat, Android Studio, profiling, and native crash stacks.
- **[`DEBUG_PROTOCOL.md`](../DEBUG_PROTOCOL.md)**: the wire protocol between a game and the debugger app.
- **[`COMPILING.md`](../COMPILING.md)** and **[`CONTRIBUTING.md`](../CONTRIBUTING.md)**: building the framework and
  contributing to it.
- **The plugin READMEs** in [`flixelgdx-plugins/`](../flixelgdx-plugins): full option references for every Gradle plugin.

Have fun making something great!
