/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.input.gamepad;

import org.jetbrains.annotations.NotNull;

/**
 * A per-slot facade that makes it easier to query a single controller without passing a gamepad ID
 * everywhere.
 *
 * <p>Think of a {@link FlixelGamepadDevice} as a "named remote control" for one slot. Instead of
 * writing {@code Flixel.gamepads.pressed(0, FlixelGamepadButton.A)} every time, you can ask for a
 * device once and reuse it:
 *
 * <pre>{@code
 * FlixelGamepadDevice pad = Flixel.gamepads.ensureDevice(0);
 * if (pad.isConnected() && pad.justPressed(FlixelGamepadButton.A)) {
 *   jump();
 * }
 * }</pre>
 *
 * <p>Instances are created on demand by {@link FlixelGamepadInputManager#ensureDevice(int)} and
 * cached for the lifetime of the manager (until {@link FlixelGamepadInputManager#reset()} is
 * called). {@link FlixelGamepadInputManager#getById(int)} returns {@code null} until
 * {@link FlixelGamepadInputManager#ensureDevice(int)} has been called for that slot at least once.
 *
 * <p>A device object is just a convenience wrapper; it always delegates to the
 * {@link FlixelGamepadInputManager}, so every query reflects the current frame's state.
 *
 * @see FlixelGamepadInputManager
 * @see FlixelGamepadButton
 * @see FlixelGamepadAxis
 */
public final class FlixelGamepadDevice {

  private final FlixelGamepadInputManager manager;
  private final int id;

  FlixelGamepadDevice(@NotNull FlixelGamepadInputManager manager, int id) {
    this.manager = manager;
    this.id = id;
  }

  /**
   * Returns the slot index for this device.
   *
   * <p>The ID is stable for as long as the controller stays at the same position in the backend's
   * list. If the player unplugs the controller and plugs in a different one, the new hardware takes
   * over the same slot and this device object automatically reflects it.
   *
   * @return Slot index in the range {@code [0, }{@link FlixelGamepadInputManager#MAX_GAMEPADS}{@code )}.
   */
  public int getId() {
    return id;
  }

  /**
   * Returns whether this slot currently has a connected controller.
   *
   * <p>A {@link FlixelGamepadDevice} object always exists once you call
   * {@link FlixelGamepadInputManager#ensureDevice(int)}, but the slot it refers to can become
   * empty at any time (player disconnects). Always check this before reading input if disconnects
   * are possible in your game:
   *
   * <pre>{@code
   * if (pad.isConnected()) {
   *   // Safe to read buttons, axes, triggers.
   * }
   * }</pre>
   *
   * @return {@code true} when a controller is connected to this slot right now.
   */
  public boolean isConnected() {
    return manager.isSlotConnected(id);
  }

  /**
   * Returns the hardware model detected for this slot.
   *
   * <p>The model is detected once when the controller is (re)connected and is not re-evaluated
   * while the same hardware stays in the slot. Use it to show the right button icons -- for
   * example, "Press X" on Xbox hardware vs. "Press Square" on PlayStation hardware:
   *
   * <pre>{@code
   * if (pad.getModel() == FlixelGamepadModel.PLAYSTATION) {
   *   showHint("Press Cross to jump");
   * } else {
   *   showHint("Press A to jump");
   * }
   * }</pre>
   *
   * @return The detected {@link FlixelGamepadModel}, or {@link FlixelGamepadModel#UNKNOWN} when the
   *     model could not be identified or the slot is empty.
   */
  @NotNull
  public FlixelGamepadModel getModel() {
    return manager.getModel(id);
  }

  /**
   * Returns {@code true} while the given button is being held down this frame.
   *
   * <p>This is the go-to method for anything that should repeat every frame while the button is
   * held, such as walking or charging a weapon. For a one-shot action (jump, fire), prefer
   * {@link #justPressed(FlixelGamepadButton)} instead so the action does not repeat.
   *
   * <pre>{@code
   * if (pad.pressed(FlixelGamepadButton.B)) {
   *   player.speed *= SPRINT_MULTIPLIER;
   * }
   * }</pre>
   *
   * <p>Pass {@link FlixelGamepadButton#ANY} to check whether any button is currently held.
   *
   * @param button A logical button token such as {@link FlixelGamepadButton#A}; must not be
   *     {@code null}.
   * @return {@code true} when the button is held this frame.
   */
  public boolean pressed(@NotNull FlixelGamepadButton button) {
    return manager.pressed(id, button);
  }

