# FlixelGDX

FlixelGDX is a high-level 2D game framework built on top of [libGDX](https://libgdx.com/). It aims to replicate the powerful and developer-friendly API of [HaxeFlixel](https://haxeflixel.com/) (based on the original ActionScript [Flixel](http://www.flixel.org/)) while leveraging the robust cross-platform capabilities and performance of the libGDX ecosystem.

The goal of FlixelGDX is to provide the familiar Flixel-like structure that developers love, while remaining open for experienced libGDX developers to integrate their own lower-level functionality.

> [!NOTE]
> FlixelGDX is an independent project and is not officially affiliated with HaxeFlixel or libGDX.

## Table of contents

- [Introduction](#flixelgdx)
- [Project navigation](#project-navigation)
- [Supported platforms](#supported-platforms)
- [Goals](#goals)
- [Open architecture](#open-architecture)
- [Features](#features)
- [Entity & sprite basics](#entity--sprite-basics)
- [State management & substates](#state-management--substates)
- [Group system](#group-system)
- [Tweening](#tweening)
- [Assets](#assets)
- [Audio](#audio)
- [Reflection](#reflection)
- [Text](#text)
- [Signals, cameras & debugging](#signals-cameras--debugging)
- [Input handling](#input-handling)
- [Logging & debugging](#logging--debugging)

# Project Navigation

- **[Contributing Guide](CONTRIBUTING.md)**: Learn how to contribute to the project, coding standards, and PR requirements.
- **[Project Structure](PROJECT.md)**: Understand the multi-module layout and how Gradle is used.
- **[Compiling & Testing](COMPILING.md)**: How to build the framework and test it as a dependency in your own projects.

# Supported Platforms

FlixelGDX supports the following platforms through its modular backend system:

- **Desktop**: Windows, macOS, and Linux via LWJGL3.
- **Android**: Full support for Android mobile devices.
- **iOS**: Support via [MobiVM](https://github.com/MobiVM/robovm).
- **Web**: Support via TeaVM.

> [!WARNING]
> FlixelGDX is a *modern* framework, which means it requires *modern* Java. You must use Java 17 or higher to be able
> to use FlixelGDX. Because of this, web games don't (and can't) support GWT; it is done through a modern
> library called TeaVM, which transpiles Java code to JavaScript to be run in a browser.

# Goals

## Replicate Flixel API

FlixelGDX attempts to bring the ease of use and rapid prototyping capabilities of HaxeFlixel to Java, while also
using the tools of libGDX to allow it to be seamlessly integrated into new and existing projects alike.

## libGDX Integration

We use libGDX's standard lifecycle and incorporate Flixel's methods (`update()`, `draw()`, `destroy()`) and performance-oriented tools (like `Poolable` interfaces)
to ensure that lifecycle and performance are handled correctly.

Because everything is built on top of libGDX, adding FlixelGDX to a new or existing project will be a breeze.

We also provide dedicated launcher classes for every platform, so you can run your game without any additional setup.

### Example of Making a Basic Game in a New Project

When you use [gdx-liftoff](https://github.com/libgdx/gdx-liftoff), you can quickly generate a new libGDX project, then you
can add FlixelGDX as a dependency and start coding your game right away.

In your core code, you can simply extend `FlixelGame` and start coding your game:

```java
public class MyGame extends FlixelGame {
  
  // Make sure to create a constructor so you can use it in a launcher!  
  public MyGame(String title, int width, int height, FlixelState initialState) {
    super(title, width, height, initialState);
  }

  @Override
  public void create() {
    super.create();
    // Your game initialization code here!
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    // Your global game loop update logic here!
  }

  @Override
  public void draw(Batch batch) {
    super.draw(batch);
    // Your global game loop drawing logic here!
  }

  @Override
  public void close() {
    super.close();
    // Your game cleanup code here!
  }
}
```

Then, create a new state class for your game to enter into when it starts:

```java
public class PlayState extends FlixelState {

  private FlixelSprite player;

  @Override
  public void create() {
    super.create();
    player = new FlixelSprite();
    player.makeGraphic(16, 16, Color.RED);
    player.setX(100);
    player.setY(100);
    add(player);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);

    // Move the player with WASD!
    if (Flixel.keys.pressed(FlixelKey.W)) {
      player.changeY(-10);
    }
    if (Flixel.keys.pressed(FlixelKey.A)) {
      player.changeX(-10);
    }
    if (Flixel.keys.pressed(FlixelKey.S)) {
      player.changeY(10);
    }
    if (Flixel.keys.pressed(FlixelKey.D)) {
      player.changeX(10);
    }
  }
}
```

Then, in a platform-specific launcher class, you can create an instance of your game and start it:

```java
// Example of how to create a new game instance and run it using the LWJGL3 launcher.
public class Lwjgl3Launcher {

  public static void main(String[] args) {
    if (StartupHelper.startNewJvmIfRequired()) { // This handles macOS support and helps on Windows.
      return;
    }

    // Create a new game instance and pass the initial state.
    MyGame game = new MyGame(
      "My Game",
      800,
      600,
      new PlayState() // The initial state the game enters when it starts!
    );

    // Start the game using the LWJGL3 launcher.
    FlixelLwjgl3Launcher.launch(game);
  }
}
```

## Open Architecture

We keep the "hood open" for developers who need to drop down into standard libGDX code when necessary. This allows you to
use the full power of libGDX without having to reinvent the wheel or use hacky workarounds, while also keeping things
simple and easy to understand by default for newbies.

One example is in the `FlixelCamera` class. While by default it works just like an `FlxCamera` from the original Flixel library,
it secretly uses a libGDX `Camera` and `Viewport` under the hood, and we allow you to access and modify these directly.

### Code Example

```java
public class Custom3DCamera extends FlixelCamera {
    
  public Custom3DCamera(float x, float y, int width, int height, float zoom) {
    // You can add your own custom camera and viewport directly!
    super(x, y, width, height, zoom, new PerspectiveCamera(), new ExtendViewport(640, 360));
  }
}
```

## Features

- **Structural hierarchy**: Logic and rendering via `FlixelBasic`, `FlixelObject`, and `FlixelSprite`.
- **State management**: State switching and substates for menus, pauses, and transitions.
- **Group system**: Batch updates, collisions, and nested groups.
- **Tweening**: Property, reflection, motion, color, shake, flicker, paths; static helpers on `FlixelTween` for the global manager.
- **Assets**: `Flixel.assets` wraps typed loading (`FlixelSource`, extension registry) on top of libGDX `AssetManager`.
- **Audio**: `Flixel.sound` (MiniAudio-backed SFX/music groups, master volume, focus pause/resume).
- **Reflection**: Pluggable `Flixel.reflect` for fields, properties, paths (used by var tweens and tooling).
- **Text**: `FlixelText` (bitmap and FreeType fonts, borders, alignment) as a `FlixelSprite` subclass.
- **Input**: Keyboard helpers via `Flixel.keys`; touch/mouse still through libGDX.
- **Logging & debugging**: `Flixel.log`, `Flixel.watch`, optional debug overlay.
- **Signals**: `Flixel.Signals` for pre/post update, draw, state switch, window focus, and more.
- ...and much more!

### Entity & Sprite Basics

FlixelGDX keeps the familiar `FlxSprite`-style workflow that HaxeFlixel developers expect, but in plain Java and on top of libGDX.

```java
public class Player extends FlixelSprite {

  public Player() {
    // Create a 16×16 white box at (0, 0).
    makeGraphic(16, 16, Color.WHITE);

    // Basic positioning and motion come from FlixelObject.
    setX(100);
    setY(80);
    velocityX = 60;  // move right (velocityY, acceleration, drag, etc. also available)
  }
}
```

**How it works**

The entity hierarchy mirrors HaxeFlixel:

- **`FlixelBasic`** — The base for everything. It has no position or graphics, only lifecycle flags: `active` (whether `update()` runs), `exists` (whether it is updated and drawn), `alive` (for game logic), and `visible` (whether it is drawn). Override `update(float)` and `draw(Batch)` for custom behavior; use `kill()` / `revive()` to temporarily disable or bring back objects.
- **`FlixelObject`** — Adds position (`x`, `y`), size (`width`, `height`), rotation (`angle`), and physics-style motion: velocity, acceleration, drag, and max velocity (X/Y and angular). Set `moves = true` (default) so `update()` calls `updateMotion()`, which applies velocity and acceleration each frame.
- **`FlixelSprite`** — Adds rendering: textures, spritesheets, animations, scale, origin, offset, color tint, and flip. Use `loadGraphic()` for images, `makeGraphic()` for solid rectangles, or `loadSparrowFrames()` for XML-based atlases; then `addAnimation()` / `addAnimationByPrefix()` and `playAnimation()` for frame-based animation.

So you get a clear separation: logic and “does it exist?” in `FlixelBasic`, position and movement in `FlixelObject`, and drawing in `FlixelSprite`. That keeps code organized whether you’re prototyping or building a larger game.

#### Using entities inside a FlixelGDX game

In a FlixelGDX project you add sprites (and other `FlixelBasic` objects) to a state with `add()`. The state’s update/draw loop will then update and draw them automatically, and you can use groups to organize them (see Group System).

```java
public class PlayState extends FlixelState {
  private Player player;

  @Override
  public void create() {
    player = new Player();
    add(player); // managed and updated by the state
  }
}
```

Because `FlixelState` implements libGDX's `Screen` API, this integrates cleanly with a normal libGDX launcher.

#### Using FlixelGDX sprites/objects in a regular libGDX project

You can use `FlixelSprite` and `FlixelObject` even if you don’t use `FlixelGame` or `FlixelState`. Construct them, call `loadGraphic()` or `makeGraphic()` as needed, and then in your own `Screen` or `ApplicationListener` call `sprite.update(delta)` and `sprite.draw(batch)` each frame. You’re responsible for the update/draw order and lifecycle; FlixelGDX doesn’t require a specific game loop.

### State Management & SubStates

States give you a high-level way to structure screens (menus, loading, gameplay, pause overlays, etc.), similar to HaxeFlixel's `FlxState` and `FlxSubState`. Each state is a `Screen` and a group: you override `create()`, `update(float)`, and `draw(Batch)` and add sprites/groups with `add()`.

**How it works**

- **`FlixelState`** extends `FlixelGroup<FlixelBasic>` and implements libGDX’s `Screen`. When the game switches to a state, `create()` is called once; then each frame the state’s `update(elapsed)` and `draw(batch)` run, which in turn update and draw all members you added. Use `Flixel.switchState(newState)` to transition; the old state is disposed automatically.
- **`FlixelSubState`** is a state that is opened *on top of* another state. By default the parent state stops updating (`persistentUpdate = false`) but keeps drawing (`persistentDraw = true`), so you get a classic “pause overlay” or modal dialog. The substate has `openCallback` and `closeCallback`; call `close()` from inside the substate to remove it and resume the parent.
- **Outros.** Override `startOutro(onOutroComplete)` on a state to run an exit animation or transition before the switch; when done, call the callback so the framework proceeds with the state change.

```java
public class MenuState extends FlixelState {

  @Override
  public void create() {
    // Set your background color and add sprites/buttons here.
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);

    if (Flixel.keys.justPressed(FlixelKey.ENTER)) {
      Flixel.switchState(new PlayState());
    }
  }
}
```

Substates are ideal for pause menus, dialogs, or any overlay that should block input to the underlying state:

```java
public class PauseSubState extends FlixelSubState {

  public PauseSubState() {
    super(new Color(0, 0, 0, 0.5f)); // semi-transparent overlay
  }

  @Override
  public void create() {
    // Add resume button, etc.
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
      close(); // returns to parent state
    }
  }
}
```

From the play state you open it with:

```java
openSubState(new PauseSubState());
```

You can nest substates (a substate can open another substate). Control whether the parent keeps updating or drawing while a substate is open via `persistentUpdate` and `persistentDraw` on the parent state.

#### Using states in a regular libGDX project

In a standard FlixelGDX setup you pass the initial `FlixelState` to `FlixelGame` and use `Flixel.switchState(...)` for transitions; the game handles `Screen` switching and disposal. If you embed FlixelGDX in an existing libGDX game, you can still instantiate states and call their `create()`, `update()`, and `draw()` yourself, then use `Flixel.switchState()` only if you’ve also initialized `Flixel` with `FlixelGame`; otherwise manage which state is “current” and call its methods from your own render loop.

### Group System

Groups batch-update and draw many objects so you don’t hand-write loops everywhere. `FlixelState` is a `FlixelGroup<FlixelBasic>`, so when you `add()` sprites to a state you’re already using the group system.

**How it works**

- **`FlixelGroup<T>`** holds a list of `FlixelBasic` members. Each frame it iterates over them and calls `update(elapsed)` then `draw(batch)`. You `add()` and `remove()` members; `clear()` removes all without destroying them; `destroy()` destroys every member and clears the list.
- **Capacity.** The constructor takes an optional `maxSize`. If `maxSize > 0`, `add()` won’t add when the group is full, which helps for fixed-size object pools (e.g. bullets).
- **Iteration.** Use `forEachMember(callback)` to run logic on every member, or `forEachMemberType(Class<C>, callback)` to iterate only over members of a given type (e.g. all `FlixelSprite` or your own `Enemy` class).

```java
public class EnemyGroup extends FlixelGroup<FlixelBasic> {

  public EnemyGroup() {
    super(0); // 0 = unlimited size
  }

  public void spawnEnemy(float x, float y) {
    Enemy enemy = new Enemy(x, y);
    add(enemy);
  }
}
```

Inside any `FlixelState` you can mix groups and single objects:

```java
EnemyGroup enemies = new EnemyGroup();
add(enemies);
enemies.spawnEnemy(200, 120);

// Or iterate over a specific type
enemies.forEachMemberType(Enemy.class, enemy -> enemy.setTarget(player));
```

Groups can contain other groups (nested structure). Because everything uses libGDX’s `Batch` and the same update/draw lifecycle, this fits into an existing libGDX game while giving you a Flixel-style hierarchy.

#### Using groups in a regular libGDX project

Use `FlixelGroup` the same way: create a group, `add()` your `FlixelBasic` (or `FlixelSprite`) instances, and each frame call `group.update(delta)` and `group.draw(batch)` from your own game loop. You don’t need `FlixelGame` or `FlixelState` for the group logic to work.

### Tweening

FlixelGDX includes a tweening system inspired by Flixel/HaxeFlixel. You can use a **fluent builder** (`FlixelTween.tween(tweenClass, builderClass)`), **static factories** on `FlixelTween` (motion, color, shake, and more), or **`FlixelTween.tween(object, settings)`** when goals are already on `FlixelTweenSettings`.

#### FlixelGDX vs Universal Tween Engine (UTE)

In UTE, tweening a type typically requires:

1. **Implement a `TweenAccessor<YourObjectType>`**: you define how to get/set values by index (e.g. index 0 = x, 1 = y).
2. **Map each value**: you manually assign which index corresponds to which field and implement `getValues()` / `setValues()` for every target type.
3. **Manually define updates**: you copy values in and out of a float array and handle each index yourself.
4. **Register the accessor**: `Tween.registerAccessor(YourObjectType.class, yourAccessor)` before use.
5. **Then tween**: finally you can call the engine with the object, type, and duration.

FlixelGDX avoids per-type accessors. You declare *what* to tween at the call site (getter/setter pairs, reflection names, or a dedicated tween type).

**UTE-style (conceptual):**

```text
1. Write TweenAccessor<FlixelSprite> with getValues/setValues and index constants.
2. Register: Tween.registerAccessor(FlixelSprite.class, accessor).
3. Later: Tween.to(sprite, SpriteAccessor.XY, 0.5f).target(400f, 200f).start();
```

**FlixelGDX with a builder:**

```java
FlixelTween.tween(FlixelPropertyTween.class, FlixelPropertyTweenBuilder.class)
  .setDuration(0.5f)
  .setEase(FlixelEase::quadOut)
  .addGoal(player::getX, 400f, player::setX)
  .addGoal(player::getY, 200f, player::setY)
  .start();
```

No accessor class or index map: duration, ease, delays, and callbacks chain on the builder. The same `tween(Class, Class)` pattern works for every **registered** tween type (property, var, motion, color, paths, etc.).

#### Static factory methods on `FlixelTween`

These add a started tween to the **global** manager (same as `addTween` after `obtainTween`):

| API                                                                                                             | Purpose                                                                                                                                                                                                            |
| --------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `tween(object, settings)`                                                                                       | Dispatches by goals on `settings`: **property** goals → `FlixelPropertyTween`; **var** goals (`addGoal(String, float)`) → `FlixelVarTween` via `Flixel.reflect`. Do not mix both kinds on one `settings` instance. |
| `num(from, to, settings, callback)`                                                                             | Single scalar; callback receives the interpolated value each update.                                                                                                                                               |
| `angle(sprite, toAngle, settings)` / `angle(sprite, fromAngle, toAngle, settings)`                              | Rotate a `FlixelObject` in degrees; the two-argument overload tweens from the current angle.                                                                                                                       |
| `color(sprite, from, to, settings)`                                                                             | Tint using `FlixelColor` (`me.stringdotjar.flixelgdx.util`); optional completion `Runnable`. `colorRaw(...)` uses libGDX `Color`.                                                                                  |
| `shake(sprite, axes, intensity, settings)`                                                                      | Screen-style offset; overload with `ShakeUnit` and `fadeOut`.                                                                                                                                                      |
| `flicker(basic, settings)`                                                                                      | Visibility blink; overload with `period`, `ratio`, `endVisibility`, optional `Predicate`.                                                                                                                          |
| `linearMotion(target, fromX, fromY, toX, toY, durationOrSpeed, useDuration, settings)`                          | Straight-line move.                                                                                                                                                                                                |
| `circularMotion(target, centerX, centerY, radius, angleDeg, clockwise, durationOrSpeed, useDuration, settings)` | Orbit around a point.                                                                                                                                                                                              |
| `quadMotion(target, fromX, fromY, cx, cy, toX, toY, durationOrSpeed, useDuration, settings)`                    | Quadratic Bézier (one control point).                                                                                                                                                                              |
| `cubicMotion(target, p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y, durationOrSpeed, useDuration, settings)`           | Cubic Bézier (endpoints p0, p3; control points p1, p2).                                                                                                                                                            |
| `linearPath(target, durationOrSpeed, useDuration, settings, x0, y0, x1, y1, …)`                                 | Polyline path; varargs are vertex pairs (at least two points).                                                                                                                                                     |
| `quadPath(target, durationOrSpeed, useDuration, settings, x0, y0, …)`                                           | Chain of quad segments: **odd** vertex count, at least three points (`start, control, end, control, end, …`).                                                                                                      |

For complex easing, delays, or extra configuration, you can still use **`FlixelTween.tween(FlixelQuadMotion.class, FlixelQuadMotionBuilder.class)`** (and the other motion/path builder pairs) for a fluent chain.

#### Registry and built-in types

Tween classes are paired with builder classes in a **registry** on `FlixelTweenManager`. `FlixelTween.tween(tweenType, builderType)` loads the builder registered for `tweenType`; the builder class you pass must match (otherwise you get a clear error). After `Flixel.initialize()`, all built-in pairs below are already registered:

**Values and properties**

- `FlixelPropertyTween` ↔ `FlixelPropertyTweenBuilder`
- `FlixelVarTween` ↔ `FlixelVarTweenBuilder`
- `FlixelNumTween` ↔ `FlixelNumTweenBuilder`

**Sprite / object effects**

- `FlixelAngleTween` ↔ `FlixelAngleTweenBuilder`
- `FlixelColorTween` ↔ `FlixelColorTweenBuilder`
- `FlixelShakeTween` ↔ `FlixelShakeTweenBuilder`
- `FlixelFlickerTween` ↔ `FlixelFlickerTweenBuilder`

**Motion** (package `tween.type.motion`)

- `FlixelLinearMotion` ↔ `FlixelLinearMotionBuilder`
- `FlixelCircularMotion` ↔ `FlixelCircularMotionBuilder`
- `FlixelQuadMotion` ↔ `FlixelQuadMotionBuilder`
- `FlixelCubicMotion` ↔ `FlixelCubicMotionBuilder`
- `FlixelLinearPath` ↔ `FlixelLinearPathBuilder`
- `FlixelQuadPath` ↔ `FlixelQuadPathBuilder`

**Custom types** — register once (e.g. at startup), with a **pool factory** so empty pools can create new instances:

```java
FlixelTween.registerTweenType(
    MyTween.class,
    MyTweenBuilder.class,
    () -> new MyTween(null)); // match your tween's pooled empty constructor
```

> #### Why a registry? Is it necessary?
>
> Yes. Each registered tween type gets its own pool so `obtainTween` returns the correct concrete type for reuse. That keeps allocation low when many tweens start and finish every frame.

#### Builder API at a glance

- **Property** — `FlixelPropertyTweenBuilder`: `addGoal(getter, toValue, setter)`; fastest when you can close over methods.
- **Var** — `FlixelVarTweenBuilder`: `setObject`, `addGoal(fieldName, value)`; names go through `Flixel.reflect` (optional dotted paths from the root object).
- **Num** — `FlixelNumTweenBuilder`: `from` / `to`, `setCallback`.
- **Angle / color / shake / flicker** — matching `*Builder` classes; mirror the static `FlixelTween.angle` / `color` / `shake` / `flicker` options with full chaining for duration, ease, and callbacks.
- **Motion** — Static `linearMotion`, `circularMotion`, `quadMotion`, `cubicMotion` mirror the main builder geometry; builders add fluent chaining for the same types.
- **Paths** — Static `linearPath` / `quadPath` accept vertex coordinates as varargs after `durationOrSpeed`, `useDuration`, and `settings`; builders use `setTarget` + repeated `addPoint` instead.

#### Global manager shortcuts (`FlixelTween`)

Most games use the single global tween manager. These static methods forward to it so you rarely need `FlixelTween.getGlobalManager()` in gameplay code (use `getGlobalManager()` when you called `setManager(...)` on a builder for a custom manager):

| Method                                                                 | Purpose                                                                       |
| ---------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `FlixelTween.updateTweens(delta)`                                      | Advance all active tweens (what `FlixelGame` calls each frame).               |
| `FlixelTween.registerTweenType(tweenClass, builderClass, poolFactory)` | Register a custom tween type; returns the manager for chaining.               |
| `FlixelTween.cancelTweensOf(object, fieldPaths...)`                    | Cancel tweens targeting `object` (optional field/path filters, OR semantics). |
| `FlixelTween.completeTweensOf(object, fieldPaths...)`                  | Snap matching **non-looping** tweens to their end in one step.                |
| `FlixelTween.completeAllTweens()`                                      | Complete all non-looping tweens.                                              |
| `FlixelTween.completeTweensOfType(Class)`                              | Complete non-looping tweens of a given runtime class.                         |
| `FlixelTween.containsTweensOf(object, fieldPaths...)`                  | Whether any active tween matches.                                             |
| `FlixelTween.cancelActiveTweens()`                                     | Cancel every active tween.                                                    |
| `FlixelTween.clearTweenPools()`                                        | Clear all tween object pools (often paired with cancel on a full reset).      |

#### Using tweens inside a FlixelGDX game

In a typical FlixelGDX project, the global tween manager is automatically updated for you by the main `FlixelGame` loop. You can create tweens anywhere (usually in your `FlixelState#create()` or as a response to an input event):

```java
public class PlayState extends FlixelState {
  private FlixelSprite player;

  @Override
  public void create() {
    player = new FlixelSprite()
      .makeGraphic(16, 16, Color.WHITE);
    add(player);

    // Fade the player in over 1 second (alpha: 0 -> 1).
    FlixelTween.tween(FlixelPropertyTween.class, FlixelPropertyTweenBuilder.class)
      .setDuration(1.0f)
      .addGoal(() -> player.getColor().a, 1.0f, player::setAlpha)
      .start();
  }
}
```

Here `addGoal` uses the `FlixelPropertyTween` path under the hood, so your sprite's setters run on every step, which keeps collision bounds, listeners, or other side effects in sync with the animation.

#### Using FlixelGDX tweens in a regular libGDX project

If you already have an existing libGDX game that does not use `FlixelGame` or `FlixelState`, you can still use the tweening system by manually updating the global tween manager:

```java
public class MyLibGdxScreen implements Screen {
  private final FlixelSprite player = new FlixelSprite();

  @Override
  public void show() {
    player.makeGraphic(16, 16, Color.WHITE);

    FlixelTween.tween(FlixelPropertyTween.class, FlixelPropertyTweenBuilder.class)
      .setDuration(0.75f)
      .setEase(FlixelEase::sineInOut)
      .addGoal(player::getScaleX, 2.0f, value -> player.setScale(value, value))
      .start();
  }

  @Override
  public void render(float delta) {
    FlixelTween.updateTweens(delta); // advance all Flixel tweens

    // ...your normal libGDX update & render code...
  }
}
```

This lets you adopt FlixelGDX's tweening in small pieces inside an existing libGDX codebase, while still giving HaxeFlixel-style ergonomics (tweens that understand your properties) and powerful configuration for experienced developers.

### Assets

After `Flixel.initialize()`, **`Flixel.assets`** is the active `FlixelAssetManager` (default: `FlixelDefaultAssetManager`). It sits on top of libGDX’s `AssetManager` and encourages **typed** loading:

- **`load(FlixelSource)`** — Preferred: the source describes both path and asset type (texture, atlas, sound, etc.).
- **`load(String path)`** — Resolves a `FlixelSource` from the file extension via an **extension registry** on the manager. Convenient for quick tests; register custom mappings with `registerExtension` if extensions are ambiguous.
- **`finishLoading()`** / progress APIs — Same idea as libGDX: queue loads, then block or poll until ready.
- **`get(...)`** — Retrieve loaded assets by key/type.

You can supply your own manager before initialization (see `Flixel` asset factory/setter APIs in the Javadoc). On **`Flixel.switchState`**, non-persistent assets are cleared (`clearNonPersist`) so each state can own its load set unless you mark content persistent.

### Audio

**`Flixel.sound`** is a **`FlixelAudioManager`** built on **MiniAudio**: separate groups for **SFX** and **music**, plus a **master volume** (clamped 0–1).

- **`play(path)`** / overloads — One-shot or looping sound effects on the SFX group; paths are resolved for internal files unless you use the “external” overloads.
- **`playMusic(path)`** / overloads — Stops previous music, plays on the music group (typical looping background track).
- **`pause()` / `resume()`** — Used when the window loses/regains focus so audio does not run in the background unintentionally.
- **`getEngine()`** — Access to MiniAudio for advanced loading or custom sounds.

Sounds are represented as **`FlixelSound`** instances (volume, loop, play/stop/dispose).

### Reflection

**`Flixel.reflect`** implements **`FlixelReflection`**: read/write **fields**, **JavaBean-style properties** (`property` / `setProperty`), **method calls**, **dotted path resolution** (`resolvePropertyPath`), and helpers used by **var tweens** and other runtime features.

Until you call **`Flixel.setReflection(...)`** with a real implementation (e.g. **`FlixelDefaultReflectionHandler`** on desktop), the default **unsupported** stub throws: configure reflection during startup if you use **`FlixelVarTween`** or anything else that depends on it.

### Text

**`FlixelText`** extends **`FlixelSprite`**, so you add labels to states and groups like any other sprite (tint, scale, rotation, alpha). It renders with libGDX **`BitmapFont`**, with optional **FreeType** generation from `.ttf` / `.otf` files via **`setFont(FileHandle)`** for crisp sizes. You get **auto-sized** bounds by default, optional **field width/height**, **alignment**, and **border** styles (shadow, outline). Graphic and atlas-loading APIs inherited from `FlixelSprite` are not valid on text and throw if used.

### Signals, cameras & debugging

- **`Flixel.Signals`** — Global **`FlixelSignal`** hooks: **`preUpdate` / `postUpdate`**, **`preDraw` / `postDraw`**, **`preStateSwitch` / `postStateSwitch`**, window focus/minimize, and game close. **`pre*`** runs before framework work; **`post*`** runs after.
- **Cameras** — **`FlixelCamera`** (used by **`FlixelGame`**) wraps a libGDX camera and viewport; games can use multiple cameras and scroll modes. See the `FlixelCamera` Javadoc for viewport access when you need raw libGDX types.
- **Debug** — **`Flixel.watch`** (**`FlixelDebugWatchManager`**) tracks values on the debug overlay. **`Flixel.setDebugOverlay(Supplier)`** supplies a custom **`FlixelDebugOverlay`**; debug drawing runs when **`Flixel.isDebugMode()`** is true under **`FlixelGame`**.

### Input Handling

FlixelGDX wraps libGDX input behind a small set of helpers so you can check “was this key just pressed?”, “is it held?”, or "is it touching the screen?" without touching `Gdx.input` directly. That keeps gameplay code focused on intent (e.g. “jump when space is pressed”) and works the same on desktop, Android, and web.

**How it works**

- **`Flixel.keys.pressed(int key)`** — Returns whether the key is currently held (same as `Gdx.input.isKeyPressed(key)`). Use for movement or continuous actions.
- **`Flixel.keys.justPressed(int key)`** — Returns whether the key was pressed this frame (one-shot). Use for jump, shoot, menu confirm, etc.
- **`Flixel.keys.justReleased(int key)`** — Returns whether the key was released this frame (one-shot). Use for menu cancel, etc.
- **`FlixelKey`** — Provides key constants such as `FlixelKey.SPACE`, `FlixelKey.LEFT`, `FlixelKey.A`, etc. Pass these as the `key` argument so your code stays readable. `Input.Keys` will also suffice if you prefer to use pure libGDX input constants instead.

```java
@Override
public void update(float elapsed) {
  super.update(elapsed);

  // One-shot: jump only when space is first pressed
  if (Flixel.keys.justPressed(FlixelKey.SPACE)) {
    player.jump();
  }

  // Held: move while arrow keys are down
  if (Flixel.keys.pressed(FlixelKey.LEFT))  player.velocityX = -100;
  if (Flixel.keys.pressed(FlixelKey.RIGHT)) player.velocityX =  100;
}
```

Touch and mouse input still go through libGDX (`Gdx.input`); FlixelGDX’s helpers are for keyboard so you don’t have to remember the exact `Gdx.input` calls.

#### Using input helpers in a regular libGDX project

`Flixel.keys.pressed(int key)`, `Flixel.keys.justPressed(int key)`, and `Flixel.keys.justReleased(int key)` delegate to `Gdx.input`, so they work in any libGDX app as long as `Gdx.input` is available (which it is once the application is running). You don’t need `FlixelGame` for these; just call them from your update loop. If you haven’t initialized Flixel at all, the static methods still work because they only use `Gdx.input`.

### Logging & Debugging

FlixelGDX provides a small logging API so you can tag messages, control how much detail is shown, and optionally attach stack traces without scattering `System.out.println` or wiring a full logging framework up front.

**How it works**

- **`Flixel.info(message)`** / **`Flixel.warn(message)`** / **`Flixel.error(message)`** — Log with the default tag. Use for general state, warnings, and errors.
- **`Flixel.info(tag, message)`** / **`Flixel.warn(tag, message)`** / **`Flixel.error(tag, message)`** — Log with a custom tag (e.g. `"Player"`, `"Save"`) so you can filter or grep logs more easily.
- **`Flixel.error(tag, message, throwable)`** — Log an error with an exception; the logger can include the stack trace depending on mode.
- **Log mode.** The default logger uses `FlixelLogMode.SIMPLE` (compact, HaxeFlixel-style) or `FlixelLogMode.DETAILED` (timestamp, class, line, method). You can replace the global logger with `Flixel.setLogger(yourLogger)` or set a default tag with `Flixel.setDefaultLogTag("MyGame")`.

```java
Flixel.info("Player entered level 2");
Flixel.warn("Save", "Quota almost full");
Flixel.error("Network", "Connection failed", exception);
```

This keeps logging consistent and readable. You can plug in your own `FlixelLogger` implementation (e.g. to your existing logging library) via `Flixel.setLogger()`.

#### Using FlixelGDX logging in a regular libGDX project

The static methods `Flixel.info`, `Flixel.warn`, and `Flixel.error` use a default logger that is created when Flixel is initialized (e.g. by `FlixelGame`). If you use FlixelGDX only as a library and never call `Flixel.initialize()`, you can still call these methods after setting your own logger with `Flixel.setLogger(logger)` so that all Flixel log calls go through your pipeline. Alternatively, use your existing logging in parallel; Flixel’s API is just a convenience and doesn’t replace libGDX or other logging.
