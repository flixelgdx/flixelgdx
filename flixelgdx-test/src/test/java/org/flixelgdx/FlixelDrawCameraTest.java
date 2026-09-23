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
package org.flixelgdx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link Flixel#isOnDrawCamera(FlixelCamera[])}, in particular how it falls back to
 * {@link FlixelCamera#defaultDrawTarget} when an object does not list any cameras of its own.
 *
 * <p>{@link Flixel#setDrawCamera(FlixelCamera)} is package-private, so this test lives in
 * {@code org.flixelgdx} to reach it directly instead of driving a full camera draw pass.
 */
class FlixelDrawCameraTest {

  @AfterEach
  void tearDown() {
    Flixel.setDrawCamera(null);
  }

  @Test
  void nullListDrawsOnDefaultDrawTargetCamera() {
    FlixelCamera camera = new FlixelCamera(200, 150);
    Flixel.setDrawCamera(camera);
    assertTrue(Flixel.isOnDrawCamera(null),
        "an object with no camera list should draw on a camera with defaultDrawTarget true");
  }

  @Test
  void emptyListDrawsOnDefaultDrawTargetCamera() {
    FlixelCamera camera = new FlixelCamera(200, 150);
    Flixel.setDrawCamera(camera);
    assertTrue(Flixel.isOnDrawCamera(new FlixelCamera[0]),
        "an empty camera list should behave the same as a null one");
  }

  @Test
  void nullListSkipsNonDefaultDrawTargetCamera() {
    FlixelCamera hud = new FlixelCamera(200, 150);
    hud.defaultDrawTarget = false;
    Flixel.setDrawCamera(hud);
    assertFalse(Flixel.isOnDrawCamera(null),
        "an object with no camera list should not draw on a HUD camera with defaultDrawTarget false");
  }

  @Test
  void emptyListSkipsNonDefaultDrawTargetCamera() {
    FlixelCamera hud = new FlixelCamera(200, 150);
    hud.defaultDrawTarget = false;
    Flixel.setDrawCamera(hud);
    assertFalse(Flixel.isOnDrawCamera(new FlixelCamera[0]),
        "an empty camera list should behave the same as a null one");
  }

  @Test
  void explicitListStillDrawsOnNonDefaultDrawTargetCamera() {
    FlixelCamera hud = new FlixelCamera(200, 150);
    hud.defaultDrawTarget = false;
    Flixel.setDrawCamera(hud);
    assertTrue(Flixel.isOnDrawCamera(new FlixelCamera[] { hud }),
        "an object that explicitly lists the HUD camera must still draw on it");
  }

  @Test
  void explicitListExcludesUnlistedCamera() {
    FlixelCamera world = new FlixelCamera(200, 150);
    FlixelCamera other = new FlixelCamera(200, 150);
    Flixel.setDrawCamera(world);
    assertFalse(Flixel.isOnDrawCamera(new FlixelCamera[] { other }),
        "an object assigned to a different camera must not draw on this one");
  }

  @Test
  void noActiveDrawCameraAlwaysDraws() {
    FlixelCamera hud = new FlixelCamera(200, 150);
    hud.defaultDrawTarget = false;
    assertTrue(Flixel.isOnDrawCamera(null),
        "outside of a camera pass, isOnDrawCamera must still return true");
    assertTrue(Flixel.isOnDrawCamera(new FlixelCamera[] { hud }));
  }
}
