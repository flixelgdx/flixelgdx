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
import com.badlogic.gdx.controllers.ControllerMapping;

import org.jetbrains.annotations.NotNull;

/**
 * Platform bridge for reading the analog float value of a gamepad button.
 *
 * <p>The gdx-controllers {@link Controller} interface only exposes digital button state via
 * {@code getButton(int)}, which returns a boolean. On platforms such as web (W3C Gamepad API),
 * trigger buttons carry an analog pressure value in {@code [0, 1]} that is otherwise inaccessible
 * through the standard interface. Implementations of this interface read that value through
 * platform-specific means.
 *
 * <p>The primary use case is populating {@link FlixelGamepadButton#AXIS_TRIGGER_L} and
 * {@link FlixelGamepadButton#AXIS_TRIGGER_R} on backends where L2 and R2 are mapped to button
 * indices (for example {@link ControllerMapping#buttonL2} on web) rather than dedicated axes.
 * Install a platform implementation via
 * {@link FlixelGamepadInputManager#setAnalogButtonReader(FlixelAnalogButtonReader)}.
 *
 * <p>On the Jamepad/SDL desktop backend, triggers are already reported as raw axes and no reader
 * is needed. {@code FlixelTeaVMAnalogButtonReader} (in the {@code flixelgdx-teavm} module) covers
 * the web case and is installed automatically by {@code FlixelTeaVMLauncher}.
 *
 * @see FlixelGamepadInputManager#setAnalogButtonReader(FlixelAnalogButtonReader)
 */
@FunctionalInterface
public interface FlixelAnalogButtonReader {

  /**
   * Returns the raw analog value of the button at {@code nativeButtonIndex} on the given
   * controller, in the range {@code [0, 1]}.
   *
   * <p>This is called from the per-frame polling loop, so the implementation must not allocate
   * or block.
   *
   * @param controller The controller to read from.
   * @param nativeButtonIndex Raw button index (for example {@link ControllerMapping#buttonL2}).
   * @return Analog value in the range {@code [0, 1]}, or {@code 0f} when unavailable.
   */
  float read(@NotNull Controller controller, int nativeButtonIndex);
}