  /**
   * Returns {@code true} on the first frame the given button transitions from released to pressed.
   *
   * <p>Use this for one-shot actions that should fire exactly once per press, such as jumping,
   * firing a weapon, or opening a menu:
   *
   * <pre>{@code
   * if (pad.justPressed(FlixelGamepadButton.A)) {
   *   player.jump();
   * }
   * }</pre>
   *
   * <p>If you need to know the first button pressed when multiple buttons may be held, use
   * {@link FlixelGamepadInputManager#firstJustPressed(int)} on the manager instead.
   *
   * @param button A logical button token such as {@link FlixelGamepadButton#A}; must not be
   *     {@code null}.
   * @return {@code true} on the frame the button first goes down.
   */
  public boolean justPressed(@NotNull FlixelGamepadButton button) {
    return manager.justPressed(id, button);
  }

  /**
   * Returns {@code true} on the first frame the given button transitions from pressed to released.
   *
   * <p>Use this for actions that trigger on release, such as releasing a charged attack or
   * confirming a long-press:
   *
   * <pre>{@code
   * if (pad.justReleased(FlixelGamepadButton.X)) {
   *   fireChargedShot();
   * }
   * }</pre>
   *
   * @param button A logical button token such as {@link FlixelGamepadButton#X}; must not be
   *     {@code null}.
   * @return {@code true} on the frame the button is first released.
   */
  public boolean justReleased(@NotNull FlixelGamepadButton button) {
    return manager.justReleased(id, button);
  }

  /**
   * Returns whether this controller reports vibration (rumble) support.
   *
   * <p>Not all platforms or hardware support rumble. Check this before calling
   * {@link #vibrate(float)} so you do not silently ignore unsupported controllers:
   *
   * <pre>{@code
   * if (pad.canVibrate()) {
   *   pad.vibrate(0.5f, 0.3f);
   * }
   * }</pre>
   *
   * @return {@code true} when the controller is connected and its hardware supports vibration.
   */
  public boolean canVibrate() {
    return manager.canVibrate(id);
  }

  /**
   * Vibrates this controller at full intensity on both motors for the given duration.
   *
   * <p>Good for big impacts where you want maximum feedback without worrying about tuning
   * individual motor strengths. For example, a heavy explosion or a knockback hit:
   *
   * <pre>{@code
   * pad.vibrate(0.3f); // Full-strength rumble for 0.3 seconds.
   * }</pre>
   *
   * @param durationSecs How long to vibrate, in seconds. Clamped internally; pass a positive value.
   */
  public void vibrate(float durationSecs) {
    manager.vibrate(id, durationSecs);
  }

  /**
   * Vibrates this controller at the given intensity on both motors for the given duration.
   *
   * <p>Use this when you want to scale the strength of the rumble without splitting the left and
   * right motors, such as a softer bump or a distance-based feedback:
   *
   * <pre>{@code
   * pad.vibrate(0.25f, 0.2f); // Soft rumble on both motors for 0.2 seconds.
   * }</pre>
   *
   * @param intensity Motor strength in the range {@code [0, 1]}. Values outside the range are
   *     clamped.
   * @param durationSecs How long to vibrate, in seconds.
   */
  public void vibrate(float intensity, float durationSecs) {
    manager.vibrate(id, intensity, durationSecs);
  }

  /**
   * Vibrates this controller with independent left and right motor intensities for the given
   * duration.
   *
   * <p>Controllers have two motors: the left (low-frequency, heavy) motor and the right
   * (high-frequency, light) motor. Driving them at different strengths produces more nuanced
   * feedback. For example, use only the left motor for a deep rumble (like a vehicle engine) and
   * only the right motor for a sharp buzz (like a small collision):
   *
   * <pre>{@code
   * // Engine idle: subtle low-frequency throb, no high-frequency buzz.
   * pad.vibrate(0.2f, 0f, 1f);
   *
   * // Controller hit: sharp right-motor buzz, no heavy rumble.
   * pad.vibrate(0f, 0.8f, 0.1f);
   * }</pre>
   *
   * @param leftIntensity Strength for the left (low-frequency) motor, in the range {@code [0, 1]}.
   *     Values outside the range are clamped.
   * @param rightIntensity Strength for the right (high-frequency) motor, in the range {@code [0, 1]}.
   *     Values outside the range are clamped.
   * @param durationSecs How long to vibrate, in seconds.
   */
  public void vibrate(float leftIntensity, float rightIntensity, float durationSecs) {
    manager.vibrate(id, leftIntensity, rightIntensity, durationSecs);
  }

  /**
   * Stops any active vibration on this controller immediately.
   *
   * <p>Call this when switching scenes, pausing the game, or whenever you need to cut rumble
   * feedback early regardless of how much time was left on the current vibration.
   */
  public void stopVibration() {
    manager.stopVibration(id);
  }

