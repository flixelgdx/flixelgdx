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
package org.flixelgdx.backend.html5.input;

import org.flixelgdx.input.gamepad.FlixelGamepad;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;

/**
 * A single controller exposed through the browser's Gamepad API.
 *
 * <p>The browser identifies each connected controller by a slot index. Reading its live state means
 * asking {@code navigator.getGamepads()} for the current snapshot every time, because most browsers
 * hand back frozen snapshots rather than objects that update in place. Each accessor here therefore
 * re-reads the slot, which keeps button and axis values current across every browser without caching
 * stale state.
 *
 * <p>This wrapper reports the "standard" Gamepad API layout, which lines up with the mapping the
 * provider installs (face buttons, shoulders, triggers as buttons, stick clicks, and the d-pad).
 * Analog trigger pressure is exposed through the trigger buttons rather than as separate axes,
 * because the standard web layout places the triggers in the button list, not the axis list.
 */
public class FlixelWebGamepad implements FlixelGamepad {

  private final int index;
  private final int buttonCount;
  private final int axisCount;

  @NotNull
  private final String id;

  /**
   * Wraps the controller at the given browser slot.
   *
   * @param index The {@code navigator.getGamepads()} slot index.
   */
  public FlixelWebGamepad(int index) {
    this.index = index;
    this.id = gamepadId(index);
    this.buttonCount = gamepadButtonCount(index);
    this.axisCount = gamepadAxisCount(index);
  }

  @Override
  @NotNull
  public String getName() {
    return id;
  }

  @Override
  public int getMinButtonIndex() {
    return 0;
  }

  @Override
  public int getMaxButtonIndex() {
    return Math.max(0, buttonCount - 1);
  }

  @Override
  public boolean getButton(int buttonIndex) {
    return buttonPressed(index, buttonIndex);
  }

  @Override
  public int getAxisCount() {
    return axisCount;
  }

  @Override
  public float getAxis(int axisIndex) {
    return (float) axisValue(index, axisIndex);
  }

  @Override
  public boolean canVibrate() {
    return hasVibration(index);
  }

  @Override
  public void startVibration(int durationMs, float strength) {
    vibrate(index, durationMs, strength);
  }

  @Override
  public void cancelVibration() {
    resetVibration(index);
  }

  /**
   * Returns the browser slot index this controller occupies.
   *
   * @return The slot index.
   */
  public int getIndex() {
    return index;
  }

  @JSBody(params = "i", script = "var g = navigator.getGamepads()[i]; return g ? g.id : '';")
  private static native String gamepadId(int i);

  @JSBody(params = "i", script = "var g = navigator.getGamepads()[i]; return g ? g.buttons.length : 0;")
  private static native int gamepadButtonCount(int i);

  @JSBody(params = "i", script = "var g = navigator.getGamepads()[i]; return g ? g.axes.length : 0;")
  private static native int gamepadAxisCount(int i);

  @JSBody(params = { "i", "b" },
      script = "var g = navigator.getGamepads()[i]; return (g && g.buttons[b]) ? g.buttons[b].pressed : false;")
  private static native boolean buttonPressed(int i, int b);

  @JSBody(params = { "i", "a" },
      script = "var g = navigator.getGamepads()[i]; return (g && g.axes.length > a) ? g.axes[a] : 0;")
  private static native double axisValue(int i, int a);

  @JSBody(params = "i", script = "var g = navigator.getGamepads()[i]; return !!(g && g.vibrationActuator);")
  private static native boolean hasVibration(int i);

  @JSBody(params = { "i", "duration", "strength" },
      script = "var g = navigator.getGamepads()[i];"
          + "if (g && g.vibrationActuator && g.vibrationActuator.playEffect) {"
          + "  g.vibrationActuator.playEffect('dual-rumble',"
          + "    { duration: duration, strongMagnitude: strength, weakMagnitude: strength });"
          + "}")
  private static native void vibrate(int i, int duration, float strength);

  @JSBody(params = "i",
      script = "var g = navigator.getGamepads()[i];"
          + "if (g && g.vibrationActuator && g.vibrationActuator.reset) { g.vibrationActuator.reset(); }")
  private static native void resetVibration(int i);
}
