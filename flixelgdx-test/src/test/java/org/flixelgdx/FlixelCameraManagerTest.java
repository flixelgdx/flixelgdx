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

import org.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.flixelgdx.testing.RecordingGraphics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlixelCameraManagerTest {

  private FlixelCamera.Manager manager;

  @BeforeEach
  void setUp() {
    Flixel.config = new FlixelConfig.Builder("Cameras").size(320, 180).build();
    Flixel.graphics = new RecordingGraphics();
    Flixel.cameras.clear();
    manager = new FlixelCamera.Manager();
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.clear();
  }

  @Test
  void creatingSetsUpOneDefaultCameraAndTheOverlayCamera() {
    manager.create();
    assertEquals(1, Flixel.cameras.getSize());
    assertEquals(320, Flixel.cameras.first().width);
    assertNotNull(manager.getOverlayCamera());
    assertEquals(-1, Flixel.cameras.indexOf(manager.getOverlayCamera(), true));
  }

  @Test
  void resetReplacesEveryCameraWithOneFreshCamera() {
    manager.create();
    FlixelCamera old = Flixel.cameras.first();
    Flixel.cameras.add(new FlixelCamera(10, 10));
    manager.reset();
    assertEquals(1, Flixel.cameras.getSize());
    assertNotSame(old, Flixel.cameras.first());
  }

  @Test
  void aStateSwitchResetsTheCameras() {
    manager.create();
    Flixel.cameras.add(new FlixelCamera(10, 10));
    manager.onStateSwitch(new StateSwitchSignalData(null));
    assertEquals(1, Flixel.cameras.getSize());
  }

  @Test
  void resumingPutsTheCamerasBackWhereTheyWereWhenPaused() {
    manager.create();
    FlixelCamera camera = Flixel.cameras.first();
    camera.scrollX = 40f;
    camera.scrollY = 25f;
    camera.setZoom(2f);
    manager.onPaused(true);
    camera.scrollX = 0f;
    camera.scrollY = 0f;
    camera.setZoom(1f);
    manager.onPaused(false);
    assertEquals(40f, camera.scrollX);
    assertEquals(25f, camera.scrollY);
    assertEquals(2f, camera.getZoom());
  }

  @Test
  void resumingAfterAResetDoesNotTouchTheNewCameras() {
    manager.create();
    manager.onPaused(true);
    manager.reset();
    FlixelCamera fresh = Flixel.cameras.first();
    fresh.scrollX = 12f;
    manager.onPaused(false);
    assertEquals(12f, fresh.scrollX);
  }

  @Test
  void updatingAndResizingReachEveryCamera() {
    manager.create();
    manager.postUpdate(0.016f);
    manager.resize(640, 360);
    assertEquals(1, Flixel.cameras.getSize());
  }

  @Test
  void destroyDestroysEveryCamera() {
    manager.create();
    manager.destroy();
    assertEquals(0, Flixel.cameras.getSize());
    assertNull(manager.getOverlayCamera());
  }

  @Test
  void theCamerasUpdateRightAfterTheRenderer() {
    assertEquals(FlixelManagers.Manager.ORDER_CAMERAS, manager.getOrder());
    assertEquals("flixel.cameras", manager.getName());
  }
}
