# FlixelGDX

FlixelGDX is a high-level 2D game framework built on top of [libGDX](https://libgdx.com/). It aims to replicate the powerful and developer-friendly API of [HaxeFlixel](https://haxeflixel.com/) (based on the original ActionScript [Flixel](http://www.flixel.org/)) while leveraging the robust cross-platform capabilities and performance of the libGDX ecosystem.

The goal of FlixelGDX is to provide the familiar Flixel-like structure that developers love, while remaining open for experienced libGDX developers to integrate their own lower-level functionality.

> [!NOTE]
> FlixelGDX is an independent project and is not officially affiliated with HaxeFlixel or libGDX.

# Project Navigation

- [**Contributing Guide**](CONTRIBUTING.md): Learn how to contribute to the project, coding standards, and PR requirements.
- [**Project Structure**](PROJECT.md): Understand the multi-module layout and how Gradle is used.
- [**Compiling & Testing**](TESTING.md): How to build the framework and test it as a dependency in your own projects.

# Supported Platforms

FlixelGDX supports the following platforms through its modular backend system:

- **Desktop**: Windows, macOS, and Linux via LWJGL3.
- **Android**: Full support for Android mobile devices.
- **iOS**: Support via RoboVM.
- **Web**: Support via TeaVM.

> [!WARNING]
> FlixelGDX is a _modern_ framework, which means it requires _modern_ Java. You must use Java 17 or higher to be able
> to use FlixelGDX. Because of this, web games don't (and can't) support GWT; it is done through a modern
> library called TeaVM, which transpiles Java code to JavaScript to be run in a browser.

# Goals

## Replicate Flixel API

FlixelGDX attempts to bring the ease of use and rapid prototyping capabilities of HaxeFlixel to Java, while also
using the tools of libGDX to allow it to be seamlessly integrated into new and existing projects alike.

## LibGDX Integration

