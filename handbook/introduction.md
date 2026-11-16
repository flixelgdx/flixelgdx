# What FlixelGDX Is: The Mental Model

This guide explains what FlixelGDX is, who it is for, and the handful of ideas the whole framework is built on.
Read it first. Every other guide in this handbook assumes you know the picture painted here.

## Table of contents

1. [FlixelGDX in one paragraph](#flixelgdx-in-one-paragraph)
2. [The one analogy to remember: a theater](#the-one-analogy-to-remember-a-theater)
3. [The five big ideas](#the-five-big-ideas)
4. [How a frame feels from the inside](#how-a-frame-feels-from-the-inside)
5. [The rules of the house](#the-rules-of-the-house)
6. [What runs where: platforms and modules](#what-runs-where-platforms-and-modules)
7. [The design goals behind every decision](#the-design-goals-behind-every-decision)
8. [Where the framework came from](#where-the-framework-came-from)
9. [A map of this handbook](#a-map-of-this-handbook)
10. [Check your understanding](#check-your-understanding)

## FlixelGDX in one paragraph

FlixelGDX is a 2D game framework for Java. It handles the boring, platform-specific parts of making a game
(opening a window, talking to the graphics card, reading the keyboard, playing audio, saving files) so you can
spend your time on the fun part: the game itself. You write your game once, in plain Java, and the same code runs
on desktop (Windows, macOS, and Linux), in a web browser, and on Android. It draws everything with WebGPU, and it
is built from the ground up to avoid the small memory hiccups that make Java games stutter.

## The one analogy to remember: a theater

The easiest way to understand how a FlixelGDX game is organized is to picture a theater.

| In the theater...                  | In FlixelGDX...                                          | What it does                                                                    |
|------------------------------------|----------------------------------------------------------|---------------------------------------------------------------------------------|
| The theater building               | Your `FlixelGame` subclass                               | There is exactly one. It is open for as long as the game runs.                  |
| A play performed on the stage      | A `FlixelState` (title screen, level one, game over)     | Only one play runs at a time. When it ends, the stage is cleared for the next. |
| A curtain lowered over part of it  | A `FlixelSubState` (pause menu, dialog box)              | Covers the play without ending it. The play waits behind it.                    |
| The actors and props               | Your objects, mostly `FlixelSprite`s                     | You add them to the play, and the play updates and draws them.                  |
| The stage crew, always backstage   | The static `Flixel` class                                | You call on it for anything: input, sound, switching plays, logging.            |
| The audience's view                | A `FlixelCamera`                                         | Decides which part of the stage is visible. It can follow, zoom, and shake.     |

Keep this picture in mind. Almost every question about "where does this code go?" has an answer in it:

- Something that lasts for the whole game belongs to the theater (`FlixelGame`) or the crew (`Flixel`).
- Something that only matters on one screen belongs to that play (`FlixelState`).
- Something you can see or touch is an actor (`FlixelSprite` or one of its relatives).

## The five big ideas

### 1. One game, many states

Your game is a single `FlixelGame` that moves between states. Each screen is its own class:

```java
public class TitleState extends FlixelState { /* ... */ }
public class PlayState extends FlixelState { /* ... */ }
public class GameOverState extends FlixelState { /* ... */ }
```

You move between them with `Flixel.switchState(PlayState::new)`. When you switch, the old state and everything you
added to it is thrown away, and the new state starts fresh. That makes a state the natural unit of cleanup: if
something only matters on one screen, put it in that state and you never have to remember to delete it. The
[States and Substates](states-and-substates.md) guide covers this in depth.

### 2. Everything visible is an object you add

Things on screen are objects. The family tree is small and nests like Russian dolls:

```
FlixelBasic            Can be updated and drawn. Has on/off flags. No position.
  FlixelObject         Adds a position, a size (its hitbox), and built-in physics.
    FlixelSprite       Adds a picture: images, animation, scale, tint, transparency.
      FlixelText       Text is a sprite.
      FlixelParticle   Particles are sprites.
      FlixelSpriteGroup  A group that moves, fades, and scales as one sprite.
```

An object does nothing until you `add(...)` it to a state (or to a group that is in a state). Once added, the state
updates and draws it every frame. See [Basics, Objects, and Sprites](sprites-and-objects.md).

### 3. The game is a loop, and you fill in the blanks

A game is a loop that runs many times per second. Each pass is a **frame**, and each frame does two things: update
(read input, move things, check collisions) and draw. You rarely write the loop yourself. Instead, you override a
few methods and the framework calls them at the right time:

```java
@Override
public void create() {               // Once, when the state starts.
  super.create();
}

@Override
public void update(float elapsed) {  // Every frame. elapsed is seconds since the last frame.
  super.update(elapsed);
}

@Override
public void destroy() {              // Once, when the state is replaced.
  super.destroy();
}
```

See [The Game Loop](game-loop.md) for exactly what happens each frame and in what order.

### 4. The `Flixel` class is the stage crew

`Flixel` is a static class you never create. It holds the global services every game needs:

```java
Flixel.keys.justPressed(FlixelKey.SPACE);               // Keyboard.
Flixel.sound.play(Flixel.files.internal("jump.ogg"));   // Sound.
Flixel.switchState(MenuState::new);                     // Move to another screen.
Flixel.info("Score is {}", score);                      // Logging.
```

Every service is an interface with a platform implementation behind it. Your game code never names a platform, so
the same line works on desktop, the web, and Android. See [The Flixel Class](the-flixel-class.md).

### 5. Input is polled, not pushed

Instead of waiting for "a key was pressed" events, you ask "what is the state of this key right now?" inside
`update(...)`. Every device answers the same three questions: `pressed(...)` (held this frame),
`justPressed(...)` (went down this exact frame), and `justReleased(...)` (came up this exact frame). This keeps game
logic simple and in one place. See [Input](input.md).

## How a frame feels from the inside

Here is the life of one frame, seen from your code's point of view. The full, exact order is in
[The Game Loop](game-loop.md), but this is enough to reason about most bugs:

1. The framework reads the hardware: keys, mouse, touches, and gamepads now have this frame's state.
2. Global tweens and timers advance.
3. Your state's `update(elapsed)` runs. Calling `super.update(elapsed)` updates every member you added, which moves
   sprites by their velocity and advances their animations.
4. Cameras update (following, shaking, fading).
5. Every camera draws the state: each visible member draws itself, in the order you added them.
6. The framework finishes the frame, which clears "just pressed" and "just released" so they are fresh next frame.

Everything happens on one thread, in this order, every frame. There are no hidden background threads changing your
objects behind your back, which means you never need locks in game code.

## The rules of the house

These are the habits the whole framework is designed around. Each one has a guide that explains the "why" in depth.

1. **Always call `super`.** When you override `create()`, `update(...)`, `draw(...)`, or `destroy()`, call the parent
   version. Forgetting `super.update(elapsed)` is the number one reason sprites "stop moving."
2. **Multiply speeds by `elapsed`.** Write "200 pixels per second," not "3 pixels per frame," so the game runs at the
   same speed on every computer.
3. **Kill and revive instead of creating and destroying.** Bullets, enemies, and coins come and go constantly.
   Reusing them avoids garbage. See [Groups and Recycling](groups-and-recycling.md).
4. **Do not create objects inside `update(...)`.** Make them fields and reuse them. See [Optimization](optimization.md).
5. **Use the framework's collections.** `FlixelArray` and `FlixelMap` instead of `ArrayList` and `HashMap`. See
   [Collections and Pooling](collections-and-pooling.md).
6. **Use services after the game starts.** `Flixel.keys`, `Flixel.sound`, and friends are ready from your first
   state's `create()` onward, not in static initializers or in your game's constructor.
7. **Stay on the main thread.** Call framework APIs from your state's methods, not from your own threads.
8. **The origin is the top-left.** X grows to the right and Y grows **downward**. "Up" means a smaller Y, and gravity
   is a positive Y acceleration.

## What runs where: platforms and modules

FlixelGDX is split into Gradle modules so your game depends only on what it needs.

| Module                | What it is                                                                                         |
|-----------------------|----------------------------------------------------------------------------------------------------|
| `flixelgdx-core`      | The framework itself. Your game code only depends on this.                                         |
| `flixelgdx-desktop`   | Windows, macOS, and Linux, through SDL3 (window and input), WebGPU (drawing), and miniaudio.        |
| `flixelgdx-html5`     | The web, through TeaVM (Java to JavaScript or WebAssembly), WebGPU, and the Web Audio API.         |
| `flixelgdx-android`   | Android 8.0 (API 26) or newer with Vulkan 1.1, on 64-bit devices.                                  |
| `flixelgdx-ios`       | Planned. Not supported yet.                                                                        |
| `flixelgdx-gpu`       | The one WebGPU renderer that every platform shares.                                                |
| Gradle plugins        | Web builds, desktop packaging, shader checking, and Android texture compression.                   |

A typical game project mirrors this split: a `core` module holds your game, and tiny `desktop`, `html5`, and
`android` modules each hold a one-line launcher. You almost never touch the launchers after the first day. See
[Getting Started](getting-started.md) and [Architecture](architecture.md).

## The design goals behind every decision

When something in FlixelGDX looks unusual, one of these goals is usually the reason.

- **Smooth on every device.** Java's garbage collector pauses the game to clean up thrown-away objects. FlixelGDX
  avoids creating garbage in anything that runs every frame, and gives you tools (pools, recycling, `FlixelString`,
  primitive collections) to do the same.
- **Write once, run everywhere.** Core code never names a platform. Platform differences live behind interfaces, so
  your game does not need `if (desktop)` checks.
- **No reflection.** Reflection breaks platforms that compile ahead of time (the web, native images). Features that
  usually need it, such as turning objects into JSON, use code generated at compile time instead.
- **Friendly to beginners, powerful for experts.** The everyday path is short (`add(sprite)`, `Flixel.switchState`),
  while every global service can be swapped for your own implementation.
- **Replaceable pieces.** The logger, the save system, the asset manager, the debugger panels: all of them can be
  replaced without forking the framework. See [Extending the Framework](extending-the-framework.md).

## Where the framework came from

FlixelGDX is heavily inspired by [HaxeFlixel](https://haxeflixel.com/), the classic Haxe framework. If you have used
HaxeFlixel, many names will feel familiar: states and substates, `FlxSprite` becoming `FlixelSprite`, `kill()` and
`revive()`, `Flixel.collide(...)`, tweens with eases, and emitters with ranges. FlixelGDX is an independent project
and is not affiliated with HaxeFlixel. Where the two differ, FlixelGDX usually traded a little familiarity for speed
(no garbage) or portability (no reflection).

## A map of this handbook

The handbook is ordered from "you need this today" to "good to know later." The [index](README.md) has the full table
of contents. A good path for a newcomer is:

1. [Getting Started](getting-started.md): make a project and move a square around.
2. [The Game Loop](game-loop.md) and [States and Substates](states-and-substates.md): where code goes.
3. [Basics, Objects, and Sprites](sprites-and-objects.md): putting things on screen.
4. [Input](input.md), [Physics and Collisions](physics-and-collisions.md), and [Groups and Recycling](groups-and-recycling.md):
   making it a game.
5. Everything else as you need it. The [Cookbook](cookbook.md) answers quick "how do I...?" questions.

## Check your understanding

1. You want a pause menu that dims the level behind it. Is that a new `FlixelState` or a `FlixelSubState`? Why?
2. Your score counter needs to survive switching from `PlayState` to `GameOverState`. Where could it live?
3. A sprite you created in `create()` never appears on screen. What is the first thing to check?

(Answers: a substate, because the level must stay alive behind it; in a constructor argument of the next state, a
field on your game class, or `Flixel.save`; whether you called `add(sprite)`.)

**Next:** [Getting Started](getting-started.md)
