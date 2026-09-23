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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the shared text-input request counter in {@link FlixelBaseInputDevice}: the
 * {@code onTextInputStarted()} and {@code onTextInputStopped()} hooks fire only on the 0 to 1 and
 * 1 to 0 transitions, several owners can nest {@code startTextInput()}/{@code stopTextInput()}
 * calls safely, an extra {@code stopTextInput()} is ignored instead of going negative, and
 * {@code setTextInputArea(int, int, int, int)} forwards through {@code onTextInputAreaChanged(...)}.
 */
class FlixelTextInputTest {

  @Test
  void startTextInputFiresHookOnlyOnFirstRequest() {
    RecordingInputDevice device = new RecordingInputDevice();

    device.startTextInput();
    device.startTextInput();
    device.startTextInput();

    assertEquals(1, device.startedCount);
    assertTrue(device.isTextInputActive());
  }

  @Test
  void stopTextInputFiresHookOnlyOnLastRelease() {
    RecordingInputDevice device = new RecordingInputDevice();

    device.startTextInput();
    device.startTextInput();
    device.stopTextInput();
    assertEquals(0, device.stoppedCount);
    assertTrue(device.isTextInputActive());

    device.stopTextInput();
    assertEquals(1, device.stoppedCount);
    assertFalse(device.isTextInputActive());
  }

  @Test
  void nestedOwnersShareOneTextInputSession() {
    RecordingInputDevice device = new RecordingInputDevice();

    // A debug console starts a permanent request, then a focused text box layers its own on top.
    device.startTextInput();
    device.startTextInput();
    assertEquals(1, device.startedCount);
    assertTrue(device.isTextInputActive());

    // The text box loses focus and releases its own request; the console's request keeps input active.
    device.stopTextInput();
    assertEquals(0, device.stoppedCount);
    assertTrue(device.isTextInputActive());
  }

  @Test
  void stopTextInputBelowZeroIsIgnored() {
    RecordingInputDevice device = new RecordingInputDevice();

    device.stopTextInput();
    device.stopTextInput();

    assertEquals(0, device.stoppedCount);
    assertFalse(device.isTextInputActive());

    // A later balanced start/stop pair must still behave normally afterward.
    device.startTextInput();
    assertEquals(1, device.startedCount);
    device.stopTextInput();
    assertEquals(1, device.stoppedCount);
  }

  @Test
  void setTextInputAreaForwardsToHook() {
    RecordingInputDevice device = new RecordingInputDevice();

    device.setTextInputArea(10, 20, 30, 40);

    assertEquals(1, device.areaChangedCount);
    assertEquals(10, device.lastAreaX);
    assertEquals(20, device.lastAreaY);
    assertEquals(30, device.lastAreaWidth);
    assertEquals(40, device.lastAreaHeight);
  }

  /** Minimal concrete {@link FlixelBaseInputDevice} that counts hook calls for assertion. */
  private static final class RecordingInputDevice extends FlixelBaseInputDevice {

    private int startedCount;
    private int stoppedCount;
    private int areaChangedCount;
    private int lastAreaX;
    private int lastAreaY;
    private int lastAreaWidth;
    private int lastAreaHeight;

    @Override
    protected void onTextInputStarted() {
      startedCount++;
    }

    @Override
    protected void onTextInputStopped() {
      stoppedCount++;
    }

    @Override
    protected void onTextInputAreaChanged(int x, int y, int w, int h) {
      areaChangedCount++;
      lastAreaX = x;
      lastAreaY = y;
      lastAreaWidth = w;
      lastAreaHeight = h;
    }
  }
}
