/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.gamepad;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.input.FlixelInputManager;
import me.stringdotjar.flixelgdx.util.signal.FlixelSignal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Global gamepad manager backed by gdx-controllers. Polls controllers each frame and mirrors the
 * keyboard and mouse frame contract from {@link me.stringdotjar.flixelgdx.FlixelGame}.
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

  /** When {@code false}, all queries return inactive state. */
  public boolean enabled = true;

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

  public boolean pressed(int gamepadId, int logicalButton) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return false;
    }

    Controller c = slotController[gamepadId];
    if (c == null) {
      return false;
    }
    if (logicalButton == FlixelGamepadInput.ANY) {
      return slotAnyPhysicalButton(gamepadId, c) || slotHasAxisBeyondDeadzone(gamepadId, c);
    }

    int nativeCode = FlixelGamepadInput.logicalButtonToNative(c, logicalButton);
    if (nativeCode == ControllerMapping.UNDEFINED || nativeCode < 0 || nativeCode >= MAX_BUTTONS) {
      return false;
    }
    return currentButtons[gamepadId][nativeCode];
  }

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

    int nativeCode = FlixelGamepadInput.logicalButtonToNative(c, logicalButton);
    if (nativeCode == ControllerMapping.UNDEFINED || nativeCode < 0 || nativeCode >= MAX_BUTTONS) {
      return false;
    }
    return currentButtons[gamepadId][nativeCode] && !previousButtons[gamepadId][nativeCode];
  }

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

    int nativeCode = FlixelGamepadInput.logicalButtonToNative(c, logicalButton);
    if (nativeCode == ControllerMapping.UNDEFINED || nativeCode < 0 || nativeCode >= MAX_BUTTONS) {
      return false;
    }
    return !currentButtons[gamepadId][nativeCode] && previousButtons[gamepadId][nativeCode];
  }

  public float getAxis(int gamepadId, int logicalAxis) {
    float v = getAxisRaw(gamepadId, logicalAxis);
    float dz = deadZoneValue();
    if (Math.abs(v) <= dz) {
      return 0f;
    }
    return v;
  }

  @NotNull
  public FlixelGamepadModel getDetectedModel(int gamepadId) {
    if (gamepadId < 0 || gamepadId >= MAX_GAMEPADS) {
      return FlixelGamepadModel.UNKNOWN;
    }
    FlixelGamepadModel m = slotModel[gamepadId];
    return m != null ? m : FlixelGamepadModel.UNKNOWN;
  }

  boolean isSlotConnected(int id) {
    return id >= 0 && id < numActiveGamepads && slotController[id] != null;
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
          FlixelGamepadModel model = FlixelGamepadDetector.detect(newC.getName());
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
    if (nativeAxis == ControllerMapping.UNDEFINED || nativeAxis < 0 || nativeAxis >= MAX_AXES) {
      return false;
    }
    return Math.abs(axisValues[slot][nativeAxis]) > dz;
  }

  private boolean slotHasAnalogOrButtonActivity(int slot) {
    Controller c = slotController[slot];
    if (c == null) {
      return false;
    }
    return slotAnyPhysicalButton(slot, c) || slotHasAxisBeyondDeadzone(slot, c);
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
    if (nat == ControllerMapping.UNDEFINED || nat < 0 || nat >= MAX_AXES) {
      return 0f;
    }
    return axisValues[gamepadId][nat];
  }
}
