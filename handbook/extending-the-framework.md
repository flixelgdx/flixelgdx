# Extending the Framework: Custom Services, Loaders, and Backends

FlixelGDX is built from replaceable pieces. This guide shows the extension points, from small (a custom asset type, a
log destination, a save location, a mod folder) to large (a new platform backend), and explains when and where to
plug each one in.

## Table of contents

1. [Extend, do not fork](#extend-do-not-fork)
2. [When to plug things in](#when-to-plug-things-in)
3. [Custom asset types](#custom-asset-types)
4. [Mod support: redirecting asset paths](#mod-support-redirecting-asset-paths)
5. [Custom save storage](#custom-save-storage)
6. [Custom log destinations](#custom-log-destinations)
7. [Replacing a platform service](#replacing-a-platform-service)
8. [Gamepad mappings and custom bindings](#gamepad-mappings-and-custom-bindings)
9. [Camera scaling for every camera](#camera-scaling-for-every-camera)
10. [Custom eases and tween types](#custom-eases-and-tween-types)
11. [Your own class hierarchy in states and groups](#your-own-class-hierarchy-in-states-and-groups)
12. [Debugger extensions](#debugger-extensions)
13. [Headless runs for tests and tools](#headless-runs-for-tests-and-tools)
14. [Writing a new backend](#writing-a-new-backend)
15. [Contributing to FlixelGDX itself](#contributing-to-flixelgdx-itself)
16. [Check your understanding](#check-your-understanding)

## Extend, do not fork

Copying the framework's source to change one behavior means you stop receiving fixes and features. Every global
service in FlixelGDX is an interface with a replaceable implementation (see [Architecture](architecture.md)), so you
can almost always plug in your own piece instead. Think of a power strip: you do not rewire the house to add a lamp;
you plug the lamp into a socket that was designed for it.

## When to plug things in

Timing matters, because some pieces must be in place before anything uses them:

| Moment                                      | Use it for                                                                |
|---------------------------------------------|---------------------------------------------------------------------------|
| `Flixel.boot.beforeStart(...)`              | Replacing platform services (files, save handler, sound factory) before the core managers are created. |
| `Flixel.boot.afterStart(...)`               | Configuring core managers (input, sound, logging, debugger) before the window opens. |
| Your game's or first state's `create()`     | Anything that needs the GPU or the window: loaders that upload textures, shaders. |

If your extension needs to follow the game's lifecycle (update every frame, draw on top of a camera, save when the
app is suspended, clean up when the game closes), register a manager in `Flixel.managers` from an `afterStart(...)`
callback instead of asking users to subclass `FlixelGame`. See
[Global hooks with managers](game-loop.md#global-hooks-with-managers).

Register boot callbacks before the game starts, for example in your game class's constructor. See
[The Boot Sequence](boot-sequence.md#hooking-into-startup-boot-callbacks).

## Custom asset types

Teach the asset manager a new file type with a `FlixelAssetLoader`, registered per file extension. Loading has two
stages so platforms with threads can do the slow part in the background:

1. `loadRaw(...)` reads and parses the file into a raw object. It may run on a worker thread, so it must not touch the
   GPU or other main-thread-only systems.
2. `finishRaw(...)` (optional) runs on the main thread to finish the object, such as uploading pixels to the GPU.

`createHandle(...)` builds the handle game code receives from `Flixel.assets.get(...)`.

```java
// A level format: JSON text parsed into a LevelData object.
Flixel.assets.registerLoader(".level", new FlixelAssetLoader<LevelData>() {
  @Override
  public Object loadRaw(FlixelAssetManager assets, String path, FlixelFile file) {
    return LevelDataJsonSerializer.fromJson(FlixelJson.parse(file.readString()));
  }

  @Override
  public FlixelAsset<LevelData> createHandle(FlixelAssetManager assets, String path) {
    return new FlixelDefaultAsset<>(assets, path);
  }
});

// Then it works like any other asset:
Flixel.assets.load("levels/forest.level");
LevelData forest = Flixel.assets.<LevelData>get("levels/forest.level").get();
```

## Mod support: redirecting asset paths

Every asset read in the framework asks a **file resolver** which file a path refers to. Replace it to let players
override assets with their own, for example from a `mods` folder next to the game:

```java
Flixel.assets.setFileResolver(path -> {
  FlixelFile modded = Flixel.files.local("mods/" + path);
  if (modded.exists()) {
    return modded; // A player's replacement wins.
  }
  return Flixel.files.internal(path);
});
```

Because every load goes through the resolver, sprites, sounds, and data all pick up mods without other code changes.

## Custom save storage

Give one save object its own `FlixelSaveHandler` (read, write, delete, exists), or change the default for every new
save object by setting `FlixelSave.defaultHandler` in a `beforeStart` callback:

```java
Flixel.boot.beforeStart(() -> FlixelSave.defaultHandler = new CloudSyncSaveHandler());
```

A save with a custom handler skips the company-name check. See [Saving Progress](saving.md#custom-storage-cloud-saves-and-tests).

## Custom log destinations

Add a `FlixelLogSink` to send log entries anywhere (a crash reporter, an in-game console, analytics), or replace the
console or file sink entirely:

```java
Flixel.boot.afterStart(() -> {
  Flixel.log.addSink(new CrashReporterSink());       // In addition to the console.
  // Flixel.log.setConsoleSink(new MyConsoleSink()); // Instead of the platform's console output.
});
```

Remember that the entry passed to `write(...)` is reused for every message, so copy what you need. See
[Logging](logging.md#custom-sinks-sending-logs-anywhere).

## Replacing a platform service

Every field on `Flixel` that holds a service can be reassigned. Do it early, so nothing has captured the old one:

```java
Flixel.boot.beforeStart(() -> {
  FlixelFiles platformFiles = Flixel.files;
  Flixel.files = new EncryptedAssetFiles(platformFiles); // Wraps the platform's files, decrypting on read.
});
```

Wrapping the platform's implementation (instead of writing everything from scratch) is usually the right move: your
class adds one behavior and passes everything else through. Services you might replace or wrap this way include
`Flixel.files`, `Flixel.alert` (a styled in-game error screen instead of a native box), `Flixel.profiler` (send
sections to your own profiler), and `FlixelSoundManager.defaultFactory`.

## Gamepad mappings and custom bindings

- Add support for unusual controllers with `Flixel.gamepads.addMappingResolver(...)`. Your resolvers are asked first.
  See [Input](input.md#unusual-controllers).
- Write your own [input action](input-actions.md) bindings by implementing `FlixelDigitalBinding` (`evaluate()`) or `FlixelAnalogBinding`
  (`accumulate(vector)`).

## Camera scaling for every camera

`FlixelCamera.viewportFactory` builds the viewport of every camera the framework creates, including the default camera
after each state switch. Replace it to change the scaling policy everywhere:

```java
FlixelCamera.viewportFactory = (w, h) -> new FlixelViewport(w, h, FlixelViewport.Scaling.EXTEND);
```

See [Cameras](cameras.md#fitting-any-window-viewports-and-scaling).

## Custom eases and tween types

An ease is a `FlixelEaseFunction`: any function from time (0 to 1) to progress:

```java
FlixelEaseFunction wobble = t -> t + (float) Math.sin(t * Math.PI * 6) * 0.05f * (1f - t);
FlixelTween.tween(sprite, new FlixelTweenSettings()
    .addGoal(sprite::getX, 300f, sprite::setX)
    .setDuration(1f)
    .setEase(wobble));
```

For a completely new kind of tween, subclass `FlixelTween`, implement `updateTweenValues()`, and register a factory so
the tween pool can create and reuse it:

```java
FlixelTween.registerTweenType(MyTween.class, () -> new MyTween(null));
```

## Your own class hierarchy in states and groups

States and groups accept anything that implements `IFlixelBasic`, not only subclasses of `FlixelBasic`. If your class
must extend something else, implement the interface (update, draw, destroy, kill, revive, and the lifecycle flags)
and add it like any other object. For finer needs, implement only the small capability interfaces a system needs, such
as `FlixelPositional` for a camera target or `FlixelColorable` for a color tween. See
[Architecture](architecture.md#the-object-model-small-capability-interfaces).

## Debugger extensions

Watches, console commands, inspector sections, trackers, custom hitboxes, custom scene children, and whole panels are
all extension points of the debugger. See [Extending the Debugger](debugger-extending.md) and
[Custom Debugger Panels](debugger-panels.md).

## Headless runs for tests and tools

`FlixelGameRunner.NOOP` is a runner that opens no window and runs no loop. With it, `Flixel.start(...)` wires up the
core and returns, which is how the framework's own unit tests run without a GPU. You can do the same to test game
logic (scoring rules, save migration, level parsing) in plain JUnit tests:

```java
Flixel.start(new MyGame(), FlixelGameRunner.NOOP);
// Core managers exist now. Graphics is the no-op manager, so skip anything that needs the GPU.
```

Keep game rules in plain classes that do not need a GPU, and these tests stay fast and simple.

## Writing a new backend

A backend is a launcher plus implementations of the seams. The shape is always the same as the built-in launchers:

1. Install logging first (`Flixel.log.setConsoleSink(...)`, a site resolver, and optionally a file sink).
2. Create and assign the platform services: `Flixel.window`, `Flixel.graphics`, `Flixel.input`, `Flixel.files`,
   `Flixel.assets`, `Flixel.host`, `Flixel.alert`, `Flixel.runtime`, `FlixelSave.defaultHandler`,
   `FlixelSoundManager.defaultFactory`, and the font rasterizer. Anything you skip stays a safe no-op. Your runtime
   device should keep a `FlixelMainThreadQueue` for `queueMainThread(...)` and `runMainThreadTasks()`.
3. Register an `afterStart` callback for pieces that attach to core managers (gamepad provider, cursor manager).
4. Set the runtime mode, and install a `FlixelDebugTransport` if the platform can reach the debugger app.
5. Write a `FlixelGameRunner` that creates the window, calls `game.create()` once the surface is ready, then each frame
   `Flixel.runtime.runMainThreadTasks()`, `advanceTime(...)`, `beginFrame()`, `update(...)`, `draw(...)`, and `endFrame()`, plus `resize(...)` and the focus and suspend
   hooks. Call `game.destroy()` on exit.
6. Call `Flixel.start(game, runner)`.

For graphics, a new platform usually only needs a jwebgpu backend, a surface for its window, and a device, then hands
them to the shared `FlixelGpuGraphics` renderer. A new platform can also define its own `FlixelPlatform` id with
`FlixelPlatform.of("MyConsole")`. Read the desktop, web, and Android launchers in the framework's source as working
examples, and the `org.flixelgdx.debug.remote` package documentation for the debugger transport contract.

## Contributing to FlixelGDX itself

If your extension would help everyone, consider contributing it. The repository's `CONTRIBUTING.md` explains the
process, and `COMPILING.md` explains how to build the framework and test it in a game project (through
`publishToMavenLocal`, a composite build, or JitPack).

## Check your understanding

1. Where do you replace `FlixelSave.defaultHandler`, and why there?
2. How can players override a sprite's image without you changing any loading code?
3. Your custom asset loader uploads a texture inside `loadRaw(...)` and crashes on desktop. Why?

(Answers: in a `beforeStart` boot callback, before any save object is created with the old default; with a custom
file resolver on `Flixel.assets` that checks a mods folder first; `loadRaw(...)` may run on a worker thread, and GPU
work must happen on the main thread, in `finishRaw(...)`.)

**Next:** [Cookbook](cookbook.md)
