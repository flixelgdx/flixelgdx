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
package org.flixelgdx.input.mouse;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.graphics.FlixelViewport;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.FlixelNoopInputDevice;
import org.flixelgdx.math.FlixelVector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FlixelHeadlessExtension.class)
class FlixelMouseInputManagerTest {

  @Test
  void scrollAccumulatesUntilEndFrame() {
    FlixelMouseInputManager m = new FlixelMouseInputManager();
    m.scrolled(0f, -2f);
    assertEquals(-2f, m.getScrollDeltaY(), 1e-6f);
    m.endFrame();
    assertEquals(0f, m.getScrollDeltaY(), 1e-6f);
  }

  @Test
  void buttonIndicesAreBounded() {
    FlixelMouseInputManager m = new FlixelMouseInputManager();
    m.update();
    assertFalse(m.justPressed(-1));
    assertFalse(m.justPressed(99));
  }

  @Test
  void setPositionIsNoOpWhenPlatformCannotWarp() {
    FlixelInputDevice previous = Flixel.input;
    Flixel.input = FlixelNoopInputDevice.INSTANCE;
    try {
      FlixelMouseInputManager m = new FlixelMouseInputManager();
      m.update();
      assertFalse(m.supportsSetPosition());
      m.setScreenPosition(120f, 80f);
      m.setScreenX(5f);
      m.setScreenY(6f);
      m.setWorldPosition(10f, 10f, null);
      assertEquals(0, m.getScreenX());
      assertEquals(0, m.getScreenY());
    } finally {
      Flixel.input = previous;
    }
  }

  @Test
  void setScreenPositionMovesPointerAndKeepsOtherAxis() {
    FlixelInputDevice previous = Flixel.input;
    WarpableDevice device = new WarpableDevice();
    Flixel.input = device;
    try {
      FlixelMouseInputManager m = new FlixelMouseInputManager();
      m.setWorldCamera(null);
      assertTrue(m.supportsSetPosition());
      m.setScreenPosition(100.4f, 50.6f);
      assertEquals(100, m.getScreenX());
      assertEquals(51, m.getScreenY());
      m.setScreenX(7f);
      assertEquals(7, m.getScreenX());
      assertEquals(51, m.getScreenY());
      m.setScreenY(9f);
      assertEquals(7, m.getScreenX());
      assertEquals(9, m.getScreenY());
      assertEquals(3, device.warps);
    } finally {
      Flixel.input = previous;
    }
  }

  @Test
  void viewportProjectInvertsUnproject() {
    FlixelViewport viewport = new FlixelViewport(640f, 360f);
    viewport.setScreenBounds(20, 10, 1280, 720);
    viewport.setCameraPosition(320f, 180f);
    FlixelVector v = new FlixelVector(123f, 77f);
    viewport.project(v);
    viewport.unproject(v);
    assertEquals(123f, v.x, 1e-3f);
    assertEquals(77f, v.y, 1e-3f);
  }

  private static final class WarpableDevice implements FlixelInputDevice {

    int warps;
    private int x;
    private int y;

    @Override
    public boolean supportsPointerWarp() {
      return true;
    }

    @Override
    public void warpPointer(int x, int y) {
      this.x = x;
      this.y = y;
      warps++;
    }

    @Override
    public int getX() {
      return x;
    }

    @Override
    public int getY() {
      return y;
    }
  }
}
