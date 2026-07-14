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

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.input.FlixelInputManager;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Global gamepad manager backed by gdx-controllers. Polls controllers each frame and mirrors the
 * keyboard and mouse frame contract from {@link org.flixelgdx.FlixelGame FlixelGame}.
 *
 * <p>The gamepad system is <strong>disabled by default</strong>. Set {@link #enabled} to {@code true} before the game
 * loop starts to opt in:
 *
 * <pre>{@code
 * Flixel.gamepads.enabled = true;
 * }</pre>
 *
 * <p>Use logical button and axis constants from {@link FlixelGamepadInput} (for example {@link FlixelGamepadInput#A})
 * with {@link #pressed(int, int)}; each {@link Controller#getMapping()} supplies native indices.
 * {@link FlixelGamepadDevice} is optional; call {@link #ensureDevice(int)} once per slot you want a facade for.
 */
public final class FlixelGamepadManager implements FlixelInputManager, ControllerListener {

  /** Maximum supported simultaneous controllers. */
  public static final int MAX_GAMEPADS = 8;

  private static final int MAX_BUTTONS = 256;
  private static final int MAX_AXES = 64;

  // Reserved slots in currentButtons/previousButtons for synthesized trigger state. Used on
  // backends such as Jamepad/SDL where L2 and R2 are reported as analog axes instead of buttons,
  // leaving ControllerMapping.buttonL2 / buttonR2 as UNDEFINED.
  private static final int SYNTHETIC_TRIGGER_L = 249;
  private static final int SYNTHETIC_TRIGGER_R = 250;
  private static final float TRIGGER_BUTTON_THRESHOLD = 0.5f;

  /** Number of controllers mapped to IDs {@code 0 .. numActiveGamepads-1} this frame. */
  public int numActiveGamepads;

  /**
   * Optional analog dead zone applied to all axis reads when non-null. When {@code null}, only
   * exact zeroing for true zero input is skipped.
   */
  @Nullable
  public Float globalDeadZone;

  @NotNull
  public final FlixelSignal<GamepadConnectedEvent> deviceConnected = new FlixelSignal<>();

  @NotNull
  public final FlixelSignal<GamepadDisconnectedEvent> deviceDisconnected = new FlixelSignal<>();

  private final GamepadConnectedEvent connectPayload = new GamepadConnectedEvent();
  private final GamepadDisconnectedEvent disconnectPayload = new GamepadDisconnectedEvent();

  private final Controller[] slotController = new Controller[MAX_GAMEPADS];
  private final FlixelGamepadModel[] slotModel = new FlixelGamepadModel[MAX_GAMEPADS];

  private final boolean[][] currentButtons = new boolean[MAX_GAMEPADS][MAX_BUTTONS];
  private final boolean[][] previousButtons = new boolean[MAX_GAMEPADS][MAX_BUTTONS];

  private final float[][] axisValues = new float[MAX_GAMEPADS][MAX_AXES];

  @Nullable
  private final FlixelGamepadDevice[] ensuredDevices = new FlixelGamepadDevice[MAX_GAMEPADS];

  @NotNull
  private FlixelHapticsProvider hapticsProvider = new FlixelDefaultHapticsProvider(this);

  @Nullable
  private FlixelAnalogButtonReader analogButtonReader;

  /**
   * Whether the gamepad system is active. When {@code false}, all queries return inactive state and no hardware is
   * polled. Defaults to {@code false}; set to {@code true} to opt in before the game loop starts.
   */
  public boolean enabled = false;

  private boolean listenerAttached;

  public FlixelGamepadManager() {}

  /**
   * Registers a {@link ControllerListener} with gdx-controllers. Safe to call more than once.
   */
  public void attach() {
    if (listenerAttached) {
      return;
    }
    try {
      Controllers.addListener(this);
      listenerAttached = true;
    } catch (Throwable ignored) {
      // Some backends may not expose Controllers until fully booted.
    }
  }

  /**
   * Unregisters listeners and clears internal slot state.
   */
  public void detach() {
    if (listenerAttached) {
      try {
        Controllers.removeListener(this);
      } catch (Throwable ignored) {
        // Ignore.
      }
      listenerAttached = false;
    }
    reset();
  }

  @Override
  public void reset() {
    numActiveGamepads = 0;
    Arrays.fill(slotController, null);
    Arrays.fill(slotModel, FlixelGamepadModel.UNKNOWN);
    Arrays.fill(ensuredDevices, null);
    for (int i = 0; i < MAX_GAMEPADS; i++) {
      Arrays.fill(currentButtons[i], false);
      Arrays.fill(previousButtons[i], false);
      Arrays.fill(axisValues[i], 0f);
    }
  }

  @Override
  public void update() {
    if (!enabled) {
      return;
    }
    attach();
    syncControllers();
    pollHardware();
  }

  @Override
  public void endFrame() {
    for (int s = 0; s < numActiveGamepads; s++) {
      System.arraycopy(currentButtons[s], 0, previousButtons[s], 0, MAX_BUTTONS);
    }
  }

  /**
   * Returns the cached {@link FlixelGamepadDevice} for a slot if the game previously called
   * {@link #ensureDevice(int)}.
   *
   * @param id Slot id.
   * @return Cached device, or {@code null}.
   */
  @Nullable
  public FlixelGamepadDevice getById(int id) {
    if (id < 0 || id >= MAX_GAMEPADS) {
      return null;
    }
    return ensuredDevices[id];
  }

  /**
   * Ensures a {@link FlixelGamepadDevice} exists for the given slot. At most one instance is
   * created per id for the lifetime of this manager (until {@link #reset()}).
   *
   * @param id Slot id.
   * @return Non-null device facade.
   */
  @NotNull
  public FlixelGamepadDevice ensureDevice(int id) {
    if (id < 0 || id >= MAX_GAMEPADS) {
      throw new IllegalArgumentException("gamepad id out of range: " + id);
    }
    FlixelGamepadDevice d = ensuredDevices[id];
    if (d == null) {
      d = new FlixelGamepadDevice(this, id);
      ensuredDevices[id] = d;
    }
    return d;
  }

  /**
   * First slot with any button beyond dead zone or analog movement this frame, or {@code -1}.
   *
   * @return Active id, or {@code -1} when none.
   */
  public int getFirstActiveGamepadId() {
    for (int s = 0; s < numActiveGamepads; s++) {
      if (slotHasAnalogOrButtonActivity(s)) {
        return s;
      }
    }
    return -1;
  }

  /**
   * Returns a device for {@link #getFirstActiveGamepadId()} only when that slot was already
   * {@linkplain #ensureDevice(int) ensured}.
   *
   * @return Device, or {@code null}.
   */
  @Nullable
  public FlixelGamepadDevice getFirstActiveGamepad() {
    int id = getFirstActiveGamepadId();
    if (id < 0) {
      return null;
    }
    return getById(id);
  }

  /**
   * Writes active slot ids in order to {@code reuseOut[0 .. count-1]}.
   *
   * @param reuseOut Caller buffer; length should be at least {@link #MAX_GAMEPADS}.
   * @return Number of ids written.
   */
  public int getActiveGamepadIds(@Nullable int[] reuseOut) {
    if (reuseOut == null) {
      return 0;
    }
    int n = Math.min(numActiveGamepads, reuseOut.length);
    for (int i = 0; i < n; i++) {
      reuseOut[i] = i;
    }
    return n;
  }

  /**
   * Fills {@code reuseOut} with {@link FlixelGamepadDevice} instances that were previously ensured
   * and are still connected.
   *
   * @param reuseOut Caller buffer.
   * @return Number of entries written.
   */
  public int getActiveGamepads(@Nullable FlixelGamepadDevice[] reuseOut) {
    if (reuseOut == null) {
      return 0;
    }
    int w = 0;
    for (int i = 0; i < MAX_GAMEPADS && w < reuseOut.length; i++) {
      FlixelGamepadDevice d = ensuredDevices[i];
      if (d != null && isSlotConnected(i)) {
        reuseOut[w++] = d;
      }
    }
    return w;
  }

  /**
   * Returns {@code true} when any active gamepad currently holds the given button.
   *
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}, such as
   *     {@link FlixelGamepadInput#A}.
   * @return {@code true} when at least one gamepad is pressing the button this frame.
   */
  public boolean anyPressed(int logicalButton) {
    if (!enabled) {
      return false;
    }
    for (int s = 0; s < numActiveGamepads; s++) {
      if (pressed(s, logicalButton)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when any active gamepad first pressed the given button this frame.
   *
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}.
   * @return {@code true} when at least one gamepad transitioned to pressed this frame.
   */
  public boolean anyJustPressed(int logicalButton) {
    if (!enabled) {
      return false;
    }
    for (int s = 0; s < numActiveGamepads; s++) {
      if (justPressed(s, logicalButton)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when any active gamepad released the given button this frame.
   *
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}.
   * @return {@code true} when at least one gamepad transitioned to released this frame.
   */
  public boolean anyJustReleased(int logicalButton) {
    if (!enabled) {
      return false;
    }
    for (int s = 0; s < numActiveGamepads; s++) {
      if (justReleased(s, logicalButton)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when any active gamepad has a button pressed or an analog input beyond the
   * dead zone this frame.
   *
   * @return {@code true} when at least one gamepad is producing input.
   */
  public boolean anyInput() {
    if (!enabled) {
      return false;
    }
    for (int s = 0; s < numActiveGamepads; s++) {
      if (slotHasAnalogOrButtonActivity(s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when any active gamepad is moving a left or right stick horizontally beyond
   * the dead zone this frame.
   *
   * @return {@code true} when at least one gamepad has horizontal stick movement.
   */
  public boolean anyMovedXAxis() {
    if (!enabled) {
      return false;
    }
    float dz = deadZoneValue();
    for (int s = 0; s < numActiveGamepads; s++) {
      if (Math.abs(getAxisRaw(s, FlixelGamepadInput.AXIS_LEFT_X)) > dz) {
        return true;
      }
      if (Math.abs(getAxisRaw(s, FlixelGamepadInput.AXIS_RIGHT_X)) > dz) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when any active gamepad is moving a left or right stick vertically beyond
   * the dead zone this frame.
   *
   * @return {@code true} when at least one gamepad has vertical stick movement.
   */
  public boolean anyMovedYAxis() {
    if (!enabled) {
      return false;
    }
    float dz = deadZoneValue();
    for (int s = 0; s < numActiveGamepads; s++) {
      if (Math.abs(getAxisRaw(s, FlixelGamepadInput.AXIS_LEFT_Y)) > dz) {
        return true;
      }
      if (Math.abs(getAxisRaw(s, FlixelGamepadInput.AXIS_RIGHT_Y)) > dz) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when the given slot is currently pressing the given button.
   *
   * <p>Pass {@link FlixelGamepadInput#ANY} as the button to check whether the slot has any
   * button pressed or meaningful analog movement this frame.
   *
   * @param gamepadId Slot index.
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}.
   * @return {@code true} when that button is pressed on the given slot this frame.
   */
  public boolean pressed(int gamepadId, int logicalButton) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return false;
    }

    Controller c = slotController[gamepadId];
    if (c == null) {
      return false;
    }
    if (logicalButton == FlixelGamepadInput.ANY) {
      return slotAnyPhysicalButton(gamepadId, c) || slotHasAxisBeyondDeadzone(gamepadId, c)
          || slotHasTriggerActivity(gamepadId, c);
    }

    int nativeCode = resolvedNativeButton(c, logicalButton);
    if (nativeCode < 0 || nativeCode >= MAX_BUTTONS) {
      return false;
    }
    return currentButtons[gamepadId][nativeCode];
  }

  /**
   * Returns {@code true} when the given slot first pressed the button this frame (was not pressed
   * last frame).
   *
   * @param gamepadId Slot index.
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}.
   * @return {@code true} on the first frame the button is pressed.
   */
  public boolean justPressed(int gamepadId, int logicalButton) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return false;
    }

    Controller c = slotController[gamepadId];
    if (c == null) {
      return false;
    }

    if (logicalButton == FlixelGamepadInput.ANY) {
      int min = c.getMinButtonIndex();
      int max = c.getMaxButtonIndex();
      for (int b = min; b <= max; b++) {
        if (b >= 0 && b < MAX_BUTTONS && currentButtons[gamepadId][b] && !previousButtons[gamepadId][b]) {
          return true;
        }
      }
      return false;
    }

    int nativeCode = resolvedNativeButton(c, logicalButton);
    if (nativeCode < 0 || nativeCode >= MAX_BUTTONS) {
      return false;
    }
    return currentButtons[gamepadId][nativeCode] && !previousButtons[gamepadId][nativeCode];
  }

  /**
   * Returns {@code true} when the given slot released the button this frame (was pressed last
   * frame, not pressed now).
   *
   * @param gamepadId Slot index.
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}.
   * @return {@code true} on the first frame the button is no longer pressed.
   */
  public boolean justReleased(int gamepadId, int logicalButton) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return false;
    }

    Controller c = slotController[gamepadId];
    if (c == null) {
      return false;
    }

    if (logicalButton == FlixelGamepadInput.ANY) {
      int min = c.getMinButtonIndex();
      int max = c.getMaxButtonIndex();
      for (int b = min; b <= max; b++) {
        if (b >= 0 && b < MAX_BUTTONS && !currentButtons[gamepadId][b] && previousButtons[gamepadId][b]) {
          return true;
        }
      }
      return false;
    }

    int nativeCode = resolvedNativeButton(c, logicalButton);
    if (nativeCode < 0 || nativeCode >= MAX_BUTTONS) {
      return false;
    }
    return !currentButtons[gamepadId][nativeCode] && previousButtons[gamepadId][nativeCode];
  }

  /**
   * Returns the current value of a logical axis on the given slot, after applying the global dead
   * zone. Returns {@code 0f} when the value is within the dead zone or the slot is out of range.
   *
   * @param gamepadId Slot index.
   * @param logicalAxis A logical axis constant from {@link FlixelGamepadInput}, such as
   *     {@link FlixelGamepadInput#AXIS_LEFT_X}.
   * @return Axis value in the range {@code [-1, 1]}, or {@code 0f} when inactive.
   */
  public float getAxis(int gamepadId, int logicalAxis) {
    float v = getAxisRaw(gamepadId, logicalAxis);
    float dz = deadZoneValue();
    if (Math.abs(v) <= dz) {
      return 0f;
    }
    return v;
  }

  /**
   * Model detected for the given slot the last time the slot was (re)bound.
   *
   * @param gamepadId Slot index.
   * @return Detected model, or {@link FlixelGamepadModel#UNKNOWN} when out of range or unrecognized.
   */
  @NotNull
  public FlixelGamepadModel getModel(int gamepadId) {
    if (gamepadId < 0 || gamepadId >= MAX_GAMEPADS) {
      return FlixelGamepadModel.UNKNOWN;
    }
    FlixelGamepadModel m = slotModel[gamepadId];
    return m != null ? m : FlixelGamepadModel.UNKNOWN;
  }

  /**
   * Replaces the haptics backend used by all vibration calls on this manager.
   *
   * <p>Each platform launcher installs a provider automatically: {@code FlixelLwjgl3Launcher}
   * installs {@code FlixelLwjgl3HapticsProvider} (Jamepad/SDL, true dual-motor), and
   * {@code FlixelTeaVMLauncher} installs {@code FlixelTeaVMHapticsProvider} (W3C Gamepad Haptics
   * API, true dual-motor). Only override this when you need platform-specific features that the
   * built-in providers do not cover, such as DualSense adaptive triggers or haptic patterns.
   *
   * @param provider Non-null replacement provider.
   * @throws NullPointerException If {@code provider} is {@code null}.
   */
  public void setHapticsProvider(@NotNull FlixelHapticsProvider provider) {
    hapticsProvider = Objects.requireNonNull(provider, "provider cannot be null.");
  }

  /**
   * Installs a platform-specific reader for analog button values, used to populate
   * {@link FlixelGamepadInput#AXIS_TRIGGER_L} and {@link FlixelGamepadInput#AXIS_TRIGGER_R} on
   * backends where triggers are exposed as buttons rather than axes (for example, the web W3C
   * Gamepad API).
   *
   * <p>{@code FlixelTeaVMAnalogButtonReader} (installed automatically by
   * {@code FlixelTeaVMLauncher}) is the only built-in implementation. Pass {@code null} to disable
   * analog button reading and fall back to the axis-only trigger behavior.
   *
   * @param reader Reader to install, or {@code null} to clear any existing reader.
   */
  public void setAnalogButtonReader(@Nullable FlixelAnalogButtonReader reader) {
    analogButtonReader = reader;
  }

  /**
   * Returns whether the controller in the given slot reports vibration support.
   *
   * @param slot Slot index.
   * @return {@code true} when the system is enabled, the slot is in range, and the hardware
   *     reports haptics capability.
   */
  public boolean canVibrate(int slot) {
    if (!enabled || slot < 0 || slot >= numActiveGamepads) {
      return false;
    }
    return hapticsProvider.canVibrate(slot);
  }

  /**
   * Vibrates the controller in the given slot at full intensity on both motors for the given
   * duration.
   *
   * <pre>{@code
   * // Short full-strength rumble on the first controller.
   * Flixel.gamepads.vibrate(0, 0.3f);
   * }</pre>
   *
   * @param slot Slot index.
   * @param durationSecs How long to vibrate in seconds.
   */
  public void vibrate(int slot, float durationSecs) {
    vibrate(slot, 1f, 1f, durationSecs);
  }

  /**
   * Vibrates the controller in the given slot at the given intensity on both motors.
   *
   * @param slot Slot index.
   * @param intensity Motor strength in the range {@code [0, 1]}.
   * @param durationSecs How long to vibrate in seconds.
   */
  public void vibrate(int slot, float intensity, float durationSecs) {
    vibrate(slot, intensity, intensity, durationSecs);
  }

  /**
   * Vibrates the controller in the given slot with independent left and right motor intensities.
   *
   * <pre>{@code
   * // Rumble only the left (low-frequency) motor at half strength for half a second.
   * Flixel.gamepads.vibrate(0, 0.5f, 0f, 0.5f);
   * }</pre>
   *
   * @param slot Slot index.
   * @param leftIntensity Strength for the left (low-frequency) motor, in the range {@code [0, 1]}.
   * @param rightIntensity Strength for the right (high-frequency) motor, in the range {@code [0, 1]}.
   * @param durationSecs How long to vibrate in seconds.
   */
  public void vibrate(int slot, float leftIntensity, float rightIntensity, float durationSecs) {
    if (!enabled || slot < 0 || slot >= numActiveGamepads) {
      return;
    }
    hapticsProvider.vibrate(slot, leftIntensity, rightIntensity, durationSecs);
  }

  /**
   * Returns the current analog pressure of the left trigger (L2) on the given slot, in the
   * range {@code [0, 1]}, after applying the global dead zone.
   *
   * <p>On the Jamepad/SDL desktop backend, triggers are reported as axes, so this reads the
   * raw trigger axis directly. On web (TeaVM/W3C Gamepad API), triggers are digital buttons;
   * this method returns {@code 0} there - use {@link #pressed(int, int)} with
   * {@link FlixelGamepadInput#L2} on web instead.
   *
   * <pre>{@code
   * float howHardL2 = Flixel.gamepads.getTriggerL(0);
   * }</pre>
   *
   * @param gamepadId Slot index.
   * @return Trigger pressure in {@code [0, 1]}, or {@code 0f} when inactive or within the dead zone.
   */
  public float getTriggerL(int gamepadId) {
    return getAxis(gamepadId, FlixelGamepadInput.AXIS_TRIGGER_L);
  }

  /**
   * Returns the current analog pressure of the right trigger (R2) on the given slot, in the
   * range {@code [0, 1]}, after applying the global dead zone.
   *
   * <p>On the Jamepad/SDL desktop backend, triggers are reported as axes, so this reads the
   * raw trigger axis directly. On web (TeaVM/W3C Gamepad API), triggers are digital buttons;
   * this method returns {@code 0} there - use {@link #pressed(int, int)} with
   * {@link FlixelGamepadInput#R2} on web instead.
   *
   * <pre>{@code
   * float howHardR2 = Flixel.gamepads.getTriggerR(0);
   * }</pre>
   *
   * @param gamepadId Slot index.
   * @return Trigger pressure in {@code [0, 1]}, or {@code 0f} when inactive or within the dead zone.
   */
  public float getTriggerR(int gamepadId) {
    return getAxis(gamepadId, FlixelGamepadInput.AXIS_TRIGGER_R);
  }

  /**
   * Stops any active vibration on the controller in the given slot immediately.
   *
   * @param slot Slot index.
   */
  public void stopVibration(int slot) {
    if (!enabled || slot < 0 || slot >= numActiveGamepads) {
      return;
    }
    hapticsProvider.stopVibration(slot);
  }

  boolean isSlotConnected(int id) {
    return id >= 0 && id < numActiveGamepads && slotController[id] != null;
  }

  /**
   * Returns the raw, underlying gdx-controllers {@link Controller} object at the given index.
   *
   * @param slot Slot index.
   * @return The gdx-controllers {@link Controller} object from the provided index, or {@code null}
   *     if it could not be found.
   */
  @Nullable
  public Controller controllerAt(int slot) {
    if (slot < 0 || slot >= MAX_GAMEPADS) {
      return null;
    }
    return slotController[slot];
  }

  @Override
  public void connected(@NotNull Controller controller) {
    syncControllers();
  }

  @Override
  public void disconnected(@NotNull Controller controller) {
    syncControllers();
  }

  @Override
  public boolean buttonDown(@NotNull Controller controller, int buttonIndex) {
    return false;
  }

  @Override
  public boolean buttonUp(@NotNull Controller controller, int buttonIndex) {
    return false;
  }

  @Override
  public boolean axisMoved(@NotNull Controller controller, int axisIndex, float value) {
    return false;
  }

  private float deadZoneValue() {
    return globalDeadZone != null ? globalDeadZone : 0f;
  }

  /**
   * Resolves a logical button to its native index, falling back to a synthetic trigger index for
   * L2 and R2 when the backend leaves their {@link ControllerMapping} entries as
   * {@link ControllerMapping#UNDEFINED}.
   */
  private int resolvedNativeButton(@NotNull Controller c, int logicalButton) {
    int code = FlixelGamepadInput.logicalButtonToNative(c, logicalButton);
    if (code != ControllerMapping.UNDEFINED) {
      return code;
    }
    if (logicalButton == FlixelGamepadInput.L2) {
      return SYNTHETIC_TRIGGER_L;
    }
    if (logicalButton == FlixelGamepadInput.R2) {
      return SYNTHETIC_TRIGGER_R;
    }
    return ControllerMapping.UNDEFINED;
  }

  private boolean slotHasTriggerActivity(int slot, @NotNull Controller c) {
    ControllerMapping m = c.getMapping();
    if (m.buttonL2 == ControllerMapping.UNDEFINED && currentButtons[slot][SYNTHETIC_TRIGGER_L]) {
      return true;
    }
    return m.buttonR2 == ControllerMapping.UNDEFINED && currentButtons[slot][SYNTHETIC_TRIGGER_R];
  }

  private void syncControllers() {
    Array<Controller> list;
    try {
      list = Controllers.getControllers();
    } catch (Throwable ignored) {
      numActiveGamepads = 0;
      return;
    }
    int n = Math.min(list.size, MAX_GAMEPADS);
    for (int i = 0; i < MAX_GAMEPADS; i++) {
      Controller newC = i < n ? list.get(i) : null;
      if (slotController[i] != newC) {
        if (slotController[i] != null) {
          disconnectPayload.set(i);
          deviceDisconnected.dispatch(disconnectPayload);
          clearSlot(i);
        }
        slotController[i] = newC;
        if (newC != null) {
          FlixelGamepadModel model = FlixelGamepadDetector.detect(newC);
          slotModel[i] = model;
          connectPayload.set(i, model);
          deviceConnected.dispatch(connectPayload);
        } else {
          slotModel[i] = FlixelGamepadModel.UNKNOWN;
        }
      }
    }
    numActiveGamepads = n;
  }

  private void clearSlot(int slot) {
    Arrays.fill(currentButtons[slot], false);
    Arrays.fill(previousButtons[slot], false);
    Arrays.fill(axisValues[slot], 0f);
  }

  private void pollHardware() {
    for (int s = 0; s < numActiveGamepads; s++) {
      Controller c = slotController[s];
      if (c == null) {
        continue;
      }
      int minB = c.getMinButtonIndex();
      int maxB = c.getMaxButtonIndex();
      for (int b = minB; b <= maxB; b++) {
        if (b >= 0 && b < MAX_BUTTONS) {
          currentButtons[s][b] = c.getButton(b);
        }
      }
      int ac = Math.min(c.getAxisCount(), MAX_AXES);
      for (int a = 0; a < ac; a++) {
        axisValues[s][a] = c.getAxis(a);
      }
      ControllerMapping m = c.getMapping();
      // On platforms where triggers are analog buttons (e.g. web W3C Gamepad API), populate the
      // trigger axis slots from the reader so that getTriggerL/R() returns pressure values.
      if (analogButtonReader != null) {
        if (m.buttonL2 != ControllerMapping.UNDEFINED) {
          axisValues[s][FlixelGamepadInput.AXIS_TRIGGER_L] = analogButtonReader.read(c, m.buttonL2);
        }
        if (m.buttonR2 != ControllerMapping.UNDEFINED) {
          axisValues[s][FlixelGamepadInput.AXIS_TRIGGER_R] = analogButtonReader.read(c, m.buttonR2);
        }
      }
      // On backends such as Jamepad/SDL, triggers are analog axes rather than digital buttons,
      // so ControllerMapping.buttonL2 and buttonR2 are UNDEFINED. Synthesize a boolean button
      // state from the trigger axis value so that pressed(), justPressed(), and justReleased()
      // work correctly on those backends.
      if (m.buttonL2 == ControllerMapping.UNDEFINED) {
        float v = FlixelGamepadInput.AXIS_TRIGGER_L < ac ? axisValues[s][FlixelGamepadInput.AXIS_TRIGGER_L] : 0f;
        currentButtons[s][SYNTHETIC_TRIGGER_L] = v > TRIGGER_BUTTON_THRESHOLD;
      }
      if (m.buttonR2 == ControllerMapping.UNDEFINED) {
        float v = FlixelGamepadInput.AXIS_TRIGGER_R < ac ? axisValues[s][FlixelGamepadInput.AXIS_TRIGGER_R] : 0f;
        currentButtons[s][SYNTHETIC_TRIGGER_R] = v > TRIGGER_BUTTON_THRESHOLD;
      }
    }
  }

  private boolean slotAnyPhysicalButton(int slot, @NotNull Controller c) {
    int min = c.getMinButtonIndex();
    int max = c.getMaxButtonIndex();
    for (int b = min; b <= max; b++) {
      if (b >= 0 && b < MAX_BUTTONS && currentButtons[slot][b]) {
        return true;
      }
    }
    return false;
  }

  private boolean slotHasAxisBeyondDeadzone(int slot, @NotNull Controller c) {
    float dz = deadZoneValue();
    int ax = FlixelGamepadInput.logicalAxisToNative(c, FlixelGamepadInput.AXIS_LEFT_X);
    if (isAxisActive(slot, ax, dz)) {
      return true;
    }
    ax = FlixelGamepadInput.logicalAxisToNative(c, FlixelGamepadInput.AXIS_LEFT_Y);
    if (isAxisActive(slot, ax, dz)) {
      return true;
    }
    ax = FlixelGamepadInput.logicalAxisToNative(c, FlixelGamepadInput.AXIS_RIGHT_X);
    if (isAxisActive(slot, ax, dz)) {
      return true;
    }
    ax = FlixelGamepadInput.logicalAxisToNative(c, FlixelGamepadInput.AXIS_RIGHT_Y);
    return isAxisActive(slot, ax, dz);
  }

  private boolean isAxisActive(int slot, int nativeAxis, float dz) {
    if (nativeAxis <= ControllerMapping.UNDEFINED || nativeAxis >= MAX_AXES) {
      return false;
    }
    return Math.abs(axisValues[slot][nativeAxis]) > dz;
  }

  private boolean slotHasAnalogOrButtonActivity(int slot) {
    Controller c = slotController[slot];
    if (c == null) {
      return false;
    }
    return slotAnyPhysicalButton(slot, c) || slotHasAxisBeyondDeadzone(slot, c)
        || slotHasTriggerActivity(slot, c);
  }

  private float getAxisRaw(int gamepadId, int logicalAxis) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return 0f;
    }
    Controller c = slotController[gamepadId];
    if (c == null) {
      return 0f;
    }
    int nat = FlixelGamepadInput.logicalAxisToNative(c, logicalAxis);
    if (nat < 0 || nat >= MAX_AXES) {
      return 0f;
    }
    return axisValues[gamepadId][nat];
  }

  /** Mutable payload reused for {@link #deviceConnected}; do not retain past the callback. */
  public static final class GamepadConnectedEvent {
    private int gamepadId;
    private FlixelGamepadModel model;

    public int gamepadId() {
      return gamepadId;
    }

    @NotNull
    public FlixelGamepadModel model() {
      return model;
    }

    void set(int gamepadId, @NotNull FlixelGamepadModel model) {
      this.gamepadId = gamepadId;
      this.model = model;
    }
  }

  /** Mutable payload reused for {@link #deviceDisconnected}; do not retain past the callback. */
  public static final class GamepadDisconnectedEvent {
    private int gamepadId;

    public int gamepadId() {
      return gamepadId;
    }

    void set(int gamepadId) {
      this.gamepadId = gamepadId;
    }
  }
}
