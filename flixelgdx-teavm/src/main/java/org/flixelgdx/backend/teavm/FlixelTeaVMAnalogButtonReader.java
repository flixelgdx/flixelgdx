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
package org.flixelgdx.backend.teavm;

import com.badlogic.gdx.controllers.Controller;

import org.flixelgdx.input.gamepad.FlixelAnalogButtonReader;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;

/**
 * Web-backend {@link FlixelAnalogButtonReader} that reads analog button pressure directly from
 * the W3C Gamepad API via {@link JSBody} inline JavaScript.
 *
 * <p>On the web, triggers (L2/R2) are represented as {@code GamepadButton} objects with both a
 * {@code pressed} boolean and a {@code value} float in {@code [0, 1]}. The gdx-controllers
 * {@link Controller} interface only exposes the boolean half via {@code getButton(int)}, so this
 * reader bypasses the gdx-controllers layer and fetches {@code value} from
 * {@code navigator.getGamepads()[index].buttons[buttonIndex].value} instead.
 *
 * <p>This reader is installed automatically by {@link FlixelTeaVMLauncher} and covers
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInput#AXIS_TRIGGER_L AXIS_TRIGGER_L} and
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInput#AXIS_TRIGGER_R AXIS_TRIGGER_R} on web.
 * You do not need to install it manually.
 */
final class FlixelTeaVMAnalogButtonReader implements FlixelAnalogButtonReader {

  @Override
  public float read(@NotNull Controller controller, int nativeButtonIndex) {
    String uid = controller.getUniqueId();
    int index;
    try {
      index = Integer.parseInt(uid);
    } catch (NumberFormatException ignored) {
      return 0f;
    }
    return readButtonValueJS(index, nativeButtonIndex);
  }

  @JSBody(params = { "index", "buttonIndex" }, script = """
      var gp = navigator.getGamepads ? navigator.getGamepads()[index] : null;
      if (!gp || !gp.buttons || buttonIndex >= gp.buttons.length) return 0;
      var b = gp.buttons[buttonIndex];
      return b ? (b.value || 0) : 0;
      """)
  private static native float readButtonValueJS(int index, int buttonIndex);
}
