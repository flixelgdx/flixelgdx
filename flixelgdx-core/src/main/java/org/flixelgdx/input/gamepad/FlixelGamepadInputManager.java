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

import org.flixelgdx.FlixelGame;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelIntArray;
import org.flixelgdx.input.FlixelInputManager;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Global gamepad manager. Polls connected gamepads each frame through the active
 * {@link FlixelGamepadProvider} and mirrors the keyboard and mouse frame contract from
 * {@link FlixelGame}.
 *
 * <p>The gamepad system is <strong>enabled by default</strong>. Set {@link #enabled} to
 * {@code false} if a game never uses controllers and wants to skip polling entirely:
 *
 * <pre>{@code
 * Flixel.gamepads.enabled = false;
 * }</pre>
 *
 * <p>Use logical button and axis tokens from {@link FlixelGamepadButton} and {@link FlixelGamepadAxis}
 * (for example {@link FlixelGamepadButton#A}) with {@link #pressed(int, FlixelGamepadButton)}; each
 * slot's {@link FlixelGamepadMapping} (resolved at connect time) supplies native indices. The same
 * tokens are used to build custom mappings, so a button minted with {@link FlixelGamepadButton#of(String)}
 * can be registered and then polled through the exact same calls as the built-in buttons.
 * {@link FlixelGamepadDevice} is optional; call {@link #ensureDevice(int)} once per slot you want
 * a facade for.
 *
 * <h2>Custom mappings</h2>
 *
 * <p>The manager consults a {@link FlixelGamepadMappingResolver} chain when a gamepad connects.
 * Add resolvers with {@link #addMappingResolver} to support exotic or custom hardware without the
 * framework needing to know it exists. User-added resolvers are checked first and take priority
 * over the framework's built-in resolver.
 */
public class FlixelGamepadInputManager implements FlixelInputManager, FlixelGamepadListener {

  /** Maximum supported simultaneous gamepads. */
  public static final int MAX_GAMEPADS = 8;

  private static final int MAX_BUTTONS = 256;
  private static final int MAX_AXES = 64;

  // Reserved button slots for synthesized trigger state. Used when the backend reports L2/R2 as
  // analog axes (e.g. Jamepad/SDL), leaving FlixelGamepadButton.L2/R2 unmapped as buttons.
  private static final int SYNTHETIC_TRIGGER_L = 249;
  private static final int SYNTHETIC_TRIGGER_R = 250;
  private static final float TRIGGER_BUTTON_THRESHOLD = 0.5f;

  /** Number of gamepads mapped to IDs {@code 0 .. numActiveGamepads-1} this frame. */
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

  private final FlixelGamepad[] slotGamepads = new FlixelGamepad[MAX_GAMEPADS];
  private final FlixelGamepadMapping[] slotMappings = new FlixelGamepadMapping[MAX_GAMEPADS];
  private final FlixelGamepadModel[] slotModel = new FlixelGamepadModel[MAX_GAMEPADS];

  private final boolean[][] currentButtons = new boolean[MAX_GAMEPADS][MAX_BUTTONS];
  private final boolean[][] previousButtons = new boolean[MAX_GAMEPADS][MAX_BUTTONS];

  private final float[][] axisValues = new float[MAX_GAMEPADS][MAX_AXES];
  private final float[] triggerL = new float[MAX_GAMEPADS];
  private final float[] triggerR = new float[MAX_GAMEPADS];
  private final FlixelIntArray[] pressedOrder = new FlixelIntArray[MAX_GAMEPADS];

  @Nullable
  private final FlixelGamepadDevice[] ensuredDevices = new FlixelGamepadDevice[MAX_GAMEPADS];

  @NotNull
  private FlixelGamepadHapticsProvider hapticsProvider = new FlixelDefaultGamepadHapticsProvider(this);

  @Nullable
  private FlixelGamepadAnalogButtonReader analogButtonReader;

  @NotNull
  private FlixelGamepadProvider gamepadProvider = FlixelNoopGamepadProvider.INSTANCE;

  private final FlixelArray<FlixelGamepadMappingResolver> mappingResolvers = new FlixelArray<>();

  /**
   * Whether the gamepad system is active. When {@code false}, all queries return inactive state
   * and no hardware is polled. Defaults to {@code true}; set to {@code false} to skip controller
   * polling entirely.
   */
  public boolean enabled = true;

  private boolean listenerAttached;

  /** Creates a new gamepad input manager with empty state for all supported gamepad slots. */
  public FlixelGamepadInputManager() {
    for (int i = 0; i < MAX_GAMEPADS; i++) {
      pressedOrder[i] = new FlixelIntArray();
    }
  }

  /**
   * Registers this manager as a listener on the active {@link FlixelGamepadProvider}. Safe to
   * call more than once.
   */
  public void attach() {
    if (listenerAttached) {
      return;
    }
    gamepadProvider.addListener(this);
    listenerAttached = true;
  }

  /** Unregisters listeners and clears internal slot state. */
  public void detach() {
    if (listenerAttached) {
      gamepadProvider.removeListener(this);
      listenerAttached = false;
    }
    reset();
  }

  /**
   * Installs the platform's gamepad source. Each backend launcher installs one automatically at
   * startup. Until then the manager sees no gamepads and reports zero active slots.
   *
   * @param provider Non-null gamepad source.
   * @throws NullPointerException If {@code provider} is {@code null}.
   */
  public void setGamepadProvider(@NotNull FlixelGamepadProvider provider) {
    boolean wasAttached = listenerAttached;
    if (wasAttached) {
      detach();
    }
    gamepadProvider = Objects.requireNonNull(provider, "provider cannot be null.");
    if (wasAttached) {
      attach();
    }
  }

  /**
   * Adds a mapping resolver to the front of the resolver chain.
   *
   * <p>Resolvers added by user code are checked before any framework-installed resolvers, so a
   * user-supplied resolver always wins when it returns a non-null result.
   *
   * @param resolver The resolver to add; must not be {@code null}.
   */
  public void addMappingResolver(@NotNull FlixelGamepadMappingResolver resolver) {
    Objects.requireNonNull(resolver, "resolver cannot be null.");
    mappingResolvers.insert(0, resolver);
  }

  /**
   * Removes a previously added mapping resolver from the chain.
   *
   * @param resolver The resolver to remove.
   */
  public void removeMappingResolver(@NotNull FlixelGamepadMappingResolver resolver) {
    mappingResolvers.removeValue(resolver, true);
  }

  @Override
  public void reset() {
    numActiveGamepads = 0;
    Arrays.fill(slotGamepads, null);
    Arrays.fill(slotMappings, null);
    Arrays.fill(slotModel, FlixelGamepadModel.UNKNOWN);
    Arrays.fill(ensuredDevices, null);
    Arrays.fill(triggerL, 0f);
    Arrays.fill(triggerR, 0f);
    for (int i = 0; i < MAX_GAMEPADS; i++) {
      Arrays.fill(currentButtons[i], false);
      Arrays.fill(previousButtons[i], false);
      Arrays.fill(axisValues[i], 0f);
      pressedOrder[i].clear();
    }
  }

  @Override
  public void update() {
    if (!enabled) {
      return;
    }
    attach();
    syncGamepads();
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
   * @param button A logical button token from {@link FlixelGamepadButton}, such as
   *     {@link FlixelGamepadButton#A}.
   * @return {@code true} when at least one gamepad is pressing the button this frame.
   */
  public boolean anyPressed(@NotNull FlixelGamepadButton button) {
    if (!enabled) {
      return false;
    }
    for (int s = 0; s < numActiveGamepads; s++) {
      if (pressed(s, button)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when any active gamepad first pressed the given button this frame.
   *
   * @param button A logical button token from {@link FlixelGamepadButton}.
   * @return {@code true} when at least one gamepad transitioned to pressed this frame.
   */
  public boolean anyJustPressed(@NotNull FlixelGamepadButton button) {
    if (!enabled) {
      return false;
    }
    for (int s = 0; s < numActiveGamepads; s++) {
      if (justPressed(s, button)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when any active gamepad released the given button this frame.
   *
   * @param button A logical button token from {@link FlixelGamepadButton}.
   * @return {@code true} when at least one gamepad transitioned to released this frame.
   */
  public boolean anyJustReleased(@NotNull FlixelGamepadButton button) {
    if (!enabled) {
      return false;
    }
    for (int s = 0; s < numActiveGamepads; s++) {
      if (justReleased(s, button)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when any active gamepad has a button pressed or an analog input beyond
   * the dead zone this frame.
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
   * Returns {@code true} when any active gamepad is moving a left or right stick horizontally
   * beyond the dead zone this frame.
   *
   * @return {@code true} when at least one gamepad has horizontal stick movement.
   */
  public boolean anyMovedXAxis() {
    if (!enabled) {
      return false;
    }
    float dz = deadZoneValue();
    for (int s = 0; s < numActiveGamepads; s++) {
      if (Math.abs(getAxisRaw(s, FlixelGamepadAxis.LEFT_X)) > dz) {
        return true;
      }
      if (Math.abs(getAxisRaw(s, FlixelGamepadAxis.RIGHT_X)) > dz) {
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
      if (Math.abs(getAxisRaw(s, FlixelGamepadAxis.LEFT_Y)) > dz) {
        return true;
      }
      if (Math.abs(getAxisRaw(s, FlixelGamepadAxis.RIGHT_Y)) > dz) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when the given slot is currently pressing the given button.
   *
   * <p>Pass {@link FlixelGamepadButton#ANY} as the button to check whether the slot has any
   * button pressed or meaningful analog movement this frame.
   *
   * @param gamepadId Slot index.
   * @param button A logical button token from {@link FlixelGamepadButton}.
   * @return {@code true} when that button is pressed on the given slot this frame.
   */
  public boolean pressed(int gamepadId, @NotNull FlixelGamepadButton button) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return false;
    }

    FlixelGamepad g = slotGamepads[gamepadId];
    if (g == null) {
      return false;
    }
    if (button == FlixelGamepadButton.ANY) {
      return slotAnyPhysicalButton(gamepadId, g) || slotHasAxisBeyondDeadzone(gamepadId)
          || slotHasTriggerActivity(gamepadId);
    }

    int nativeCode = resolvedNativeButton(gamepadId, button);
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
   * @param button A logical button token from {@link FlixelGamepadButton}.
   * @return {@code true} on the first frame the button is pressed.
   */
  public boolean justPressed(int gamepadId, @NotNull FlixelGamepadButton button) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return false;
    }

    FlixelGamepad g = slotGamepads[gamepadId];
    if (g == null) {
      return false;
    }

    if (button == FlixelGamepadButton.ANY) {
      int min = g.getMinButtonIndex();
      int max = g.getMaxButtonIndex();
      for (int b = min; b <= max; b++) {
        if (b >= 0 && b < MAX_BUTTONS && currentButtons[gamepadId][b] && !previousButtons[gamepadId][b]) {
          return true;
        }
      }
      return false;
    }

    int nativeCode = resolvedNativeButton(gamepadId, button);
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
   * @param button A logical button token from {@link FlixelGamepadButton}.
   * @return {@code true} on the first frame the button is no longer pressed.
   */
  public boolean justReleased(int gamepadId, @NotNull FlixelGamepadButton button) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return false;
    }

    FlixelGamepad g = slotGamepads[gamepadId];
    if (g == null) {
      return false;
    }

    if (button == FlixelGamepadButton.ANY) {
      int min = g.getMinButtonIndex();
      int max = g.getMaxButtonIndex();
      for (int b = min; b <= max; b++) {
        if (b >= 0 && b < MAX_BUTTONS && !currentButtons[gamepadId][b] && previousButtons[gamepadId][b]) {
          return true;
        }
      }
      return false;
    }

    int nativeCode = resolvedNativeButton(gamepadId, button);
    if (nativeCode < 0 || nativeCode >= MAX_BUTTONS) {
      return false;
    }
    return !currentButtons[gamepadId][nativeCode] && previousButtons[gamepadId][nativeCode];
  }

  /**
   * Returns the logical button that has been held the longest on the given slot, or
   * {@link FlixelGamepadButton#NONE} when no button is currently held.
   *
   * <p>Press order is tracked across frames: the first physical button detected as pressed since
   * the slot was connected (or last cleared) is always returned first, regardless of how many
   * buttons are held simultaneously.
   *
   * <pre>{@code
   * FlixelGamepadButton btn = Flixel.gamepads.firstPressed(0);
   * if (btn != FlixelGamepadButton.NONE) {
   *   System.out.println("Oldest held button: " + btn);
   * }
   * }</pre>
   *
   * @param gamepadId Slot index.
   * @return Logical button token from {@link FlixelGamepadButton}, or {@link FlixelGamepadButton#NONE}
   *     when no button is held or the slot is inactive.
   */
  @NotNull
  public FlixelGamepadButton firstPressed(int gamepadId) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return FlixelGamepadButton.NONE;
    }
    FlixelGamepadMapping m = slotMappings[gamepadId];
    if (m == null) {
      return FlixelGamepadButton.NONE;
    }
    FlixelIntArray order = pressedOrder[gamepadId];
    if (order.getSize() == 0) {
      return FlixelGamepadButton.NONE;
    }
    return nativeButtonToButton(m, order.first());
  }

  /**
   * Returns the logical button that transitioned to pressed this frame on the given slot, or
   * {@link FlixelGamepadButton#NONE} when no button was just pressed.
   *
   * <p>When multiple buttons are pressed in the same frame, the one with the lowest native index
   * is returned. Native buttons the mapping does not name are skipped.
   *
   * <pre>{@code
   * FlixelGamepadButton btn = Flixel.gamepads.firstJustPressed(0);
   * if (btn == FlixelGamepadButton.A) {
   *   jump();
   * }
   * }</pre>
   *
   * @param gamepadId Slot index.
   * @return Logical button token from {@link FlixelGamepadButton}, or {@link FlixelGamepadButton#NONE}
   *     when no button was just pressed or the slot is inactive.
   */
  @NotNull
  public FlixelGamepadButton firstJustPressed(int gamepadId) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return FlixelGamepadButton.NONE;
    }
    FlixelGamepad g = slotGamepads[gamepadId];
    FlixelGamepadMapping m = slotMappings[gamepadId];
    if (g == null || m == null) {
      return FlixelGamepadButton.NONE;
    }
    int min = g.getMinButtonIndex();
    int max = g.getMaxButtonIndex();
    for (int b = min; b <= max; b++) {
      if (b >= 0 && b < MAX_BUTTONS && currentButtons[gamepadId][b] && !previousButtons[gamepadId][b]) {
        FlixelGamepadButton button = nativeButtonToButton(m, b);
        if (button != FlixelGamepadButton.NONE) {
          return button;
        }
      }
    }
    if (m.getButtonIndex(FlixelGamepadButton.L2) == FlixelGamepadMapping.UNDEFINED
        && currentButtons[gamepadId][SYNTHETIC_TRIGGER_L]
        && !previousButtons[gamepadId][SYNTHETIC_TRIGGER_L]) {
      return FlixelGamepadButton.L2;
    }
    if (m.getButtonIndex(FlixelGamepadButton.R2) == FlixelGamepadMapping.UNDEFINED
        && currentButtons[gamepadId][SYNTHETIC_TRIGGER_R]
        && !previousButtons[gamepadId][SYNTHETIC_TRIGGER_R]) {
      return FlixelGamepadButton.R2;
    }
    return FlixelGamepadButton.NONE;
  }

  /**
   * Returns the logical button that was released this frame on the given slot, or
   * {@link FlixelGamepadButton#NONE} when no button was just released.
   *
   * <p>When multiple buttons are released in the same frame, the one with the lowest native index
   * is returned. Native buttons the mapping does not name are skipped.
   *
   * <pre>{@code
   * FlixelGamepadButton btn = Flixel.gamepads.firstJustReleased(0);
   * if (btn == FlixelGamepadButton.A) {
   *   stopCharge();
   * }
   * }</pre>
   *
   * @param gamepadId Slot index.
   * @return Logical button token from {@link FlixelGamepadButton}, or {@link FlixelGamepadButton#NONE}
   *     when no button was just released or the slot is inactive.
   */
  @NotNull
  public FlixelGamepadButton firstJustReleased(int gamepadId) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return FlixelGamepadButton.NONE;
    }
    FlixelGamepad g = slotGamepads[gamepadId];
    FlixelGamepadMapping m = slotMappings[gamepadId];
    if (g == null || m == null) {
      return FlixelGamepadButton.NONE;
    }
    int min = g.getMinButtonIndex();
    int max = g.getMaxButtonIndex();
    for (int b = min; b <= max; b++) {
      if (b >= 0 && b < MAX_BUTTONS && !currentButtons[gamepadId][b] && previousButtons[gamepadId][b]) {
        FlixelGamepadButton button = nativeButtonToButton(m, b);
        if (button != FlixelGamepadButton.NONE) {
          return button;
        }
      }
    }
    if (m.getButtonIndex(FlixelGamepadButton.L2) == FlixelGamepadMapping.UNDEFINED
        && !currentButtons[gamepadId][SYNTHETIC_TRIGGER_L]
        && previousButtons[gamepadId][SYNTHETIC_TRIGGER_L]) {
      return FlixelGamepadButton.L2;
    }
    if (m.getButtonIndex(FlixelGamepadButton.R2) == FlixelGamepadMapping.UNDEFINED
        && !currentButtons[gamepadId][SYNTHETIC_TRIGGER_R]
        && previousButtons[gamepadId][SYNTHETIC_TRIGGER_R]) {
      return FlixelGamepadButton.R2;
    }
    return FlixelGamepadButton.NONE;
  }

  /**
   * Returns the current value of a logical axis on the given slot, after applying the global dead
   * zone. Returns {@code 0f} when the value is within the dead zone or the slot is out of range.
   *
   * @param gamepadId Slot index.
   * @param axis A logical axis token from {@link FlixelGamepadAxis}, such as
   *     {@link FlixelGamepadAxis#LEFT_X}.
   * @return Axis value in the range {@code [-1, 1]}, or {@code 0f} when inactive.
   */
  public float getAxis(int gamepadId, @NotNull FlixelGamepadAxis axis) {
    float v = getAxisRaw(gamepadId, axis);
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
   * built-in providers do not cover.
   *
   * @param provider Non-null replacement provider.
   * @throws NullPointerException If {@code provider} is {@code null}.
   */
  public void setHapticsProvider(@NotNull FlixelGamepadHapticsProvider provider) {
    hapticsProvider = Objects.requireNonNull(provider, "provider cannot be null.");
  }

  /**
   * Installs a platform-specific reader for analog button values, used to populate trigger
   * pressure on backends where L2 and R2 are exposed as buttons rather than axes (for example,
   * the web W3C Gamepad API).
   *
   * <p>{@code FlixelTeaVMAnalogButtonReader} (installed automatically by
   * {@code FlixelTeaVMLauncher}) is the only built-in implementation. Pass {@code null} to disable
   * analog button reading and fall back to the axis-only trigger behavior.
   *
   * @param reader Reader to install, or {@code null} to clear any existing reader.
   */
  public void setAnalogButtonReader(@Nullable FlixelGamepadAnalogButtonReader reader) {
    analogButtonReader = reader;
  }

  /**
   * Returns whether the gamepad in the given slot reports vibration support.
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
   * Vibrates the gamepad in the given slot at full intensity on both motors for the given
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
   * Vibrates the gamepad in the given slot at the given intensity on both motors.
   *
   * @param slot Slot index.
   * @param intensity Motor strength in the range {@code [0, 1]}.
   * @param durationSecs How long to vibrate in seconds.
   */
  public void vibrate(int slot, float intensity, float durationSecs) {
    vibrate(slot, intensity, intensity, durationSecs);
  }

  /**
   * Vibrates the gamepad in the given slot with independent left and right motor intensities.
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
   * pressure is read through the analog button reader installed at startup.
   *
   * <pre>{@code
   * float howHardL2 = Flixel.gamepads.getTriggerL(0);
   * }</pre>
   *
   * @param gamepadId Slot index.
   * @return Trigger pressure in {@code [0, 1]}, or {@code 0f} when inactive or within dead zone.
   */
  public float getTriggerL(int gamepadId) {
    return getAxis(gamepadId, FlixelGamepadAxis.L2);
  }

  /**
   * Returns the current analog pressure of the right trigger (R2) on the given slot, in the
   * range {@code [0, 1]}, after applying the global dead zone.
   *
   * <pre>{@code
   * float howHardR2 = Flixel.gamepads.getTriggerR(0);
   * }</pre>
   *
   * @param gamepadId Slot index.
   * @return Trigger pressure in {@code [0, 1]}, or {@code 0f} when inactive or within dead zone.
   */
  public float getTriggerR(int gamepadId) {
    return getAxis(gamepadId, FlixelGamepadAxis.R2);
  }

  /**
   * Stops any active vibration on the gamepad in the given slot immediately.
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
    return id >= 0 && id < numActiveGamepads && slotGamepads[id] != null;
  }

  /**
   * Returns the {@link FlixelGamepad} bound to the given slot, or {@code null} when none.
   *
   * <p>This is the escape hatch for advanced or platform-specific gamepad work (haptics providers
   * use it, and {@link FlixelGamepad#getNativeHandle()} reaches the raw backend controller from
   * here). Ordinary games use the higher-level query methods on this manager instead.
   *
   * @param slot Slot index.
   * @return The gamepad at the given slot, or {@code null} if there is none.
   */
  @Nullable
  public FlixelGamepad gamepadAt(int slot) {
    if (slot < 0 || slot >= MAX_GAMEPADS) {
      return null;
    }
    return slotGamepads[slot];
  }

  /**
   * Returns the resolved {@link FlixelGamepadMapping} for the given slot, or {@code null} when
   * none. Useful for resolver implementations and advanced diagnostics.
   *
   * @param slot Slot index.
   * @return The mapping for that slot, or {@code null} when the slot is empty.
   */
  @Nullable
  public FlixelGamepadMapping mappingAt(int slot) {
    if (slot < 0 || slot >= MAX_GAMEPADS) {
      return null;
    }
    return slotMappings[slot];
  }

  @Override
  public void connected(@NotNull FlixelGamepad gamepad) {
    syncGamepads();
  }

  @Override
  public void disconnected(@NotNull FlixelGamepad gamepad) {
    syncGamepads();
  }

  @Override
  public boolean buttonDown(@NotNull FlixelGamepad gamepad, int buttonIndex) {
    return false;
  }

  @Override
  public boolean buttonUp(@NotNull FlixelGamepad gamepad, int buttonIndex) {
    return false;
  }

  @Override
  public boolean axisMoved(@NotNull FlixelGamepad gamepad, int axisIndex, float value) {
    return false;
  }

  private float deadZoneValue() {
    return globalDeadZone != null ? globalDeadZone : 0f;
  }

  @NotNull
  private FlixelGamepadButton nativeButtonToButton(@NotNull FlixelGamepadMapping m, int nativeButton) {
    if (nativeButton == SYNTHETIC_TRIGGER_L) {
      return FlixelGamepadButton.L2;
    }
    if (nativeButton == SYNTHETIC_TRIGGER_R) {
      return FlixelGamepadButton.R2;
    }
    FlixelGamepadButton button = m.getButtonForIndex(nativeButton);
    return button != null ? button : FlixelGamepadButton.NONE;
  }

  private int resolvedNativeButton(int gamepadId, @NotNull FlixelGamepadButton button) {
    FlixelGamepadMapping m = slotMappings[gamepadId];
    if (m == null) {
      return FlixelGamepadMapping.UNDEFINED;
    }
    int code = m.getButtonIndex(button);
    if (code != FlixelGamepadMapping.UNDEFINED) {
      return code;
    }
    // The mapping does not name L2/R2 as buttons when the backend reports them as analog axes;
    // fall back to the synthesized trigger button slots the poll loop maintains.
    if (button == FlixelGamepadButton.L2) {
      return SYNTHETIC_TRIGGER_L;
    }
    if (button == FlixelGamepadButton.R2) {
      return SYNTHETIC_TRIGGER_R;
    }
    return FlixelGamepadMapping.UNDEFINED;
  }

  private boolean slotHasTriggerActivity(int slot) {
    FlixelGamepadMapping m = slotMappings[slot];
    if (m == null) {
      return false;
    }
    if (m.getButtonIndex(FlixelGamepadButton.L2) == FlixelGamepadMapping.UNDEFINED
        && currentButtons[slot][SYNTHETIC_TRIGGER_L]) {
      return true;
    }
    return m.getButtonIndex(FlixelGamepadButton.R2) == FlixelGamepadMapping.UNDEFINED
        && currentButtons[slot][SYNTHETIC_TRIGGER_R];
  }

  private void syncGamepads() {
    int n = Math.min(gamepadProvider.getGamepadCount(), MAX_GAMEPADS);
    for (int i = 0; i < MAX_GAMEPADS; i++) {
      FlixelGamepad newG = i < n ? gamepadProvider.getGamepadAt(i) : null;
      if (slotGamepads[i] != newG) {
        if (slotGamepads[i] != null) {
          disconnectPayload.set(i);
          deviceDisconnected.dispatch(disconnectPayload);
          clearSlot(i);
        }
        slotGamepads[i] = newG;
        if (newG != null) {
          slotMappings[i] = resolveMapping(newG);
          FlixelGamepadModel model = FlixelGamepadDetector.detect(newG);
          slotModel[i] = model;
          connectPayload.set(i, model);
          deviceConnected.dispatch(connectPayload);
        } else {
          slotMappings[i] = null;
          slotModel[i] = FlixelGamepadModel.UNKNOWN;
        }
      }
    }
    numActiveGamepads = n;
  }

  private FlixelGamepadMapping resolveMapping(@NotNull FlixelGamepad gamepad) {
    Object[] resolvers = mappingResolvers.getItems();
    int count = mappingResolvers.getSize();
    for (int i = 0; i < count; i++) {
      FlixelGamepadMappingResolver resolver = (FlixelGamepadMappingResolver) resolvers[i];
      FlixelGamepadMapping result = resolver.resolve(gamepad);
      if (result != null) {
        return result;
      }
    }
    return new FlixelGamepadMapping();
  }

  private void clearSlot(int slot) {
    Arrays.fill(currentButtons[slot], false);
    Arrays.fill(previousButtons[slot], false);
    Arrays.fill(axisValues[slot], 0f);
    triggerL[slot] = 0f;
    triggerR[slot] = 0f;
    pressedOrder[slot].clear();
  }

  private void pollHardware() {
    for (int s = 0; s < numActiveGamepads; s++) {
      FlixelGamepad g = slotGamepads[s];
      if (g == null) {
        continue;
      }
      FlixelGamepadMapping m = slotMappings[s];
      if (m == null) {
        continue;
      }

      int minB = g.getMinButtonIndex();
      int maxB = g.getMaxButtonIndex();
      for (int b = minB; b <= maxB; b++) {
        if (b >= 0 && b < MAX_BUTTONS) {
          boolean newState = g.getButton(b);
          if (newState && !previousButtons[s][b]) {
            if (pressedOrder[s].indexOf(b) < 0) {
              pressedOrder[s].add(b);
            }
          } else if (!newState && previousButtons[s][b]) {
            pressedOrder[s].removeValue(b);
          }
          currentButtons[s][b] = newState;
        }
      }

      int ac = Math.min(g.getAxisCount(), MAX_AXES);
      for (int a = 0; a < ac; a++) {
        axisValues[s][a] = g.getAxis(a);
      }

      // Populate trigger pressure from the mapping.
      int l2Button = m.getButtonIndex(FlixelGamepadButton.L2);
      int r2Button = m.getButtonIndex(FlixelGamepadButton.R2);
      int l2Axis = m.getAxisIndex(FlixelGamepadAxis.L2);
      int r2Axis = m.getAxisIndex(FlixelGamepadAxis.R2);

      if (l2Button != FlixelGamepadMapping.UNDEFINED && analogButtonReader != null) {
        triggerL[s] = analogButtonReader.read(g, l2Button);
      } else if (l2Axis != FlixelGamepadMapping.UNDEFINED && l2Axis < ac) {
        triggerL[s] = axisValues[s][l2Axis];
      } else {
        triggerL[s] = 0f;
      }

      if (r2Button != FlixelGamepadMapping.UNDEFINED && analogButtonReader != null) {
        triggerR[s] = analogButtonReader.read(g, r2Button);
      } else if (r2Axis != FlixelGamepadMapping.UNDEFINED && r2Axis < ac) {
        triggerR[s] = axisValues[s][r2Axis];
      } else {
        triggerR[s] = 0f;
      }

      // Synthesize button state for triggers that are analog axes (e.g. Jamepad/SDL).
      if (l2Button == FlixelGamepadMapping.UNDEFINED) {
        boolean newTriggerL = triggerL[s] > TRIGGER_BUTTON_THRESHOLD;
        if (newTriggerL && !previousButtons[s][SYNTHETIC_TRIGGER_L]) {
          if (pressedOrder[s].indexOf(SYNTHETIC_TRIGGER_L) < 0) {
            pressedOrder[s].add(SYNTHETIC_TRIGGER_L);
          }
        } else if (!newTriggerL && previousButtons[s][SYNTHETIC_TRIGGER_L]) {
          pressedOrder[s].removeValue(SYNTHETIC_TRIGGER_L);
        }
        currentButtons[s][SYNTHETIC_TRIGGER_L] = newTriggerL;
      }

      if (r2Button == FlixelGamepadMapping.UNDEFINED) {
        boolean newTriggerR = triggerR[s] > TRIGGER_BUTTON_THRESHOLD;
        if (newTriggerR && !previousButtons[s][SYNTHETIC_TRIGGER_R]) {
          if (pressedOrder[s].indexOf(SYNTHETIC_TRIGGER_R) < 0) {
            pressedOrder[s].add(SYNTHETIC_TRIGGER_R);
          }
        } else if (!newTriggerR && previousButtons[s][SYNTHETIC_TRIGGER_R]) {
          pressedOrder[s].removeValue(SYNTHETIC_TRIGGER_R);
        }
        currentButtons[s][SYNTHETIC_TRIGGER_R] = newTriggerR;
      }
    }
  }

  private boolean slotAnyPhysicalButton(int slot, @NotNull FlixelGamepad g) {
    int min = g.getMinButtonIndex();
    int max = g.getMaxButtonIndex();
    for (int b = min; b <= max; b++) {
      if (b >= 0 && b < MAX_BUTTONS && currentButtons[slot][b]) {
        return true;
      }
    }
    return false;
  }

  private boolean slotHasAxisBeyondDeadzone(int slot) {
    FlixelGamepadMapping m = slotMappings[slot];
    if (m == null) {
      return false;
    }
    float dz = deadZoneValue();
    if (isAxisActive(slot, m.getAxisIndex(FlixelGamepadAxis.LEFT_X), dz)) {
      return true;
    }
    if (isAxisActive(slot, m.getAxisIndex(FlixelGamepadAxis.LEFT_Y), dz)) {
      return true;
    }
    if (isAxisActive(slot, m.getAxisIndex(FlixelGamepadAxis.RIGHT_X), dz)) {
      return true;
    }
    return isAxisActive(slot, m.getAxisIndex(FlixelGamepadAxis.RIGHT_Y), dz);
  }

  private boolean isAxisActive(int slot, int nativeAxis, float dz) {
    if (nativeAxis <= FlixelGamepadMapping.UNDEFINED || nativeAxis >= MAX_AXES) {
      return false;
    }
    return Math.abs(axisValues[slot][nativeAxis]) > dz;
  }

  private boolean slotHasAnalogOrButtonActivity(int slot) {
    FlixelGamepad g = slotGamepads[slot];
    if (g == null) {
      return false;
    }
    return slotAnyPhysicalButton(slot, g) || slotHasAxisBeyondDeadzone(slot)
        || slotHasTriggerActivity(slot);
  }

  private float getAxisRaw(int gamepadId, @NotNull FlixelGamepadAxis axis) {
    if (!enabled || gamepadId < 0 || gamepadId >= numActiveGamepads) {
      return 0f;
    }
    // Triggers are surfaced through the per-slot trigger pressure the poll loop maintains, so they
    // read consistently whether the backend exposes them as axes (desktop) or buttons (web).
    if (axis == FlixelGamepadAxis.L2) {
      return triggerL[gamepadId];
    }
    if (axis == FlixelGamepadAxis.R2) {
      return triggerR[gamepadId];
    }
    FlixelGamepadMapping m = slotMappings[gamepadId];
    if (m == null) {
      return 0f;
    }
    int nat = m.getAxisIndex(axis);
    if (nat < 0 || nat >= MAX_AXES) {
      return 0f;
    }
    return axisValues[gamepadId][nat];
  }

  /** Mutable payload reused for {@link #deviceConnected}; do not retain past the callback. */
  public static final class GamepadConnectedEvent {
    private int gamepadId;
    private FlixelGamepadModel model;

    /** Returns the ID of the connected gamepad. */
    public int gamepadId() {
      return gamepadId;
    }

    /** Returns the model of the connected gamepad. */
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

    /** Returns the ID of the disconnected gamepad. */
    public int gamepadId() {
      return gamepadId;
    }

    void set(int gamepadId) {
      this.gamepadId = gamepadId;
    }
  }
}
