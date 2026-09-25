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

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import org.flixelgdx.input.gamepad.FlixelGamepad;
import org.jetbrains.annotations.NotNull;

/**
 * A single connected Android gamepad, backed by an {@link InputDevice} from the system.
 *
 * <p>Button and axis state are written from the UI thread (via key and motion event handlers
 * in {@link FlixelAndroidGamepadProvider}) and read from the GL thread (via the
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadProvider} polling calls). Volatile fields
 * provide the necessary memory ordering without any per-event allocation.
 *
 * <p>Native button indices are compact sequential values (0 to {@link #BUTTON_COUNT} minus one)
 * defined as constants in this class. The corresponding mapping to logical
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadButton} tokens is built in
 * {@link FlixelAndroidGamepadProvider#buildStandardMapping()}.
 *
 * <p>Native axis indices are also compact (0 to {@link #AXIS_COUNT} minus one):
 * 0 = left stick X, 1 = left stick Y, 2 = right stick X, 3 = right stick Y,
 * 4 = left trigger [0, 1], 5 = right trigger [0, 1].
 *
 * @see FlixelAndroidGamepadProvider
 */
public class FlixelAndroidGamepad implements FlixelGamepad {

  // Compact native button indices. These define the bit positions in the button bitmask.
  static final int BUTTON_A          = 0;
  static final int BUTTON_B          = 1;
  static final int BUTTON_X          = 2;
  static final int BUTTON_Y          = 3;
  static final int BUTTON_L1         = 4;
  static final int BUTTON_R1         = 5;
  static final int BUTTON_THUMBL     = 6;
  static final int BUTTON_THUMBR     = 7;
  static final int BUTTON_START      = 8;
  static final int BUTTON_SELECT     = 9;
  static final int BUTTON_MODE       = 10;
  static final int BUTTON_DPAD_UP    = 11;
  static final int BUTTON_DPAD_DOWN  = 12;
  static final int BUTTON_DPAD_LEFT  = 13;
  static final int BUTTON_DPAD_RIGHT = 14;

  /** Total number of compact button slots. */
  static final int BUTTON_COUNT = 15;

  // Compact native axis indices.
  static final int AXIS_LEFT_X  = 0;
  static final int AXIS_LEFT_Y  = 1;
  static final int AXIS_RIGHT_X = 2;
  static final int AXIS_RIGHT_Y = 3;
  static final int AXIS_L2      = 4;
  static final int AXIS_R2      = 5;

  /** Total number of compact axis slots. */
  static final int AXIS_COUNT = 6;

  /** Half-threshold for hat-switch synthesized d-pad button detection. */
  private static final float HAT_THRESHOLD = 0.5f;

  private final int deviceId;
  private final int vendorId;
  private final int productId;

  @NotNull
  private final String name;

  @NotNull
  private final Vibrator vibrator;

  /**
   * Packed button state as a bitmask: bit {@code i} is set when compact button {@code i} is
   * held. Written by the UI thread via {@link #setButton(int, boolean)} and
   * {@link #applyHatAxes(float, float)}; read by the GL thread via {@link #getButton(int)}.
   * A volatile int is atomically readable and writable on all supported architectures.
   */
  private volatile int buttons;

  // Axis state written by the UI thread and read by the GL thread.
  // Individual volatile int fields (holding float bits) provide atomic 32-bit access.
  private volatile int axisLeftX;
  private volatile int axisLeftY;
  private volatile int axisRightX;
  private volatile int axisRightY;
  private volatile int axisL2;
  private volatile int axisR2;

  /**
   * Creates a gamepad backed by the given Android input device.
   *
   * @param device The {@link InputDevice} representing the connected controller.
   */
  FlixelAndroidGamepad(@NotNull InputDevice device) {
    this.deviceId = device.getId();
    this.vendorId = device.getVendorId();
    this.productId = device.getProductId();
    String n = device.getName();
    this.name = (n != null && !n.isEmpty()) ? n : "Gamepad";
    this.vibrator = device.getVibrator();
  }

  /** Returns the Android input device ID for this gamepad. */
  int getDeviceId() {
    return deviceId;
  }

  /**
   * Updates the pressed state for a compact button index. Called from the UI thread on key
   * events whose source includes {@link InputDevice#SOURCE_GAMEPAD}.
   *
   * @param compactIndex A compact button index, such as {@link #BUTTON_A}.
   * @param pressed {@code true} when the button is pressed, {@code false} when released.
   */
  void setButton(int compactIndex, boolean pressed) {
    if (compactIndex < 0 || compactIndex >= BUTTON_COUNT) {
      return;
    }
    int mask = 1 << compactIndex;
    if (pressed) {
      buttons |= mask;
    } else {
      buttons &= ~mask;
    }
  }

