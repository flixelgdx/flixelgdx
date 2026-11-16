# Signals: Events Without Tangled Code

A signal is a list of listeners that can all be notified at once. Signals let one part of your game announce that
something happened ("the player was hurt," "the level was cleared") without knowing or caring who reacts. This guide
covers creating and dispatching signals, one-time listeners, removing listeners, the framework's global signals, and
how to avoid leaks and garbage.

## Table of contents

1. [Why signals?](#why-signals)
2. [Creating and dispatching a signal](#creating-and-dispatching-a-signal)
3. [Signals with no data](#signals-with-no-data)
4. [One-time listeners](#one-time-listeners)
5. [Removing listeners](#removing-listeners)
6. [The order listeners run in](#the-order-listeners-run-in)
7. [Changing listeners during a dispatch](#changing-listeners-during-a-dispatch)
8. [The framework's global signals](#the-frameworks-global-signals)
9. [Other signals around the framework](#other-signals-around-the-framework)
10. [Avoiding leaks](#avoiding-leaks)
11. [Avoiding garbage](#avoiding-garbage)
12. [Pattern: a game-wide event hub](#pattern-a-game-wide-event-hub)
13. [Check your understanding](#check-your-understanding)

## Why signals?

Imagine the player gets hurt. The health bar should shrink, a sound should play, the screen should flash, and the
achievement tracker should note it. Without signals, the player class has to know about all four systems:

```java
public void hurt(int damage) {
  health -= damage;
  hud.updateHealth(health);          // The player now depends on the HUD...
  Flixel.sound.play(hurtSound);      // ...and on audio...
  camera.flash(FlixelColor.RED);     // ...and on the camera...
  achievements.onPlayerHurt();       // ...and on achievements.
}
```

Every new reaction means editing the player. Signals turn this around. Think of a school bell: when it rings, every
class that cares hears it, and the bell does not need a list of classrooms. The player just rings its "hurt" bell:

```java
public final FlixelSignal<Integer> onHurt = new FlixelSignal<>();

public void hurt(int damage) {
  health -= damage;
  onHurt.dispatch(health);
}
```

And each system subscribes on its own:

```java
player.onHurt.add(health -> healthBar.setValue(health));
player.onHurt.add(health -> camera.flash(FlixelColor.RED, 0.1f));
```

Now the player knows nothing about the HUD, audio, or achievements. You can add or remove reactions without touching
it.

## Creating and dispatching a signal

`FlixelSignal<T>` is generic: `T` is the type of data sent to listeners.

```java
FlixelSignal<Integer> onScoreChanged = new FlixelSignal<>();

onScoreChanged.add(score -> scoreText.setText("Score: " + score)); // Subscribe.

onScoreChanged.dispatch(100); // Every listener runs with 100.
```

A listener is a `FlixelSignal.SignalHandler<T>`, a single-method interface, so a lambda or a method reference works:

```java
onScoreChanged.add(this::refreshScore);

private void refreshScore(Integer score) {
  // ...
}
```

## Signals with no data

Use `Void` as the type when there is nothing to send, and call the no-argument `dispatch()`. The listener still
receives one parameter (always `null`); name it `_` to show it is unused:

```java
FlixelSignal<Void> onLevelCleared = new FlixelSignal<>();
onLevelCleared.add(_ -> Flixel.switchState(NextLevelState::new));
onLevelCleared.dispatch();
```

## One-time listeners

`addOnce(...)` registers a listener that removes itself after its first run. It is ideal for "the first time this
happens" moments, such as a tutorial hint:

```java
player.onHurt.addOnce(health -> showHint("Tip: health potions restore 25 HP!"));
```

## Removing listeners

Remove a listener by passing the **same object** you added. Lookup is by identity, not by equality, so two identical
lambdas written in two places are different listeners. Keep a reference if you plan to remove it:

```java
private final FlixelSignal.SignalHandler<Integer> onHurtHandler = health -> healthBar.setValue(health);

// Subscribe:
player.onHurt.add(onHurtHandler);

// Later:
player.onHurt.remove(onHurtHandler);
```

`clear()` removes every listener at once.

## The order listeners run in

When a signal is dispatched, permanent listeners (added with `add`) run first, in the order they were added. Then
one-time listeners (added with `addOnce`) run, and are removed. Do not write code that depends on one listener running
before another; if the order matters, it is usually one piece of logic that belongs in one listener.

## Changing listeners during a dispatch

Listeners may add or remove listeners (including themselves) while a signal is being dispatched. The current dispatch
keeps going over the list as it was when it started, and the changes apply from the next dispatch. You never need to
defer changes or worry about the list changing under you.

## The framework's global signals

`Flixel.Signals` holds the events the framework fires at important moments:

| Signal                                | Fires                                                       | Data |
|---------------------------------------|-------------------------------------------------------------|------|
| `preUpdate` / `postUpdate`            | Before and after every update pass.                         | `UpdateSignalData`: read `elapsed()` |
| `preDraw` / `postDraw`                | Before and after every draw pass.                           | none |
| `preStateSwitch` / `postStateSwitch`  | Around every state switch.                                  | `StateSwitchSignalData`: read `state()` |
| `windowFocused` / `windowUnfocused`   | The window gains or loses focus.                            | none |
| `windowMinimized`                     | The desktop window is minimized.                            | none |
| `gameSuspended`                       | The platform may stop the game soon. Save here.             | none |
| `preGameClose` / `postGameClose`      | Around a clean shutdown.                                    | none |

```java
Flixel.Signals.postStateSwitch.add(data -> Flixel.info("Entered {}", data.state()));
Flixel.Signals.gameSuspended.add(_ -> Flixel.save.flush());
```

For `preStateSwitch`, `state()` is the state being left; for `postStateSwitch`, it is the state just entered.

## Other signals around the framework

Signals show up wherever something can "happen":

| Signal                                  | Fires when                                         |
|-----------------------------------------|----------------------------------------------------|
| `sound.onComplete`                      | A sound finishes. See [Audio](audio.md).           |
| `sprite.animation.onFrameChanged`       | An animation shows a new frame. See [Animation](animation.md). |
| `sprite.animation.onAnimationFinished`  | A non-looping clip ends.                           |
| `Flixel.gamepads.deviceConnected`       | A controller is plugged in. See [Input](input.md). |
| `Flixel.gamepads.deviceDisconnected`    | A controller is unplugged.                         |
| `Flixel.host.onTextPasted()`            | Text arrives from the clipboard. See [Window and Platform Services](window-and-platform-services.md). |

## Avoiding leaks

A listener keeps alive everything it refers to. If a level state adds a listener to a global signal and never removes
it, the global signal keeps that listener alive, and the listener keeps the whole destroyed state alive, forever. Worse,
the listener keeps running, now acting on objects that no longer exist.

Listeners added to **global** signals (`Flixel.Signals`, `Flixel.gamepads`, or a long-lived object of your own) stay
registered across state switches. Remove them when their owner goes away:

```java
private final FlixelSignal.SignalHandler<Void> onSuspend = _ -> saveProgress();

@Override
public void create() {
  super.create();
  Flixel.Signals.gameSuspended.add(onSuspend);
}

@Override
public void destroy() {
  Flixel.Signals.gameSuspended.remove(onSuspend);
  super.destroy();
}
```

Listeners on objects that belong to the state (the player's `onHurt`, a sprite's animation signals) die with the state,
so they need no cleanup.

## Avoiding garbage

Two habits keep signals garbage-free in code that runs often:

- **Register once.** Add listeners in `create()` or a constructor, never in `update(...)`.
- **Reuse the data object.** `dispatch(new HitEvent(...))` every frame creates garbage. For frequent signals, keep one
  mutable data object and fill it before each dispatch. The framework does this for `preUpdate` and `postUpdate`,
  which is why you must read `elapsed()` inside the callback and never keep the `UpdateSignalData` object:

```java
// Right: read the value during the callback.
Flixel.Signals.postUpdate.add(data -> mySystem.tick(data.elapsed()));
```

Boxed numbers count too: `FlixelSignal<Integer>` boxes the number on every dispatch outside the small cached range.
For a signal that fires every frame with a number, prefer a reused data object holding a primitive field.

## Pattern: a game-wide event hub

For events that many unrelated systems care about, a small class of static signals acts like a public notice board:

```java
public final class GameEvents {

  public static final FlixelSignal<Void> levelCleared = new FlixelSignal<>();
  public static final FlixelSignal<String> achievementUnlocked = new FlixelSignal<>();

  private GameEvents() {}
}

// Anywhere:
GameEvents.achievementUnlocked.dispatch("first_blood");

// The achievement popup, registered once:
GameEvents.achievementUnlocked.add(id -> popup.show(id));
```

Because these signals live forever, the same leak rules as the global signals apply: remove state-owned listeners in
`destroy()`.

## Check your understanding

1. You wrote `player.onHurt.remove(health -> healthBar.setValue(health))`, but the listener keeps running. Why?
2. A destroyed level state keeps reacting to `Flixel.Signals.windowUnfocused`. What did you forget?
3. Why is storing the `UpdateSignalData` object from `postUpdate` in a field a bug?

(Answers: removal is by identity, and that lambda is a new object, not the one you added; removing the listener in the
state's `destroy()`; the framework reuses that one object every frame, so its values change after the callback
returns.)

**Next:** [Particles](particles.md)
