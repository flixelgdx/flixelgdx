# Tweens and Timers: Smooth Changes and Delayed Actions

Tweens smoothly change values over time (slide a menu in, fade a logo, bounce a coin). Timers run code later or on a
repeating rhythm (spawn a boss in 30 seconds, an enemy every 2 seconds). This guide covers both: goals and eases,
every ready-made tween type, chaining, looping, motion paths, controlling and canceling tweens, and global and
per-state timers.

## Table of contents

1. [What a tween is](#what-a-tween-is)
2. [Your first tween](#your-first-tween)
3. [Settings: duration, ease, delays, and callbacks](#settings-duration-ease-delays-and-callbacks)
4. [Eases: the shape of the motion](#eases-the-shape-of-the-motion)
5. [Tween types: once, loop, ping-pong, backward, persist](#tween-types-once-loop-ping-pong-backward-persist)
6. [Chaining tweens](#chaining-tweens)
7. [Ready-made tweens: numbers, colors, angles, shake, flicker](#ready-made-tweens-numbers-colors-angles-shake-flicker)
8. [Motion paths](#motion-paths)
9. [Controlling and canceling tweens](#controlling-and-canceling-tweens)
10. [Timers](#timers)
11. [Timers that belong to one state](#timers-that-belong-to-one-state)
12. [Tweens, timers, time scale, and pausing](#tweens-timers-time-scale-and-pausing)
13. [Memory: why tweens and timers are pooled](#memory-why-tweens-and-timers-are-pooled)
14. [Check your understanding](#check-your-understanding)

## What a tween is

"Tween" is short for "in-between." In hand-drawn animation, a lead artist draws the key poses and an assistant draws
the frames in between. A tween is that assistant: you say "the title is at y = -50 now, and it should be at y = 80 in
one second," and the tween fills in every frame between.

An **ease** shapes the in-between frames. Linear motion moves at a constant speed and looks robotic. Real things speed
up and slow down, and eases let your tweens do the same.

## Your first tween

A tween needs to know three things about each value it changes: how to read it, where it should end up, and how to
write it. You give it those as a **goal**:

```java
// Slide the title to y = 80 over 1 second, landing with a bounce.
FlixelTween.tween(title, new FlixelTweenSettings()
    .addGoal(title::getY, 80f, title::setY)   // Read with getY, target 80, write with setY.
    .setDuration(1f)
    .setEase(FlixelEase::bounceOut));
```

Several goals in one tween move together:

```java
FlixelTween.tween(player, new FlixelTweenSettings()
    .addGoal(player::getX, 500f, player::setX)
    .addGoal(player::getY, 300f, player::setY)
    .addGoal(player::getAlpha, 0.5f, player::setAlpha)
    .setDuration(1.5f)
    .setEase(FlixelEase::quadOut));
```

Getters and setters (method references such as `title::getY`) mean no reflection is involved, which keeps tweens fast
and working on every platform. The first argument to `tween(...)` is the object the tween belongs to; it is what
`cancelTweensOf(...)` looks for later.

## Settings: duration, ease, delays, and callbacks

`FlixelTweenSettings` is a small builder. Every setter returns the settings, so they chain:

| Setting                        | Meaning                                                                  |
|--------------------------------|--------------------------------------------------------------------------|
| `setDuration(seconds)`         | How long one run takes.                                                  |
| `setEase(FlixelEase::name)`    | The shape of the motion. Linear by default.                              |
| `setType(FlixelTweenType...)`  | What happens at the end (see below). `ONESHOT` by default.               |
| `setStartDelay(seconds)`       | Wait before starting.                                                    |
| `setLoopDelay(seconds)`        | Wait between repeats of a looping tween.                                 |
| `setFramerate(fps)`            | Update the value at a fixed rate, for a choppy, retro "stepped" look.    |
| `setOnStart(tween -> ...)`     | Runs when the tween starts.                                              |
| `setOnUpdate(tween -> ...)`    | Runs every time the value changes.                                       |
| `setOnComplete(tween -> ...)`  | Runs when the tween finishes.                                            |

```java
logo.setAlpha(0f);
FlixelTween.tween(logo, new FlixelTweenSettings()
    .addGoal(logo::getAlpha, 1f, logo::setAlpha)
    .setDuration(0.5f)
    .setStartDelay(0.25f)
    .setOnComplete(tween -> Flixel.info("Logo is visible!")));
```

## Eases: the shape of the motion

All eases live in `FlixelEase`. Each family comes in three variants:

- `...In`: starts slow, ends fast (like a car pulling away).
- `...Out`: starts fast, ends slow (like a car braking). The most common choice for UI.
- `...InOut`: slow at both ends, fast in the middle.

| Family                           | Feel                                                   |
|----------------------------------|--------------------------------------------------------|
| `linear`                         | Constant speed. No variants.                           |
| `quad`, `cube`, `quart`, `quint` | Gentle to very sharp acceleration curves.              |
| `sine`                           | Very soft. Good for floating and breathing motion.     |
| `circ`, `expo`                   | Sharp, snappy starts or stops.                         |
| `smoothStep`, `smootherStep`     | Smooth S-curves.                                       |
| `back`                           | Overshoots the target slightly, then settles. Playful. |
| `elastic`                        | Springs past the target and wobbles. Cartoony.         |
| `bounce`                         | Bounces off the target like a dropped ball.            |

So `FlixelEase::quadOut`, `FlixelEase::backInOut`, `FlixelEase::bounceOut`, and so on. An ease is just a function
from time (0 to 1) to progress, so you can write your own:

```java
FlixelEaseFunction stepped = t -> (float) Math.floor(t * 4f) / 4f; // Four jumpy steps.
```

## Tween types: once, loop, ping-pong, backward, persist

Pass a `FlixelTweenType` to the settings constructor or to `setType(...)`:

| Type       | Behavior                                                                  |
|------------|---------------------------------------------------------------------------|
| `ONESHOT`  | Runs once, then is released. The default.                                 |
| `LOOPING`  | Restarts from the beginning forever. `onComplete` runs every cycle.       |
| `PINGPONG` | Goes to the target, back to the start, and so on forever. `onComplete` runs every cycle. |
| `BACKWARD` | Runs once in reverse, from the target to the start, then stays around like `PERSIST`. |
| `PERSIST`  | Runs once, then stays around so you can `start()` it again later.         |

```java
// Make a coin bob up and down forever.
FlixelTween.tween(coin, new FlixelTweenSettings(FlixelTweenType.PINGPONG)
    .addGoal(coin::getY, coin.getY() - 6f, coin::setY)
    .setDuration(0.6f)
    .setEase(FlixelEase::sineInOut));
```

## Chaining tweens

`then(...)` starts another tween as soon as one finishes. Each `then(...)` waits for the tween returned by the step
before it, so you can build a sequence:

```java
// Slide right, drop down, then spin.
FlixelTween.tween(box, new FlixelTweenSettings()
        .addGoal(box::getX, 300f, box::setX)
        .setDuration(0.5f)
        .setEase(FlixelEase::quadOut))
    .then(() -> FlixelTween.tween(box, new FlixelTweenSettings()
        .addGoal(box::getY, 200f, box::setY)
        .setDuration(0.5f)
        .setEase(FlixelEase::bounceOut)))
    .then(() -> FlixelTween.angle(box, 180f, new FlixelTweenSettings().setDuration(0.3f)));
```

The next tween is built when the previous one ends, so it starts from wherever the object is at that moment.

## Ready-made tweens: numbers, colors, angles, shake, flicker

Common effects have their own one-liners:

```java
// A plain number, delivered to a callback. Great for counting up a score.
FlixelTween.num(0f, 5000f, new FlixelTweenSettings().setDuration(1.5f), value -> {
  scoreBuffer.set("Score: ").concat((int) value);
  scoreText.setText(scoreBuffer);
});

// A color change from one color to another, applied to anything with a tint.
FlixelTween.color(sky, FlixelColor.CYAN, FlixelColor.ORANGE, new FlixelTweenSettings().setDuration(5f));

// Rotation to an angle, or from one angle to another.
FlixelTween.angle(arrow, 90f, new FlixelTweenSettings().setDuration(0.2f));
FlixelTween.angle(arrow, 0f, 360f, new FlixelTweenSettings(FlixelTweenType.LOOPING).setDuration(1f));

// A shake on one or both axes. Intensity is a fraction of the object's size.
FlixelTween.shake(player, FlixelAxes.XY, 0.02f, new FlixelTweenSettings().setDuration(0.2f));

// Blink after being hit (invincibility frames).
FlixelTween.flicker(player, new FlixelTweenSettings().setDuration(1f));
```

Shake is not limited to sprites. Anything that can shake works, including the window itself
(`Flixel.window`), which is a fun effect for big explosions on desktop.

## Motion paths

Motion tweens move an object along a path. Each takes a `durationOrSpeed` and a `useDuration` flag: with `true`, the
number is a duration in seconds; with `false`, it is a speed in pixels per second, so long paths take longer.

```java
// A straight line from (0, 0) to (400, 200) in 2 seconds.
FlixelTween.linearMotion(enemy, 0f, 0f, 400f, 200f, 2f, true, new FlixelTweenSettings());

// A curve through a control point (a quadratic Bezier curve).
FlixelTween.quadMotion(ball, 0f, 300f, 200f, 0f, 400f, 300f, 1.5f, true, new FlixelTweenSettings());

// A full circle around (320, 180) with radius 100, starting at 0 degrees, clockwise, at 120 pixels per second.
FlixelTween.circularMotion(moon, 320f, 180f, 100f, 0f, true, 120f, false,
    new FlixelTweenSettings(FlixelTweenType.LOOPING));

// A path through several points, given as x, y pairs.
FlixelTween.linearPath(guard, 60f, false, new FlixelTweenSettings(FlixelTweenType.PINGPONG),
    100f, 100f,
    300f, 100f,
    300f, 250f);
```

`cubicMotion(...)` takes four points for an S-shaped curve, and `quadPath(...)` makes a smooth curve through many
points.

## Controlling and canceling tweens

Every factory returns the `FlixelTween`, which you can keep to control it:

```java
FlixelTween slide = FlixelTween.tween(panel, settings);
slide.pause();
slide.resume();
slide.cancel();   // Stop now, leaving the value where it is.
slide.finish();   // Jump to the end and complete.
boolean done = slide.isFinished();
```

You do not have to keep tweens around to cancel them. Ask by object instead:

```java
FlixelTween.cancelTweensOf(player);      // Cancel every tween on the player.
FlixelTween.completeTweensOf(player);    // Jump every tween on the player to its end.
boolean busy = FlixelTween.containsTweensOf(player);
```

Canceling the old tweens before starting a new one is a good habit for things that can be triggered repeatedly, such
as a button that bounces when hovered. Otherwise two tweens fight over the same value.

**Do not keep a reference to a finished or canceled tween.** Tweens are recycled (see below), so a reference to a
finished tween may soon point at a completely different tween. `PERSIST` and `BACKWARD` tweens are the exception;
they stay yours until you cancel them.

## Timers

Timers run code after a delay or on a rhythm, without you counting `elapsed` by hand:

```java
// Once, after 2 seconds.
FlixelTimer.wait(2f, timer -> spawnBoss());

// Every 0.5 seconds, 10 times.
FlixelTimer.loop(0.5f, timer -> spawnEnemy(), 10);

// Every 3 seconds, forever (0 means "loop forever").
FlixelTimer spawner = FlixelTimer.loop(3f, timer -> dropPowerup(), 0);

// Later, when the wave ends:
spawner.cancel();
```

The callback receives the timer, which tells you where it is:

```java
FlixelTimer.loop(1f, timer -> {
  countdownBuffer.set("Starting in ").concat(timer.getLoopsLeft());
  countdownText.setText(countdownBuffer);
}, 3);
```

Useful timer members: `getTimeLeft()`, `getProgress()` (0 to 1), `getLoopsLeft()`, `elapsedLoops`, `restart(seconds)`,
and the `active` flag, which pauses a single timer while `false`.

A timer with a duration of `0` runs its callback on the **next** frame, not immediately. That is a handy way to say
"do this once the current update has finished."

Like tweens, **do not keep a timer reference after it finishes or after you cancel it**; it goes back to a pool and may
be reused for another timer.

## Timers that belong to one state

The static `FlixelTimer.wait(...)` and `loop(...)` use one global manager, and **every state switch cancels all global
timers**. That keeps a level's timers from firing on the menu screen.

Sometimes you want more control, such as timers that freeze while a pause menu is open. A `FlixelTimerManager` is a
`FlixelBasic`, so you can add one to a state, and it updates only when the state updates:

```java
private final FlixelTimerManager levelTimers = new FlixelTimerManager();

@Override
public void create() {
  super.create();
  add(levelTimers); // Updated and destroyed with the state.
  levelTimers.start(30f, timer -> spawnBoss());
  levelTimers.start(2f, timer -> spawnEnemy(), 0); // Every 2 seconds, forever.
}
```

Because a state stops updating while a substate is open (unless `persistentUpdate` is on), these timers pause with the
level automatically, while global timers keep ticking.

## Tweens, timers, time scale, and pausing

- Tweens and timers use the scaled `elapsed`, so `Flixel.timeScale = 0.5f` slows them down too.
- They stop when the game is paused by losing focus or by the debugger.
- Global tweens and timers keep running while a pause substate is open. Use a state-owned `FlixelTimerManager` for
  timers that should freeze with the level.
- A state switch cancels global tweens and timers. To keep tweens alive across a switch (for example a music fade),
  use `Flixel.switchState(NextState::new, false)`; to keep timers too, pass `false` twice.

## Memory: why tweens and timers are pooled

Effects like "bob every coin" or "start a timer per enemy" can create thousands of tweens and timers over a play
session. Instead of making a new object each time, the framework keeps finished tweens and timers in pools and hands
them out again. After the first few seconds of play, starting a tween or timer creates no garbage. The cost is the rule
above: once a tween or timer is finished or canceled, it is no longer yours.

The settings object and the lambdas you pass are still new objects each time you build them, so avoid starting
tweens every frame. Start them in response to events (a button press, a hit, a state starting).

## Check your understanding

1. A menu panel sometimes jitters when you hover it quickly several times. What is likely happening, and how do you
   fix it?
2. Which ease would you pick for a pop-up that overshoots slightly and settles?
3. Your "spawn wave" timer keeps firing while the pause menu is open. How do you make it pause with the level?

(Answers: several tweens are fighting over the same value; call `FlixelTween.cancelTweensOf(panel)` before starting a
new one; `FlixelEase::backOut`; start it on a `FlixelTimerManager` that is added to the level state instead of using
the global `FlixelTimer.loop(...)`.)

**Next:** [Signals](signals.md)