  /**
   * Returns the current value of a logical axis on this controller, after dead-zone processing.
   *
   * <p>Analog sticks report a float in {@code [-1, 1]} on each axis. Negative X means left,
   * positive X means right; negative Y means up, positive Y means down (y-down convention,
   * backend-dependent). Any value whose absolute value is at or below the configured dead zone
   * ({@link FlixelGamepadInputManager#globalDeadZone}) is returned as exactly {@code 0f} to
   * prevent stick drift from moving things when the controller is untouched:
   *
   * <pre>{@code
   * float tiltX = pad.getAxis(FlixelGamepadAxis.LEFT_X);
   * float tiltY = pad.getAxis(FlixelGamepadAxis.LEFT_Y);
   * player.velocityX = tiltX * SPEED;
   * player.velocityY = tiltY * SPEED;
   * }</pre>
   *
   * @param axis A logical axis token such as {@link FlixelGamepadAxis#LEFT_X}; must not be
   *     {@code null}.
   * @return Axis value in the range {@code [-1, 1]}, or {@code 0f} when inactive or within the
   *     dead zone.
   */
  public float getAxis(@NotNull FlixelGamepadAxis axis) {
    return manager.getAxis(id, axis);
  }

  /**
   * Shorthand for the left stick horizontal axis ({@link FlixelGamepadAxis#LEFT_X}).
   *
   * <p>Negative means left, positive means right. Equivalent to
   * {@code getAxis(FlixelGamepadAxis.LEFT_X)}, but shorter to type when only the left stick is
   * needed:
   *
   * <pre>{@code
   * player.velocityX = pad.getXAxis() * SPEED;
   * }</pre>
   *
   * @return Horizontal axis value in the range {@code [-1, 1]}, or {@code 0f} within the dead zone.
   */
  public float getXAxis() {
    return manager.getAxis(id, FlixelGamepadAxis.LEFT_X);
  }

  /**
   * Shorthand for the left stick vertical axis ({@link FlixelGamepadAxis#LEFT_Y}).
   *
   * <p>The sign convention is backend-dependent (typically positive = down). Equivalent to
   * {@code getAxis(FlixelGamepadAxis.LEFT_Y)}, but shorter to type when only the left stick is
   * needed:
   *
   * <pre>{@code
   * player.velocityY = pad.getYAxis() * SPEED;
   * }</pre>
   *
   * @return Vertical axis value in the range {@code [-1, 1]}, or {@code 0f} within the dead zone.
   */
  public float getYAxis() {
    return manager.getAxis(id, FlixelGamepadAxis.LEFT_Y);
  }

  /**
   * Returns the analog pressure of the left trigger (L2 / LT), in the range {@code [0, 1]}, after
   * dead-zone processing.
   *
   * <p>Use this to read how hard the player is squeezing the trigger rather than just whether it
   * is pressed. A common use is to scale an in-game action by the trigger depth. For example,
   * easing into a brake or gradually aiming down sights:
   *
   * <pre>{@code
   * float brakeStrength = pad.getTriggerL();
   * vehicle.brake(brakeStrength);
   * }</pre>
   *
   * <p>On backends that report triggers as analog axes (desktop), the returned value is the raw
   * hardware pressure mapped to {@code [0, 1]}. On backends that report them as digital buttons
   * (web), the value is exactly {@code 0f} when released and {@code 1f} when pressed, because the
   * hardware has no analog resolution. Either way, this method works without any branching on your
   * side.
   *
   * <p>If you only need to know whether the trigger is pressed at all, use
   * {@link #pressed(FlixelGamepadButton)} with {@link FlixelGamepadButton#L2} instead, which works
   * correctly on both backend types.
   *
   * @return Trigger pressure in {@code [0, 1]}, or {@code 0f} when released or within the dead zone.
   */
  public float getTriggerL() {
    return manager.getTriggerL(id);
  }

  /**
   * Returns the analog pressure of the right trigger (R2 / RT), in the range {@code [0, 1]}, after
   * dead-zone processing.
   *
   * <p>Use this to read how hard the player is squeezing the trigger rather than just whether it
   * is pressed. A common use is to scale an in-game action by the trigger depth. For example,
   * controlling acceleration or how hard a character is pushing something:
   *
   * <pre>{@code
   * float accel = pad.getTriggerR();
   * vehicle.accelerate(accel);
   * }</pre>
   *
   * <p>On backends that report triggers as analog axes (desktop), the returned value is the raw
   * hardware pressure mapped to {@code [0, 1]}. On backends that report them as digital buttons
   * (web), the value is exactly {@code 0f} when released and {@code 1f} when pressed, because the
   * hardware has no analog resolution. Either way, this method works without any branching on your
   * side.
   *
   * <p>If you only need to know whether the trigger is pressed at all, use
   * {@link #pressed(FlixelGamepadButton)} with {@link FlixelGamepadButton#R2} instead, which works
   * correctly on both backend types.
   *
   * @return Trigger pressure in {@code [0, 1]}, or {@code 0f} when released or within the dead zone.
   */
  public float getTriggerR() {
    return manager.getTriggerR(id);
  }
}
