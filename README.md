<div align="center">

  # FlixelGDX

  [![CI](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml/badge.svg)](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml)
  [![Maven Central](https://img.shields.io/maven-central/v/org.flixelgdx/flixelgdx-core)](https://central.sonatype.com/artifact/org.flixelgdx/flixelgdx-core)
  [![JitPack](https://jitpack.io/v/flixelgdx/flixelgdx.svg)](https://jitpack.io/#flixelgdx/flixelgdx)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![Website](https://img.shields.io/badge/website-flixelgdx.org-blue)](https://flixelgdx.org)
  [![Java 25+](https://img.shields.io/badge/Java-25%2B-orange)](https://adoptium.net/temurin/releases?version=25&os=any&arch=any)
  [![Platforms](https://img.shields.io/badge/platforms-Desktop%20%7C%20Web%20%7C%20Android-brightgreen)](https://flixelgdx.org)

  FlixelGDX is a feature-packed game framework for the Java ecosystem, with heavy inspiration from 
  [HaxeFlixel](https://haxeflixel.com/). It's designed to bring the classic style of its Haxe-based cousin, with heavy 
  improvements of its architecture, primarily focusing on simplicity, extensibility and performance. 
  
  With its modular API, it's perfect for people of all experience levels — from students wishing to create something 
  amazing while learning programming, to advanced engine power users wanting something they don't have to fight.
</div>

> [!NOTE]
> FlixelGDX is an independent project and is not officially affiliated with HaxeFlixel.

> [!IMPORTANT]
> FlixelGDX is still relatively new and currently supports desktop, browser and Android. iOS support is coming soon!

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

    if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
      Flixel.switchState(() -> new MainMenuState());
    }
  }
}
```

### Animations

The framework contains a wide set of animation utilities for Sparrow spritesheets and Adobe Animate rig atlases — including
out-of-the-box support for the Better Texture Atlas extension:

```java
// A Sparrow atlas loaded and parsed automatically. "images/player" points at
// images/player.png and images/player.xml sitting next to each other.
FlixelSprite sparrow = new FlixelSprite();
sparrow.ensureAnimation();
sparrow.animation.addSparrowFrames(Flixel.files.internal("images/player"));
sparrow.animation.addByPrefix("walk", "PLAYER WALKING0", 24, false);
sparrow.animation.play("walk");

// An Adobe Animate rig sprite. Note how a Sparrow can also be mixed with it as well. 
FlixelAnimateSprite atlas = new FlixelAnimateSprite();
atlas.addSpritemapAndAnimation(Flixel.files.internal("atlas-folder"));
atlas.animation.addSparrowFrames(Flixel.files.internal("images/player"));
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
FlixelTween.shake(player, FlixelAxes.XY, 0.008f, new FlixelTweenSettings());
```

### Particles

Emitters launch hundreds of lightweight particles without allocating a single object while your game runs, and draw
them all in one batch. They're configured with HaxeFlixel-style ranges:

```java
FlixelEmitter<FlixelParticle> sparks = new FlixelEmitter<>(64, FlixelParticle::new);
sparks.loadGraphic(Flixel.files.internal("images/spark.png"));
sparks.setBlendMode(FlixelBlendMode.ADD);
sparks.launchAngle.set(-150f, -30f);   // Spray upward.
sparks.speed.set(100f, 250f);
sparks.acceleration.set(0f, 400f);     // Gravity.
sparks.alpha.set(1f, 1f, 0f, 0f);      // Fade out over each particle's life.
sparks.alpha.ease = FlixelEase::quadIn;
add(sparks);

sparks.start(true, 0f, 20); // Burst 20 sparks at once.
```

### Remote Debugger

A game running in debug mode streams its internals to a separate desktop debugger app: frame times and memory, the
log, live watches, loaded textures, the scene tree, a property inspector with editing, a command console, the save
data, and a live map of the screen that lets you pick objects, drag them, and fly the camera from the app (handy on a
phone, which has no keyboard or mouse). It can also press keys, move and click a pointer, and type text in the game, and
take a picture of the finished frame, so a tool (or an AI agent) can drive and watch the game. Register your own values
and panels with a line each:

```java
Flixel.debug.watches.addFloat("Player X", player::getX);
Flixel.debug.console.register("god", "god [on|off]", "Makes the player invincible.", args -> {
  player.setInvincible(args.getBoolean(0, true));
});
```

Start the game in debug mode (`-Dflixel.mode=debug` on desktop, `?flixel.mode=debug` on the web, or any debuggable
Android build) and it connects to the debugger app on `127.0.0.1:6510` by itself. Nothing runs, and nothing is sent, in
release builds. The game is the WebSocket client and retries every two seconds, so you can start the app before or after
the game.

| Platform | Reach another computer with |
|----------|-----------------------------|
| Desktop  | `-Dflixel.debugHost=host[:port]` |
| Android  | `adb shell am start ... -e flixel.debugHost host[:port]`, or `adb reverse tcp:6510 tcp:6510` for a USB device (see [Debugging on Android](ANDROID_DEBUGGING.md)) |
| Web      | `?flixel.debugHost=host[:port]` in the page address |

A page served over `https` may only reach `localhost`, so keep the debugger app on the computer that runs the browser.
The wire format is in the [Remote Debug Protocol](DEBUG_PROTOCOL.md).

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

While its API remains very simple on the surface, every single system can be easily replaced directly inside your own
game — all without requiring you to maintain a fork of the framework.

Want to replace the logger? Swap the audio system for something different? Replace the asset manager? Add a whole new platform?
Not a problem at all. FlixelGDX is perfect for people of all experience levels.

### Plugins and Extensions

The framework contains many first-party plugins and extensions to automate all the tedious grunt work — allowing you to 
focus on what's important: making a game.

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
- **[Debugging on Android](ANDROID_DEBUGGING.md)**: Logs, the debugger, runtime modes, profiling, and native crash traces.
- **[Remote Debug Protocol](DEBUG_PROTOCOL.md)**: The wire protocol between a running game and the desktop debugger app.
- **[Code of Conduct](CODE_OF_CONDUCT.md)**: Rules set in place for a stable open source community.
- **[Project Roles](GOVERNANCE.md)**: How each role for the project operates, including project leaders and maintainers.
