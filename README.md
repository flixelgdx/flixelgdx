# FlixelGDX

<img src="docs/readme/flxgdx ii.png" width="400" alt="FlixelGDX logo">

[![CI](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml/badge.svg)](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml)
[![JitPack](https://jitpack.io/v/flixelgdx/flixelgdx.svg)](https://jitpack.io/#flixelgdx/flixelgdx)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

FlixelGDX is a Java game framework built on top of [libGDX](https://libgdx.com/). It brings a clean, state-based game architecture inspired by [HaxeFlixel](https://haxeflixel.com/) and the original ActionScript [Flixel](http://www.flixel.org/) into the Java ecosystem, while keeping the full platform reach and rendering power of libGDX underneath.

> [!NOTE]
> FlixelGDX is an independent project and is not officially affiliated with HaxeFlixel or libGDX.

> [!IMPORTANT]
> FlixelGDX is still relatively new and currently supports desktop and web. Mobile support is coming soon.

---

## Why FlixelGDX?

Raw libGDX is powerful but low-level. You manage `SpriteBatch`, call `begin()` and `end()` every frame, clear the screen manually, wire up your own delta-time calculations, and pull in separate libraries for tweens, timers, and animations. FlixelGDX builds all of that infrastructure into one cohesive framework so your code stays focused on game logic rather than plumbing.

The result is dramatically less boilerplate – especially for small-to-medium games – and a developer experience that closely mirrors HaxeFlixel while remaining fully idiomatic Java or Kotlin.

---

## Our Mission ❤️

FlixelGDX is built specifically for people who want to have the features of a modern game engine but are stuck with low-end hardware.
We believe that **everyone** – regardless of hardware or financial limitations – should be able to have access to powerful tools.
Nobody's creativity (and access to their favorite games) should be limited and diminished just because their computer can't handle it.

This framework is designed to be a featherweight powerhouse for everyone without having to pay a single penny. Developers should be able to
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
- **Animation** - `FlixelAnimationController` for spritemaps and `FlixelAnimateSprite` for Adobe Animate-exported rigs.
- **Assets** - `FlixelAssetManager` and `FlixelAssetPaths` for path-safe, typed asset loading across all platforms.
- **Saves** - `FlixelSave` for cross-platform persistent game data.
- **Debug overlay** - `FlixelImGuiDebugOverlay` (desktop) provides a live, in-game debug panel powered by Dear ImGui.
- **Logging** - `FlixelLogger` with accurate stack traces via a compile-time bytecode plugin, multiple log levels, console and file sinks.
- **Multi-platform** - desktop via LWJGL3 and browser via TeaVM. Android and iOS support is in progress.

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

libGDX has no built-in tween system. You must implement animation manually or pull in a third-party library. FlixelGDX ships a full tween engine:

```java
// Slide a sprite to x=500, y=300 over 1.5 seconds with a bounce-out ease.
FlixelTween.tween(player, new FlixelTweenSettings()
  .addGoal(player::getX, 500f, player::setX)
  .addGoal(player::getY, 300f, player::setY)
  .setDuration(1.5f)
  .setEase(FlixelEase::bounceOut));

// Shake the camera for 0.4 seconds on a hit.
FlixelTween.shake(Flixel.camera, 0.4f, new FlixelTweenSettings());

// Flicker a sprite for 1.2 seconds after taking damage.
FlixelTween.flicker(player, 1.2f, 0.04f, new FlixelTweenSettings());
```

### Groups and collision

```java
// Create a group of enemies.
FlixelGroup enemies = new FlixelGroup();
for (int i = 0; i < 10; i++) {
  FlixelSprite e = new FlixelSprite(i * 60f, 100f);
  e.makeGraphic(32, 32, Color.RED);
  enemies.add(e);
}
add(enemies);

// Check one bullet against every enemy in a single call.
if (Flixel.overlap(bullet, enemies)) {
  bullet.kill();
}
```

---

## Memory efficiency

FlixelGDX is built for low memory usage:

- Objects are pooled and reused rather than allocated each frame.
- Indexed `for` loops are used throughout the framework to avoid iterator allocation.
- Internal class field layouts follow strict padding rules to minimize struct size.

When compiled to a native binary with **GraalVM Native Image**, the JVM heap for a typical or even complex FlixelGDX game sits at roughly **40-50 MB of system RAM**.

> [!NOTE]
> The 40-50 MB figure covers managed heap only. Native memory - which includes GPU textures, audio tracks, and other OS-managed resources - is outside the JVM heap and outside what the framework can control. How efficiently your game uses native memory depends entirely on how big your assets are and managed.

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
- **API docs (Javadoc)**: Built from `master` via GitHub Actions and published at `https://flixelgdx.org/`. Run `./gradlew javadocAll` locally to generate HTML under each module's `build/docs/javadoc`.
