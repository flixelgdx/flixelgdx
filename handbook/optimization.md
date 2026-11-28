# Optimization: Writing Fast, Garbage-Free Games

A game has a strict deadline: at 60 frames per second, each frame gets about 16.7 milliseconds for everything. This
guide explains what makes Java games slow or stuttery, how to measure before you change anything, and the habits
FlixelGDX is designed around: no garbage in the frame loop, reuse instead of re-create, efficient drawing, and careful
memory use.

## Table of contents

1. [Two kinds of slow](#two-kinds-of-slow)
2. [Why garbage causes stutter](#why-garbage-causes-stutter)
3. [Measure first](#measure-first)
4. [Habit 1: create in create(), not in update()](#habit-1-create-in-create-not-in-update)
5. [Habit 2: recycle and pool](#habit-2-recycle-and-pool)
6. [Habit 3: use the framework's collections](#habit-3-use-the-frameworks-collections)
7. [Habit 4: text without new strings](#habit-4-text-without-new-strings)
8. [Habit 5: store lambdas and comparators in fields](#habit-5-store-lambdas-and-comparators-in-fields)
9. [Habit 6: avoid boxing](#habit-6-avoid-boxing)
10. [Habit 7: plain loops in hot code](#habit-7-plain-loops-in-hot-code)
11. [Drawing efficiently](#drawing-efficiently)
12. [Update less](#update-less)
13. [Collisions](#collisions)
14. [Assets and memory](#assets-and-memory)
15. [Platform-specific advice](#platform-specific-advice)
16. [A before-and-after example](#a-before-and-after-example)
17. [The checklist](#the-checklist)
18. [Check your understanding](#check-your-understanding)

## Two kinds of slow

- **Low frame rate:** every frame takes too long, so the game runs at 30 FPS instead of 60. Usually too much drawing
  or too much work per object.
- **Stutter (hitches):** most frames are fine, but every few seconds one frame takes far too long, and the motion
  jerks. In Java games, this is very often **garbage collection**.

They have different causes and different fixes, so find out which one you have before changing code.

## Why garbage causes stutter

Every `new` creates an object in memory. When nothing refers to an object anymore, it becomes **garbage**, and Java's
garbage collector eventually stops to clean it up. Modern collectors are fast, but cleanup still competes with your
game for time, and on phones and in browsers it can pause the game for a noticeable moment.

Think of a kitchen during a dinner rush. If cooks throw every used plate on the floor, someone eventually has to stop
service and sweep. If cooks rinse and reuse plates, service never stops. FlixelGDX is built so that the frame loop
reuses its "plates": the framework itself creates no garbage per frame, and it gives you the tools to do the same.

One `new` per frame is harmless. One `new` per object per frame, with a thousand objects, is 60,000 objects per
second.

## Measure first

Guessing wastes time. Look at numbers first:

- **The debugger's Stats and Performance panels** (see [Debugging](debugging.md)) show frame time, render time, draw
  calls, and memory over time. A memory graph that climbs and drops in a sawtooth pattern means garbage is being
  created and collected. A flat line is what you want.
- **Render statistics:** `Flixel.graphics.getStats()` gives draw calls, triangles, and texture memory for the last frame.
- **Profiler sections:** mark your own expensive code so it shows up in Android system traces (Perfetto, Android
  Studio):

```java
private static final String TRACE_PATHFINDING = "game.pathfinding"; // A constant name, 127 characters or fewer.

Flixel.profiler.begin(TRACE_PATHFINDING);
try {
  findPaths();
} finally {
  Flixel.profiler.end();
}
```

  The framework already marks the state update (`flixel.update.state`) and every manager by name (`flixel.render`,
  `flixel.keys`, `flixel.sound`, and others).
- **General Java tools** on desktop, such as Java Flight Recorder or VisualVM, show where time is spent and where
  objects are created. Browser developer tools have a performance tab for the web build.

Measure on your weakest target device. A game that is smooth on a gaming PC may stutter on a phone.

## Habit 1: create in create(), not in update()

If you need a temporary vector, rectangle, color, or array every frame, make it a field and reuse it:

```java
// Bad: a new color every frame.
@Override
public void update(float elapsed) {
  super.update(elapsed);
  sprite.setColor(new FlixelColor(1f, health / 100f, health / 100f, 1f));
}

// Good: one color, reused.
private final FlixelColor tint = new FlixelColor();

@Override
public void update(float elapsed) {
  super.update(elapsed);
  tint.set(1f, health / 100f, health / 100f, 1f);
  sprite.setColor(tint);
}
```

For short calculations, borrow pooled math objects with `FlixelVector.get()` and `put()` (see
[Collections and Pooling](collections-and-pooling.md)).

## Habit 2: recycle and pool

Things that come and go (bullets, enemies, coins, damage numbers) should be reused, not created and destroyed:

- Game objects: `kill()` them and get them back with a group's `recycle()`. See [Groups and Recycling](groups-and-recycling.md).
- Particles: use a `FlixelEmitter`, which never allocates after construction. See [Particles](particles.md).
- Plain objects: use a `FlixelPool`.
- Sounds you play constantly: create once and replay. See [Audio](audio.md#reusing-sounds-you-play-often).
- Tweens and timers are pooled by the framework already.

## Habit 3: use the framework's collections

`ArrayList` and `HashMap` create iterator and entry objects, and box primitive values. Use `FlixelArray`, `FlixelMap`,
and the primitive collections instead. See [Collections and Pooling](collections-and-pooling.md).

## Habit 4: text without new strings

`"Score: " + score` builds a new `String` every time it runs. For text that changes every frame, write into a reused
`FlixelString`:

```java
private final FlixelString scoreBuffer = new FlixelString();

scoreBuffer.set("Score: ").concat(score);
scoreText.setText(scoreBuffer);
```

Avoid `String.format(...)`, `String.valueOf(...)`, and `toString()` in per-frame code for the same reason. Log
messages should use placeholders (`Flixel.debug("x is {}", x)`), which cost nothing when the level is filtered out.

## Habit 5: store lambdas and comparators in fields

A lambda that uses variables from around it ("captures" them) can create a new object every time the line runs. In
code that runs every frame, create it once:

```java
// Created once, reused every frame.
private final BiConsumer<FlixelObject, FlixelObject> onCoin = (p, coin) -> {
  coin.kill();
  score++;
};
private static final Comparator<FlixelSprite> BY_Y = (a, b) -> Float.compare(a.getY(), b.getY());

@Override
public void update(float elapsed) {
  super.update(elapsed);
  Flixel.overlap(player, coins, onCoin, null);
  characters.sort(BY_Y);
}
```

The same goes for callbacks passed to `forEachMember(...)`, tweens started often, and signal listeners.

## Habit 6: avoid boxing

Putting a primitive (`int`, `float`) where Java expects an object turns it into a new `Integer` or `Float` object.
It hides in generics (`FlixelArray<Integer>`, `FlixelSignal<Float>`, `Supplier<Float>`) and in varargs. Prefer:

- Primitive collections: `FlixelIntArray`, `FlixelIntMap`, `FlixelObjectIntMap`.
- Primitive functional interfaces: `FloatSupplier`, `IntSupplier`, `FloatConsumer` (the framework has the ones Java
  is missing in `org.flixelgdx.functional`).
- Typed debugger watches: `addFloat(...)` rather than `add(...)`.

## Habit 7: plain loops in hot code

Streams (`list.stream().filter(...)`) create several objects per use. In per-frame code, use indexed `for` loops:

```java
Enemy[] items = enemies.getItems();
for (int i = 0, n = enemies.getSize(); i < n; i++) {
  items[i].think(elapsed);
}
```

## Drawing efficiently

The GPU is fastest when it can draw many sprites in one batch. A batch ends whenever the texture, blend mode, or
shader changes. See [Graphics and Shaders](graphics-and-shaders.md#batching-why-draw-order-affects-speed).

- **Pack images into atlases.** Sprites that share one texture batch together. Sparrow atlases are ideal.
- **Group by blend mode and shader.** Keep `ADD` sprites next to each other in draw order, not interleaved with
  `NORMAL` ones.
- **Prefer camera or global shaders** over per-sprite shaders for effects on many sprites.
- **Hide what is off screen.** Objects far outside the view still cost update time; kill or deactivate them.
- **Use render scale on weak devices.** `Flixel.graphics.setRenderScale(0.75f)` draws fewer pixels with little visible
  difference on a small phone screen.
- **Watch the draw call count** in the stats panel. Hundreds of draw calls for a simple scene means batches are being
  broken.

## Update less

- **`active = false`** freezes an object that does not need to think right now (an enemy far away), while it still
  draws.
- **`exists = false`** (or `kill()`) removes it from both update and draw.
- **Do expensive work less often.** Pathfinding, AI decisions, and line-of-sight checks rarely need to run every frame.
  A timer every 0.2 seconds is usually indistinguishable to the player.
- **Do not create animation controllers you do not need.** `ensureAnimation()` only on sprites that animate.

## Collisions

`Flixel.overlap` and `Flixel.collide` flatten both sides into plain lists and then pick the cheaper strategy:

- **Few pairs** (for example one player against a group of coins): every pair is tested directly.
- **Many pairs** (for example 200 bullets against 200 enemies): the second side is sorted into a uniform spatial
  grid first, and each object on the first side is only tested against objects in the cells it touches. Instead of
  40,000 tests, most objects are only checked against a handful of neighbors.

Neither path creates garbage, as long as the callbacks live in fields. You can still help:

- Collide only the groups that can actually interact (player bullets against enemies, not against everything).
- Keep groups small by killing what leaves the screen.
- Keep `Flixel.setWorldBounds(...)` close to the level size. Objects completely outside it are skipped entirely.
- Very large objects (a floor spanning the whole level) are tested against every object on the other side, so a level
  built from many long walls is cheaper as several shorter pieces or a tilemap.

## Assets and memory

- **Preload on a loading screen.** Loading a large file the first time it is needed causes a hitch mid-game. See
  [Files and Assets](files-and-assets.md#building-a-loading-screen).
- **Unload what you are done with.** Assets stay in memory until `Flixel.assets.unload(path)`. Free big level art when
  the level is behind you.
- **Watch for duplicates.** The debugger's textures panel marks textures loaded twice under different paths.
- **Compress textures on Android.** The Basis Universal plugin keeps textures compressed in GPU memory. See
  [Building and Shipping](building-and-shipping.md).
- **Remove global listeners.** A listener left on a global signal keeps a whole destroyed state in memory. See
  [Signals](signals.md#avoiding-leaks).

A state switch triggers a garbage collection by default, which is the perfect moment for it: the old state just became
garbage, and a brief pause during a transition is invisible. Do not call `System.gc()` yourself during gameplay.

## Platform-specific advice

- **Desktop:** the most forgiving platform. Give packaged games enough memory with a JVM argument (for example
  `jvmArg('-Xmx1G')` in the packaging plugin) if they load a lot of art.
- **Web:** the WebAssembly build is faster than the JavaScript build, and the generated page picks it when the browser
  supports it. Garbage collection in browsers is less predictable, so garbage-free habits matter even more here. Keep
  the initial download small: every asset is downloaded before the game starts, so exclude files the web build does not
  need.
- **Android:** phones throttle when they get hot, so a game that barely makes 60 FPS will drop after a few minutes.
  Leave headroom, use render scale, compress textures, and test on a real mid-range device rather than an emulator.

## A before-and-after example

```java
// Before: four kinds of garbage every frame.
@Override
public void update(float elapsed) {
  super.update(elapsed);
  scoreText.setText("Score: " + score);                                  // A new String.
  Flixel.overlap(player, coins, (p, c) -> { c.kill(); score++; }, null); // Maybe a new lambda.
  FlixelVector toPlayer = new FlixelVector(player.getX() - boss.getX(),  // A new vector.
      player.getY() - boss.getY());
  for (Enemy e : enemyList) {                                             // An ArrayList iterator.
    e.think(elapsed);
  }
}
```

```java
// After: nothing is created per frame.
private final FlixelString scoreBuffer = new FlixelString();
private final FlixelVector toPlayer = new FlixelVector();
private final BiConsumer<FlixelObject, FlixelObject> onCoin = (p, c) -> {
  c.kill();
  score++;
};

@Override
public void update(float elapsed) {
  super.update(elapsed);
  scoreBuffer.set("Score: ").concat(score);
  scoreText.setText(scoreBuffer);
  Flixel.overlap(player, coins, onCoin, null);
  toPlayer.set(player.getX() - boss.getX(), player.getY() - boss.getY());
  Enemy[] items = enemies.getItems();
  for (int i = 0, n = enemies.getSize(); i < n; i++) {
    items[i].think(elapsed);
  }
}
```

## The checklist

| Check                                                               | Guide |
|---------------------------------------------------------------------|-------|
| No `new` inside `update(...)` or `draw(...)`.                       | This guide |
| Frequent objects are recycled, pooled, or emitted.                  | [Groups and Recycling](groups-and-recycling.md) |
| `FlixelArray` and `FlixelMap` instead of `ArrayList` and `HashMap`. | [Collections and Pooling](collections-and-pooling.md) |
| Changing text uses `FlixelString`.                                  | [Text, HUDs, and Bars](text-and-ui.md) |
| Per-frame lambdas and comparators live in fields.                   | This guide |
| No streams or boxing in hot code.                                   | This guide |
| Images share atlases; blend modes and shaders are grouped.          | [Graphics and Shaders](graphics-and-shaders.md) |
| Off-screen and idle objects are inactive or killed.                 | [Basics, Objects, and Sprites](sprites-and-objects.md) |
| Collision checks only include groups that can interact.             | [Physics and Collisions](physics-and-collisions.md) |
| Big assets are preloaded, and unused ones are unloaded.             | [Files and Assets](files-and-assets.md) |
| Global signal listeners are removed in `destroy()`.                 | [Signals](signals.md) |
| The memory graph in the debugger is flat during gameplay.           | [Debugging](debugging.md) |

## Check your understanding

1. The game runs at a steady 60 FPS, but freezes for a moment every few seconds. What is the most likely cause, and
   how do you confirm it?
2. Why is `"HP: " + hp` fine in `create()` but not in `update(...)`?
3. Your stats panel shows 300 draw calls for a scene of 400 sprites. What should you look at?

(Answers: garbage collection; watch the memory graph in the debugger's performance panel for a sawtooth; `create()`
runs once, while `update(...)` runs 60 or more times per second, creating a new string each time; texture, blend mode,
and shader changes between consecutive sprites, which break batches; pack images into atlases and group sprites by
blend mode and shader.)

**Next:** [Platform Guide](platforms.md)
