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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link FlixelInputEventQueue} keeps a press and its release on separate frames, so
 * a click or key tap that happens entirely between two frames is never lost.
 */
@ExtendWith(FlixelHeadlessExtension.class)
class FlixelInputEventQueueTest {

  private FlixelInputDevice previousInput;
  private QueuedInputDevice device;
  private FlixelMouseInputManager mouse;
  private FlixelKeyInputManager keys;

  @BeforeEach
  void setUp() {
    previousInput = Flixel.input;
    device = new QueuedInputDevice();
    Flixel.input = device;
    mouse = new FlixelMouseInputManager();
    keys = new FlixelKeyInputManager();
    device.addMouseListener(mouse);
    device.addKeyboardListener(keys);
  }

  @AfterEach
  void tearDown() {
    Flixel.input = previousInput;
  }

  @Test
  void sameFrameClickReportsPressThenReleaseOnNextFrame() {
    device.queue.postMouseDown(FlixelMouseButton.LEFT, 10, 20);
    device.queue.postMouseUp(FlixelMouseButton.LEFT, 10, 20);

    beginFrame();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT));
    assertTrue(mouse.pressed(FlixelMouseButton.LEFT));
    assertFalse(mouse.justReleased(FlixelMouseButton.LEFT));
    assertEquals(1, device.queue.getSize());
    mouse.endFrame();

    beginFrame();
    assertFalse(mouse.justPressed(FlixelMouseButton.LEFT));
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT));
    assertTrue(mouse.justReleased(FlixelMouseButton.LEFT));
    assertTrue(device.queue.isEmpty());
    mouse.endFrame();

    beginFrame();
    assertFalse(mouse.justReleased(FlixelMouseButton.LEFT));
  }

  @Test
  void sameFrameKeyTapReportsPressThenReleaseOnNextFrame() {
    device.queue.postKeyDown(FlixelKey.SPACE);
    device.queue.postKeyUp(FlixelKey.SPACE);

    beginFrame();
    assertTrue(keys.justPressed(FlixelKey.SPACE));
    assertFalse(keys.justReleased(FlixelKey.SPACE));
    keys.endFrame();

    beginFrame();
    assertFalse(keys.justPressed(FlixelKey.SPACE));
    assertTrue(keys.justReleased(FlixelKey.SPACE));
  }

  @Test
  void releaseOfEarlierPressIsDeliveredInTheSameDrain() {
    device.queue.postMouseDown(FlixelMouseButton.LEFT, 0, 0);
    beginFrame();
    mouse.endFrame();

    device.queue.postMouseUp(FlixelMouseButton.LEFT, 0, 0);
    beginFrame();
    assertTrue(mouse.justReleased(FlixelMouseButton.LEFT));
    assertTrue(device.queue.isEmpty());
  }

  @Test
  void unrelatedEventsAreNotHeldBack() {
    device.queue.postMouseDown(FlixelMouseButton.LEFT, 0, 0);
    device.queue.postKeyDown(FlixelKey.A);
    device.queue.postMouseUp(FlixelMouseButton.RIGHT, 0, 0);
    device.queue.postKeyUp(FlixelKey.B);

    device.queue.drain(device);
    assertTrue(device.queue.isEmpty());
  }

  @Test
  void eventsAfterAHeldBackReleaseKeepTheirOrder() {
    device.queue.postMouseDown(FlixelMouseButton.LEFT, 0, 0);
    device.queue.postMouseUp(FlixelMouseButton.LEFT, 0, 0);
    device.queue.postMouseDown(FlixelMouseButton.LEFT, 0, 0);
    device.queue.postMouseUp(FlixelMouseButton.LEFT, 0, 0);

    device.queue.drain(device);
    assertEquals("down", device.log.toString());
    device.queue.drain(device);
    assertEquals("down,up,down", device.log.toString());
    device.queue.drain(device);
    assertEquals("down,up,down,up", device.log.toString());
  }

  @Test
  void consecutiveMovesAndScrollsAreCoalesced() {
    device.queue.postMouseMoved(1, 2);
    device.queue.postMouseMoved(3, 4);
    device.queue.postScrolled(0f, 1f);
    device.queue.postScrolled(0.5f, 2f);
    assertEquals(2, device.queue.getSize());

    device.queue.drain(device);
    assertEquals(3, device.getX());
    assertEquals(4, device.getY());
    assertEquals(0.5f, mouse.getScrollDeltaX(), 1e-6f);
    assertEquals(3f, mouse.getScrollDeltaY(), 1e-6f);
  }

  @Test
  void fullQueueRejectsNewEvents() {
    FlixelInputEventQueue queue = new FlixelInputEventQueue(2);
    assertTrue(queue.postKeyDown(FlixelKey.A));
    assertTrue(queue.postKeyDown(FlixelKey.B));
    assertFalse(queue.postKeyDown(FlixelKey.C));
    assertEquals(2, queue.getSize());
    queue.clear();
    assertTrue(queue.isEmpty());
  }

  @Test
  void capacityMustBeAPowerOfTwo() {
    assertThrows(IllegalArgumentException.class, () -> new FlixelInputEventQueue(0));
    assertThrows(IllegalArgumentException.class, () -> new FlixelInputEventQueue(3));
    assertEquals(FlixelInputEventQueue.DEFAULT_CAPACITY, new FlixelInputEventQueue().getCapacity());
  }

  /** Drains the device and refreshes the managers, the way a backend starts each frame. */
  private void beginFrame() {
    device.queue.drain(device);
    keys.update();
    mouse.update();
  }

  /** Minimal queued backend: keeps polled button state and forwards drained events to listeners. */
  private static final class QueuedInputDevice extends FlixelBaseInputDevice
      implements FlixelInputEventQueue.Receiver {

    private final FlixelInputEventQueue queue = new FlixelInputEventQueue();
    private final boolean[] buttons = new boolean[8];
    private final StringBuilder log = new StringBuilder();
    private int x;
    private int y;

    @Override
    public void keyDown(int keycode) {
      dispatchKeyDown(keycode);
    }

    @Override
    public void keyUp(int keycode) {
      dispatchKeyUp(keycode);
    }

    @Override
    public void mouseDown(int button, int x, int y) {
      buttons[button] = true;
      record("down");
      dispatchMouseDown(button, x, y);
    }

    @Override
    public void mouseUp(int button, int x, int y) {
      buttons[button] = false;
      record("up");
      dispatchMouseUp(button, x, y);
    }

    @Override
    public void mouseMoved(int x, int y) {
      this.x = x;
      this.y = y;
      dispatchMouseMoved(x, y);
    }

    @Override
    public void scrolled(float amountX, float amountY) {
      dispatchScrolled(amountX, amountY);
    }

    @Override
    public boolean isButtonPressed(int button) {
      return button >= 0 && button < buttons.length && buttons[button];
    }

    @Override
    public int getX() {
      return x;
    }

    @Override
    public int getY() {
      return y;
    }

    private void record(String entry) {
      if (!log.isEmpty()) {
        log.append(',');
      }
      log.append(entry);
    }
  }
}
