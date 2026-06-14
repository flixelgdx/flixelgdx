# FlixelGDX

<img src="docs/readme/flxgdx.png" width="500" alt="FlixelGDX logo">

### Logo Artist: [LeoThM](https://www.instagram.com/leoxthm_/)

[![CI](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml/badge.svg)](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.flixelgdx/flixelgdx-core)](https://central.sonatype.com/artifact/org.flixelgdx/flixelgdx-core)
[![JitPack](https://jitpack.io/v/flixelgdx/flixelgdx.svg)](https://jitpack.io/#flixelgdx/flixelgdx)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Website](https://img.shields.io/badge/website-flixelgdx.org-blue)](https://flixelgdx.org)
[![Stars](https://img.shields.io/github/stars/flixelgdx/flixelgdx)](https://github.com/flixelgdx/flixelgdx/stargazers)
[![Forks](https://img.shields.io/github/forks/flixelgdx/flixelgdx)](https://github.com/flixelgdx/flixelgdx/forks)
[![Issues](https://img.shields.io/github/issues/flixelgdx/flixelgdx)](https://github.com/flixelgdx/flixelgdx/issues)
[![Pull Requests](https://img.shields.io/github/issues-pr/flixelgdx/flixelgdx)](https://github.com/flixelgdx/flixelgdx/pulls)
[![Last Commit](https://img.shields.io/github/last-commit/flixelgdx/flixelgdx/develop)](https://github.com/flixelgdx/flixelgdx/commits/develop)
[![Contributors](https://img.shields.io/github/contributors/flixelgdx/flixelgdx)](https://github.com/flixelgdx/flixelgdx/graphs/contributors)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net/temurin/releases?version=17&os=any&arch=any)
[![libGDX 1.14.0](https://img.shields.io/badge/libGDX-1.14.0-red)](https://libgdx.com/)
[![Platforms](https://img.shields.io/badge/platforms-Desktop%20%7C%20Web-brightgreen)](https://flixelgdx.org)

FlixelGDX is a featherweight powerhouse of a Java game framework built on top of [libGDX](https://libgdx.com/). It brings a clean, state-based game architecture inspired by [HaxeFlixel](https://haxeflixel.com/) and the original ActionScript [Flixel](http://www.flixel.org/) into the Java ecosystem, while keeping the full platform reach and rendering power of libGDX underneath.

> [!NOTE]
> FlixelGDX is an independent project and is not officially affiliated with HaxeFlixel or libGDX.

> [!IMPORTANT]
> FlixelGDX is still relatively new and currently supports desktop and web. Mobile support is coming soon!

---

## Why FlixelGDX?

Raw libGDX is powerful but low-level. You manage `SpriteBatch`, call `begin()` and `end()` every frame, clear the screen manually, wire up your own delta-time calculations, and pull in separate libraries for tweens, timers, and animations. FlixelGDX builds all of that infrastructure into one cohesive framework so your code stays focused on game logic rather than plumbing.

The result is dramatically less boilerplate and a developer experience that closely mirrors HaxeFlixel (especially when paired with Kotlin) while remaining fully idiomatic.

---

## Our Mission ❤️

FlixelGDX is built specifically for people who want to have the features of a modern game engine but are stuck with low-end hardware.
We believe that **everyone** – regardless of hardware or financial limitations – should be able to have access to powerful tools.
Nobody's creativity (and access to their favorite games) should be limited and diminished just because their computer can't handle it.

This framework is designed to be powerful yet approachable for everyone – all without having to pay a single penny. Developers should be able to
make beautiful and amazing creations and shouldn't be stopped or discouraged by a greedy fee. Games are an art form that should be accessible to everyone, not just
people with a lot of money and an expensive gaming rig.

---

## Features

- **State architecture** - `FlixelGame` + `FlixelState` + `FlixelSubState` replace the raw `ApplicationListener`/`Screen` pattern with a lifecycle-managed scene stack. States handle their own setup, update, and teardown cleanly.
- **Sprites and groups** - `FlixelSprite`, `FlixelGroup`, and `FlixelSpriteGroup` with built-in pooling support. The framework reuses objects rather than allocating on every frame.
- **Built-in tween engine** - `FlixelTween` with 30+ easing functions, motion paths (linear, quad, cubic, circular), color tweens, flicker effects, camera shake, and angle tweens. No third-party library needed.
- **Timers** - `FlixelTimer` for delayed callbacks and repeating events with zero allocation per fire.
- **Input** - unified `Flixel.keys`, `Flixel.mouse`, and `Flixel.gamepad`, plus a Steam-compatible action-set system (`FlixelActionSet`) for more complex input binding.
- **Audio** - `FlixelSound` and `FlixelAudioManager` wrapping libGDX audio with Flixel-style helpers.
- **Text and fonts** - `FlixelText` with `FlixelFontRegistry` for bitmap fonts and runtime font loading.
- **Camera** - `FlixelCamera` with built-in shake, flash, fade, and follow modes.
- **Collision** - `Flixel.overlap(a, b)` and `Flixel.collide(a, b)` work on single objects or entire groups, with optional per-pair callbacks.
- **Animation** - Full animation support for Sparrow spritesheets and spritemap atlases via `FlixelAnimateSprite` for Adobe Animate-exported rigs (including out-of-the-box support for the [Better Texture Atlas](https://github.com/Dot-Stuff/BetterTextureAtlas) extension).
- **Assets** - `FlixelAssetManager` and `FlixelAssetPaths` for path-safe, typed asset loading across all platforms.
- **Saves** - `FlixelSave` for cross-platform persistent game data.
- **Debug overlay** - `FlixelImGuiDebugOverlay` (desktop) provides a live, in-game debug panel powered by Dear ImGui.
- **Logging** - `FlixelLogger` with accurate stack traces via a compile-time bytecode plugin, multiple log levels, console and file sinks.
- **Multi-platform** - desktop via LWJGL3 and browser via TeaVM. (Android and iOS support is in progress!)

---

## Code comparisons

### Rendering a sprite and handling input

**With raw libGDX:**

```java
public class MyGame extends ApplicationAdapter {

  private SpriteBatch batch;
  private Texture texture;
  private float x, y;

  @Override
  public void create() {
    // Here, we have to manually manage a batching object, a texture, and its coordinates.
    // If we forget to dispose of any resources, we can leak memory and cause performance issues.
    batch = new SpriteBatch();
    texture = new Texture("player.png");
    x = 100;
    y = 100;
  }

  @Override
  public void render() {
    float dt = Gdx.graphics.getDeltaTime();
    if (Gdx.input.isKeyPressed(Input.Keys.W)) y += 200f * dt;
    if (Gdx.input.isKeyPressed(Input.Keys.A)) x -= 200f * dt;
    if (Gdx.input.isKeyPressed(Input.Keys.S)) y -= 200f * dt;
    if (Gdx.input.isKeyPressed(Input.Keys.D)) x += 200f * dt;

    // Manually handle rendering and sprite batching.
    ScreenUtils.clear(0, 0, 0, 1);
    batch.begin();
    batch.draw(texture, x, y);
    batch.end();
  }

  @Override
  public void dispose() {
    batch.dispose();
    texture.dispose();
  }
}
```

**With FlixelGDX:**

```java
public class PlayState extends FlixelState {

  private FlixelSprite player;

  @Override
  public void create() {
    super.create();
    // Create a simple sprite at (100, 100), load an image, and add it to the state.
    // That's it, everything else is handled and tracked by the framework!
    player = new FlixelSprite(100, 100);
    player.loadGraphic("player.png");
    add(player);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (Flixel.keys.pressed(FlixelKey.W)) player.changeY(200f * elapsed);
    if (Flixel.keys.pressed(FlixelKey.A)) player.changeX(-200f * elapsed);
    if (Flixel.keys.pressed(FlixelKey.S)) player.changeY(-200f * elapsed);
    if (Flixel.keys.pressed(FlixelKey.D)) player.changeX(200f * elapsed);
  }
}
```

The `SpriteBatch`, screen clearing, manual disposal, and delta-time calculation are handled by the framework. Your state only describes what the game does.

### Tweening

FlixelGDX provides a very flexible and simple yet robust tweening engine:

```java
// Slide a sprite to x=500, y=300 over 1.5 seconds with a bounce-out ease.
FlixelTween.tween(player, new FlixelTweenSettings()
  .addGoal(player::getX, 500f, player::setX)
  .addGoal(player::getY, 300f, player::setY)
  .setDuration(1.5f)
  .setEase(FlixelEase::bounceOut));

// Shake a sprite for 0.4 seconds on a hit.
FlixelTween.shake(enemy, FlixelAxes.XY, 0.008f, new FlixelTweenSettings().setDuration(0.4f));

// Flicker a sprite for 1.2 seconds after taking damage (default period and ratio).
FlixelTween.flicker(player, new FlixelTweenSettings().setDuration(1.2f));
```

### Groups and collision

**`FlixelGroup<T>`** is a framework-agnostic container with no automatic lifecycle. Use it when your members are not `IFlixelBasic` / `FlixelBasic` objects, or when you need a type outside the FlixelGDX framework:

```java
// FlixelGroup<T> requires an array factory and manages no lifecycle itself.
FlixelGroup<MyActor> actors = new FlixelGroup<>(MyActor[]::new);
for (int i = 0; i < 10; i++) {
  actors.add(new MyActor(i * 60f, 100f));
}

// update() and draw() are your responsibility.
actors.forEachMember(e -> e.act(elapsed));
actors.forEachMember(e -> e.draw(batch));
```

**`FlixelBasicGroup<T>`** is abstract and gives you a group with full `FlixelBasic` lifecycle (automatic `update`, `draw`, and `destroy` for all members). Subclass it and supply the array factory:

```java
// Subclass FlixelBasicGroup to add group-level logic (wave spawning, shared state, etc.).
public class EnemyGroup extends FlixelBasicGroup<FlixelSprite> {

  public EnemyGroup() {
    super(FlixelSprite[]::new);
  }
}

// Then use it in a state like any other FlixelBasic.
EnemyGroup enemies = new EnemyGroup();
for (int i = 0; i < 10; i++) {
  FlixelSprite enemy = new FlixelSprite(i * 60f, 200f);
  enemy.makeGraphic(32, 32, FlixelColor.RED);
  enemies.add(enemy);
}
add(enemies); // update() and draw() propagate to all members automatically.
```

**`FlixelSpriteGroup`** gives members the full FlixelGDX lifecycle automatically and works with `Flixel.overlap` and `Flixel.collide`:

```java
// FlixelSpriteGroup handles update, draw, and destroy for every member.
FlixelSpriteGroup enemies = new FlixelSpriteGroup();
for (int i = 0; i < 10; i++) {
  FlixelSprite e = new FlixelSprite(i * 60f, 100f);
  e.makeGraphic(32, 32, FlixelColor.RED);
  enemies.add(e);
}
add(enemies);

// Check one bullet against every enemy. Callback fires for each overlapping pair.
Flixel.overlap(bullet, enemies, (b, e) -> {
  b.kill();
  e.kill();
});
```

---

## Memory efficiency

FlixelGDX is built for low memory usage:

- Objects are pooled and reused rather than allocated each frame.
- Indexed `for` loops are used throughout the framework to avoid iterator allocation.
- Internal class field layouts follow strict padding rules to minimize struct size.

The JVM heap for a typical or even complex FlixelGDX game sits at roughly **4-8MB on average**.

It gets even better. When compiled to a native binary with GraalVM Native Image, the total system RAM (including the embedded garbage collector Graal provides) stays around **40-50MB of system RAM**.

For context, a similarly sized game built on raw libGDX typically uses 30-80MB of managed heap before significant game logic is added. A similarly sized HaxeFlixel game commonly reaches several hundred megabytes of system RAM, because HaxeFlixel tends to keep assets (textures, audio) resident in managed memory rather than properly handing them off to native buffers.

> [!NOTE]
> The 4-8MB figure covers managed heap only. Native memory - which includes GPU textures, audio tracks, and other OS-managed resources - is outside the JVM heap and outside what the framework can control. How efficiently your game uses native memory depends entirely on how big your assets are and managed.

---

## Adding FlixelGDX to your project

FlixelGDX is published to **Maven Central** under the group `org.flixelgdx`. Stable releases are available there with no extra repository configuration beyond `mavenCentral()`.

**1. Depend on core** from your libGDX `core` module's `build.gradle`:

```gradle
dependencies {
  implementation 'org.flixelgdx:flixelgdx-core:<version>'
}
```

**2. Add the backend module on each launcher.** Example for desktop (LWJGL3):

```gradle
dependencies {
  implementation 'org.flixelgdx:flixelgdx-lwjgl3:<version>'
}
```

Other modules include `flixelgdx-teavm` and `flixelgdx-teavm-plugin` for browser targets. Match versions across all modules.

### Using JitPack for snapshots

If you need the latest unreleased code from `develop` or a specific commit, [JitPack](https://jitpack.io/#flixelgdx/flixelgdx) can build any branch or commit on demand. Add the JitPack repository and use the `com.github.flixelgdx.flixelgdx` group:

```gradle
repositories {
  mavenCentral()
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.github.flixelgdx.flixelgdx:flixelgdx-core:develop-SNAPSHOT'
}
```

For full wiring (launchers, composite builds, TeaVM setup), see **[COMPILING.md](COMPILING.md)**.

---

## Project navigation

- **[Contributing Guide](CONTRIBUTING.md)**: Coding standards, PR requirements, and how to contribute.
- **[Project Structure](PROJECT.md)**: The multi-module layout and how Gradle is used.
- **[Compiling & Testing](COMPILING.md)**: How to build the framework and test it as a dependency in your own projects.
- **[Code of Conduct](CODE_OF_CONDUCT.md)**: Rules set in place for a stable open source community.
- **[Project Roles](GOVERNANCE.md)**: How each role for the project operates, including project leaders and maintainers.
