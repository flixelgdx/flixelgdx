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
package org.flixelgdx.input.touch;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.input.FlixelBaseInputDevice;
import org.flixelgdx.input.FlixelTouchListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelTouchManagerTest {

  private FlixelTouchManager touches;

  @BeforeEach
  void setUp() {
    touches = new FlixelTouchManager(4);
    touches.enabled = true;
  }

  @Test
  void aTouchGoesThroughPressDragAndRelease() {
    touches.touched(0, 10, 20);
    FlixelTouch t = touches.list[0];
    assertTrue(t.pressed());
    assertTrue(t.justPressed());
    assertTrue(touches.pressed(0));
    assertTrue(touches.justPressed(0));
    assertTrue(touches.anyPressed());
    assertTrue(touches.anyJustPressed());
    assertEquals(1, touches.count());

    touches.endFrame();
    assertFalse(touches.justPressed(0));
    assertFalse(touches.anyJustPressed());
    touches.touchDragged(0, 15, 25);
    assertTrue(t.dragging());
    assertEquals(15, t.screenX);

    touches.touchReleased(0, 16, 26);
    assertFalse(t.pressed());
    assertFalse(t.dragging());
    assertTrue(t.justReleased());
    assertTrue(touches.justReleased(0));
    assertTrue(touches.anyJustReleased());
    touches.endFrame();
    assertFalse(touches.anyJustReleased());
    assertFalse(touches.anyPressed());
  }

  @Test
  void aCancelledTouchIsNotAReleaseButStopsThePress() {
    touches.touched(1, 0, 0);
    touches.touchDragged(1, 1, 1);
    touches.touchCancelled(1, 1, 1);
    FlixelTouch t = touches.list[1];
    assertFalse(t.pressed());
    assertTrue(t.justCancelled());
    assertFalse(t.justReleased());
    assertFalse(t.dragging());
  }

  @Test
  void pointersPastTheLimitAreIgnored() {
    touches.touched(9, 1, 1);
    touches.touchDragged(9, 1, 1);
    touches.touchReleased(9, 1, 1);
    touches.touchCancelled(9, 1, 1);
    touches.injectTouched(-1, 0, 0);
    touches.injectTouchDragged(9, 0, 0);
    touches.injectTouchReleased(9, 0, 0);
    assertFalse(touches.pressed(9));
    assertFalse(touches.pressed(-1));
    assertFalse(touches.justPressed(9));
    assertFalse(touches.justReleased(-1));
    assertFalse(touches.isInjected(-1));
    assertFalse(touches.isInjected(9));
  }

  @Test
  void aDisabledManagerReportsNothing() {
    touches.touched(0, 5, 5);
    touches.enabled = false;
    touches.update();
    assertFalse(touches.pressed(0));
    assertFalse(touches.justPressed(0));
    assertFalse(touches.anyPressed());
    assertFalse(touches.anyJustPressed());
    assertFalse(touches.anyJustReleased());
    assertEquals(0, touches.count());
    assertFalse(touches.touchingScreen(100f, 100f, 10f, 10f));
    assertFalse(touches.touchingWorld(100f, 100f, 10f, 10f));
    assertFalse(touches.justTouchedScreen(100f, 100f, 10f, 10f));
    assertFalse(touches.justTouchedWorld(100f, 100f, 10f, 10f));
  }

  @Test
  void regionsMatchScreenAndWorldPositions() {
    touches.injectTouched(0, 50, 60);
    touches.touched(1, 500, 500);
    touches.update();
    FlixelTouch t = touches.list[0];
    assertEquals(50f, t.worldX, "without a camera world and screen are the same");
    assertTrue(touches.touchingScreen(40f, 50f, 20f, 20f));
    assertTrue(touches.touchingWorld(40f, 50f, 20f, 20f));
    assertTrue(touches.justTouchedScreen(40f, 50f, 20f, 20f));
    assertTrue(touches.justTouchedWorld(40f, 50f, 20f, 20f));
    assertFalse(touches.touchingScreen(100f, 100f, 10f, 10f));
    assertFalse(touches.touchingWorld(100f, 100f, 10f, 10f));
    assertFalse(touches.justTouchedScreen(100f, 100f, 10f, 10f));
    assertFalse(touches.justTouchedWorld(100f, 100f, 10f, 10f));
    assertFalse(touches.touchingScreen(55f, 0f, 10f, 10f));
    assertFalse(touches.touchingWorld(55f, 70f, 10f, 10f));
    assertEquals(0, touches.list[1].screenX, "a real touch reads its position from the input device");
  }

  @Test
  void aWorldCameraConvertsTouchPositions() {
    FlixelCamera cam = new FlixelCamera(100, 100);
    touches.setWorldCamera(cam);
    assertSame(cam, touches.getWorldCamera());
    touches.injectTouched(0, 10, 10);
    touches.update();
    FlixelTouch t = touches.list[0];
    assertEquals(t.getWorldX(cam), t.worldX, 1e-3f);
    assertEquals(t.getWorldY(cam), t.worldY, 1e-3f);
    touches.setWorldCamera(null);
    assertNull(touches.getWorldCamera());

    Flixel.cameras.add(cam);
    touches.update();
    assertEquals(t.getWorldX(cam), t.worldX, 1e-3f, "the first global camera is used by default");
  }

  @Test
  void injectedTouchesReachTheInputDevice() {
    AtomicInteger events = new AtomicInteger();
    FlixelBaseInputDevice device = new FlixelBaseInputDevice() {
    };
    device.addTouchListener(new FlixelTouchListener() {
      @Override
      public void touched(int pointer, int x, int y) {
        events.incrementAndGet();
      }

      @Override
      public void touchDragged(int pointer, int x, int y) {
        events.incrementAndGet();
      }

      @Override
      public void touchReleased(int pointer, int x, int y) {
        events.incrementAndGet();
      }
    });
    Flixel.input = device;
    touches.injectTouched(2, 1, 1);
    assertTrue(touches.isInjected(2));
    touches.injectTouchDragged(2, 2, 2);
    touches.injectTouchReleased(2, 3, 3);
    assertFalse(touches.isInjected(2));
    assertEquals(3, events.get());
  }

  @Test
  void maxPointersCanGrowAndShrink() {
    FlixelTouchManager plain = new FlixelTouchManager();
    assertEquals(FlixelTouchManager.DEFAULT_MAX_POINTERS, plain.getMaxPointers());
    touches.touched(1, 5, 5);
    touches.setMaxPointers(4);
    touches.setMaxPointers(6);
    assertEquals(6, touches.getMaxPointers());
    assertTrue(touches.pressed(1), "existing touches are kept");
    assertEquals(5, touches.list[5].pointer);
    touches.setMaxPointers(2);
    assertEquals(2, touches.list.length);
    assertThrows(IllegalArgumentException.class, () -> touches.setMaxPointers(0));
  }

  @Test
  void resetClearsEveryTouch() {
    touches.injectTouched(0, 5, 5);
    touches.update();
    touches.reset();
    FlixelTouch t = touches.list[0];
    assertFalse(t.pressed());
    assertEquals(0, t.screenX);
    assertEquals(0f, t.worldX);
    assertFalse(touches.isInjected(0));
  }
}
