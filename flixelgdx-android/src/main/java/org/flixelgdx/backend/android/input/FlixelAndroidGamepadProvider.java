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
package org.flixelgdx.backend.android.input;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import org.flixelgdx.input.gamepad.FlixelGamepad;
import org.flixelgdx.input.gamepad.FlixelGamepadAxis;
import org.flixelgdx.input.gamepad.FlixelGamepadButton;
import org.flixelgdx.input.gamepad.FlixelGamepadMapping;
import org.flixelgdx.input.gamepad.FlixelGamepadMappingResolver;
import org.flixelgdx.input.gamepad.FlixelGamepadProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Android gamepad feed, backed by the platform's {@link InputManager}.
 *
 * <p>This provider implements both {@link FlixelGamepadProvider} (the raw pad feed polled every
 * frame by the gamepad manager) and {@link FlixelGamepadMappingResolver} (the single mapping
 * shared by every Android pad). Install both in the launcher:
 *
 * <pre>{@code
 * Flixel.gamepads.setGamepadProvider(provider);
 * Flixel.gamepads.addMappingResolver(provider);
 * }</pre>
 *
 * <p>Call {@link #start()} once after installing to enumerate already-connected devices and
 * begin listening for hot-plug events. Call {@link #stop()} in {@code onDestroy} to release the
 * listener.
 *
 * <p>The pad list is written by the main thread (via the {@link InputManager.InputDeviceListener}
 * callbacks) and read by the GL thread (via {@link #getGamepadAt(int)} and
 * {@link #getGamepadCount()}). All pad-list mutations and reads are synchronized on this instance
 * so neither thread sees a partial state.
 *
 * <p>Axis and button state inside each {@link FlixelAndroidGamepad} use volatile fields, so no
 * synchronization is needed there.
 *
 * @see FlixelAndroidGamepad
 */
public class FlixelAndroidGamepadProvider
    implements FlixelGamepadProvider, FlixelGamepadMappingResolver,
    InputManager.InputDeviceListener {

  /** Maximum simultaneous gamepads supported. Exceeding this silently drops new devices. */
  private static final int MAX_PADS = 8;

  @NotNull
  private final InputManager inputManager;

  /** Connected pads; access is synchronized on {@code this}. */
  private final FlixelAndroidGamepad[] pads = new FlixelAndroidGamepad[MAX_PADS];

  /** Number of entries in {@link #pads} that are currently occupied; access synchronized. */
  private int padCount;

  /** The single standard mapping shared by every Android gamepad. */
  @NotNull
  private final FlixelGamepadMapping standardMapping = buildStandardMapping();

  /**
   * Creates a provider that will use the given context's {@link InputManager}.
   *
   * @param context Any context; typically the host activity.
   */
  public FlixelAndroidGamepadProvider(@NotNull Context context) {
    this.inputManager =
        (InputManager) context.getApplicationContext().getSystemService(Context.INPUT_SERVICE);
  }

  /**
   * Enumerates already-connected gamepads and registers the hot-plug listener. Call this once
   * from the launcher's {@code Flixel.boot.afterStart} callback.
   */
  public void start() {
    inputManager.registerInputDeviceListener(this, new Handler(Looper.getMainLooper()));
    int[] ids = inputManager.getInputDeviceIds();
    for (int id : ids) {
      tryAdd(id);
    }
  }

  /**
   * Unregisters the hot-plug listener. Call this from the activity's {@code onDestroy}.
   */
  public void stop() {
    inputManager.unregisterInputDeviceListener(this);
  }

  /**
   * Receives a gamepad button key event from the input device's key listener and updates the
   * matching pad's button state. Called from the UI thread.
   *
   * @param keyCode The Android {@code KeyEvent.KEYCODE_*} value.
   * @param event The raw key event, used to identify the source device.
   */
  public void onKeyEvent(int keyCode, @NotNull KeyEvent event) {
    FlixelAndroidGamepad pad = findById(event.getDeviceId());
    if (pad == null) {
      return;
    }
    int compactIndex = FlixelAndroidGamepad.keyCodeToButtonIndex(keyCode);
    if (compactIndex < 0) {
      return;
    }
    pad.setButton(compactIndex, event.getAction() == KeyEvent.ACTION_DOWN);
  }

  /**
   * Receives a joystick motion event from the input device's generic motion listener and updates
   * the matching pad's axis state. Called from the UI thread.
   *
   * @param event The raw motion event with {@link InputDevice#SOURCE_JOYSTICK} as the source.
   */
  public void onMotionEvent(@NotNull MotionEvent event) {
    FlixelAndroidGamepad pad = findById(event.getDeviceId());
    if (pad == null) {
      return;
    }
    // Left and right sticks.
    pad.setAxis(FlixelAndroidGamepad.AXIS_LEFT_X,
        clampAxis(event.getAxisValue(MotionEvent.AXIS_X)));
    pad.setAxis(FlixelAndroidGamepad.AXIS_LEFT_Y,
        clampAxis(event.getAxisValue(MotionEvent.AXIS_Y)));
    pad.setAxis(FlixelAndroidGamepad.AXIS_RIGHT_X,
        clampAxis(event.getAxisValue(MotionEvent.AXIS_Z)));
    pad.setAxis(FlixelAndroidGamepad.AXIS_RIGHT_Y,
        clampAxis(event.getAxisValue(MotionEvent.AXIS_RZ)));

    // Triggers: prefer AXIS_LTRIGGER/RTRIGGER, fall back to AXIS_BRAKE/GAS.
    float lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
    if (lt == 0f) {
      lt = event.getAxisValue(MotionEvent.AXIS_BRAKE);
    }
    float rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
    if (rt == 0f) {
      rt = event.getAxisValue(MotionEvent.AXIS_GAS);
    }
    pad.setAxis(FlixelAndroidGamepad.AXIS_L2, clamp01(lt));
    pad.setAxis(FlixelAndroidGamepad.AXIS_R2, clamp01(rt));

    // D-pad hat switch: some controllers report the d-pad as AXIS_HAT_X/Y.
    float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
    float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
    pad.applyHatAxes(hatX, hatY);
  }

  // InputManager.InputDeviceListener ----------------------------------------------------------

  @Override
  public void onInputDeviceAdded(int deviceId) {
    tryAdd(deviceId);
  }

  @Override
  public void onInputDeviceRemoved(int deviceId) {
    synchronized (this) {
      int idx = indexOfId(deviceId);
      if (idx < 0) {
        return;
      }
      // Shift remaining entries left to keep the array compact.
      int last = padCount - 1;
      if (idx < last) {
        pads[idx] = pads[last];
      }
      pads[last] = null;
      padCount--;
    }
  }

  @Override
  public void onInputDeviceChanged(int deviceId) {
    // No-op: device identity does not change in a way that requires re-creating the pad.
  }

  // FlixelGamepadProvider ---------------------------------------------------------------------

  @Override
  public synchronized int getGamepadCount() {
    return padCount;
  }

  @Nullable
  @Override
  public synchronized FlixelGamepad getGamepadAt(int index) {
    return (index >= 0 && index < padCount) ? pads[index] : null;
  }

  // FlixelGamepadMappingResolver --------------------------------------------------------------

  @Nullable
  @Override
  public FlixelGamepadMapping resolve(@NotNull FlixelGamepad gamepad) {
    return gamepad instanceof FlixelAndroidGamepad ? standardMapping : null;
  }

  // Internal helpers --------------------------------------------------------------------------

  /** Attempts to add the device with the given ID if it is a gamepad or joystick. */
  private void tryAdd(int deviceId) {
    InputDevice device = InputDevice.getDevice(deviceId);
    if (device == null) {
      return;
    }
    int sources = device.getSources();
    boolean isGamepad = (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
    boolean isJoystick = (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    if (!isGamepad && !isJoystick) {
      return;
    }
    synchronized (this) {
      if (padCount >= MAX_PADS || indexOfId(deviceId) >= 0) {
        return; // Already tracked or no space.
      }
      pads[padCount] = new FlixelAndroidGamepad(device);
      padCount++;
    }
  }

  /**
   * Returns the index in {@link #pads} for the given device ID, or {@code -1} when not present.
   * Must be called while holding the lock on {@code this}.
   */
  private int indexOfId(int deviceId) {
    for (int i = 0; i < padCount; i++) {
      if (pads[i].getDeviceId() == deviceId) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the pad with the given device ID, or {@code null} when no pad matches. This
   * searches without holding the lock because it is called from event handlers that update
   * per-pad volatile state, and the worst case (a stale view of the pad list during a
   * connect/disconnect) results in a silently dropped event rather than a crash.
   */
  @Nullable
  private FlixelAndroidGamepad findById(int deviceId) {
    int count = padCount;
    for (int i = 0; i < count; i++) {
      FlixelAndroidGamepad p = pads[i];
      if (p != null && p.getDeviceId() == deviceId) {
        return p;
      }
    }
    return null;
  }

  /**
   * Builds the single standard mapping from compact native indices to logical
   * {@link FlixelGamepadButton} and {@link FlixelGamepadAxis} tokens.
   */
  @NotNull
  private static FlixelGamepadMapping buildStandardMapping() {
    FlixelGamepadMapping mapping = new FlixelGamepadMapping();

    mapping.registerButton(FlixelGamepadButton.A,           FlixelAndroidGamepad.BUTTON_A);
    mapping.registerButton(FlixelGamepadButton.B,           FlixelAndroidGamepad.BUTTON_B);
    mapping.registerButton(FlixelGamepadButton.X,           FlixelAndroidGamepad.BUTTON_X);
    mapping.registerButton(FlixelGamepadButton.Y,           FlixelAndroidGamepad.BUTTON_Y);
    mapping.registerButton(FlixelGamepadButton.L1,          FlixelAndroidGamepad.BUTTON_L1);
    mapping.registerButton(FlixelGamepadButton.R1,          FlixelAndroidGamepad.BUTTON_R1);
    mapping.registerButton(FlixelGamepadButton.LEFT_STICK,  FlixelAndroidGamepad.BUTTON_THUMBL);
    mapping.registerButton(FlixelGamepadButton.RIGHT_STICK, FlixelAndroidGamepad.BUTTON_THUMBR);
    mapping.registerButton(FlixelGamepadButton.START,       FlixelAndroidGamepad.BUTTON_START);
    mapping.registerButton(FlixelGamepadButton.BACK,        FlixelAndroidGamepad.BUTTON_SELECT);
    mapping.registerButton(FlixelGamepadButton.MODE,        FlixelAndroidGamepad.BUTTON_MODE);
    mapping.registerButton(FlixelGamepadButton.DPAD_UP,     FlixelAndroidGamepad.BUTTON_DPAD_UP);
    mapping.registerButton(FlixelGamepadButton.DPAD_DOWN,   FlixelAndroidGamepad.BUTTON_DPAD_DOWN);
    mapping.registerButton(FlixelGamepadButton.DPAD_LEFT,   FlixelAndroidGamepad.BUTTON_DPAD_LEFT);
    mapping.registerButton(FlixelGamepadButton.DPAD_RIGHT,  FlixelAndroidGamepad.BUTTON_DPAD_RIGHT);

    mapping.registerAxis(FlixelGamepadAxis.LEFT_X,  FlixelAndroidGamepad.AXIS_LEFT_X);
    mapping.registerAxis(FlixelGamepadAxis.LEFT_Y,  FlixelAndroidGamepad.AXIS_LEFT_Y);
    mapping.registerAxis(FlixelGamepadAxis.RIGHT_X, FlixelAndroidGamepad.AXIS_RIGHT_X);
    mapping.registerAxis(FlixelGamepadAxis.RIGHT_Y, FlixelAndroidGamepad.AXIS_RIGHT_Y);
    mapping.registerAxis(FlixelGamepadAxis.L2,      FlixelAndroidGamepad.AXIS_L2);
    mapping.registerAxis(FlixelGamepadAxis.R2,      FlixelAndroidGamepad.AXIS_R2);

    return mapping;
  }

  /** Clamps a stick axis to [-1, 1]. */
  private static float clampAxis(float v) {
    if (v < -1f) {
      return -1f;
    }
    return Math.min(v, 1f);
  }

  /** Clamps a trigger value to [0, 1]. */
  private static float clamp01(float v) {
    if (v < 0f) {
      return 0f;
    }
    return Math.min(v, 1f);
  }
}
