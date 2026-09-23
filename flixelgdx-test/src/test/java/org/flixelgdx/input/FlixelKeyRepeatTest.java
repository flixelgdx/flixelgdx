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
package org.flixelgdx.input;

import org.flixelgdx.collections.FlixelIntArray;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link FlixelBaseInputDevice#dispatchKeyRepeated(int)} reaches
 * {@link FlixelKeyboardListener#keyRepeated(int)} only, never {@link FlixelKeyboardListener#keyDown(int)},
 * and that repeats leave {@code justPressed}-style state untouched.
 */
class FlixelKeyRepeatTest {

  @Test
  void repeatedKeyReachesOnlyKeyRepeated() {
    RecordingInputDevice device = new RecordingInputDevice();
    RecordingListener listener = new RecordingListener();
    device.addKeyboardListener(listener);

    device.triggerKeyDown(FlixelKey.BACKSPACE);
    device.triggerKeyRepeated(FlixelKey.BACKSPACE);
    device.triggerKeyRepeated(FlixelKey.BACKSPACE);
    device.triggerKeyUp(FlixelKey.BACKSPACE);

    assertEquals(1, listener.downEvents.getSize());
    assertEquals(2, listener.repeatEvents.getSize());
    assertEquals(1, listener.upEvents.getSize());
    assertEquals(FlixelKey.BACKSPACE, listener.downEvents.first());
    assertEquals(FlixelKey.BACKSPACE, listener.repeatEvents.first());
    assertEquals(FlixelKey.BACKSPACE, listener.upEvents.first());
  }

  @Test
  void eventsArriveInOrderAndNeverAsKeyDown() {
    RecordingInputDevice device = new RecordingInputDevice();
    RecordingListener listener = new RecordingListener();
    device.addKeyboardListener(listener);

    device.triggerKeyDown(FlixelKey.BACKSPACE);
    device.triggerKeyRepeated(FlixelKey.BACKSPACE);
    device.triggerKeyRepeated(FlixelKey.BACKSPACE);

    assertEquals("down,repeat,repeat", listener.callOrder.toString());
  }

  @Test
  void keyRepeatedDoesNotAffectJustPressedState() {
    RecordingInputDevice device = new RecordingInputDevice();
    FlixelKeyInputManager keys = new FlixelKeyInputManager();
    device.addKeyboardListener(keys);

    device.triggerKeyDown(FlixelKey.BACKSPACE);
    assertTrue(keys.justPressed(FlixelKey.BACKSPACE));
    keys.endFrame();
    assertFalse(keys.justPressed(FlixelKey.BACKSPACE));

    // Repeats must not re-arm justPressed while the key is still held.
    device.triggerKeyRepeated(FlixelKey.BACKSPACE);
    assertFalse(keys.justPressed(FlixelKey.BACKSPACE));
    assertTrue(keys.pressed(FlixelKey.BACKSPACE));

    device.triggerKeyUp(FlixelKey.BACKSPACE);
    assertFalse(keys.pressed(FlixelKey.BACKSPACE));
  }

  /** Minimal concrete {@link FlixelBaseInputDevice} that exposes the protected dispatch calls. */
  private static final class RecordingInputDevice extends FlixelBaseInputDevice {

    void triggerKeyDown(int keycode) {
      dispatchKeyDown(keycode);
    }

    void triggerKeyUp(int keycode) {
      dispatchKeyUp(keycode);
    }

    void triggerKeyRepeated(int keycode) {
      dispatchKeyRepeated(keycode);
    }
  }

  /** Records every callback it receives, in order, for assertion. */
  private static final class RecordingListener implements FlixelKeyboardListener {

    private final FlixelIntArray downEvents = new FlixelIntArray();
    private final FlixelIntArray upEvents = new FlixelIntArray();
    private final FlixelIntArray repeatEvents = new FlixelIntArray();
    private final StringBuilder callOrder = new StringBuilder();

    @Override
    public void keyDown(int keycode) {
      downEvents.add(keycode);
      appendOrder("down");
    }

    @Override
    public void keyUp(int keycode) {
      upEvents.add(keycode);
      appendOrder("up");
    }

    @Override
    public void keyRepeated(int keycode) {
      repeatEvents.add(keycode);
      appendOrder("repeat");
    }

    private void appendOrder(String event) {
      if (callOrder.length() > 0) {
        callOrder.append(',');
      }
      callOrder.append(event);
    }
  }
}