We use libGDX's standard lifecycle methods (`update()`, `draw()`, `dispose()`) and performance-oriented tools (like `Poolable` interfaces)
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

    MyGame game = new MyGame(
      "My Game",
      800,
      600,
      new InitialState() // The initial state the game enters when it starts!
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

- **Entity Component-ish System**: Clean separation of logic and rendering via `FlixelBasic`, `FlixelObject`, and `FlixelSprite`.
- **State Management**: Simple state switching and substates for menus, pauses, and transitions.
- **Group System**: Powerful grouping for batch updates, collisions, and nested transformations.
- **Tweening**: Built-in tweening system for smooth animations and transitions, with property-based tweens that work naturally with getters/setters.
- **Input Handling**: Simplified keyboard and touch/mouse input.
- **Logging & Debugging**: Integrated logging system with stack trace support.

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

**How it Works** 

The entity hierarchy mirrors HaxeFlixel:

- **`FlixelBasic`** - The base for everything. It has no position or graphics, only lifecycle flags: `active` (whether `update()` runs), `exists` (whether it is updated and drawn), `alive` (for game logic), and `visible` (whether it is drawn). Override `update(float)` and `draw(Batch)` for custom behavior; use `kill()` / `revive()` to temporarily disable or bring back objects.
- **`FlixelObject`** - Adds position (`x`, `y`), size (`width`, `height`), rotation (`angle`), and physics-style motion: velocity, acceleration, drag, and max velocity (X/Y and angular). Set `moves = true` (default) so `update()` calls `updateMotion()`, which applies velocity and acceleration each frame.
- **`FlixelSprite`** - Adds rendering: textures, spritesheets, animations, scale, origin, offset, color tint, and flip. Use `loadGraphic()` for images, `makeGraphic()` for solid rectangles, or `loadSparrowFrames()` for XML-based atlases; then `addAnimation()` / `addAnimationByPrefix()` and `playAnimation()` for frame-based animation.

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

**How it works.**

- **`FlixelState`** extends `FlixelGroup<FlixelBasic>` and implements libGDX’s `Screen`. When the game switches to a state, `create()` is called once; then each frame the state’s `update(delta)` and `draw(batch)` run, which in turn update and draw all members you added. Use `Flixel.switchState(newState)` to transition; the old state is disposed automatically.
- **`FlixelSubState`** is a state that is opened *on top of* another state. By default the parent state stops updating (`persistentUpdate = false`) but keeps drawing (`persistentDraw = true`), so you get a classic “pause overlay” or modal dialog. The substate has `openCallback` and `closeCallback`; call `close()` from inside the substate to remove it and resume the parent.
- **Outros.** Override `startOutro(onOutroComplete)` on a state to run an exit animation or transition before the switch; when done, call the callback so the framework proceeds with the state change.

```java
public class MenuState extends FlixelState {

  @Override
  public void create() {
    // Set your background color and add sprites/buttons here.
  }

  @Override
  public void update(float delta) {
    super.update(delta);

    if (Flixel.keyJustPressed(FlixelKey.ENTER)) {
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
  public void update(float delta) {
    super.update(delta);
    if (Flixel.keyJustPressed(FlixelKey.ESCAPE)) {
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

**How it Works**

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

FlixelGDX includes a built-in tweening system inspired by Flixel/HaxeFlixel that aims to solve two common problems:

- **Reducing boilerplate compared to Universal Tween Engine (UTE)**, which typically requires writing a `TweenAccessor` for every type you want to animate.
- **Providing both beginner-friendly defaults and power features** that experienced users expect (easing, delays, callbacks, looping), without forcing you into reflection-heavy or brittle APIs.

At the core is `FlixelTween` plus a settings object `FlixelTweenSettings`. For property-based tweens, `FlixelPropertyTween` lets you supply getters and setters directly, so you can tween real Java properties with side effects instead of raw fields.

```java
FlixelSprite player = new FlixelSprite()
  .makeGraphic(16, 16, Color.WHITE);

// Tween the player's X and Y using getters/setters.
FlixelTween.tween(
  new FlixelTweenSettings()
    .setDuration(0.5f)
    .setEase(FlixelEase::quadOut)
    .addGoal(player::getX, 400f, player::setX)
    .addGoal(player::getY, 200f, player::setY)
);
```

This pattern is familiar to HaxeFlixel users (property-style tweens) while being very approachable for beginners:

- **Beginners** just pass method references (`player::getX`, `player::setX`) and a target value.
- **Experienced users** can configure easing, durations, delays, callbacks, and loop behavior through `FlixelTweenSettings`, or drop down to lower-level types like `FlixelVarTween` if they want reflection-based tweens.

Compared to Universal Tween Engine's `TweenAccessor` pattern, you do not need to:

- Implement a custom accessor interface per type.
- Register accessors in a global registry.
- Manually map indices to fields.

Instead, you declare exactly what you want to tween with strongly typed getter/setter pairs. This keeps the API compact while still preserving all the niceties of having tweens as first-class objects.

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
    FlixelTween.tween(
      new FlixelTweenSettings()
        .setDuration(1.0f)
        .addGoal(() -> player.getColor().a, 1.0f, player::setAlpha)
    );
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

    FlixelTween.tween(
      new FlixelTweenSettings()
        .setDuration(0.75f)
        .setEase(FlixelEase::sineInOut)
        .addGoal(player::getScaleX, 2.0f, value -> player.setScale(value, value))
    );
  }

  @Override
  public void render(float delta) {
    FlixelTween.getGlobalManager().update(delta); // advance all Flixel tweens

    // ...your normal libGDX update & render code...
  }
}
```

This lets you adopt FlixelGDX's tweening in small pieces inside an existing libGDX codebase, while still giving HaxeFlixel-style ergonomics (tweens that understand your properties) and powerful configuration for experienced developers.

### Input Handling

FlixelGDX wraps libGDX input behind a small set of helpers so you can check “was this key just pressed?” or “is it held?” without touching `Gdx.input` directly. That keeps gameplay code focused on intent (e.g. “jump when space is pressed”) and works the same on desktop, Android, and web.

**How it Works**

- **`Flixel.keyPressed(int key)`** - Returns whether the key is currently held (same as `Gdx.input.isKeyPressed(key)`). Use for movement or continuous actions.
- **`Flixel.keyJustPressed(int key)`** - Returns whether the key was pressed this frame (one-shot). Use for jump, shoot, menu confirm, etc., so the action doesn’t repeat every frame.
- **`FlixelKey`** - Extends `Input.Keys` and provides the same constants (e.g. `FlixelKey.SPACE`, `FlixelKey.LEFT`, `FlixelKey.A`). Pass these as the `key` argument so your code stays readable.

```java
@Override
public void update(float delta) {
  super.update(delta);

  // One-shot: jump only when space is first pressed
  if (Flixel.keyJustPressed(FlixelKey.SPACE)) {
    player.jump();
  }

  // Held: move while arrow keys are down
  if (Flixel.keyPressed(FlixelKey.LEFT))  player.velocityX = -100;
  if (Flixel.keyPressed(FlixelKey.RIGHT)) player.velocityX =  100;
}
```

Touch and mouse input still go through libGDX (`Gdx.input`); FlixelGDX’s helpers are for keyboard so you don’t have to remember the exact `Gdx.input` calls.

#### Using input helpers in a regular libGDX project

`Flixel.keyPressed()` and `Flixel.keyJustPressed()` delegate to `Gdx.input`, so they work in any libGDX app as long as `Gdx.input` is available (which it is once the application is running). You don’t need `FlixelGame` for these; just call them from your update loop. If you haven’t initialized Flixel at all, the static methods still work because they only use `Gdx.input`.

### Logging & Debugging

FlixelGDX provides a small logging API so you can tag messages, control how much detail is shown, and optionally attach stack traces - without scattering `System.out.println` or wiring a full logging framework up front.

**How it Works**

- **`Flixel.info(message)`** / **`Flixel.warn(message)`** / **`Flixel.error(message)`** - Log with the default tag. Use for general state, warnings, and errors.
- **`Flixel.info(tag, message)`** / **`Flixel.warn(tag, message)`** / **`Flixel.error(tag, message)`** - Log with a custom tag (e.g. `"Player"`, `"Save"`) so you can filter or grep logs more easily.
- **`Flixel.error(tag, message, throwable)`** - Log an error with an exception; the logger can include the stack trace depending on mode.
- **Log mode.** The default logger uses `FlixelLogMode.SIMPLE` (compact, HaxeFlixel-style) or `FlixelLogMode.DETAILED` (timestamp, class, line, method). You can replace the global logger with `Flixel.setLogger(yourLogger)` or set a default tag with `Flixel.setDefaultLogTag("MyGame")`.

```java
Flixel.info("Player entered level 2");
Flixel.warn("Save", "Quota almost full");
Flixel.error("Network", "Connection failed", exception);
```

This keeps logging consistent and readable. You can plug in your own `FlixelLogger` implementation (e.g. to your existing logging library) via `Flixel.setLogger()`.

#### Using FlixelGDX logging in a regular libGDX project

The static methods `Flixel.info`, `Flixel.warn`, and `Flixel.error` use a default logger that is created when Flixel is initialized (e.g. by `FlixelGame`). If you use FlixelGDX only as a library and never call `Flixel.initialize()`, you can still call these methods after setting your own logger with `Flixel.setLogger(logger)` so that all Flixel log calls go through your pipeline. Alternatively, use your existing logging in parallel; Flixel’s API is just a convenience and doesn’t replace libGDX or other logging.
