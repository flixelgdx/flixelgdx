# Input: Keyboard, Mouse, Touch, and Gamepads

This guide covers every way a player can talk to your game: the keyboard, the mouse, touch screens, and gamepads. It
explains polling, the three questions every device answers, screen versus world coordinates, typed text, and the
raw input layer underneath. For rebindable controls that work across devices, see [Input Actions](input-actions.md).

## Table of contents

1. [Polling: asking instead of waiting](#polling-asking-instead-of-waiting)
2. [The three questions every device answers](#the-three-questions-every-device-answers)
3. [Keyboard](#keyboard)
4. [Mouse](#mouse)
5. [Screen space versus world space](#screen-space-versus-world-space)
6. [Touch](#touch)
7. [Gamepads](#gamepads)
8. [Typed text and the raw input layer](#typed-text-and-the-raw-input-layer)
9. [Supporting several devices at once](#supporting-several-devices-at-once)
10. [Platform differences](#platform-differences)
11. [Common mistakes](#common-mistakes)
12. [Check your understanding](#check-your-understanding)

## Polling: asking instead of waiting

There are two ways to learn about input. You can wait for the doorbell to ring (events), or you can look out the
window every so often to see if someone is there (polling). Games usually poll: once per frame, inside
`update(...)`, you ask "is the jump key down right now?" and act on the answer.

Polling keeps all of your game logic in one place, in a predictable order, with no callbacks firing at surprising
times. The framework reads the hardware at the start of every frame, so every question you ask during that frame gets
a consistent answer. When you really do need events (a text box that needs every keystroke in order), the raw layer
described at the end of this guide provides them.

## The three questions every device answers

| Method               | True when...                                   | Use it for                                          |
|----------------------|------------------------------------------------|-----------------------------------------------------|
| `pressed(...)`       | The button is held down this frame.            | Continuous actions: walking, aiming, charging.      |
| `justPressed(...)`   | The button went down **this exact frame**.     | One-shot actions: jumping, shooting, confirming a menu choice. |
| `justReleased(...)`  | The button came up **this exact frame**.       | Releasing a charged attack, ending a drag.          |

The "just" versions are true for exactly one frame. If you used `pressed(...)` for jumping, the player would jump
again every frame they held the key.

## Keyboard

The keyboard lives on `Flixel.keys`. Key codes are constants in `FlixelKey`.

```java
if (Flixel.keys.pressed(FlixelKey.D)) {
  player.changeX(150f * elapsed);
}
if (Flixel.keys.justPressed(FlixelKey.SPACE)) {
  jump();
}
if (Flixel.keys.justReleased(FlixelKey.SHIFT_LEFT)) {
  stopSprinting();
}
```

### Several keys at once

`anyPressed(...)`, `anyJustPressed(...)`, and `anyJustReleased(...)` take several keys and return `true` if at least
one matches. Up to six keys are checked without creating an array, so they are safe to call every frame:

```java
boolean left = Flixel.keys.anyPressed(FlixelKey.A, FlixelKey.LEFT);
if (Flixel.keys.anyJustPressed(FlixelKey.ENTER, FlixelKey.Z, FlixelKey.SPACE)) {
  confirm();
}
```

### Any key and no key

`FlixelKey.ANY` matches any key, which is handy for "press any key to start":

```java
if (Flixel.keys.justPressed(FlixelKey.ANY)) {
  Flixel.switchState(MenuState::new);
}
```

### Finding out which key was pressed

`firstJustPressed()` returns the first key that went down this frame, and `firstPressed()` the key held the longest.
Both return `FlixelKey.NONE` when there is nothing to report. This is the heart of a "press a key to rebind" screen:

```java
int key = Flixel.keys.firstJustPressed();
if (key != FlixelKey.NONE) {
  jumpKey = key;
  label.setText("Jump: " + FlixelKey.toString(key)); // A readable name such as "Space".
}
```

`FlixelKey.toString(code)` gives a readable name for a settings screen, and `FlixelKey.fromString(name)` turns it back
into a code, which is how you store bindings in a save file.

### Blocking keys

`block(key)` hides a key from the game until `unblock(key)` is called; `pressed(...)` and friends then report it as
up. The `raw...` methods (`rawPressed(...)` and so on) still see it. Blocking is meant for tools such as the debugger,
which blocks its own hotkeys so the game does not react to them.

## Mouse

The mouse lives on `Flixel.mouse`. Button codes are constants in `FlixelMouseButton`: `LEFT`, `RIGHT`, `MIDDLE`,
`BACK`, and `FORWARD`.

```java
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
  // Where in the game world was the click? This accounts for camera scroll and zoom.
  spawnAt(Flixel.mouse.getWorldX(), Flixel.mouse.getWorldY());
}

if (Flixel.mouse.overlap(playButton) && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
  Flixel.switchState(PlayState::new);
}
```

### The scroll wheel

Scroll amounts are collected during the frame and read as deltas:

```java
float scroll = Flixel.mouse.getScrollDeltaY();
if (scroll != 0f) {
  camera.changeZoom(scroll * 0.1f);
}
```

The size of one "notch" differs between mice, trackpads, and operating systems, and so can the sign. Offer an option to
invert scrolling in your settings.

### Moving the pointer

Desktop games can move the pointer with `setScreenPosition(x, y)` or `setWorldPosition(x, y)`. Browsers and phones do
not allow this, so the calls do nothing there; check `supportsSetPosition()` first if your game depends on it.

### Changing the cursor

`Flixel.mouse.icons` changes the operating system's cursor:

```java
Flixel.mouse.icons.setCursor(FlixelMouseCursor.HAND);   // Hovering a button.
Flixel.mouse.icons.setCursor(FlixelMouseCursor.IBEAM);  // Hovering a text field.
Flixel.mouse.icons.resetCursor();                       // Back to the arrow.
```

The presets are `ARROW`, `IBEAM`, `WAIT`, `CROSSHAIR`, `HAND`, `GRAB`, `GRABBING`, `HORIZONTAL_RESIZE`,
`VERTICAL_RESIZE`, `NORTH_WEST_SOUTH_EAST_RESIZE`, `NORTH_EAST_SOUTH_WEST_RESIZE`, `ALL_RESIZE`, and `NOT_ALLOWED`.
Not every platform has every shape. `setCustomCursor(graphic, hotspotX, hotspotY)` uses your own image where the
platform supports it (check `supportsCustomCursors()`).

On the web, the cursor only changes while the pointer is over the game canvas, since browsers style cursors per page
element. Every preset is available there, and custom cursors must be graphics loaded from a bundled image asset, not
ones made at runtime with `makeGraphic(...)`. Keep cursor images at 32 by 32 pixels or smaller; most browsers ignore
anything over 128 by 128.

### Letting UI eat a click

When a UI layer handles a click, call `Flixel.mouse.consume(FlixelMouseButton.LEFT)` so the rest of your game does not
also treat it as "shoot." From then on the button reads as released until the player lets go of it, and the game never
sees the matching release. Consume as early in the frame as you can, before other code reads the mouse. The `raw...`
methods (`rawPressed(...)` and friends) are never affected, so the UI itself can still read the button.

## Screen space versus world space

Every pointer position comes in two flavors, and mixing them up is a classic bug.

- **Screen space** (`getScreenX()`, `getScreenY()`): pixels from the top-left of the window. It ignores the camera.
  Use it for HUD elements that never scroll.
- **World space** (`getWorldX()`, `getWorldY()`): the position in your game world, after undoing the camera's scroll
  and zoom. Use it for anything in the level.

Imagine looking at a big map through a window. Screen space says where your finger is on the glass. World space says
which town on the map your finger is pointing at. If you slide the map (scroll the camera), your finger stays at the
same spot on the glass but points at a different town.

By default the first camera is used for the conversion. Change it with `Flixel.mouse.setWorldCamera(camera)`, or ask
once with `Flixel.mouse.getWorldX(camera)`. Both spaces use the same top-left origin with Y growing downward.

## Touch

Touch lives on `Flixel.touches`. It is switched on automatically on Android. On desktop and the web it starts switched
off; turn it on if your game supports touchscreen monitors:

```java
Flixel.touches.enabled = true;
```

### Any finger

When you do not care which finger did it:

```java
if (Flixel.touches.anyJustPressed()) {
  jump();
}
int fingers = Flixel.touches.count(); // How many fingers are down right now.
```

### Specific fingers

Each finger is a `FlixelTouch` in the pre-allocated `Flixel.touches.list` array. Slot `0` is the first finger of the
current gesture, slot `1` the second, and so on. Reading the fields directly is the fastest way to get at them:

```java
FlixelTouch first = Flixel.touches.list[0];
if (first.justPressed()) {
  spawnEffect(first.worldX, first.worldY);
}
if (first.dragging()) {
  aimAt(first.worldX, first.worldY);
}
if (first.justReleased() || first.justCancelled()) {
  fire();
}
```

`justCancelled()` is set for one frame when the system takes a touch away without a release, for example when a phone
call interrupts. Treat it like a release.

### Rectangle hit tests

Most touch controls are "is a finger inside this area?":

```java
// True every frame a finger is inside the button (world space).
if (Flixel.touches.touchingWorld(button.getX(), button.getY(), button.getWidth(), button.getHeight())) {
  button.setColor(FlixelColor.YELLOW);
}

// True only on the frame a finger first lands inside it.
if (Flixel.touches.justTouchedWorld(button.getX(), button.getY(), button.getWidth(), button.getHeight())) {
  onButtonTapped();
}

// Screen-space versions for HUD controls: the left half of the screen.
if (Flixel.touches.touchingScreen(0f, 0f, Flixel.window.getWidth() / 2f, Flixel.window.getHeight())) {
  moveLeft();
}
```

The default limit is 10 fingers (`FlixelTouchManager.DEFAULT_MAX_POINTERS`); change it with `setMaxPointers(n)`.

## Gamepads

Gamepads live on `Flixel.gamepads`. Controllers are numbered by **slot**: `0` is the first controller, `1` the second,
up to `FlixelGamepadInputManager.MAX_GAMEPADS`.

```java
if (Flixel.gamepads.justPressed(0, FlixelGamepadButton.A)) {
  player.jump();
}
if (Flixel.gamepads.anyJustPressed(FlixelGamepadButton.START)) { // Any controller.
  pauseGame();
}

float moveX = Flixel.gamepads.getAxis(0, FlixelGamepadAxis.LEFT_X); // -1 (left) to 1 (right).
float moveY = Flixel.gamepads.getAxis(0, FlixelGamepadAxis.LEFT_Y); // -1 (up) to 1 (down).
float gas = Flixel.gamepads.getTriggerR(0);                          // 0 to 1.
```

### Logical buttons, not hardware numbers

The "A" button on an Xbox controller and the cross button on a PlayStation controller are reported as different
hardware numbers by different controllers. `FlixelGamepadButton` and `FlixelGamepadAxis` are **logical** names that
always mean "the bottom face button" or "the left stick's X axis," whatever is plugged in. A mapping for each slot
translates them to the real numbers.

Buttons: `A`, `B`, `X`, `Y`, `L1`, `R1`, `L2`, `R2`, `LEFT_STICK`, `RIGHT_STICK`, `START`, `BACK`, `MODE`,
`DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`, and `TOUCHPAD`, plus `ANY` and `NONE`. Axes: `LEFT_X`, `LEFT_Y`,
`RIGHT_X`, `RIGHT_Y`, `L2`, and `R2`.

### A handle for one controller

If you prefer holding on to one controller instead of passing its slot every time, get a `FlixelGamepadDevice`:

```java
FlixelGamepadDevice pad = Flixel.gamepads.getFirstActiveGamepad(); // null when none is connected.
if (pad != null) {
  player.setVelocityX(pad.getXAxis() * 200f);
  if (pad.justPressed(FlixelGamepadButton.A)) {
    player.jump();
  }
}

FlixelGamepadDevice second = Flixel.gamepads.ensureDevice(1); // Always the same object for slot 1.
```

### Dead zones

Analog sticks rarely rest at exactly zero, so a character can drift slowly with nobody touching the stick. A dead zone
ignores small values:

```java
Flixel.gamepads.globalDeadZone = 0.15f; // Ignore anything below 15% of a full push.
```

### Which controller is it?

`FlixelGamepadModel` tells you the controller family (for example `PS4`, `PS5`, `XBOX_360`, `XBOX_ONE`,
`SWITCH_PRO`), so you can show the right button prompts:

```java
FlixelGamepadModel model = Flixel.gamepads.getModel(0);
```

### Plugging in and unplugging

```java
Flixel.gamepads.deviceConnected.add(event -> Flixel.info("Controller {} connected: {}", event.gamepadId(), event.model()));
Flixel.gamepads.deviceDisconnected.add(event -> openSubState(new ControllerLostSubState()));
```

The event objects are reused, so read what you need inside the callback and do not keep them.

### Rumble

Vibration drives two motors: a heavy left one and a light right one.

```java
Flixel.gamepads.vibrate(0, 0.3f);              // Both motors, full strength, 0.3 seconds.
Flixel.gamepads.vibrate(0, 0.5f, 0f, 0.5f);    // Left motor only, half strength, 0.5 seconds.
Flixel.gamepads.stopVibration(0);
boolean canRumble = Flixel.gamepads.canVibrate(0);
```

On the web, rumble needs a Chromium-based browser.

### Unusual controllers

The framework knows common controllers out of the box (on desktop, SDL3's database covers hundreds). For anything
else, add a mapping resolver. It runs when a controller connects; return a mapping for hardware you recognize, or
`null` to let the next resolver try:

```java
Flixel.gamepads.addMappingResolver(gamepad -> {
  if (gamepad.getVendorId() == MY_VID && gamepad.getProductId() == MY_PID) {
    FlixelGamepadMapping m = new FlixelGamepadMapping();
    m.registerButton(FlixelGamepadButton.A, 0);
    m.registerButton(FlixelGamepadButton.B, 1);
    m.registerAxis(FlixelGamepadAxis.LEFT_X, 0);
    m.registerAxis(FlixelGamepadAxis.LEFT_Y, 1);
    return m;
  }
  return null;
});
```

### Turning gamepads off

If your game never uses controllers, skip polling them entirely:

```java
Flixel.gamepads.enabled = false;
```

## Typed text and the raw input layer

Under the polled managers sits `Flixel.input`, the raw `FlixelInputDevice` each platform installs. You rarely need it,
except for two jobs.

**Typing text.** Key codes are about physical keys; they do not know that Shift+2 is "@" on one keyboard and a
quotation mark on another, or that an accented letter took two keystrokes. For a name entry box, ask the platform for
typed characters instead:

```java
private final FlixelKeyboardListener typing = new FlixelKeyboardListener() {
  @Override
  public void keyTyped(char character) {
    nameBuffer.concat(character);
    nameLabel.setText(nameBuffer);
  }
};

// When the text box gains focus:
Flixel.input.startTextInput();          // Also opens the on-screen keyboard where there is one.
Flixel.input.addKeyboardListener(typing);

// When it loses focus:
Flixel.input.stopTextInput();
Flixel.input.removeKeyboardListener(typing);
```

**Reacting the moment input arrives.** Listeners (`FlixelKeyboardListener`, `FlixelMouseListener`,
`FlixelTouchListener`) receive every raw event in order, as it happens. Every registered listener sees each event,
and listeners do not block one another. Remove them when you are done, usually in your state's `destroy()`.

## Supporting several devices at once

Checking the keyboard, the gamepad, and touch separately for every action gets repetitive fast:

```java
boolean jump = Flixel.keys.justPressed(FlixelKey.SPACE)
    || Flixel.gamepads.anyJustPressed(FlixelGamepadButton.A)
    || Flixel.touches.anyJustPressed();
```

The [Input Actions](input-actions.md) system lets you define "jump" once, bind it to any number of keys, buttons,
and touch areas, and then only ask about "jump." It also makes rebindable controls and Steam Input support much
easier.

## Platform differences

| Feature                    | Desktop                 | Web                                       | Android                  |
|----------------------------|-------------------------|-------------------------------------------|--------------------------|
| Keyboard                   | Yes                     | Yes (by physical key position)            | Hardware keyboards only  |
| Mouse                      | Yes                     | Yes                                       | Rarely                   |
| Moving the pointer         | Yes                     | No                                        | No                       |
| Touch                      | Touchscreen monitors (turn on) | Taps arrive as mouse clicks        | Yes, on by default       |
| Gamepads                   | Yes (SDL3)              | Yes (Gamepad API)                         | Yes                      |
| Rumble                     | Yes                     | Chromium-based browsers                   | Depends on the device    |

## Common mistakes

- **Using `pressed(...)` for one-shot actions.** The jump fires every frame the key is held. Use `justPressed(...)`.
- **Using screen coordinates for world objects.** The click lands in the wrong place as soon as the camera scrolls.
- **Forgetting touch is off on desktop.** Set `Flixel.touches.enabled = true` if you test touch on a touchscreen laptop.
- **Keeping gamepad event objects.** They are reused; copy the values you need.
- **Reading input in a background thread.** Input is only valid on the main thread, during the frame.

## Check your understanding

1. Your character jumps twice from one tap of the space bar. Which method should you use?
2. A click spawns an explosion at the wrong spot after the camera follows the player. What went wrong?
3. Why should a name-entry field use `keyTyped(char)` instead of key codes?

(Answers: `justPressed(...)`; you used screen coordinates instead of world coordinates; key codes describe physical
keys, while `keyTyped` gives the actual characters the player meant, including shifted and accented ones.)

**Next:** [Input Actions](input-actions.md)
