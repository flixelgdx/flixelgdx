<div align="center">
  <img src="markdown-assets/readme/flxgdx.png" width="500" alt="FlixelGDX logo">

  # FlixelGDX
    
  **Logo Artist: [LeoThM](https://www.instagram.com/leoxthm_/)**

  [![CI](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml/badge.svg)](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml)
  [![Maven Central](https://img.shields.io/maven-central/v/org.flixelgdx/flixelgdx-core)](https://central.sonatype.com/artifact/org.flixelgdx/flixelgdx-core)
  [![JitPack](https://jitpack.io/v/flixelgdx/flixelgdx.svg)](https://jitpack.io/#flixelgdx/flixelgdx)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![Website](https://img.shields.io/badge/website-flixelgdx.org-blue)](https://flixelgdx.org)
  [![Stars](https://img.shields.io/github/stars/flixelgdx/flixelgdx)](https://github.com/flixelgdx/flixelgdx/stargazers)
  [![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net/temurin/releases?version=17&os=any&arch=any)
  [![Platforms](https://img.shields.io/badge/platforms-Desktop%20%7C%20Web-brightgreen)](https://flixelgdx.org)

  FlixelGDX is a feature-packed game framework for the Java ecosystem, with heavy inspiration from [HaxeFlixel](https://haxeflixel.com/),
  the original ActionScript [Flixel](http://www.flixel.org/), and [libGDX](https://libgdx.com/). It's designed
  to bring the classic style of it's Haxe-based cousin, with heavy improvements of its architecture, primarily
  focusing on simplicity, extensibility and performance. 
  
  With its simplistic and very modular API, it's perfect for people of all experience levels — from students wishing to
  create something amazing while learning programming, to advanced engine developers wanting a simple API while remaining
  extensible.
</div>

> [!NOTE]
> FlixelGDX is an independent project and is not officially affiliated with HaxeFlixel.

> [!IMPORTANT]
> FlixelGDX is still relatively new and currently supports desktop and HTML5. Mobile support is coming soon!

---

## Features

FlixelGDX is packed with a comprehensive toolset, ranging from simple utility classes to multiple plugins — all made 
with developer experience in mind.

### State Management

The framework provides the iconic and simple state-based architecture that every veteran knows and loves:

```java
public class PlayState extends FlixelState {
  private FlixelSprite player;
  
  @Override
  public void create() {
    player = new FlixelSprite();
    player.loadGraphic(Flixel.files.internal("player.png"));
    player.screenCenter();
    add(player);
  }
  
  @Override
  public void update(float elapsed) {
    float speed = 500f * elapsed;
    if (Flixel.keys.pressed(FlixelKey.LEFT)) {
      player.changeX(-speed);
    }
    if (Flixel.keys.pressed(FlixelKey.RIGHT)) {
      player.changeX(speed);
    }
    // So on...
  }
}
```

### Animations

The framework contains a wide set of animation utilities for Sparrow spritesheets and Adobe Animate rig atlases — including
out-of-the-box support for the Better Texture Atlas extension:

```java
// A simple sparrow atlas being loaded and parsed automatically.
FlixelSprite sparrow = new FlixelSprite();
sparrow.ensureAnimation();
sparrow.animation.addSparrowAtlas(Flixel.files.internal("sparrow-folder"));
sparrow.animation.addByPrefix("walk", "PLAYER WALKING0", 24, false);
sparrow.animation.play("walk");

// An Adobe Animate rig sprite. Note how a Sparrow can also be mixed with it as well. 
FlixelAnimateSprite atlas = new FlixelAnimateSprite();
atlas.addSpritemapAndAnimation(Flixel.files.internal("atlas-folder"));
atlas.animation.addSparrowAtlas(Flixel.files.internal("sparrow-folder"));
atlas.animation.play("jump");  // Play an animation from the rig just like how you would for Sparrows.
```

If you want more advanced control, you can also set a finite state machine:

```java
// After loading frames and registering clips on the sprite:
var fsm = new FlixelAnimationStateMachine(player);
fsm.addState("idle", "idle").allowTo("run", "attack");
fsm.addState("run", "run").allowTo("idle", "attack");
fsm.addState("attack", "attack")
   .autoAdvanceTo("idle")
   .onEnter(() -> sword.swing());
fsm.setState("idle");

// Add it to a controller so it's ticked automatically:
player.animation.setStateMachine(fsm);

// In your game code:
if (attackPressed){
  fsm.setState("attack");
} else if (speed > 0.1f) {
  fsm.setState("run");
} else {
  fsm.setState("idle");
}
```

### Tweening

FlixelGDX provides a very flexible and simple yet robust tweening engine:

```java
// Slide a sprite to x=500, y=300 over 1.5 seconds with a bounce-out ease.
FlixelTween.tween(player, new FlixelTweenSettings()
  .addGoal(player::getX, 500f, player::setX)
  .addGoal(player::getY, 300f, player::setY)
  .setDuration(1.5f)
  .setEase(FlixelEase::bounceOut));

// Shake a sprite on both the X and Y axis.
Flixel.shake(player, FlixelAxes.XY, 0.008f, new FlixelTweenSettings());
```

### Performant Collection System

FlixelGDX contains a large, performant-first collection system designed to be simple while being safe to use in hot loops,
beating Java's standard garbage-filling collection system:

```java
FlixelArray<String> names = new FlixelArray<>();
names.add("Foo");
names.add("Bar");

names.get(1); // Returns "Bar".

// Every iterable collection reuses its iterator, meaning no
// allocations are ever made inside for-each loops.
for (String name : names) {
  Flixel.info(name);
}

// Access the raw underlying array.
String[] rawNames = names.getItems();

// Use the built-in snapshot mode to modify the collection mid-loop.
String[] snapshot = names.begin();
for (int i = 0; i < names.getSize(); i++) {
  String name = snapshot[i];
  if (notValidName(name)) {
    names.removeValue(name, true);
  }
}
names.end();

// Simple pooling system to reuse objects.
FlixelPool<Bullet> bullets = new FlixelPool<>() {
  @Override
  protected Bullet newObject() {
    return new Bullet();
  }
};

Bullet b = bullet.obtain();
// Use the bullet...
bullets.free(b);
```

### Extremely Flexible Modularity

While it's API remains very simple on the surface, every single system can be easily replaced directly inside your own
game — all without requiring you to maintain a fork of the framework.

Want to replace the logger? Swap the audio system for something different? Replace the asset manager? Add a whole new platform?
Not a problem at all. FlixelGDX is perfect for people of all experience levels.

### Plugins and Extensions

The framework contains many first-party plugins and extensions to automate all the tedious grunt work — allowing you to 
focus on what's important: making a game.

The key stars of the show are:

#### Shader Plugin

Since FlixelGDX's native platforms (primarily desktop and mobile) are built on [bgfx](https://github.com/bkaradzic/bgfx),
that usually means handling shaders would be a nightmare: download bgfx's source code and its required dependencies,
wait forever until it compiles, write different shaders for each graphics API, etc.

Not here. The framework's `org.flixelgdx.shader` plugin pre-bundles the `shaderc` binary for every platform, and
cross-compiles a single GLSL shader into every `.bin` shader file for each graphics API:

```groovy
plugins {
  id 'java'
  id 'org.flixelgdx.shaders' version '<flixel-version>'
}

flixelShaders {
  // Where the .glsl sources live (default: src/main/shaders).
  sourceDir = rootProject.file('assets/shaders')

  // Link and register a shader with an ID you'll use in your game.
  shader('grayscale') {
    fragment = 'grayscale.frag.glsl'
  }

  shader('wave') {
    fragment = 'wave.frag.glsl'
    vertex = 'wave.vert.glsl'  // Optional.
  }
}
```

Then inside a game:

```java
FlixelShader grayscale = FlixelShaders.load("grayscale");
FlixelShader wave = FlixelShaders.load("wave");

sprite.setShader(grayscale);
camera.setShader(wave);
```

#### Basis Universal Plugin

The framework provides a Basis Universal compression plugin that automates converting images to small `.ktx2` files

```groovy
flixelgdxBasisu {
  // Explicitly override when it's enabled.
  enabled = true

  // Use higher-quality UASTC instead of the default smaller ETC1S mode (default: false).
  useUastc = false

  // ETC1S quality level, 1 (smallest, worst) to 255 (largest, best). Ignored in UASTC mode.
  // Default: 128.
  etc1sQuality = 128

  // UASTC encoding level, 0 (fastest, worst) to 4 (slowest, best). Ignored in ETC1S mode.
  // Default: 2.
  uastcLevel = 2

  // Ant-style glob patterns to ignore any assets that don't need compression.
  // A trailing /** excludes an entire folder.
  excludes = [
    'ui/icons/app-icon.png',
    'fonts/**'
  ]
}
```

#### Kotlin Extension

Kotlin in FlixelGDX is a first-class citizen. The framework provides an out-of-the-box extension to provide
idiomatic syntax for our Kotlin users:

```kotlin
// Simple access and modification of collections.
val inventory = flixelArrayOf(Sword(), Steak())
inventory[1] // Returns the Steak object.
inventory += Potion() // Add a new object.

// Simple index-based loop.
for (i in inventory.indices) { ... }

// Inlined forEach method for primitive collections, providing safe iterating in hot-loops.
val ids = flixelIntArrayOf(...)
ids.forEach { ... }

// Straightforward pool creation.
val bullets = flixelPool() { Bullet() }

// Use goal-based tweening on any object.
player.tween(duration = 1.5f, ease = FlixelEase::bounceOut) {
  goal(player::getX, 500f, player::setX)
  goal(player::getY, 300f, player::setY)
}
```

---

## Getting Started

If you want to start using the framework, you don't need to worry about configuring anything too crazy — the framework's 
already got you covered. You can get started right way with the following links:

- [🔧 Project Generator](https://flixelgdx.org/getting-started)
- [📖 Docs](https://flixelgdx.org/docs)
- [⚙️ API Pages](https://flixelgdx.org/api)

---

## Project navigation

- **[Contributing Guide](CONTRIBUTING.md)**: Coding standards, PR requirements, and how to contribute.
- **[Project Structure](ARCHITECTURE.md)**: The multi-module layout and how Gradle is used.
- **[Compiling & Testing](COMPILING.md)**: How to build the framework and test it as a dependency in your own projects.
- **[Code of Conduct](CODE_OF_CONDUCT.md)**: Rules set in place for a stable open source community.
- **[Project Roles](GOVERNANCE.md)**: How each role for the project operates, including project leaders and maintainers.
