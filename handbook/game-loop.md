# The Game Loop: Create, Update, Draw, and Destroy

Every game is a loop that runs many times per second. This guide explains what the loop does, the exact order of
every step in a frame, what `elapsed` means, how slow motion and pausing work, and which hooks you can override to
react to the window and the platform.

## Table of contents

1. [Why games are loops](#why-games-are-loops)
2. [The four lifecycle methods](#the-four-lifecycle-methods)
3. [What `elapsed` is and why you multiply by it](#what-elapsed-is-and-why-you-multiply-by-it)
4. [The exact order of one frame](#the-exact-order-of-one-frame)
5. [Always call `super`](#always-call-super)
6. [Slow motion with `timeScale`](#slow-motion-with-timescale)
7. [Pausing: auto-pause, substates, and the debugger](#pausing-auto-pause-substates-and-the-debugger)
8. [Focus, minimize, suspend, and close](#focus-minimize-suspend-and-close)
9. [Global hooks with managers](#global-hooks-with-managers)
10. [Global hooks with signals](#global-hooks-with-signals)
11. [Things that live across states](#things-that-live-across-states)
12. [Quitting the game](#quitting-the-game)
13. [Check your understanding](#check-your-understanding)

## Why games are loops

A movie is a stack of still pictures shown quickly enough that your eye sees motion. A game is the same, except the
next picture is not known in advance: it depends on what the player does. So a game repeats the same two steps
forever:

1. **Update:** look at the input, move things, check collisions, run timers. The world changes a tiny bit.
2. **Draw:** take a picture of the world as it is now and show it.

Each pass is called a **frame**. At 60 frames per second (FPS), each frame gets about 16.7 milliseconds to do both
steps. The framework owns the loop itself; you fill in the update logic, and objects mostly draw themselves.

## The four lifecycle methods

Every state (and most objects) has the same four moments in its life:

| Method                  | When it runs                                   | What to put in it                                            |
|-------------------------|------------------------------------------------|--------------------------------------------------------------|
| `create()`              | Once, right after the state becomes active.    | Build the scene: create sprites, load data, `add(...)` them. |
| `update(float elapsed)` | Every frame, unless paused.                    | Game logic: input, movement, collisions, scoring.            |
| `draw(FlixelBatch)`     | Every frame, once per camera.                  | Rarely overridden. Every added member draws itself.          |
| `destroy()`             | Once, when the state is replaced or the game closes. | Release things you created yourself, remove global listeners. |

```java
public class PlayState extends FlixelState {

  private FlixelSprite player;

  @Override
  public void create() {
    super.create();
    player = new FlixelSprite(100f, 100f, Flixel.files.internal("images/player.png"));
    add(player);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (Flixel.keys.pressed(FlixelKey.RIGHT)) {
      player.changeX(150f * elapsed);
    }
  }

  @Override
  public void destroy() {
    // Members you add()-ed are destroyed for you. Only clean up what the state does not own.
    super.destroy();
  }
}
```

Why build the scene in `create()` instead of the constructor? Because the state's constructor runs when the
framework calls your recipe (`PlayState::new`), and `create()` runs after the old state is gone and the cameras are
reset. Doing setup in `create()` means everything you touch is ready, and it keeps the constructor free for simple
data such as `new GameOverState(score)`.

## What `elapsed` is and why you multiply by it

`elapsed` is the number of seconds since the previous frame. At 60 FPS it is about `0.0167`; at 144 FPS it is about
`0.0069`.

Think of the difference between "walk 3 steps every time the clock ticks" and "walk 180 steps per minute." With the
first, a faster clock makes you walk faster. With the second, your pace is the same no matter how fast the clock
ticks. Multiplying by `elapsed` turns the first kind of instruction into the second kind.

```java
// Bad: 5 pixels per frame. Twice as fast on a 120 Hz monitor as on a 60 Hz one.
player.changeX(5f);

// Good: 300 pixels per second, on every machine.
player.changeX(300f * elapsed);
```

You can read the current frame's value anywhere with `Flixel.getElapsed()`. Built-in motion (velocity,
acceleration, drag), animations, tweens, and timers already use it, so you only need it for movement you do by hand.

Two safety details:

- `elapsed` is **clamped** between `Flixel.MIN_ELAPSED` (one microsecond) and `Flixel.MAX_ELAPSED` (0.1 seconds). If
  the game freezes for two seconds (a slow load, a breakpoint), the next frame acts as if only 0.1 seconds passed.
  Things slow down briefly instead of teleporting through walls.
- `elapsed` already includes `Flixel.timeScale` (see below). `Flixel.getRawElapsed()` returns the clamped value
  *before* scaling, which is useful for things that must ignore slow motion, such as a menu animation.

## The exact order of one frame

Here is everything that happens in one frame, in order. You do not need to memorize it, but it answers questions
like "why is `justPressed` still true here?" and "does my camera move before or after my sprite?"

**Before the frame.** The platform measures how long the last frame took and calls `advanceTime(...)`, which clamps
it and applies `Flixel.timeScale`. Then every manager runs `beginFrame()`, even while the game is paused.

**Update.**

1. `Flixel.Signals.preUpdate` fires.
2. Every manager in `Flixel.managers` runs `preUpdate(...)`. In debug mode, the debugger applies the input it
   injected here.
3. Every manager runs `update(...)`, from the lowest order to the highest:
   1. Input managers read the hardware: `Flixel.keys`, `Flixel.mouse`, `Flixel.touches`, and `Flixel.gamepads`.
   2. Input actions update, so they see this frame's hardware state.
   3. In debug mode, the debugger takes the input it owns (the free camera and picking).
   4. Global tweens and global timers advance.
   5. The sound manager updates (fades, finished sounds).
   6. Your own managers update, with the default order.
4. If the game is not paused, the state chain updates: the active state, then each open substate. A state with an
   open substate only updates if its `persistentUpdate` flag is `true`.
5. Every manager runs `postUpdate(...)`, from the lowest order to the highest:
   1. Every camera updates (follow, shake, flash, fade), in `Flixel.cameraManager`.
   2. The global overlay updates, if it is on, in `Flixel.overlay`.
   3. In debug mode, the debugger samples the frame.
   4. Your own managers run their `postUpdate(...)`.
6. `Flixel.Signals.postUpdate` fires.

While the game is paused, managers whose `isPausable()` returns `true` (the default) skip steps 2, 3, and 5. Input
and the debugger are not pausable, so they keep running; tweens, timers, sound, the cameras, and the overlay are, so
they stop with the state.

**Draw.**

1. `Flixel.Signals.preDraw` fires.
2. Every manager runs `preDraw(...)`.
3. Every manager runs `draw(...)`. The renderer, `Flixel.render`, comes first and draws the whole picture:
   1. The screen is cleared to `Flixel.render.bgColor`.
   2. For each camera in `Flixel.cameras`, in order:
      1. The camera fills its background color.
      2. The state chain draws: the active state, then each open substate. A state with an open substate only draws
         if its `persistentDraw` flag is `true` (the default).
      3. Every manager runs `drawCamera(...)` for this camera.
      4. The camera draws its effects on top (flash and fade).
      5. If the camera has a shader, its picture is run through the shader.
   3. Global shaders, if any, run over the combined picture.
   4. Every manager runs `drawOverlay(...)` on top of everything. The global overlay draws here.
   5. The picture is stretched to fill the window.

   Managers with a later order than the renderer draw straight on top of the finished picture.
4. Every manager runs `postDraw(...)`. In debug mode, the debugger draws hitboxes here if they are switched on.
5. `Flixel.Signals.postDraw` fires.

**After the frame.** `endFrame()` runs every manager's `endFrame()`, which is where the input managers and action
sets finish the input state: `justPressed` and `justReleased` are cleared so they are fresh next frame. Because this
happens *after* drawing, edge checks stay true for the whole frame, including any code you run in `draw(...)` or in
signal listeners.

## Always call `super`

When you override `create()`, `update(...)`, `draw(...)`, or `destroy()`, call the parent version. The parent class
does real work in each of them:

- `FlixelState.update(...)` updates every member you added.
- `FlixelSprite.update(...)` moves the sprite by its velocity and advances its animation.
- `FlixelState.draw(...)` draws every member.
- `destroy()` releases memory and GPU textures.

If you forget `super.update(elapsed)` in a state, nothing in it moves or animates. If you forget it in a sprite
subclass, that sprite ignores its velocity. Where you call it matters too: input-driven changes to velocity are
usually made *before* `super.update(elapsed)` so they take effect this frame, while collision checks are usually made
*after* it, once everything has moved.

```java
@Override
public void update(float elapsed) {
  // 1. Decide what the player wants to do this frame.
  player.setAccelerationX(Flixel.keys.pressed(FlixelKey.RIGHT) ? 800f : 0f);

  // 2. Move everything.
  super.update(elapsed);

  // 3. Fix up the results.
  Flixel.collide(player, walls);
}
```

## Slow motion with `timeScale`

`Flixel.timeScale` multiplies `elapsed` before anything sees it. Set it to `0.5f` and the whole game (movement,
animations, tweens, timers) runs at half speed. Set it to `2f` for fast-forward.

```java
// A dramatic slow-motion moment when the boss is defeated.
Flixel.timeScale = 0.25f;
FlixelTimer.wait(0.5f, timer -> Flixel.timeScale = 1f); // 0.5 game seconds, which is 2 real seconds.
```

Remember that timers see scaled time too. In the example above the timer waits 0.5 *scaled* seconds, which is two
real seconds at a quarter speed. Use `Flixel.getRawElapsed()` for anything that should keep real time.

## Pausing: auto-pause, substates, and the debugger

There are three different kinds of "paused," and they serve different purposes.

| Kind                     | How                                                  | What stops                                       |
|--------------------------|------------------------------------------------------|--------------------------------------------------|
| Auto-pause               | Happens when the window loses focus, if `Flixel.autoPause` is `true` (the default). | Updates and audio. Rendering continues at a low frame rate. |
| A pause menu             | `openSubState(new PauseSubState())`                  | The parent state's updates (unless `persistentUpdate`). Global tweens and timers keep running. |
| The debugger's pause     | F3 in debug mode, or `Flixel.debug.controls.setPaused(true)` | All game updates, tweens, timers, and audio. Input and the debugger keep running. |

Set `Flixel.autoPause = false` to keep the game running in the background, for example in an idle game. On mobile
that also means audio keeps playing when the app is not in front.

A pause menu built with a substate is covered in [States and Substates](states-and-substates.md).

## Focus, minimize, suspend, and close

The platform tells the game when the world around it changes. Each event reaches your active state through a method
you can override, and through a global signal.

| Event                         | State method       | Signal                              | When it happens                                         |
|-------------------------------|--------------------|-------------------------------------|---------------------------------------------------------|
| The window lost focus         | `onFocusLost()`    | `Flixel.Signals.windowUnfocused`    | Alt-tab, a click elsewhere, the app goes to the background. |
| The window regained focus     | `onFocusGained()`  | `Flixel.Signals.windowFocused`      | The player comes back.                                  |
| The window was minimized      | `onMinimized()`    | `Flixel.Signals.windowMinimized`    | Desktop only. Focus loss arrives first.                 |
| The window was resized        | `resize(w, h)`     | (none)                              | Mostly desktop.                                         |
| The game may be stopped soon  | (none)             | `Flixel.Signals.gameSuspended`      | Android pause, a hidden browser tab, page unload, and every clean close. |
| The game is closing           | (none)             | `Flixel.Signals.preGameClose` and `postGameClose` | A clean shutdown, usually desktop.         |

The most important one is `gameSuspended`. Browsers and phones rarely shut a game down cleanly: a closed tab or an
app swiped away is simply stopped, so `preGameClose` may never run there. `gameSuspended` fires at the last moment
each platform reliably gives you, which makes it the right place to save:

```java
Flixel.Signals.gameSuspended.add(data -> Flixel.save.flush());
```

A common pattern is opening the pause menu when the player tabs out:

```java
@Override
public void onFocusLost() {
  super.onFocusLost();
  if (getSubState() == null) {
    openSubState(new PauseSubState());
  }
}
```

## Global hooks with managers

When a whole system has to follow the game from start to finish (a library, an achievement tracker, a networking
layer), make it a **manager**. A manager is like a passenger on a train: once it boards, it rides through every stop
of the loop without the game having to know about it. Implement `FlixelManagers.Manager`, override only the hooks you
need (every hook has an empty default), and register it in `Flixel.managers`:

```java
public class AchievementManager implements FlixelManagers.Manager {

  @Override
  public void update(float elapsed) {
    // Runs every frame, right before the state updates.
  }

  @Override
  public void onSuspended() {
    // The platform may stop the game soon, so save progress now.
  }
}

Flixel.boot.afterStart(() -> Flixel.managers.add(new AchievementManager()));
```

Managers run from the lowest `getOrder()` to the highest, and `destroy()` runs in the reverse order. The framework's own
systems are managers too, at the orders named by the `ORDER_*` constants of `FlixelManagers.Manager`: assets, fonts,
the renderer, the cameras, the overlay, keys, mouse, touches, gamepads, input actions, the debugger, tweens, timers,
and sound, in that order. They are spaced
apart so yours can slip in between, for example `ORDER_ACTIONS + 50` to read this frame's input before the debugger
takes the input it owns. The default order (`ORDER_DEFAULT`) runs after all of them. A manager added
to a running game is created right away, and one removed from a running game is destroyed right away. Return `false`
from `isPausable()` for work that must keep running while the game is paused. Return a constant from `getName()`
(such as `"mylibrary.network"`) to name your manager's section in platform profilers.

Prefer a manager over subclassing `FlixelGame` or listening to several signals at once: it gets every hook in one
place, in a predictable order.

## Global hooks with signals

Sometimes code needs to run every frame no matter which state is active, such as a music system or an achievement
tracker. Instead of copying it into every state, listen to `Flixel.Signals.preUpdate` or `postUpdate`:

```java
private final FlixelSignal.SignalHandler<UpdateSignalData> tick = data -> achievements.tick(data.elapsed());

// Once, for example in your first state:
Flixel.Signals.postUpdate.add(tick);
```

The `UpdateSignalData` object is reused every frame to avoid garbage, so read `elapsed()` inside the callback and do
not keep the object. Listeners on global signals stay registered across state switches until you remove them. See
[Signals](signals.md).

## Things that live across states

The state is replaced on every switch, but the game object and the managers are not. Put behavior that applies for
the whole run of the program in a manager (see [Global hooks with managers](#global-hooks-with-managers)). The
framework also has a **global overlay**, `Flixel.overlay`: a group drawn on top of every camera with its own camera
that never scrolls. Anything you add to it stays on screen across state switches, which suits an FPS counter or a
"now playing" banner:

```java
FlixelText fps = new FlixelText(4f, 4f, 0f, "", 8);
Flixel.overlay.add(fps);       // Add to the overlay, not the state.
Flixel.overlay.setEnabled(true);
```

The overlay is separate from the debugger and is drawn after global shaders, so shaders never affect it.

## Quitting the game

Call `Flixel.quit()` to close the game. It runs the clean shutdown (`preGameClose`, the game's `destroy()`,
`postGameClose`) and closes the window. Calling `Flixel.game.destroy()` yourself only releases resources without
ending the program, so prefer `Flixel.quit()`.

On the web and on mobile, "quitting" is up to the browser or the operating system. Hide the quit button there, or
send the player back to the title screen instead.

## Check your understanding

1. You set velocity in `update(...)` *after* `super.update(elapsed)`. When does the sprite start moving?
2. Why might a timer you set to 1 second take 2 real seconds?
3. Where should you save the player's progress so it survives a browser tab being closed?

(Answers: on the next frame, because this frame's movement already happened inside `super.update(elapsed)`;
`Flixel.timeScale` is `0.5f`, and timers see scaled time; in a `Flixel.Signals.gameSuspended` listener, or right
after the progress changes.)

**Next:** [States and Substates](states-and-substates.md)
