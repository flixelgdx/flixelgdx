# States and Substates: Organizing Your Game into Screens

A state is one screen of your game: the title screen, a menu, a level, the game over screen. A substate is a smaller
screen that opens on top of a state without replacing it: a pause menu, a dialog box, an inventory. This guide shows
how to build, switch, restart, and layer them, and how to pass data between them.

## Table of contents

1. [Why split a game into states?](#why-split-a-game-into-states)
2. [Writing a state](#writing-a-state)
3. [Switching states](#switching-states)
4. [What happens during a switch](#what-happens-during-a-switch)
5. [Restarting the current state](#restarting-the-current-state)
6. [Passing data between states](#passing-data-between-states)
7. [A state is a group](#a-state-is-a-group)
8. [Background color](#background-color)
9. [Substates: pause menus and pop-ups](#substates-pause-menus-and-pop-ups)
10. [Controlling what keeps running behind a substate](#controlling-what-keeps-running-behind-a-substate)
11. [Stacking substates](#stacking-substates)
12. [Smooth transitions](#smooth-transitions)
13. [Reacting to focus and resize](#reacting-to-focus-and-resize)
14. [Common mistakes](#common-mistakes)
15. [Check your understanding](#check-your-understanding)

## Why split a game into states?

Picture a theater again. Each play has its own set, its own actors, and its own script. When one play ends, the crew
clears the stage completely before the next one starts. Nobody has to remember to take down every prop from the last
play, because clearing the stage is part of the routine.

States give your game the same routine. Everything you `add(...)` to a state belongs to it and is destroyed when you
leave. Tweens and timers are canceled too. So a state is the natural unit of cleanup: put things that only matter on
one screen in that screen's state, and the framework tidies up for you.

States also keep code readable. `MenuState` only knows about menus, `PlayState` only knows about playing. Neither has
to check "am I on the menu right now?" before doing something.

## Writing a state

Subclass `FlixelState`, build the scene in `create()`, and run logic in `update(...)`.

```java
import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelState;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.text.FlixelText;
import org.flixelgdx.util.FlixelAxes;

public class TitleState extends FlixelState {

  @Override
  public void create() {
    super.create();

    FlixelText title = new FlixelText(0f, 80f, 0f, "My Awesome Game", 32);
    title.screenCenter(FlixelAxes.X);
    add(title);

    FlixelText prompt = new FlixelText(0f, 240f, 0f, "Press ENTER to start", 16);
    prompt.screenCenter(FlixelAxes.X);
    add(prompt);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (Flixel.keys.justPressed(FlixelKey.ENTER)) {
      Flixel.switchState(PlayState::new);
    }
  }
}
```

## Switching states

Move to another state with `Flixel.switchState(...)`, passing a recipe that builds it:

```java
Flixel.switchState(PlayState::new);

// A state that needs data can use a lambda.
Flixel.switchState(() -> new GameOverState(score));
```

Why a recipe (`Supplier<FlixelState>`) instead of `new PlayState()`? Because the framework remembers the recipe. That
is what lets `Flixel.resetState()` build a brand new copy of the current state later, with the same arguments.

`switchState(...)` has longer forms for special cases:

```java
// Keep active tweens running across the switch (for example a music fade).
Flixel.switchState(NextState::new, false);

// Keep tweens and timers.
Flixel.switchState(NextState::new, false, false);

// Keep tweens and timers, and skip the garbage collection the switch normally triggers.
Flixel.switchState(NextState::new, false, false, false);
```

## What happens during a switch

When you call `Flixel.switchState(recipe)`, the framework does this, in order:

1. Calls the recipe to build the new state (its constructor runs now).
2. Fires `Flixel.Signals.preStateSwitch`.
3. Destroys the old state, which destroys every member it holds.
4. Stops and clears sounds that are not marked persistent (see [Audio](audio.md)).
5. Cancels active tweens and timers (unless you asked it not to).
6. Resets the cameras to a single default camera.
7. Calls the new state's `create()`.
8. Asks Java to collect garbage (unless you asked it not to), since the old state just became garbage.
9. Fires `Flixel.Signals.postStateSwitch`.

Two consequences are easy to miss:

- **Cameras are reset on every switch.** If a state needs a HUD camera or a zoom level, set it up in that state's
  `create()`. See [Cameras](cameras.md).
- **The switch happens immediately.** Code after `Flixel.switchState(...)` in the same method still runs, but the old
  state is already destroyed. Return right after switching:

```java
if (player.getHealth() <= 0) {
  Flixel.switchState(() -> new GameOverState(score));
  return; // Do not touch this state's objects after switching.
}
```

## Restarting the current state

`Flixel.resetState()` calls the remembered recipe again and switches to the fresh copy. It is the easiest way to
restart a level after the player dies:

```java
if (Flixel.keys.justPressed(FlixelKey.R)) {
  Flixel.resetState();
}
```

## Passing data between states

Since the old state is destroyed, data that must survive a switch has to live somewhere else. Pick the smallest home
that works:

| Where                         | Lives until...                  | Good for                                          |
|-------------------------------|---------------------------------|---------------------------------------------------|
| A constructor argument        | The next state is destroyed.    | The score for the game over screen, a level number. |
| A field on your game class    | The game closes.                | Settings, the current run's inventory.            |
| `Flixel.save`                 | Forever, across restarts.       | High scores, unlocked levels, options.            |

```java
public class GameOverState extends FlixelState {

  private final int score;

  public GameOverState(int score) {
    this.score = score; // Simple data in the constructor is fine.
  }

  @Override
  public void create() {
    super.create();
    add(new FlixelText(0f, 100f, 0f, "Score: " + score, 24)); // Once per create(), so this string is fine.
  }
}
```

A game class field can be reached through `Flixel.game`:

```java
public class MyGame extends FlixelGame {
  public int coins; // Survives state switches.
  // ...
}

((MyGame) Flixel.game).coins += 10;
```

See [Saving Progress](saving.md) for data that must survive a restart.

## A state is a group

`FlixelState` is a `FlixelBasicGroup`, which is why `add(...)` works in it. Everything you learned about groups
applies: members are updated and drawn in the order they were added, `remove(...)` takes a member out without
destroying it, and you can give the state its own recycling recipe by overriding `createMemberForRecycle()`. See
[Groups and Recycling](groups-and-recycling.md).

Draw order follows add order: the first thing added is at the back, the last is in front. Add the background first and
the HUD last.

## Background color

Set the color behind everything with `setBgColor(...)` in `create()`:

```java
setBgColor(new FlixelColor(0.1f, 0.1f, 0.2f, 1f)); // A dark blue night sky.
```

## Substates: pause menus and pop-ups

A `FlixelSubState` opens *on top of* the current state without replacing it. Think of a sheet of glass held in front
of the stage: the play behind it is still there and still visible, but it is frozen until you take the glass away.

```java
import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelSubState;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.text.FlixelText;
import org.flixelgdx.util.FlixelColor;

public class PauseSubState extends FlixelSubState {

  public PauseSubState() {
    super(new FlixelColor(0f, 0f, 0f, 0.5f)); // Dim the game behind it by 50%.
  }

  @Override
  public void create() {
    super.create();
    FlixelText label = new FlixelText(0f, 0f, 0f, "PAUSED", 32);
    label.screenCenter();
    add(label);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
      close(); // Go back to the game.
    }
  }
}
```

Open it from the parent state:

```java
// In PlayState.update(...):
if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
  openSubState(new PauseSubState());
}
```

Note that the same key (Escape) opens and closes the menu. This works because the parent stops updating while the
substate is open, so only one of them sees the key press.

Useful substate hooks:

```java
PauseSubState pause = new PauseSubState();
pause.openCallback = () -> Flixel.sound.pause();   // Runs after the substate's create().
pause.closeCallback = () -> Flixel.sound.resume(); // Runs when it closes.
openSubState(pause);
```

From the parent's side, `getSubState()` returns the open substate (or `null`), and `closeSubState()` closes it.

By default a closed substate is destroyed. If you open the same heavy substate often, you can keep it alive by setting
`destroySubStates = false` on the parent. Be aware that `create()` runs again each time the substate opens, so a
reused substate should build its members only once (for example behind a `boolean built` field).

## Controlling what keeps running behind a substate

Two flags on the **parent** state decide what it does while a substate is open:

| Flag               | Default | Meaning                                                         |
|--------------------|---------|-----------------------------------------------------------------|
| `persistentDraw`   | `true`  | The parent keeps drawing, so it stays visible behind the substate. |
| `persistentUpdate` | `false` | The parent stops updating, so the game behind is frozen.        |

```java
// An inventory that opens while the world keeps moving.
persistentUpdate = true;
openSubState(new InventorySubState());

// A full-screen menu that hides the level entirely.
persistentDraw = false;
openSubState(new MapSubState());
```

Global tweens and timers are not tied to a state, so they keep running while a pause menu is open. If something must
freeze with the level, give the state its own `FlixelTimerManager` (see [Tweens and Timers](tweens-and-timers.md)).

## Stacking substates

A substate is itself a state, so it can open its own substate. This is perfect for "Are you sure you want to quit?"
on top of a pause menu:

```java
// Inside PauseSubState.update(...):
if (Flixel.keys.justPressed(FlixelKey.Q)) {
  openSubState(new ConfirmQuitSubState());
}
```

Each state holds at most one substate directly. Opening a new substate on a state that already has one closes the old
one first.

## Smooth transitions

A camera fade with a callback is the classic way to move between states without a hard cut:

```java
Flixel.cameras.first().fade(FlixelColor.BLACK, 0.5f, false, () -> Flixel.switchState(NextState::new), false);
```

And in the next state's `create()`, fade back in:

```java
Flixel.cameras.first().fade(FlixelColor.BLACK, 0.5f, true); // true = fade in from black.
```

See [Cameras](cameras.md) for the other camera effects.

## Reacting to focus and resize

States can react to the window by overriding these methods. The default versions pass the event on to any open
substate, so keep the `super` call.

```java
@Override
public void onFocusLost() {
  super.onFocusLost();
  if (getSubState() == null) {
    openSubState(new PauseSubState()); // Auto-open the pause menu when the player tabs out.
  }
}

@Override
public void onFocusGained() {
  super.onFocusGained();
}

@Override
public void resize(int width, int height) {
  super.resize(width, height); // Called when the window changes size, mostly on desktop.
}
```

## Common mistakes

- **Building the scene in the constructor.** Use `create()`. The constructor runs before the old state is destroyed and
  before the cameras are reset.
- **Touching objects after switching.** The old state is destroyed during `switchState(...)`. Return right away.
- **Expecting a HUD camera to survive a switch.** Cameras reset on every switch. Set them up in each state's `create()`.
- **Leaving global listeners behind.** If a state adds a listener to `Flixel.Signals`, remove it in that state's
  `destroy()`, or the listener (and the state it captured) will live forever.

## Check your understanding

1. A player opens the pause menu, and the enemies keep moving. Which flag is set, and on which state?
2. Why is `Flixel.switchState(PlayState::new)` better than a hypothetical `Flixel.switchState(new PlayState())`?
3. You want a "Game Paused" overlay that does not dim anything. What do you pass to the `FlixelSubState` constructor?

(Answers: `persistentUpdate = true` on the parent state; the framework remembers the recipe, so `resetState()` can
build a fresh copy and the state is created at the right moment; nothing, use the no-argument constructor, or pass a
fully transparent color.)

**Next:** [Basics, Objects, and Sprites](sprites-and-objects.md)