  /**
   * Synthesizes d-pad button state from the analog hat-switch axes. Controllers that report the
   * d-pad as a POV hat (values in {@code [-1, 1]}) arrive as motion-event axes rather than key
   * events, so this method translates them to button bits.
   *
   * @param hatX The {@link MotionEvent#AXIS_HAT_X} value, in the range {@code [-1, 1]}.
   * @param hatY The {@link MotionEvent#AXIS_HAT_Y} value, in the range {@code [-1, 1]}.
   */
  void applyHatAxes(float hatX, float hatY) {
    setButton(BUTTON_DPAD_LEFT,  hatX < -HAT_THRESHOLD);
    setButton(BUTTON_DPAD_RIGHT, hatX > HAT_THRESHOLD);
    setButton(BUTTON_DPAD_UP,    hatY < -HAT_THRESHOLD);
    setButton(BUTTON_DPAD_DOWN,  hatY > HAT_THRESHOLD);
  }

  /**
   * Updates an axis by compact index. All values are already clamped to the expected range
   * before this is called.
   *
   * @param axisIndex A compact axis index, such as {@link #AXIS_LEFT_X}.
   * @param value The axis value, in {@code [-1, 1]} for sticks or {@code [0, 1]} for triggers.
   */
  void setAxis(int axisIndex, float value) {
    int bits = Float.floatToRawIntBits(value);
    switch (axisIndex) {
      case AXIS_LEFT_X:  axisLeftX  = bits; break;
      case AXIS_LEFT_Y:  axisLeftY  = bits; break;
      case AXIS_RIGHT_X: axisRightX = bits; break;
      case AXIS_RIGHT_Y: axisRightY = bits; break;
      case AXIS_L2:      axisL2     = bits; break;
      case AXIS_R2:      axisR2     = bits; break;
      default: break;
    }
  }

  /**
   * Maps an Android {@code KeyEvent.KEYCODE_BUTTON_*} or {@code KEYCODE_DPAD_*} value to
   * the compact button index for this class, or returns {@code -1} when unmapped.
   *
   * @param keyCode An Android key event code.
   * @return The compact button index, or {@code -1} when the key is not a recognized gamepad
   *     button.
   */
  static int keyCodeToButtonIndex(int keyCode) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_BUTTON_A:      return BUTTON_A;
      case KeyEvent.KEYCODE_BUTTON_B:      return BUTTON_B;
      case KeyEvent.KEYCODE_BUTTON_X:      return BUTTON_X;
      case KeyEvent.KEYCODE_BUTTON_Y:      return BUTTON_Y;
      case KeyEvent.KEYCODE_BUTTON_L1:     return BUTTON_L1;
      case KeyEvent.KEYCODE_BUTTON_R1:     return BUTTON_R1;
      case KeyEvent.KEYCODE_BUTTON_THUMBL: return BUTTON_THUMBL;
      case KeyEvent.KEYCODE_BUTTON_THUMBR: return BUTTON_THUMBR;
      case KeyEvent.KEYCODE_BUTTON_START:  return BUTTON_START;
      case KeyEvent.KEYCODE_BUTTON_SELECT: return BUTTON_SELECT;
      case KeyEvent.KEYCODE_BUTTON_MODE:   return BUTTON_MODE;
      case KeyEvent.KEYCODE_DPAD_UP:       return BUTTON_DPAD_UP;
      case KeyEvent.KEYCODE_DPAD_DOWN:     return BUTTON_DPAD_DOWN;
      case KeyEvent.KEYCODE_DPAD_LEFT:     return BUTTON_DPAD_LEFT;
      case KeyEvent.KEYCODE_DPAD_RIGHT:    return BUTTON_DPAD_RIGHT;
      default: return -1;
    }
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getVendorId() {
    return vendorId;
  }

  @Override
  public int getProductId() {
    return productId;
  }

  @Override
  public int getMinButtonIndex() {
    return 0;
  }

  @Override
  public int getMaxButtonIndex() {
    return BUTTON_COUNT - 1;
  }

  @Override
  public boolean getButton(int buttonIndex) {
    if (buttonIndex < 0 || buttonIndex >= BUTTON_COUNT) {
      return false;
    }
    return (buttons & (1 << buttonIndex)) != 0;
  }

  @Override
  public int getAxisCount() {
    return AXIS_COUNT;
  }

  @Override
  public float getAxis(int axisIndex) {
    switch (axisIndex) {
      case AXIS_LEFT_X:  return Float.intBitsToFloat(axisLeftX);
      case AXIS_LEFT_Y:  return Float.intBitsToFloat(axisLeftY);
      case AXIS_RIGHT_X: return Float.intBitsToFloat(axisRightX);
      case AXIS_RIGHT_Y: return Float.intBitsToFloat(axisRightY);
      case AXIS_L2:      return Float.intBitsToFloat(axisL2);
      case AXIS_R2:      return Float.intBitsToFloat(axisR2);
      default:           return 0f;
    }
  }

  @Override
  public boolean canVibrate() {
    return vibrator.hasVibrator();
  }

  @Override
  public void startVibration(int durationMs, float leftIntensity, float rightIntensity) {
    if (!vibrator.hasVibrator()) {
      return;
    }
    float intensity = Math.max(leftIntensity, rightIntensity);
    int amplitude = (int) (clamp01(intensity) * 255f);
    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude));
  }

  @Override
  public void cancelVibration() {
    vibrator.cancel();
  }

  private static float clamp01(float v) {
    if (v < 0f) {
      return 0f;
    }
    return Math.min(v, 1f);
  }
}
