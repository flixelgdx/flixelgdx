# Input Actions: Rebindable Controls for Every Device

Input actions let you describe *what the player wants to do* ("jump," "move," "pause") separately from *which buttons
do it*. You define each action once, bind it to any number of keys, mouse buttons, gamepad buttons, sticks, and touch
areas, and then your game only asks about the action. This guide covers digital and analog actions, every kind of
binding, menu-friendly repeat timing, and rebinding screens.

## Table of contents

1. [Why actions?](#why-actions)
2. [The pieces](#the-pieces)
3. [A complete control scheme](#a-complete-control-scheme)
4. [Using the actions in a state](#using-the-actions-in-a-state)
5. [Digital actions](#digital-actions)
6. [Analog actions and the Y direction](#analog-actions-and-the-y-direction)
7. [Every kind of binding](#every-kind-of-binding)
8. [Hold-repeat for menus](#hold-repeat-for-menus)
9. [Callbacks](#callbacks)
10. [Building a rebinding screen](#building-a-rebinding-screen)
11. [When actions update](#when-actions-update)
12. [Check your understanding](#check-your-understanding)

## Why actions?

Writing `Flixel.keys.pressed(FlixelKey.SPACE)` everywhere works for a prototype. Then you add gamepad support, and
every one of those lines grows an `||`. Then players ask for rebindable keys, and you have to hunt down every
hardcoded key. Then you want touch controls on Android.

Actions flip the relationship. Think of a TV remote: the "volume up" button means the same thing no matter which TV
it controls, and you can program a universal remote to send "volume up" from a different button. Your gameplay code is
the TV: it only listens for "volume up" (`jump.justPressed()`). Which physical button sends it is configured in one
place, and can change at any time without touching gameplay code.

## The pieces

| Class                   | Role                                                                                       |
|-------------------------|--------------------------------------------------------------------------------------------|
| `FlixelActionDigital`   | An on/off action, such as "jump." True when any of its bindings is active.                 |
| `FlixelActionAnalog`    | A 2D direction, such as "move." Adds up its bindings and caps the length at 1.             |
| `FlixelDigitalBinding`  | Connects a key, mouse button, gamepad button, or touch area to a digital action.           |
| `FlixelAnalogBinding`   | Connects half of a key pair or a gamepad axis to one axis of an analog action.             |
| `FlixelActionSet`       | A group of actions, usually one per player or per context (gameplay, menus).               |
| `FlixelActionSets`      | The global registry. The game loop updates every registered set for you.                   |

## A complete control scheme

Subclass `FlixelActionSet`, create actions in the constructor, add bindings, and `add(...)` each action. The set
registers itself with the global registry as soon as it is created.

```java
import org.flixelgdx.input.action.FlixelActionAnalog;
import org.flixelgdx.input.action.FlixelActionDigital;
import org.flixelgdx.input.action.FlixelActionSet;
import org.flixelgdx.input.action.FlixelAnalogBinding;
import org.flixelgdx.input.action.FlixelDigitalBinding;
import org.flixelgdx.input.gamepad.FlixelGamepadAxis;
import org.flixelgdx.input.gamepad.FlixelGamepadButton;
import org.flixelgdx.input.keyboard.FlixelKey;

public class PlayerControls extends FlixelActionSet {

  public final FlixelActionDigital jump;
  public final FlixelActionDigital pause;
  public final FlixelActionAnalog move;

  public PlayerControls() {
    jump = new FlixelActionDigital("jump");
    jump.addBinding("kb", FlixelDigitalBinding.key(FlixelKey.SPACE));
    jump.addBinding("pad", FlixelDigitalBinding.gamepadButton(0, FlixelGamepadButton.A));
    jump.addBinding("touch", FlixelDigitalBinding.touchRegion(0.5f, 0.5f, 0.5f, 0.5f)); // Bottom-right quarter.
    add(jump);

    pause = new FlixelActionDigital("pause");
    pause.addBinding("kb", FlixelDigitalBinding.key(FlixelKey.ESCAPE));
    pause.addBinding("pad", FlixelDigitalBinding.gamepadButton(0, FlixelGamepadButton.START));
    add(pause);

    move = new FlixelActionAnalog("move");
    move.addBinding("left", FlixelAnalogBinding.negXKey(FlixelKey.A));
    move.addBinding("right", FlixelAnalogBinding.posXKey(FlixelKey.D));
    move.addBinding("up", FlixelAnalogBinding.negYKey(FlixelKey.W));
    move.addBinding("down", FlixelAnalogBinding.posYKey(FlixelKey.S));
    move.addBinding("stickX", FlixelAnalogBinding.gamepadAxisX(0, FlixelGamepadAxis.LEFT_X));
    move.addBinding("stickY", FlixelAnalogBinding.gamepadAxisY(0, FlixelGamepadAxis.LEFT_Y));
    add(move);
  }
}
```

The first argument to `addBinding(...)` is a **slot name** of your choosing ("kb", "pad", "up"). Adding a binding with
a slot name that already exists replaces it, which is exactly what a rebinding screen needs.

## Using the actions in a state

```java
private PlayerControls controls;

@Override
public void create() {
  super.create();
  controls = new PlayerControls();
}

@Override
public void update(float elapsed) {
  super.update(elapsed);

  if (controls.jump.justPressed() && player.isTouching(FlixelDirectionFlags.FLOOR)) {
    player.setVelocityY(-350f);
  }
  player.setVelocityX(controls.move.getX() * 200f);

  if (controls.pause.justPressed()) {
    openSubState(new PauseSubState());
  }
}

@Override
public void destroy() {
  controls.destroy(); // Unregister from the global registry.
  super.destroy();
}
```

Always `destroy()` an action set you no longer need. Otherwise it stays registered and keeps updating after its state
is gone. If one set should last for the whole game (for example a shared menu set), create it once and keep it on your
game class instead.

## Digital actions

A digital action answers the same questions as a key:

| Method            | True when...                                                       |
|-------------------|--------------------------------------------------------------------|
| `pressed()`       | Any binding is active this frame.                                  |
| `justPressed()`   | The action became active this frame.                               |
| `justReleased()`  | The action stopped being active this frame.                        |
| `held()`          | Autorepeat: true on the first press, then again every repeat interval while held. |

## Analog actions and the Y direction

An analog action adds up all of its bindings into one 2D vector, then caps its length at 1. So pressing W and D
together gives roughly `(0.71, 0.71)`, not `(1, 1)`, and diagonals are never faster than straight lines. A stick and
keys can be mixed freely.

| Method                     | Returns                                                       |
|----------------------------|---------------------------------------------------------------|
| `getX()` / `getY()`        | This frame's vector, each from -1 to 1.                       |
| `getPrevX()` / `getPrevY()`| Last frame's vector.                                          |
| `moved()`                  | Whether the vector is not zero.                               |
| `flicked()`                | True for one frame when the stick first crosses the flick threshold. |
| `flickedRepeating()`       | Like `flicked()`, plus hold-repeat while it stays pushed.     |

**Y points down, just like the world.** Actions use the same y-down coordinate system as every position and
velocity in the framework: negative Y is up and positive Y is down. So you can feed an action straight into
movement with no sign flips:

```java
player.setVelocity(controls.move.getX() * 150f, controls.move.getY() * 150f);
```

Gamepad sticks already report down as positive on every platform, so their values pass through untouched.

## Every kind of binding

**Digital bindings** (`FlixelDigitalBinding`):

| Factory                                        | Active while...                                                        |
|------------------------------------------------|------------------------------------------------------------------------|
| `key(FlixelKey.X)`                             | The key is held.                                                       |
| `mouseButton(FlixelMouseButton.LEFT)`          | The mouse button is held.                                              |
| `gamepadButton(slot, FlixelGamepadButton.A)`   | The button is held on that controller.                                 |
| `touch(pointer)`                               | That finger is down.                                                   |
| `touchRegion(x, y, w, h)`                      | A finger is inside an area given as fractions of the screen (0 to 1). Ideal for on-screen buttons that adapt to any resolution. |
| `touchRegionWorld(x, y, w, h)`                 | A finger is inside an area in world coordinates.                       |

**Analog bindings** (`FlixelAnalogBinding`):

| Factory                                  | Adds                                                      |
|------------------------------------------|-----------------------------------------------------------|
| `negXKey(key)` / `posXKey(key)`          | -1 or +1 on X while the key is held.                      |
| `negYKey(key)` / `posYKey(key)`          | -1 or +1 on Y while the key is held (-1 is up).           |
| `gamepadAxisX(slot, axis)`               | The stick's X value.                                      |
| `gamepadAxisY(slot, axis)`               | The stick's Y value, as reported (down is positive).      |

Both binding types are tiny interfaces (`evaluate()` for digital, `accumulate(FlixelVector)` for analog), so you can
write your own, for example a binding driven by an on-screen joystick widget:

```java
jump.addBinding("ui", () -> jumpButton.isDown()); // Any boolean source works.
```

To remove bindings, use `removeBinding(slotName)` or `clearBindings()`.

## Hold-repeat for menus

In a menu, holding "down" should move the cursor once, wait a moment, then repeat quickly, like holding a key in a
text editor. `held()` (digital) and `flickedRepeating()` (analog) do exactly that. The timing is configurable per
action:

```java
menuDown.setHoldDelay(0.4f);     // Wait 0.4 seconds before the first repeat (default 0.5).
menuDown.setHoldInterval(0.08f); // Then repeat every 0.08 seconds (default 0.1).

// In update(...):
if (menuDown.held()) {
  selectNextItem();
}
if (menuMove.flickedRepeating()) {
  moveCursor(menuMove.getX(), menuMove.getY());
}
```

## Callbacks

Every action has an optional `callback` that runs on the frame the action is first pressed. Assign it once, not every
frame:

```java
controls.pause.callback = () -> openSubState(new PauseSubState());
```

Polling with `justPressed()` is usually clearer, because the logic stays in your `update(...)`. Callbacks shine for
global actions such as "take a screenshot" that should work in every state.

## Building a rebinding screen

Because bindings live in named slots, rebinding is "wait for a key, then replace the slot":

```java
private FlixelActionDigital waitingFor; // The action the player is rebinding, or null.

// When the player picks "Jump" in the controls menu:
waitingFor = controls.jump;
prompt.setText("Press a key for Jump...");

// In update(...):
if (waitingFor != null) {
  int key = Flixel.keys.firstJustPressed();
  if (key != FlixelKey.NONE) {
    waitingFor.addBinding("kb", FlixelDigitalBinding.key(key)); // Same slot name, so it replaces the old key.
    Flixel.save.data.put("bind." + waitingFor.getName(), FlixelKey.toString(key));
    Flixel.save.flush();
    waitingFor = null;
  }
}
```

When the game starts, read the saved names back with `FlixelKey.fromString(...)` and apply them before play begins.
Gamepads work the same way with `Flixel.gamepads.firstJustPressed(slot)`.

## When actions update

The game loop updates actions for you, in this order every frame:

1. The keyboard, mouse, touch, and gamepad managers read the hardware.
2. `FlixelActionSets.MANAGER`, which runs right after them in `Flixel.managers`, updates every registered action set.
3. Your state's `update(...)` runs and sees consistent action values.
4. After drawing, the managers and action sets finish the frame, clearing `justPressed` and `justReleased`.

So action values are always ready inside `update(...)`, and they never lag a frame behind the raw input.

## Check your understanding

1. You press W and the character moves down the screen. What is going on?
2. How do you give "jump" a second keyboard key without losing the first one?
3. Why should you call `destroy()` on an action set when its state ends?

(Answers: W is probably bound with `posYKey`, but up is negative Y, so bind it with `negYKey` instead; add it under a new
slot name, such as `jump.addBinding("kb2", FlixelDigitalBinding.key(FlixelKey.UP))`; the set registers itself
globally and would keep updating after the state is gone.)

**Next:** [Animation](animation.md)
